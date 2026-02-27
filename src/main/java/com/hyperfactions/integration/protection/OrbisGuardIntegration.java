package com.hyperfactions.integration.protection;

import com.hyperfactions.util.Logger;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Integration with OrbisGuard for region protection.
 *
 * <p>OrbisGuard provides a region protection system similar to WorldGuard.
 * This integration allows HyperFactions to:
 * - Prevent players from claiming faction territory in OrbisGuard-protected regions
 * - Check if a location has OrbisGuard protection before allowing claims
 * - Provide OrbisGuard region data for the world map
 *
 * <p>API methods used (from IRegionContainer):
 * - getRegionsAt(String, int, int, int) → Set{@code <IRegion>}
 * - canCreateClaim(String, BlockVector3, BlockVector3) → boolean
 * - getLoadedManagers() → Collection{@code <IRegionManager>}
 *
 * <p>This is a soft dependency - if OrbisGuard is not installed, everything is a no-op.
 */
public final class OrbisGuardIntegration {

  private static volatile boolean initialized = false;

  private static volatile boolean available = false;

  private static volatile String initError = null;

  // Cached API objects and method handles
  private static volatile Object regionContainer = null;

  private static volatile MethodHandle getRegionsAtHandle = null;       // (String, int, int, int) → Set
  private static volatile MethodHandle canCreateClaimHandle = null;     // (String, BlockVector3, BlockVector3) → boolean
  private static volatile MethodHandle getLoadedManagersHandle = null;  // () → Collection
  private static volatile MethodHandle blockVector3AtHandle = null;     // static (int, int, int) → BlockVector3

  private OrbisGuardIntegration() {}

  /**
   * Initializes the OrbisGuard integration.
   * Should be called during plugin startup.
   */
  public static void init() {
    if (initialized) {
      return;
    }

    try {
      // Find OrbisGuard API
      Class<?> apiClass = Class.forName("com.orbisguard.api.OrbisGuardAPI");
      Method getInstanceMethod = apiClass.getMethod("getInstance");
      Object api = getInstanceMethod.invoke(null);

      if (api == null) {
        initError = "OrbisGuardAPI.getInstance() returned null";
        available = false;
        Logger.debug("OrbisGuard API returned null instance");
        initialized = true;
        return;
      }

      // Get the RegionContainer
      Method getRegionContainerMethod = api.getClass().getMethod("getRegionContainer");
      regionContainer = getRegionContainerMethod.invoke(api);

      if (regionContainer == null) {
        initError = "getRegionContainer() returned null";
        available = false;
        initialized = true;
        return;
      }

      MethodHandles.Lookup lookup = MethodHandles.publicLookup();

      // Resolve getRegionsAt(String, int, int, int) → Set
      Method getRegionsAt = regionContainer.getClass()
          .getMethod("getRegionsAt", String.class, int.class, int.class, int.class);
      getRegionsAtHandle = lookup.unreflect(getRegionsAt).bindTo(regionContainer);

      // Resolve getLoadedManagers() → Collection
      Method getLoadedManagers = regionContainer.getClass().getMethod("getLoadedManagers");
      getLoadedManagersHandle = lookup.unreflect(getLoadedManagers).bindTo(regionContainer);

      // Resolve BlockVector3.at(int, int, int) for chunk-level claim checks
      try {
        Class<?> bv3Class = Class.forName("com.orbisguard.api.BlockVector3");
        Method atMethod = bv3Class.getMethod("at", int.class, int.class, int.class);
        blockVector3AtHandle = lookup.unreflect(atMethod);

        // Resolve canCreateClaim(String, BlockVector3, BlockVector3) → boolean
        Method canCreateClaim = regionContainer.getClass()
            .getMethod("canCreateClaim", String.class, bv3Class, bv3Class);
        canCreateClaimHandle = lookup.unreflect(canCreateClaim).bindTo(regionContainer);

        Logger.debug("OrbisGuard: canCreateClaim API resolved for chunk-level checks");
      } catch (ClassNotFoundException | NoSuchMethodException e) {
        // BlockVector3 or canCreateClaim not available — fall back to point checks
        Logger.debug("OrbisGuard: canCreateClaim not available, using point-based checks");
      }

      available = true;
      Logger.info("[Integration] OrbisGuard API enabled - claim conflict detection active");

    } catch (ClassNotFoundException e) {
      available = false;
      initError = "OrbisGuard not found";
      Logger.debug("OrbisGuard not installed (optional)");
    } catch (NoSuchMethodException e) {
      available = false;
      initError = "OrbisGuard API mismatch: " + e.getMessage();
      Logger.warn("OrbisGuard API version incompatible: %s", e.getMessage());
    } catch (Exception e) {
      available = false;
      initError = e.getClass().getSimpleName() + ": " + e.getMessage();
      Logger.warn("Error initializing OrbisGuard integration: %s", e.getMessage());
    }

    initialized = true;
  }

