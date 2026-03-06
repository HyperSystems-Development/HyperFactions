package com.hyperfactions.integration.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Facade that detects which mixin system(s) are available (HyperProtect, OrbisGuard, or both)
 * and provides a unified API for registering and querying hooks.
 *
 * <p>Detection priority:
 * <ol>
 *   <li>BOTH: HyperProtect-Mixin AND OrbisGuard-Mixins installed (HP runs in compatible mode)</li>
 *   <li>HYPERPROTECT: only HyperProtect-Mixin installed (standalone mode)</li>
 *   <li>ORBISGUARD: only OrbisGuard-Mixins installed</li>
 *   <li>NONE: graceful degradation</li>
 * </ol>
 *
 * <p>When BOTH systems are installed, HyperProtect's {@code HyperProtectConfigPlugin} disables
 * its 17 conflicting mixins. OrbisGuard handles those features, while HP's 5 unique mixins
 * remain active. HyperFactions registers hooks with both systems accordingly.
 *
 * <p>Note: Hyxin does NOT call the early plugin's setup() method, so system properties
 * like {@code hyperprotect.bridge.active} are NOT set at startup. Detection falls back
 * to checking for the JAR file in the earlyplugins/ directory.
 */
public final class ProtectionMixinBridge {

  /** MixinProvider enum. */
  public enum MixinProvider { NONE, ORBISGUARD, HYPERPROTECT, BOTH }

  private static volatile MixinProvider activeProvider = MixinProvider.NONE;

  private static volatile boolean initialized = false;

  private ProtectionMixinBridge() {}

  /**
   * Detects which mixin system(s) are available.
   * Called during plugin startup.
   */
  public static void init() {
    if (initialized) {
      return;
    }

    boolean hpDetected = detectHyperProtect();

    OrbisMixinsIntegration.init();
    boolean ogDetected = OrbisMixinsIntegration.isMixinsAvailable()
        || isJarInstalled("OrbisGuard-Mixins");

    if (hpDetected && ogDetected) {
      activeProvider = MixinProvider.BOTH;
      Logger.debug("Mixin provider: BOTH detected (HP compatible mode + OG)");
    } else if (hpDetected) {
      activeProvider = MixinProvider.HYPERPROTECT;
      Logger.debug("Mixin provider: HyperProtect-Mixin detected (standalone)");
    } else if (ogDetected) {
      activeProvider = MixinProvider.ORBISGUARD;
      Logger.debug("Mixin provider: OrbisGuard-Mixins detected");
    } else {
      activeProvider = MixinProvider.NONE;
      Logger.debug("Mixin provider: none detected");
    }

    initialized = true;
  }

  /**
   * Multi-signal detection for HyperProtect-Mixin.
   *
   * <p>Detection priority:
   * <ol>
   *   <li>PresenceMarker method (most reliable — method exists = HP mixins applied)</li>
   *   <li>System property (set in onLoad, available early)</li>
   *   <li>Bridge array existence</li>
   *   <li>JAR scan (existing fallback)</li>
   * </ol>
   */
  private static boolean detectHyperProtect() {
    // Signal 1: Mixin-injected PresenceMarker method (most reliable)
    try {
      Class<?> pluginManager = Class.forName("com.hypixel.hytale.server.core.plugin.PluginManager");
      Method m = pluginManager.getDeclaredMethod("isHyperProtectLoaded");
      Object result = m.invoke(null);
      if (Boolean.TRUE.equals(result)) {
        Logger.debugMixin("HP detected via PresenceMarker method");
        return true;
      }
    } catch (ClassNotFoundException | NoSuchMethodException e) {
      // Old HP or not installed — fall through
    } catch (Exception e) {
      Logger.debugMixin("HP PresenceMarker check failed: %s", e.getMessage());
    }

    // Signal 2: System properties (set in onLoad)
    if ("true".equalsIgnoreCase(System.getProperty("hyperprotect.mixins.active"))) {
      Logger.debugMixin("HP detected via hyperprotect.mixins.active property");
      return true;
    }
    if ("true".equalsIgnoreCase(System.getProperty("hyperprotect.bridge.active"))) {
      Logger.debugMixin("HP detected via hyperprotect.bridge.active property");
      return true;
    }

    // Signal 3: Intercept properties (set when mixin target classes load)
    if ("true".equalsIgnoreCase(System.getProperty("hyperprotect.intercept.block_break"))
        || "true".equalsIgnoreCase(System.getProperty("hyperprotect.intercept.block_place"))) {
      Logger.debugMixin("HP detected via intercept properties");
      return true;
    }

    // Signal 4: Bridge array exists
    if (System.getProperties().get("hyperprotect.bridge") != null) {
      Logger.debugMixin("HP detected via bridge array");
      return true;
    }

    // Signal 5: JAR scan fallback
    if (isJarInstalled("HyperProtect-Mixin")) {
      Logger.debugMixin("HP detected via JAR scan");
      return true;
    }

    return false;
  }

