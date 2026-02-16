package com.hyperfactions.integration.protection;

import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Integration with OrbisGuard for region protection.
 *
 * OrbisGuard provides a region protection system similar to WorldGuard.
 * This integration allows HyperFactions to:
 * - Prevent players from claiming faction territory in OrbisGuard-protected regions
 * - Check if a location has OrbisGuard protection before allowing claims
 *
 * This is a soft dependency - if OrbisGuard is not installed, claims proceed normally.
 */
public final class OrbisGuardIntegration {

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static volatile String initError = null;

    // Cached reflection handles for performance
    private static volatile Object orbisGuardAPI = null;
    private static volatile MethodHandle getRegionContainerHandle = null;
    private static volatile MethodHandle getApplicableRegionsHandle = null;
    private static volatile MethodHandle hasProtectiveRegionsHandle = null;

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
            // Try to find the OrbisGuard API class
            Class<?> apiClass = Class.forName("com.orbisguard.api.OrbisGuardAPI");

            // Get the static getInstance() method
            Method getInstanceMethod = apiClass.getMethod("getInstance");
            orbisGuardAPI = getInstanceMethod.invoke(null);

            if (orbisGuardAPI == null) {
                initError = "OrbisGuardAPI.getInstance() returned null";
                available = false;
                Logger.debug("OrbisGuard API returned null instance");
                initialized = true;
                return;
            }

            // Get method handles for region checking
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();

            // getRegionContainer() method
            Method getRegionContainerMethod = orbisGuardAPI.getClass().getMethod("getRegionContainer");
            getRegionContainerHandle = lookup.unreflect(getRegionContainerMethod);

            available = true;
            Logger.info("OrbisGuard integration enabled - claim conflict detection active");

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
     *
     * @return true if OrbisGuard is installed and the API is accessible
     */
    public static boolean isAvailable() {
        if (!initialized) {
            init();
        }
        return available;
    }

    /**
     * Gets the initialization error message if any.
     *
     * @return error message, or null if no error
     */
    public static String getInitError() {
        return initError;
    }

    /**
     * Checks if a location has OrbisGuard protective regions.
     *
     * A protective region is one that has build/interact protection enabled,
     * which would conflict with faction claiming.
     *
     * @param worldName the world name
     * @param x         the X coordinate
     * @param y         the Y coordinate
     * @param z         the Z coordinate
     * @return true if the location is protected by OrbisGuard
     */
    public static boolean hasProtectiveRegions(@NotNull String worldName, int x, int y, int z) {
        if (!isAvailable()) {
            return false;
        }

        try {
            // Get the region container
            Object regionContainer = getRegionContainerHandle.invoke(orbisGuardAPI);
            if (regionContainer == null) {
                return false;
            }

            // Try to get applicable regions at the location
            // OrbisGuard API: regionContainer.getApplicableRegions(worldName, x, y, z)
            Method getApplicableRegions = regionContainer.getClass()
                    .getMethod("getApplicableRegions", String.class, int.class, int.class, int.class);
            Object regionSet = getApplicableRegions.invoke(regionContainer, worldName, x, y, z);

            if (regionSet == null) {
                return false;
            }

            // Check if any regions are present
            // The region set should have a size() or isEmpty() method
            try {
                Method sizeMethod = regionSet.getClass().getMethod("size");
                Object sizeResult = sizeMethod.invoke(regionSet);
                if (sizeResult instanceof Number) {
                    int size = ((Number) sizeResult).intValue();
                    if (size > 0) {
                        Logger.debug("OrbisGuard: Found %d region(s) at %s/%d/%d/%d",
                                size, worldName, x, y, z);
                        return true;
                    }
                }
            } catch (NoSuchMethodException e) {
                // Try isEmpty() instead
                try {
                    Method isEmptyMethod = regionSet.getClass().getMethod("isEmpty");
                    Object isEmptyResult = isEmptyMethod.invoke(regionSet);
                    if (isEmptyResult instanceof Boolean) {
                        boolean isEmpty = (Boolean) isEmptyResult;
                        if (!isEmpty) {
                            Logger.debug("OrbisGuard: Regions found at %s/%d/%d/%d",
                                    worldName, x, y, z);
                            return true;
                        }
                    }
                } catch (NoSuchMethodException e2) {
                    // Can't determine region presence, assume none
                    Logger.debug("OrbisGuard: Cannot determine region presence (no size/isEmpty method)");
                }
            }

            return false;

        } catch (Throwable e) {
            Logger.warn("Error checking OrbisGuard regions at %s/%d/%d/%d: %s",
                    worldName, x, y, z, e.getMessage());
            // Fail-open - allow claiming if we can't check
            return false;
        }
    }

    /**
     * Checks if a chunk has any OrbisGuard protection.
     *
     * Checks the center of the chunk to determine if OrbisGuard regions
     * are present. For more thorough checking, all four corners could be checked,
     * but center-only is usually sufficient and more performant.
     *
     * @param worldName the world name
     * @param chunkX    the chunk X coordinate
     * @param chunkZ    the chunk Z coordinate
     * @return true if the chunk has OrbisGuard protection
     */
    public static boolean isChunkProtected(@NotNull String worldName, int chunkX, int chunkZ) {
        if (!isAvailable()) {
            return false;
        }

        // Check center of chunk (block coordinates)
        int x = (chunkX << 4) + 8;
        int z = (chunkZ << 4) + 8;
        int y = 64; // Check at sea level, regions typically span all Y levels

        boolean hasRegions = hasProtectiveRegions(worldName, x, y, z);

        if (hasRegions) {
            Logger.debug("OrbisGuard: Chunk (%d, %d) in %s is protected", chunkX, chunkZ, worldName);
        }

        return hasRegions;
    }

    /**
     * Gets a human-readable status message for the integration.
     *
     * @return status message
     */
    @NotNull
    public static String getStatusMessage() {
        if (!initialized) {
            return "Not initialized";
        }
        if (available) {
            return "Enabled - claim conflict detection active";
        }
        if (initError != null) {
            return "Disabled - " + initError;
        }
        return "Disabled";
    }
}