  /**
   * Checks if OrbisGuard is available.
   */
  public static boolean isAvailable() {
    if (!initialized) {
      init();
    }
    return available;
  }

  /**
   * Gets the initialization error message if any.
   */
  @Nullable
  public static String getInitError() {
    return initError;
  }

  // ========== Region Checking ==========

  /**
   * Checks if a location has non-global OrbisGuard regions.
   *
   * @return true if the location is inside an OrbisGuard region (excluding __global__)
   */
  public static boolean hasProtectiveRegions(@NotNull String worldName, int x, int y, int z) {
    if (!isAvailable() || getRegionsAtHandle == null) {
      return false;
    }

    try {
      Set<?> regions = (Set<?>) getRegionsAtHandle.invoke(worldName, x, y, z);
      if (regions == null || regions.isEmpty()) {
        return false;
      }

      // Filter out GLOBAL regions — they cover the whole world and shouldn't block claims
      for (Object region : regions) {
        try {
          Method getType = region.getClass().getMethod("getType");
          Object type = getType.invoke(region);
          if (type != null && !"GLOBAL".equals(type.toString())) {
            Logger.debug("OrbisGuard: Non-global region found at %s/%d/%d/%d", worldName, x, y, z);
            return true;
          }
        } catch (NoSuchMethodException e) {
          // Can't check type, treat as protective
          return true;
        }
      }
      return false;

    } catch (Throwable e) {
      Logger.warn("Error checking OrbisGuard regions at %s/%d/%d/%d: %s",
          worldName, x, y, z, e.getMessage());
      return false; // Fail-open
    }
  }

  /**
   * Checks if a chunk has any OrbisGuard protection.
   *
   * <p>Uses OrbisGuard's canCreateClaim API if available (checks all 8 corners + center
   * + full overlap detection). Falls back to multi-point checking otherwise.
   *
   * @param worldName the world name
   * @param chunkX    the chunk X coordinate
   * @param chunkZ    the chunk Z coordinate
   * @return true if the chunk overlaps with an OrbisGuard region
   */
  public static boolean isChunkProtected(@NotNull String worldName, int chunkX, int chunkZ) {
    if (!isAvailable()) {
      return false;
    }

    // Hytale chunks are 32 blocks wide (shift by 5)
    int baseX = chunkX << 5;
    int baseZ = chunkZ << 5;

    // Prefer OG's canCreateClaim — it checks corners, center, AND overlap
    if (canCreateClaimHandle != null && blockVector3AtHandle != null) {
      try {
        Object min = blockVector3AtHandle.invoke(baseX, 0, baseZ);
        Object max = blockVector3AtHandle.invoke(baseX + 31, 255, baseZ + 31);
        boolean canClaim = (boolean) canCreateClaimHandle.invoke(worldName, min, max);
        if (!canClaim) {
          Logger.debugClaim("OrbisGuard: Chunk (%d, %d) in %s blocked by canCreateClaim",
              chunkX, chunkZ, worldName);
          return true;
        }
        return false;
      } catch (Throwable e) {
        Logger.debugMixin("OrbisGuard canCreateClaim error, falling back to point check: %s", e.getMessage());
        // Fall through to point-based check
      }
    }

    // Fallback: check 5 points (4 corners + center) at sea level
    int y = 64;
    boolean isProtected = hasProtectiveRegions(worldName, baseX, y, baseZ)
        || hasProtectiveRegions(worldName, baseX + 31, y, baseZ)
        || hasProtectiveRegions(worldName, baseX, y, baseZ + 31)
        || hasProtectiveRegions(worldName, baseX + 31, y, baseZ + 31)
        || hasProtectiveRegions(worldName, baseX + 16, y, baseZ + 16);

    if (isProtected) {
      Logger.debugClaim("OrbisGuard: Chunk (%d, %d) in %s is protected (point check)",
          chunkX, chunkZ, worldName);
    }

    return isProtected;
  }

  // ========== Region Data (for map rendering) ==========

  /**
   * Represents an OrbisGuard region's spatial data for map rendering.
   */
  public record RegionInfo(
      String id,
      String worldName,
      String type,
      int priority,
      int minX, int minY, int minZ,
      int maxX, int maxY, int maxZ
  ) {
    /** Returns true if this is a global region (covers entire world). */
    public boolean isGlobal() {
      return "GLOBAL".equals(type);
    }

    /** Returns the minimum chunk X coordinate this region overlaps. */
    public int minChunkX() {
      return minX >> 4;
    }

    /** Returns the maximum chunk X coordinate this region overlaps. */
    public int maxChunkX() {
      return maxX >> 4;
    }

    /** Returns the minimum chunk Z coordinate this region overlaps. */
    public int minChunkZ() {
      return minZ >> 4;
    }

    /** Returns the maximum chunk Z coordinate this region overlaps. */
    public int maxChunkZ() {
      return maxZ >> 4;
    }
  }