  /**
   * Checks if a JAR with the given prefix exists in the earlyplugins/ directory.
   */
  private static boolean isJarInstalled(String jarPrefix) {
    try {
      Path serverDir = Path.of(System.getProperty("user.dir"));
      Path epDir = serverDir.resolve("earlyplugins");
      if (Files.isDirectory(epDir)) {
        try (var stream = Files.list(epDir)) {
          return stream.anyMatch(p ->
              p.getFileName().toString().startsWith(jarPrefix)
              && p.getFileName().toString().endsWith(".jar"));
        }
      }
    } catch (Exception e) {
      Logger.debugMixin("Failed to check earlyplugins/ for %s: %s", jarPrefix, e.getMessage());
    }
    return false;
  }

  /**
   * Returns the currently active mixin provider.
   */
  @NotNull
  public static MixinProvider getProvider() {
    if (!initialized) {
      init();
    }
    return activeProvider;
  }

  /**
   * Whether any mixin system is available.
   */
  public static boolean isMixinsAvailable() {
    if (!initialized) {
      init();
    }
    return activeProvider != MixinProvider.NONE;
  }

  /**
   * Checks if a specific mixin feature is available.
   *
   * @param featureName the feature name (e.g. "block_break", "explosion", "hammer")
   * @return true if the feature's interceptor is loaded
   */
  public static boolean isMixinFeatureAvailable(@NotNull String featureName) {
    if (!initialized) {
      init();
    }

    return switch (activeProvider) {
      case HYPERPROTECT -> isHyperProtectFeatureAvailable(featureName);
      case ORBISGUARD -> isOrbisGuardFeatureAvailable(featureName);
      case BOTH -> isHyperProtectFeatureAvailable(featureName)
           || isOrbisGuardFeatureAvailable(featureName);
      case NONE -> false;
    };
  }

  private static boolean isHyperProtectFeatureAvailable(String featureName) {
    // In BOTH/compatible mode, HP only has its unique features active.
    // Check the mode system property to determine which features HP provides.
    boolean compatMode = "compatible".equals(System.getProperty("hyperprotect.mode"));

    if (compatMode) {
      // Only features from HP's unique mixins (not covered by OG)
      return switch (featureName) {
        case "container_open", "block_place", "entity_damage",
          "teleporter", "portal", "hammer", "use", "seat",
          "respawn", "interaction_log",
          // v1.2.0 unique features (OG has no equivalents)
          "crafting_resource", "map_marker_filter", "fluid_spread",
          "prefab_spawn", "projectile_launch", "mount", "barter_trade" -> true;
        default -> false;
      };
    }

    // Standalone mode: all HP features available
    return switch (featureName) {
      case "block_break", "explosion", "fire_spread", "builder_tools",
        "item_pickup", "death_drop", "durability", "container_access",
        "mob_spawn", "teleporter", "portal", "command", "interaction_log",
        "entity_damage", "container_open", "block_place",
        "hammer", "use", "seat", "respawn",
        // v1.2.0 features
        "crafting_resource", "map_marker_filter", "fluid_spread",
        "prefab_spawn", "projectile_launch", "mount", "barter_trade" -> true;
      default -> false;
    };
  }

  private static boolean isOrbisGuardFeatureAvailable(String featureName) {
    // OG has limited feature detection — most hooks are always present if mixins are loaded
    if (!OrbisMixinsIntegration.isMixinsAvailable()) {
      return false;
    }

    return switch (featureName) {
      case "block_break" -> true; // harvest mixin
      case "item_pickup" -> OrbisMixinsIntegration.isPickupMixinLoaded();
      case "death_drop" -> OrbisMixinsIntegration.isDeathMixinLoaded();
      case "durability" -> OrbisMixinsIntegration.isDurabilityMixinLoaded();
      case "mob_spawn" -> true; // spawn mixin
      case "explosion" -> true;
      case "command" -> true;
      case "hammer", "use", "seat", "block_place" -> true; // interaction hooks
      // OG doesn't have these hooks
      case "fire_spread", "builder_tools", "container_access",
        "teleporter", "portal", "entity_damage",
        "container_open", "interaction_log", "respawn" -> false;
      default -> false;
    };
  }

  /**
   * Gets a human-readable status summary for logging/GUI.
   */
  @NotNull
  public static String getStatusSummary() {
    if (!initialized) {
      init();
    }

    return switch (activeProvider) {
      case HYPERPROTECT -> "HYPERPROTECT (27 hooks active)";
      case ORBISGUARD -> "ORBISGUARD (" + OrbisMixinsIntegration.getStatusSummary() + ")";
      case BOTH -> "BOTH (OG: 11 hooks + HP: 12 unique mixins)";
      case NONE -> "NONE";
    };
  }