  /**
   * Gets all OrbisGuard regions for a specific world.
   * Excludes GLOBAL regions. Used for map rendering.
   *
   * @param worldName the world to query
   * @return list of region info, or empty list if OG unavailable
   */
  @NotNull
  public static List<RegionInfo> getRegionsForWorld(@NotNull String worldName) {
    if (!isAvailable() || getLoadedManagersHandle == null) {
      return Collections.emptyList();
    }

    try {
      Collection<?> managers = (Collection<?>) getLoadedManagersHandle.invoke();
      if (managers == null || managers.isEmpty()) {
        return Collections.emptyList();
      }

      for (Object manager : managers) {
        Method getWorldName = manager.getClass().getMethod("getWorldName");
        String mgrWorld = (String) getWorldName.invoke(manager);
        if (!worldName.equals(mgrWorld)) {
          continue;
        }

        Method getRegions = manager.getClass().getMethod("getRegions");
        Collection<?> regions = (Collection<?>) getRegions.invoke(manager);
        if (regions == null || regions.isEmpty()) {
          return Collections.emptyList();
        }

        List<RegionInfo> result = new ArrayList<>();
        for (Object region : regions) {
          RegionInfo info = extractRegionInfo(region, worldName);
          if (info != null && !info.isGlobal()) {
            result.add(info);
          }
        }
        return result;
      }

      return Collections.emptyList();

    } catch (Throwable e) {
      Logger.warn("Error getting OrbisGuard regions for world %s: %s", worldName, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Gets all OrbisGuard regions across all worlds.
   * Excludes GLOBAL regions. Used for map rendering.
   */
  @NotNull
  public static List<RegionInfo> getAllRegions() {
    if (!isAvailable() || getLoadedManagersHandle == null) {
      return Collections.emptyList();
    }

    try {
      Collection<?> managers = (Collection<?>) getLoadedManagersHandle.invoke();
      if (managers == null || managers.isEmpty()) {
        return Collections.emptyList();
      }

      List<RegionInfo> result = new ArrayList<>();
      for (Object manager : managers) {
        Method getWorldName = manager.getClass().getMethod("getWorldName");
        String worldName = (String) getWorldName.invoke(manager);

        Method getRegions = manager.getClass().getMethod("getRegions");
        Collection<?> regions = (Collection<?>) getRegions.invoke(manager);
        if (regions == null) {
          continue;
        }

        for (Object region : regions) {
          RegionInfo info = extractRegionInfo(region, worldName);
          if (info != null && !info.isGlobal()) {
            result.add(info);
          }
        }
      }
      return result;

    } catch (Throwable e) {
      Logger.warn("Error getting all OrbisGuard regions: %s", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Extracts region info from an IRegion object via reflection.
   */
  @Nullable
  private static RegionInfo extractRegionInfo(Object region, String worldName) {
    try {
      Method getId = region.getClass().getMethod("getId");
      Method getType = region.getClass().getMethod("getType");
      Method getMin = region.getClass().getMethod("getMinimumPoint");
      Method getMax = region.getClass().getMethod("getMaximumPoint");

      String id = (String) getId.invoke(region);
      Object type = getType.invoke(region);
      Object min = getMin.invoke(region);
      Object max = getMax.invoke(region);

      if (min == null || max == null) {
        return null;
      }

      int priority = 0;
      try {
        Method getPriority = region.getClass().getMethod("getPriority");
        priority = (int) getPriority.invoke(region);
      } catch (NoSuchMethodException ignored) {}

      // BlockVector3 has x(), y(), z() accessor methods (it's a record)
      Method xMethod = min.getClass().getMethod("x");
      Method yMethod = min.getClass().getMethod("y");
      Method zMethod = min.getClass().getMethod("z");

      return new RegionInfo(
          id, worldName,
          type != null ? type.toString() : "UNKNOWN",
          priority,
          (int) xMethod.invoke(min), (int) yMethod.invoke(min), (int) zMethod.invoke(min),
          (int) xMethod.invoke(max), (int) yMethod.invoke(max), (int) zMethod.invoke(max)
      );
    } catch (Exception e) {
      Logger.debugMixin("Error extracting OG region info: %s", e.getMessage());
      return null;
    }
  }

  // ========== Status ==========

  /**
   * Gets a human-readable status message for the integration.
   */
  @NotNull
  public static String getStatusMessage() {
    if (!initialized) {
      return "Not initialized";
    }
    if (available) {
      String detail = canCreateClaimHandle != null ? "full API" : "point-check only";
      return "Enabled (" + detail + ")";
    }
    if (initError != null) {
      return "Disabled - " + initError;
    }
    return "Disabled";
  }
}