  /**
   * Registers all applicable hooks with the active mixin system(s).
   *
   * <p>For HyperProtect, registers ALL hooks unconditionally because Hyxin does NOT call
   * the early plugin's setup() — system properties aren't set until mixin target classes
   * load. The bridge is created lazily and hooks are populated at all slots so they're
   * ready when mixin interceptors eventually fire.
   *
   * <p>For BOTH mode, registers OG hooks for OG-handled features and HP hooks only
   * for HP's unique features (5 mixins).
   *
   * @param hf the HyperFactions instance (provides ProtectionChecker access)
   */
  public static void registerAllHooks(@NotNull HyperFactions hf) {
    if (!initialized) {
      init();
    }

    switch (activeProvider) {
      case HYPERPROTECT -> {
        HyperProtectIntegration.registerAllHooks(hf);
        Logger.debug("Registered 27 protection hooks via HyperProtect bridge (standalone)");
      }
      case ORBISGUARD -> {
        registerAllOrbisGuardHooks(hf);
        Logger.debug("Registered protection hooks via OrbisGuard registry");
      }
      case BOTH -> {
        // Register OG hooks for the 11 features OG handles
        registerAllOrbisGuardHooks(hf);
        // Register HP hooks ONLY for unique features (5 mixins)
        HyperProtectIntegration.registerUniqueHooks(hf);
        Logger.debug("Registered protection hooks via BOTH systems (OG: 11 + HP: unique)");
      }
      case NONE -> Logger.debug("No mixin provider — mixin-dependent protection disabled");
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /**
   * Unregisters all hooks from the active mixin system(s).
   */
  public static void unregisterAllHooks() {
    switch (activeProvider) {
      case HYPERPROTECT -> HyperProtectIntegration.unregisterAllHooks();
      case ORBISGUARD -> OrbisMixinsIntegration.unregisterAllHooks();
      case BOTH -> {
        OrbisMixinsIntegration.unregisterAllHooks();
        HyperProtectIntegration.unregisterUniqueHooks();
      }
      case NONE -> {}
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /**
   * Registers all applicable OrbisGuard hooks.
   * Wires all 11 OG hooks (previously only 3 were registered).
   */
  private static void registerAllOrbisGuardHooks(@NotNull HyperFactions hf) {
    // === Previously wired (3 hooks) ===

    OrbisMixinsIntegration.registerPickupHook(
        (playerUuid, worldName, x, y, z, mode) -> {
          boolean allowed = hf.getProtectionChecker().canPickupItem(playerUuid, worldName, x, y, z, mode);
          Logger.debugInteraction("[OG:Pickup] player=%s, world=%s, pos=(%.1f,%.1f,%.1f), mode=%s, allowed=%b",
            playerUuid, worldName, x, y, z, mode, allowed);
          return allowed;
        });

    OrbisMixinsIntegration.registerHarvestHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean allowed = hf.getProtectionChecker().canPickupItem(
              playerUuid, worldName, x, 0, z, "manual");
          Logger.debugInteraction("[OG:Harvest] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
            playerUuid, worldName, x, y, z, allowed);
          return allowed ? null : "You cannot pick up items manually here.";
        });

    OrbisMixinsIntegration.registerSpawnHook(
        (worldName, x, y, z) -> {
          boolean blocked = hf.getProtectionChecker().shouldBlockSpawn(worldName, x, y, z);
          Logger.debugSpawning("[OG:Spawn] world=%s, pos=(%d,%d,%d), blocked=%b",
            worldName, x, y, z, blocked);
          return blocked;
        });

    // === Newly wired OG hooks (8 hooks) ===

    OrbisMixinsIntegration.registerExplosionHook(
        (worldName, x, y, z) -> {
          boolean blocked = hf.getProtectionChecker().shouldBlockExplosion(worldName, x, y, z);
          Logger.debugInteraction("[OG:Explosion] world=%s, pos=(%d,%d,%d), blocked=%b",
            worldName, x, y, z, blocked);
          return blocked;
        });

    OrbisMixinsIntegration.registerCommandHook(
        (playerUuid, worldName, x, y, z, command) -> {
          var result = hf.getProtectionChecker().checkCommandBlock(playerUuid, worldName, x, y, z, command);
          Logger.debugInteraction("[OG:Command] player=%s, command=%s, blocked=%b",
            playerUuid, command, result.block());
          return result;
        });

    OrbisMixinsIntegration.registerDeathHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean keep = hf.getProtectionChecker().shouldKeepInventory(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Death] player=%s, world=%s, pos=(%d,%d,%d), keepInventory=%b",
            playerUuid, worldName, x, y, z, keep);
          return keep;
        });

    OrbisMixinsIntegration.registerDurabilityHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean prevent = hf.getProtectionChecker().shouldPreventDurability(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Durability] player=%s, world=%s, pos=(%d,%d,%d), preventWear=%b",
            playerUuid, worldName, x, y, z, prevent);
          return prevent;
        });

    OrbisMixinsIntegration.registerHammerHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean allowed = hf.getProtectionChecker().canBuildAt(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Hammer] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
            playerUuid, worldName, x, y, z, allowed);
          return allowed ? null : "You cannot use hammers here.";
        });

    OrbisMixinsIntegration.registerUseHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean allowed = hf.getProtectionChecker().canInteractAt(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Use] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
            playerUuid, worldName, x, y, z, allowed);
          return allowed;
        });

    OrbisMixinsIntegration.registerSeatHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean allowed = hf.getProtectionChecker().canSeatAt(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Seat] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
            playerUuid, worldName, x, y, z, allowed);
          return allowed;
        });

    OrbisMixinsIntegration.registerPlaceHook(
        (playerUuid, worldName, x, y, z) -> {
          boolean allowed = hf.getProtectionChecker().canPlaceAt(playerUuid, worldName, x, y, z);
          Logger.debugInteraction("[OG:Place] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
            playerUuid, worldName, x, y, z, allowed);
          return allowed;
        });
  }
}
