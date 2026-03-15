package com.hyperfactions.integration.protection;

import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Integration with OrbisGuard-Mixins for enhanced protection coverage.
 *
 * <p>OrbisGuard-Mixins uses System.getProperties() for cross-classloader communication,
 * allowing plugins to register hooks without a direct dependency.
 *
 * <p>When OrbisGuard-Mixins is installed (requires Hyxin mixin loader), it sets
 * flags in system properties and provides a hook registry. Plugins can register
 * callbacks to control various behaviors that are not accessible via normal events.
 *
 * <p>Available Hooks:
 * - Pickup: F-key and auto item pickup protection
 * - Hammer: Hammer block cycling protection
 * - Harvest: F-key crop harvesting protection
 * - Place: Bucket/fluid placement protection
 * - Use: Block interaction (campfire, lantern toggle) protection
 * - Seat: Seating on blocks protection
 * - Explosion: Explosion block damage protection
 * - Command: Command blocking in zones
 * - Death: Keep inventory on death
 * - Durability: Durability loss prevention
 * - Spawn: Mob spawning control
 *
 * <p>If OrbisGuard-Mixins is not available, this integration is gracefully disabled
 * and a warning is logged to inform admins.
 */
public final class OrbisMixinsIntegration {

  // System property keys for detection
  private static final String MIXINS_LOADED_KEY = "orbisguard.mixins.loaded";

  private static final String MIXIN_PICKUP_LOADED = "orbisguard.mixin.pickup.loaded";

  private static final String MIXIN_DEATH_LOADED = "orbisguard.mixin.death.loaded";

  private static final String MIXIN_DURABILITY_LOADED = "orbisguard.mixin.durability.loaded";

  private static final String MIXIN_SEATING_LOADED = "orbisguard.mixin.seating.loaded";

  // Hook registry key
  private static final String HOOK_REGISTRY_KEY = "orbisguard.hook.registry";

  // HyperFactions hook keys (using our prefix to avoid conflicts)
  private static final String HF_PICKUP_HOOK_KEY = "hyperfactions.pickup.hook";

  private static final String HF_HAMMER_HOOK_KEY = "hyperfactions.hammer.hook";

  private static final String HF_HARVEST_HOOK_KEY = "hyperfactions.harvest.hook";

  private static final String HF_PLACE_HOOK_KEY = "hyperfactions.place.hook";

  private static final String HF_USE_HOOK_KEY = "hyperfactions.use.hook";

  private static final String HF_SEAT_HOOK_KEY = "hyperfactions.seat.hook";

  private static final String HF_EXPLOSION_HOOK_KEY = "hyperfactions.explosion.hook";

  private static final String HF_COMMAND_HOOK_KEY = "hyperfactions.command.hook";

  private static final String HF_DEATH_HOOK_KEY = "hyperfactions.death.hook";

  private static final String HF_DURABILITY_HOOK_KEY = "hyperfactions.durability.hook";

  private static final String HF_SPAWN_HOOK_KEY = "hyperfactions.spawn.hook";

  // OrbisGuard-Mixins hook registry keys (where mixins look for hooks)
  private static final String OG_PICKUP_HOOK = "orbisguard.pickup.hook";

  private static final String OG_HAMMER_HOOK = "orbisguard.hammer.hook";

  private static final String OG_HARVEST_HOOK = "orbisguard.harvest.hook";

  private static final String OG_PLACE_HOOK = "orbisguard.place.hook";

  private static final String OG_USE_HOOK = "orbisguard.use.hook";

  private static final String OG_SEAT_HOOK = "orbisguard.seat.hook";

  private static final String OG_EXPLOSION_HOOK = "orbisguard.explosion.hook";

  private static final String OG_COMMAND_HOOK = "orbisguard.command.hook";

  private static final String OG_DEATH_HOOK = "orbisguard.death.hook";

  private static final String OG_DURABILITY_HOOK = "orbisguard.durability.hook";

  private static final String OG_SPAWN_HOOK = "orbisguard.spawn.hook";

  private static volatile boolean initialized = false;

  private static volatile boolean mixinsAvailable = false;

  private static volatile String initError = null;

  // Track which mixin features are loaded
  private static volatile boolean pickupMixinLoaded = false;

  private static volatile boolean deathMixinLoaded = false;

  private static volatile boolean durabilityMixinLoaded = false;

  private static volatile boolean seatingMixinLoaded = false;

  private OrbisMixinsIntegration() {}

  /**
   * Initializes the OrbisGuard-Mixins integration.
   * Should be called during plugin startup.
   *
   * <p>Detection strategy: OrbisGuard-Mixins sets system properties in mixin class
   * static initializers. The general "orbisguard.mixins.loaded" flag is only
   * set by the pickup mixin (which targets a class that may not be loaded yet).
   * However, the durability mixin targets Player.class which loads early,
   * so we use "orbisguard.mixin.durability.loaded" as a fallback indicator.
   */
  public static void init() {
    if (initialized) {
      return;
    }

    try {
      // Check which specific mixins are loaded (these are set in mixin static initializers)
      // The durability mixin targets Player.class which loads early, so it's a reliable indicator
      durabilityMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_DURABILITY_LOADED));
      pickupMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_PICKUP_LOADED));
      deathMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_DEATH_LOADED));
      seatingMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_SEATING_LOADED));

      // Check if OrbisGuard-Mixins is loaded via system property
      // The general flag is only set by the pickup mixin, which may not be loaded yet
      // So we also accept any individual mixin property as evidence of OrbisGuard-Mixins
      String loadedFlag = System.getProperty(MIXINS_LOADED_KEY);
      boolean generalFlagSet = "true".equalsIgnoreCase(loadedFlag);
      boolean anyMixinLoaded = durabilityMixinLoaded || pickupMixinLoaded || deathMixinLoaded || seatingMixinLoaded;

      mixinsAvailable = generalFlagSet || anyMixinLoaded;

      if (mixinsAvailable) {
        Logger.debug("OrbisGuard-Mixins detected - enhanced protection hooks available");
        Logger.debugMixin("Detection: generalFlag=%b, durability=%b, pickup=%b, death=%b, seating=%b",
          generalFlagSet, durabilityMixinLoaded, pickupMixinLoaded, deathMixinLoaded, seatingMixinLoaded);
      } else {
        initError = "OrbisGuard-Mixins not installed";
        Logger.debugMixin("OrbisGuard-Mixins not detected - no mixin properties set");
        Logger.debugMixin("Checked: %s, %s, %s, %s, %s",
          MIXINS_LOADED_KEY, MIXIN_DURABILITY_LOADED, MIXIN_PICKUP_LOADED, MIXIN_DEATH_LOADED, MIXIN_SEATING_LOADED);
      }

    } catch (Exception e) {
      mixinsAvailable = false;
      initError = e.getClass().getSimpleName() + ": " + e.getMessage();
      ErrorHandler.report("Error checking OrbisGuard-Mixins availability", e);
    }

    initialized = true;
  }

  /**
   * Checks if OrbisGuard-Mixins is available.
   *
   * @return true if OrbisGuard-Mixins is loaded and available
   */
  public static boolean isMixinsAvailable() {
    if (!initialized) {
      init();
    }
    return mixinsAvailable;
  }

  /**
   * Refreshes the mixin detection status.
   * Call this later in startup if early detection failed, as some mixin
   * classes might not be loaded until their target classes are used.
   */
  public static void refreshStatus() {
    // Re-check all mixin properties
    boolean oldDurability = durabilityMixinLoaded;
    boolean oldPickup = pickupMixinLoaded;
    boolean oldDeath = deathMixinLoaded;
    boolean oldSeating = seatingMixinLoaded;
    boolean oldMixins = mixinsAvailable;

    durabilityMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_DURABILITY_LOADED));
    pickupMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_PICKUP_LOADED));
    deathMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_DEATH_LOADED));
    seatingMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_SEATING_LOADED));

    String loadedFlag = System.getProperty(MIXINS_LOADED_KEY);
    boolean generalFlagSet = "true".equalsIgnoreCase(loadedFlag);
    boolean anyMixinLoaded = durabilityMixinLoaded || pickupMixinLoaded || deathMixinLoaded || seatingMixinLoaded;

    mixinsAvailable = generalFlagSet || anyMixinLoaded;

    // Log changes
    if (mixinsAvailable != oldMixins) {
      if (mixinsAvailable) {
        Logger.debug("OrbisGuard-Mixins now detected (late load)");
        initError = null;
      }
    }
    if (durabilityMixinLoaded != oldDurability || pickupMixinLoaded != oldPickup
      || deathMixinLoaded != oldDeath || seatingMixinLoaded != oldSeating) {
      Logger.debugMixin("Mixin status refreshed: durability=%b, pickup=%b, death=%b, seating=%b",
        durabilityMixinLoaded, pickupMixinLoaded, deathMixinLoaded, seatingMixinLoaded);
    }
  }

  /**
   * Gets the initialization error message if any.
   *
   * @return error message, or null if no error
   */
  @Nullable
  public static String getInitError() {
    return initError;
  }

  /**
   * Gets a status summary string for logging/display.
   *
   * @return human-readable status string
   */
  public static String getStatusSummary() {
    if (!initialized) {
      init();
    }
    if (!mixinsAvailable) {
      return "NOT DETECTED";
    }
    StringBuilder sb = new StringBuilder("DETECTED (");
    boolean first = true;
    if (durabilityMixinLoaded) {
      sb.append("durability");
      first = false;
    }
    if (pickupMixinLoaded) {
      sb.append(first ? "" : ", ").append("pickup");
      first = false;
    }
    if (deathMixinLoaded) {
      sb.append(first ? "" : ", ").append("death");
      first = false;
    }
    if (seatingMixinLoaded) {
      sb.append(first ? "" : ", ").append("seating");
      first = false;
    }
    if (first) {
      sb.append("general");
    }
    sb.append(")");
    return sb.toString();
  }

  // ========== Feature availability checks ==========

  /**
   * Checks if the pickup mixin is loaded.
   * Note: This mixin targets PlayerItemEntityPickupSystem which may not load until
   * a player attempts to pick up an item.
   */
  public static boolean isPickupMixinLoaded() {
    if (!pickupMixinLoaded) {
      // Re-check in case it loaded since init
      pickupMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_PICKUP_LOADED));
    }
    return pickupMixinLoaded;
  }

  /**
   * Checks if the death mixin is loaded.
   * Note: This mixin targets DeathSystems.DropPlayerDeathItems which may not load
   * until a player dies.
   */
  public static boolean isDeathMixinLoaded() {
    if (!deathMixinLoaded) {
      // Re-check in case it loaded since init
      deathMixinLoaded = "true".equalsIgnoreCase(System.getProperty(MIXIN_DEATH_LOADED));
    }
    return deathMixinLoaded;
  }

  /**
   * Checks if the durability mixin is loaded.
   * This mixin targets Player.class which loads early, making it a reliable
   * indicator of OrbisGuard-Mixins availability.
   */
  public static boolean isDurabilityMixinLoaded() {
    return durabilityMixinLoaded;
  }

  /**
   * Checks if the seating mixin is loaded.
   */
  public static boolean isSeatingMixinLoaded() {
    return seatingMixinLoaded;
  }

  // ========== Hook Registry Access ==========

  @SuppressWarnings("unchecked")
  private static Map<String, Object> getOrCreateRegistry() {
    Object registry = System.getProperties().get(HOOK_REGISTRY_KEY);
    if (registry instanceof Map) {
      return (Map<String, Object>) registry;
    }

    synchronized (OrbisMixinsIntegration.class) {
      registry = System.getProperties().get(HOOK_REGISTRY_KEY);
      if (registry instanceof Map) {
        return (Map<String, Object>) registry;
      }

      Map<String, Object> newRegistry = new ConcurrentHashMap<>();
      System.getProperties().put(HOOK_REGISTRY_KEY, newRegistry);
      return newRegistry;
    }
  }

  private static void registerHookInRegistry(String key, Object hook) {
    Map<String, Object> registry = getOrCreateRegistry();
    registry.put(key, hook);
    Logger.debugMixin("Registered hook: %s", key);
  }

  private static void unregisterHookFromRegistry(String key) {
    Object registry = System.getProperties().get(HOOK_REGISTRY_KEY);
    if (registry instanceof Map) {
      ((Map<?, ?>) registry).remove(key);
    }
  }

  // ========== Hook Chaining Support ==========
  // When OrbisGuard registers hooks before us, we capture the originals and chain calls.
  // This ensures OG's region-based checks still run alongside HF's faction-based checks.

  /** Stores original hooks so we can chain calls and restore on unregister. */
  private static final Map<String, Object> originalHooks = new ConcurrentHashMap<>();

  /**
   * Gets the existing hook at the given registry key before we overwrite it.
   */
  @Nullable
  private static Object getExistingHook(String key) {
    Object registry = System.getProperties().get(HOOK_REGISTRY_KEY);
    if (registry instanceof Map) {
      return ((Map<?, ?>) registry).get(key);
    }
    return null;
  }

  /**
   * Captures and stores the existing hook before overwriting, then returns it.
   * Used in register methods to enable chaining.
   *
   * @return the existing hook, or null if none registered
   */
  @Nullable
  private static Object captureExisting(String key) {
    Object existing = getExistingHook(key);
    if (existing != null) {
      originalHooks.putIfAbsent(key, existing);
      Logger.debugMixin("Captured existing hook for chaining: %s (%s)",
        key, existing.getClass().getSimpleName());
    }
    return existing;
  }

  /**
   * Resolves a MethodHandle for calling a method on the original hook object.
   * Returns null if the method doesn't exist (e.g., hook is from a different plugin).
   */
  @Nullable
  private static MethodHandle resolveMethod(@NotNull Object hook, String methodName, MethodType methodType) {
    try {
      return MethodHandles.publicLookup()
        .findVirtual(hook.getClass(), methodName, methodType)
        .bindTo(hook);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      Logger.debugMixin("Could not resolve chained method %s on %s: %s",
        methodName, hook.getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  /**
   * Restores the original hook at the given key, or removes the key if no original.
   * Called during unregistration to leave things as we found them.
   */
  private static void restoreOrRemoveHook(String key) {
    Object original = originalHooks.remove(key);
    if (original != null) {
      Map<String, Object> registry = getOrCreateRegistry();
      registry.put(key, original);
      Logger.debugMixin("Restored original hook: %s", key);
    } else {
      unregisterHookFromRegistry(key);
    }
  }

  // ========== Pickup Protection Hook ==========

  /**
   * Registers a pickup protection hook with OrbisGuard-Mixins.
   *
   * <p>The callback will be invoked whenever OrbisGuard-Mixins processes an
   * F-key or auto pickup event. If the callback returns false, the pickup
   * is blocked.
   *
   * <p>Note: Hooks are registered unconditionally. The mixin will find and use
   * the hook if it's loaded. Detection is only for logging purposes.
   *
   * @param callback the callback to check if pickup is allowed
   * @return true if hook was registered successfully
   */
  public static boolean registerPickupHook(@NotNull PickupCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_PICKUP_HOOK);
      PickupHookWrapper wrapper = new PickupHookWrapper(callback, existing);
      registerHookInRegistry(OG_PICKUP_HOOK, wrapper);
      Logger.debug("Registered pickup protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register pickup hook", e);
      return false;
    }
  }

  /** Unregister Pickup Hook. */
  public static void unregisterPickupHook() {
    restoreOrRemoveHook(OG_PICKUP_HOOK);
    Logger.debugMixin("Unregistered pickup hook");
  }

  /** PickupCheckCallback interface. */
  @FunctionalInterface
  public interface PickupCheckCallback {
    boolean isPickupAllowed(@NotNull UUID playerUuid, @NotNull String worldName,
                double x, double y, double z, @NotNull String mode);
  }

  /** PickupHookWrapper class. */
  public static final class PickupHookWrapper {
    private final PickupCheckCallback callback;

    private final @Nullable MethodHandle originalCheck;

    /** Creates a new PickupHookWrapper. */
    public PickupHookWrapper(@NotNull PickupCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalCheck = originalHook != null
        ? resolveMethod(originalHook, "check",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            double.class, double.class, double.class, String.class))
        : null;
    }

    /**
     * Called by OrbisGuard-Mixins via reflection.
     * Chains with original: both must allow for pickup to proceed.
     */
    public boolean check(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
      try {
        if (originalCheck != null) {
          try {
            boolean originalAllowed = (boolean) originalCheck.invoke(playerUuid, worldName, x, y, z, mode);
            if (!originalAllowed) {
              Logger.debugProtection("[Mixin:Pickup] Blocked by chained hook (OrbisGuard)");
              return false;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained pickup hook: %s", t.getMessage());
          }
        }

        boolean allowed = callback.isPickupAllowed(playerUuid, worldName, x, y, z, mode);
        Logger.debugProtection("[Mixin:Pickup] player=%s, world=%s, pos=(%.1f,%.1f,%.1f), mode=%s, allowed=%b",
          playerUuid, worldName, x, y, z, mode, allowed);
        return allowed;
      } catch (Exception e) {
        Logger.debugMixin("Error in pickup check: %s", e.getMessage());
        return true; // Fail-open
      }
    }
  }

  // ========== Hammer Protection Hook ==========

  /**
   * Registers a hammer protection hook for block cycling protection.
   */
  public static boolean registerHammerHook(@NotNull HammerCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_HAMMER_HOOK);
      HammerHookWrapper wrapper = new HammerHookWrapper(callback, existing);
      registerHookInRegistry(OG_HAMMER_HOOK, wrapper);
      Logger.debug("Registered hammer protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register hammer hook", e);
      return false;
    }
  }

  /** Unregister Hammer Hook. */
  public static void unregisterHammerHook() {
    restoreOrRemoveHook(OG_HAMMER_HOOK);
  }

  @FunctionalInterface
  public interface HammerCheckCallback {
    /**
     * Check if hammer cycling (block group change) is allowed.
     * @return null if allowed, denial message if blocked
     */
    @Nullable String checkHammer(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /**
   * Wrapper for hammer hook that matches OrbisGuard-Mixins expected signatures.
   * OrbisGuard's CycleBlockGroupInteraction codec replacement calls isHammerAllowed() -> boolean.
   * OrbisGuard-Mixins 0.8.3+ CycleBlockGroupInteractionMixin calls checkMessage() -> String.
   */
  public static final class HammerHookWrapper {
    private final HammerCheckCallback callback;

    private final @Nullable MethodHandle originalIsHammerAllowed;

    private final @Nullable MethodHandle originalCheckMessage;

    public HammerHookWrapper(@NotNull HammerCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      if (originalHook != null) {
        MethodType boolType = MethodType.methodType(boolean.class,
          UUID.class, String.class, int.class, int.class, int.class);
        MethodType strType = MethodType.methodType(String.class,
          UUID.class, String.class, int.class, int.class, int.class);
        this.originalIsHammerAllowed = resolveMethod(originalHook, "isHammerAllowed", boolType);
        this.originalCheckMessage = resolveMethod(originalHook, "checkMessage", strType);
      } else {
        this.originalIsHammerAllowed = null;
        this.originalCheckMessage = null;
      }
    }

    /**
     * Called by OrbisGuard's CycleBlockGroupInteraction codec replacement (boolean API).
     * Chains: both must allow.
     */
    public boolean isHammerAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalIsHammerAllowed != null) {
          try {
            boolean originalAllowed = (boolean) originalIsHammerAllowed.invoke(playerUuid, worldName, x, y, z);
            if (!originalAllowed) {
              Logger.debugProtection("[Mixin:Hammer] Blocked by chained hook (OrbisGuard)");
              return false;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained hammer hook: %s", t.getMessage());
          }
        }

        String result = callback.checkHammer(playerUuid, worldName, x, y, z);
        boolean allowed = result == null;
        Logger.debugProtection("[Mixin:Hammer] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return allowed;
      } catch (Exception e) {
        Logger.debugMixin("Error in hammer check: %s", e.getMessage());
        return true; // Fail-open
      }
    }

    /**
     * Called by CycleBlockGroupInteractionMixin (OG-Mixins 0.8.3+).
     * Chains: first non-null denial wins.
     * @return null if allowed, denial message if blocked
     */
    public String checkMessage(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalCheckMessage != null) {
          try {
            String originalResult = (String) originalCheckMessage.invoke(playerUuid, worldName, x, y, z);
            if (originalResult != null) {
              Logger.debugProtection("[Mixin:HammerMessage] Blocked by chained hook (OrbisGuard)");
              return originalResult;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained hammer checkMessage: %s", t.getMessage());
          }
        }

        String result = callback.checkHammer(playerUuid, worldName, x, y, z);
        boolean allowed = result == null;
        Logger.debugProtection("[Mixin:HammerMessage] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return result;
      } catch (Exception e) {
        Logger.debugMixin("Error in hammer checkMessage: %s", e.getMessage());
        return null; // Fail-open
      }
    }
  }

  // ========== Explosion Protection Hook ==========

  /**
   * Registers an explosion protection hook for explosion block damage.
   */
  public static boolean registerExplosionHook(@NotNull ExplosionCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_EXPLOSION_HOOK);
      ExplosionHookWrapper wrapper = new ExplosionHookWrapper(callback, existing);
      registerHookInRegistry(OG_EXPLOSION_HOOK, wrapper);
      Logger.debug("Registered explosion protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register explosion hook", e);
      return false;
    }
  }

  /** Unregister Explosion Hook. */
  public static void unregisterExplosionHook() {
    restoreOrRemoveHook(OG_EXPLOSION_HOOK);
  }

  /** ExplosionCheckCallback interface. */
  @FunctionalInterface
  public interface ExplosionCheckCallback {
    boolean shouldBlockExplosion(@NotNull String worldName, int x, int y, int z);
  }

  /** ExplosionHookWrapper class. */
  public static final class ExplosionHookWrapper {
    private final ExplosionCheckCallback callback;

    private final @Nullable MethodHandle originalShouldBlockExplosion;

    /** Creates a new ExplosionHookWrapper. */
    public ExplosionHookWrapper(@NotNull ExplosionCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      // OG's explosion hook takes (World, int, int, int) but World is cross-classloader,
      // so we use Object to avoid ClassCastException
      this.originalShouldBlockExplosion = originalHook != null
        ? resolveMethod(originalHook, "shouldBlockExplosion",
          MethodType.methodType(boolean.class, World.class, int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: if EITHER blocks, explosion is blocked.
     */
    public boolean shouldBlockExplosion(World world, int x, int y, int z) {
      try {
        if (originalShouldBlockExplosion != null) {
          try {
            boolean originalBlocked = (boolean) originalShouldBlockExplosion.invoke(world, x, y, z);
            if (originalBlocked) {
              Logger.debugProtection("[Mixin:Explosion] Blocked by chained hook (OrbisGuard)");
              return true;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained explosion hook: %s", t.getMessage());
          }
        }

        String worldName = world != null ? world.getName() : "";
        boolean blocked = callback.shouldBlockExplosion(worldName, x, y, z);
        Logger.debugProtection("[Mixin:Explosion] world=%s, pos=(%d,%d,%d), blocked=%b",
          worldName, x, y, z, blocked);
        return blocked;
      } catch (Exception e) {
        Logger.debugMixin("Error in explosion check: %s", e.getMessage());
        return false; // Fail-open
      }
    }
  }

  // ========== Command Protection Hook ==========

  /**
   * Registers a command protection hook for blocking commands in zones.
   */
  public static boolean registerCommandHook(@NotNull CommandCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_COMMAND_HOOK);
      CommandHookWrapper wrapper = new CommandHookWrapper(callback, existing);
      registerHookInRegistry(OG_COMMAND_HOOK, wrapper);
      Logger.debug("Registered command protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register command hook", e);
      return false;
    }
  }

  /** Unregister Command Hook. */
  public static void unregisterCommandHook() {
    restoreOrRemoveHook(OG_COMMAND_HOOK);
  }

  /** CommandCheckCallback interface. */
  @FunctionalInterface
  public interface CommandCheckCallback {
    /**
     * Checks if a command should be blocked.
     *
     * @param playerUuid the player's UUID
     * @param worldName the world name
     * @param x player X coordinate
     * @param y player Y coordinate
     * @param z player Z coordinate
     * @param command the command being executed
     * @return result containing whether to block and denial message
     */
    CommandCheckResult shouldBlockCommand(@NotNull UUID playerUuid, @NotNull String worldName,
                       int x, int y, int z, @NotNull String command);
  }

  /** CommandCheckResult record. */
  public record CommandCheckResult(boolean block, @Nullable String denialMessage) {
    /** Allow. */
    public static CommandCheckResult allow() {
      return new CommandCheckResult(false, null);
    }

    /** Deny. */
    public static CommandCheckResult deny(@Nullable String message) {
      return new CommandCheckResult(true, message);
    }
  }

  /** CommandHookWrapper class. */
  public static final class CommandHookWrapper {
    private final CommandCheckCallback callback;

    private volatile CommandCheckResult lastResult = CommandCheckResult.allow();

    private final @Nullable MethodHandle originalShouldBlockCommand;

    private final @Nullable MethodHandle originalGetDenialMessage;

    /** Creates a new CommandHookWrapper. */
    public CommandHookWrapper(@NotNull CommandCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      if (originalHook != null) {
        this.originalShouldBlockCommand = resolveMethod(originalHook, "shouldBlockCommand",
          MethodType.methodType(boolean.class, Player.class, String.class));
        this.originalGetDenialMessage = resolveMethod(originalHook, "getDenialMessage",
          MethodType.methodType(String.class));
      } else {
        this.originalShouldBlockCommand = null;
        this.originalGetDenialMessage = null;
      }
    }

    /**
     * Chains: if EITHER blocks, command is blocked.
     */
    public boolean shouldBlockCommand(Player player, String command) {
      try {
        if (player == null || command == null) {
          return false;
        }

        // Check original hook first
        if (originalShouldBlockCommand != null) {
          try {
            boolean originalBlocked = (boolean) originalShouldBlockCommand.invoke(player, command);
            if (originalBlocked) {
              // Capture original's denial message for getDenialMessage()
              String msg = null;
              if (originalGetDenialMessage != null) {
                try { msg = (String) originalGetDenialMessage.invoke(); } catch (Throwable ignored) {}
              }
              lastResult = CommandCheckResult.deny(msg != null ? msg : "You cannot use that command here.");
              return true;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained command hook: %s", t.getMessage());
          }
        }

        UUID uuid = player.getUuid();
        String worldName = "";
        int x = 0, y = 0, z = 0;

        lastResult = callback.shouldBlockCommand(uuid, worldName, x, y, z, command);
        return lastResult.block();
      } catch (Exception e) {
        Logger.debugMixin("Error in command check: %s", e.getMessage());
        return false; // Fail-open
      }
    }

    /** Returns the denial message. */
    public String getDenialMessage() {
      return lastResult.denialMessage() != null ? lastResult.denialMessage() : "You cannot use that command here.";
    }
  }

  // ========== Death (Keep Inventory) Hook ==========

  /**
   * Registers a death hook for keep-inventory protection.
   */
  public static boolean registerDeathHook(@NotNull DeathCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_DEATH_HOOK);
      DeathHookWrapper wrapper = new DeathHookWrapper(callback, existing);
      registerHookInRegistry(OG_DEATH_HOOK, wrapper);
      Logger.debug("Registered death (keep inventory) hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register death hook", e);
      return false;
    }
  }

  /** Unregister Death Hook. */
  public static void unregisterDeathHook() {
    restoreOrRemoveHook(OG_DEATH_HOOK);
  }

  /** DeathCheckCallback interface. */
  @FunctionalInterface
  public interface DeathCheckCallback {
    boolean shouldKeepInventory(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /** DeathHookWrapper class. */
  public static final class DeathHookWrapper {
    private final DeathCheckCallback callback;

    private final @Nullable MethodHandle originalShouldKeepInventory;

    /** Creates a new DeathHookWrapper. */
    public DeathHookWrapper(@NotNull DeathCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalShouldKeepInventory = originalHook != null
        ? resolveMethod(originalHook, "shouldKeepInventory",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: if EITHER says keep inventory, keep it.
     */
    public boolean shouldKeepInventory(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalShouldKeepInventory != null) {
          try {
            boolean originalKeep = (boolean) originalShouldKeepInventory.invoke(playerUuid, worldName, x, y, z);
            if (originalKeep) {
              Logger.debugCombat("[Mixin:Death] Keep inventory by chained hook (OrbisGuard)");
              return true;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained death hook: %s", t.getMessage());
          }
        }

        boolean keepInventory = callback.shouldKeepInventory(playerUuid, worldName, x, y, z);
        Logger.debugCombat("[Mixin:Death] player=%s, world=%s, pos=(%d,%d,%d), keepInventory=%b",
          playerUuid, worldName, x, y, z, keepInventory);
        return keepInventory;
      } catch (Exception e) {
        Logger.debugMixin("Error in death check: %s", e.getMessage());
        return false; // Fail-open
      }
    }
  }

  // ========== Durability Protection Hook ==========

  /**
   * Registers a durability hook to prevent tool/armor durability loss.
   */
  public static boolean registerDurabilityHook(@NotNull DurabilityCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_DURABILITY_HOOK);
      DurabilityHookWrapper wrapper = new DurabilityHookWrapper(callback, existing);
      registerHookInRegistry(OG_DURABILITY_HOOK, wrapper);
      Logger.debug("Registered durability protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register durability hook", e);
      return false;
    }
  }

  /** Unregister Durability Hook. */
  public static void unregisterDurabilityHook() {
    restoreOrRemoveHook(OG_DURABILITY_HOOK);
  }

  /** DurabilityCheckCallback interface. */
  @FunctionalInterface
  public interface DurabilityCheckCallback {
    boolean shouldPreventDurabilityLoss(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /** DurabilityHookWrapper class. */
  public static final class DurabilityHookWrapper {
    private final DurabilityCheckCallback callback;

    private final @Nullable MethodHandle originalShouldPreventDurabilityLoss;

    /** Creates a new DurabilityHookWrapper. */
    public DurabilityHookWrapper(@NotNull DurabilityCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalShouldPreventDurabilityLoss = originalHook != null
        ? resolveMethod(originalHook, "shouldPreventDurabilityLoss",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: if EITHER says prevent, durability loss is prevented.
     */
    public boolean shouldPreventDurabilityLoss(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalShouldPreventDurabilityLoss != null) {
          try {
            boolean originalPrevent = (boolean) originalShouldPreventDurabilityLoss.invoke(playerUuid, worldName, x, y, z);
            if (originalPrevent) {
              Logger.debugProtection("[Mixin:Durability] Prevented by chained hook (OrbisGuard)");
              return true;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained durability hook: %s", t.getMessage());
          }
        }

        boolean prevent = callback.shouldPreventDurabilityLoss(playerUuid, worldName, x, y, z);
        Logger.debugProtection("[Mixin:Durability] player=%s, world=%s, pos=(%d,%d,%d), preventLoss=%b",
          playerUuid, worldName, x, y, z, prevent);
        return prevent;
      } catch (Exception e) {
        Logger.debugMixin("Error in durability check: %s", e.getMessage());
        return false; // Fail-open
      }
    }
  }

  // ========== Use (Interaction) Protection Hook ==========

  /**
   * Registers a use hook for block interaction protection (campfire, lantern toggle, etc).
   */
  public static boolean registerUseHook(@NotNull UseCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_USE_HOOK);
      UseHookWrapper wrapper = new UseHookWrapper(callback, existing);
      registerHookInRegistry(OG_USE_HOOK, wrapper);
      Logger.debug("Registered use protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register use hook", e);
      return false;
    }
  }

  /** Unregister Use Hook. */
  public static void unregisterUseHook() {
    restoreOrRemoveHook(OG_USE_HOOK);
  }

  /** UseCheckCallback interface. */
  @FunctionalInterface
  public interface UseCheckCallback {
    boolean isUseAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /** UseHookWrapper class. */
  public static final class UseHookWrapper {
    private final UseCheckCallback callback;

    private final @Nullable MethodHandle originalIsUseAllowed;

    /** Creates a new UseHookWrapper. */
    public UseHookWrapper(@NotNull UseCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalIsUseAllowed = originalHook != null
        ? resolveMethod(originalHook, "isUseAllowed",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: both must allow for use to proceed.
     */
    public boolean isUseAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalIsUseAllowed != null) {
          try {
            boolean originalAllowed = (boolean) originalIsUseAllowed.invoke(playerUuid, worldName, x, y, z);
            if (!originalAllowed) {
              Logger.debugInteraction("[Mixin:Use] Blocked by chained hook (OrbisGuard)");
              return false;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained use hook: %s", t.getMessage());
          }
        }

        boolean allowed = callback.isUseAllowed(playerUuid, worldName, x, y, z);
        Logger.debugInteraction("[Mixin:Use] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return allowed;
      } catch (Exception e) {
        Logger.debugMixin("Error in use check: %s", e.getMessage());
        return true; // Fail-open
      }
    }
  }

  // ========== Seat Protection Hook ==========

  /**
   * Registers a seat hook for seating-on-blocks protection.
   */
  public static boolean registerSeatHook(@NotNull SeatCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_SEAT_HOOK);
      SeatHookWrapper wrapper = new SeatHookWrapper(callback, existing);
      registerHookInRegistry(OG_SEAT_HOOK, wrapper);
      Logger.debug("Registered seat protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register seat hook", e);
      return false;
    }
  }

  /** Unregister Seat Hook. */
  public static void unregisterSeatHook() {
    restoreOrRemoveHook(OG_SEAT_HOOK);
  }

  /** SeatCheckCallback interface. */
  @FunctionalInterface
  public interface SeatCheckCallback {
    boolean isSeatAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /** SeatHookWrapper class. */
  public static final class SeatHookWrapper {
    private final SeatCheckCallback callback;

    private final @Nullable MethodHandle originalIsSeatAllowed;

    /** Creates a new SeatHookWrapper. */
    public SeatHookWrapper(@NotNull SeatCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalIsSeatAllowed = originalHook != null
        ? resolveMethod(originalHook, "isSeatAllowed",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: both must allow for seating to proceed.
     */
    public boolean isSeatAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalIsSeatAllowed != null) {
          try {
            boolean originalAllowed = (boolean) originalIsSeatAllowed.invoke(playerUuid, worldName, x, y, z);
            if (!originalAllowed) {
              Logger.debugInteraction("[Mixin:Seat] Blocked by chained hook (OrbisGuard)");
              return false;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained seat hook: %s", t.getMessage());
          }
        }

        boolean allowed = callback.isSeatAllowed(playerUuid, worldName, x, y, z);
        Logger.debugInteraction("[Mixin:Seat] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return allowed;
      } catch (Exception e) {
        Logger.debugMixin("Error in seat check: %s", e.getMessage());
        return true; // Fail-open
      }
    }
  }

  // ========== Harvest Protection Hook ==========

  /**
   * Registers a harvest hook for F-key crop harvesting protection.
   */
  public static boolean registerHarvestHook(@NotNull HarvestCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_HARVEST_HOOK);
      HarvestHookWrapper wrapper = new HarvestHookWrapper(callback, existing);
      registerHookInRegistry(OG_HARVEST_HOOK, wrapper);
      Logger.debug("Registered harvest protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register harvest hook", e);
      return false;
    }
  }

  /** Unregister Harvest Hook. */
  public static void unregisterHarvestHook() {
    restoreOrRemoveHook(OG_HARVEST_HOOK);
  }

  /**
   * Callback for harvest/F-key pickup protection.
   * Returns null if allowed, or a denial message if blocked.
   */
  @FunctionalInterface
  public interface HarvestCheckCallback {
    /**
     * Check if F-key pickup is allowed.
     * @return null if allowed, denial message if blocked
     */
    @Nullable String checkPickup(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /**
   * Wrapper for harvest hook that matches OrbisGuard-Mixins expected signature.
   * BlockHarvestUtilsMixin looks for:
   * - check(UUID, String, int, int, int) -> String (block break, null=allowed)
   * - checkPickup(UUID, String, int, int, int) -> String (pickup, null=allowed)
   */
  public static final class HarvestHookWrapper {
    private final HarvestCheckCallback callback;

    private final @Nullable MethodHandle originalCheck;

    private final @Nullable MethodHandle originalCheckPickup;

    private final @Nullable MethodHandle originalCheckScytheHarvest;

    /** Creates a new HarvestHookWrapper. */
    public HarvestHookWrapper(@NotNull HarvestCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      if (originalHook != null) {
        MethodType strType = MethodType.methodType(String.class,
          UUID.class, String.class, int.class, int.class, int.class);
        this.originalCheck = resolveMethod(originalHook, "check", strType);
        this.originalCheckPickup = resolveMethod(originalHook, "checkPickup", strType);
        this.originalCheckScytheHarvest = resolveMethod(originalHook, "checkScytheHarvest", strType);
      } else {
        this.originalCheck = null;
        this.originalCheckPickup = null;
        this.originalCheckScytheHarvest = null;
      }
    }

    /**
     * Called by BlockHarvestUtilsMixin for block break permission.
     * Chains: first non-null denial wins.
     * @return null to allow, denial message to block
     */
    public String check(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalCheck != null) {
          try {
            String originalResult = (String) originalCheck.invoke(playerUuid, worldName, x, y, z);
            if (originalResult != null) {
              Logger.debugProtection("[Mixin:Harvest] Blocked by chained hook (OrbisGuard)");
              return originalResult;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained harvest check: %s", t.getMessage());
          }
        }

        String result = callback.checkPickup(playerUuid, worldName, x, y, z);
        boolean allowed = result == null;
        Logger.debugProtection("[Mixin:Harvest] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return result;
      } catch (Exception e) {
        Logger.debugMixin("Error in harvest check: %s", e.getMessage());
        return null; // Fail-open
      }
    }

    /**
     * Called by BlockHarvestUtilsMixin for pickup permission.
     * Chains: first non-null denial wins.
     * @return null if allowed, denial message if blocked
     */
    public String checkPickup(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalCheckPickup != null) {
          try {
            String originalResult = (String) originalCheckPickup.invoke(playerUuid, worldName, x, y, z);
            if (originalResult != null) {
              Logger.debugProtection("[Mixin:HarvestPickup] Blocked by chained hook (OrbisGuard)");
              return originalResult;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained harvest pickup: %s", t.getMessage());
          }
        }

        String result = callback.checkPickup(playerUuid, worldName, x, y, z);
        boolean allowed = result == null;
        Logger.debugProtection("[Mixin:HarvestPickup] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return result;
      } catch (Exception e) {
        Logger.debugMixin("Error in harvest pickup check: %s", e.getMessage());
        return null; // Fail-open
      }
    }

    /**
     * Called by HarvestCropInteractionMixin (OG-Mixins 0.8.3+) for scythe/tool crop harvesting.
     * Chains: first non-null denial wins.
     * @return null if allowed, denial message if blocked
     */
    public String checkScytheHarvest(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalCheckScytheHarvest != null) {
          try {
            String originalResult = (String) originalCheckScytheHarvest.invoke(playerUuid, worldName, x, y, z);
            if (originalResult != null) {
              Logger.debugProtection("[Mixin:ScytheHarvest] Blocked by chained hook (OrbisGuard)");
              return originalResult;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained scythe harvest: %s", t.getMessage());
          }
        }

        String result = callback.checkPickup(playerUuid, worldName, x, y, z);
        boolean allowed = result == null;
        Logger.debugProtection("[Mixin:ScytheHarvest] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return result;
      } catch (Exception e) {
        Logger.debugMixin("Error in scythe harvest check: %s", e.getMessage());
        return null; // Fail-open
      }
    }
  }

  // ========== Place Protection Hook ==========

  /**
   * Registers a place hook for bucket/fluid placement protection.
   */
  public static boolean registerPlaceHook(@NotNull PlaceCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_PLACE_HOOK);
      PlaceHookWrapper wrapper = new PlaceHookWrapper(callback, existing);
      registerHookInRegistry(OG_PLACE_HOOK, wrapper);
      Logger.debug("Registered place protection hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register place hook", e);
      return false;
    }
  }

  /** Unregister Place Hook. */
  public static void unregisterPlaceHook() {
    restoreOrRemoveHook(OG_PLACE_HOOK);
  }

  /** PlaceCheckCallback interface. */
  @FunctionalInterface
  public interface PlaceCheckCallback {
    boolean isPlaceAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
  }

  /** PlaceHookWrapper class. */
  public static final class PlaceHookWrapper {
    private final PlaceCheckCallback callback;

    private final @Nullable MethodHandle originalIsPlaceAllowed;

    /** Creates a new PlaceHookWrapper. */
    public PlaceHookWrapper(@NotNull PlaceCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalIsPlaceAllowed = originalHook != null
        ? resolveMethod(originalHook, "isPlaceAllowed",
          MethodType.methodType(boolean.class, UUID.class, String.class,
            int.class, int.class, int.class))
        : null;
    }

    /**
     * Chains: both must allow for placement to proceed.
     */
    public boolean isPlaceAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
      try {
        if (originalIsPlaceAllowed != null) {
          try {
            boolean originalAllowed = (boolean) originalIsPlaceAllowed.invoke(playerUuid, worldName, x, y, z);
            if (!originalAllowed) {
              Logger.debugProtection("[Mixin:Place] Blocked by chained hook (OrbisGuard)");
              return false;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained place hook: %s", t.getMessage());
          }
        }

        boolean allowed = callback.isPlaceAllowed(playerUuid, worldName, x, y, z);
        Logger.debugInteraction("[Mixin:Place] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
          playerUuid, worldName, x, y, z, allowed);
        return allowed;
      } catch (Exception e) {
        Logger.debugMixin("Error in place check: %s", e.getMessage());
        return true; // Fail-open
      }
    }
  }

  // ========== Spawn Protection Hook ==========

  /**
   * Registers a spawn hook for mob spawning control.
   */
  public static boolean registerSpawnHook(@NotNull SpawnCheckCallback callback) {
    try {
      Object existing = captureExisting(OG_SPAWN_HOOK);
      SpawnHookWrapper wrapper = new SpawnHookWrapper(callback, existing);
      registerHookInRegistry(OG_SPAWN_HOOK, wrapper);
      Logger.debug("Registered spawn control hook");
      return true;
    } catch (Exception e) {
      ErrorHandler.report("Failed to register spawn hook", e);
      return false;
    }
  }

  /** Unregister Spawn Hook. */
  public static void unregisterSpawnHook() {
    restoreOrRemoveHook(OG_SPAWN_HOOK);
  }

  /** SpawnCheckCallback interface. */
  @FunctionalInterface
  public interface SpawnCheckCallback {
    /**
     * Checks if a mob spawn should be blocked.
     * Note: OrbisGuard-Mixins does not pass the NPC type.
     */
    boolean shouldBlockSpawn(@NotNull String worldName, int x, int y, int z);
  }

  /** SpawnHookWrapper class. */
  public static final class SpawnHookWrapper {
    private final SpawnCheckCallback callback;

    private final @Nullable MethodHandle originalShouldBlockSpawn;

    /** Creates a new SpawnHookWrapper. */
    public SpawnHookWrapper(@NotNull SpawnCheckCallback callback, @Nullable Object originalHook) {
      this.callback = callback;
      this.originalShouldBlockSpawn = originalHook != null
        ? resolveMethod(originalHook, "shouldBlockSpawn",
          MethodType.methodType(boolean.class, String.class, int.class, int.class, int.class))
        : null;
    }

    /**
     * Called by OrbisGuard-Mixins via reflection.
     * Method signature must match: shouldBlockSpawn(String, int, int, int) -> boolean
     *
     * <p>Chains with original hook (e.g., OrbisGuard's): if EITHER blocks, spawn is blocked.
     */
    public boolean shouldBlockSpawn(String worldName, int x, int y, int z) {
      try {
        // Check original hook first (OrbisGuard's region-based check)
        if (originalShouldBlockSpawn != null) {
          try {
            boolean originalBlocked = (boolean) originalShouldBlockSpawn.invoke(worldName, x, y, z);
            if (originalBlocked) {
              Logger.debugSpawning("[Mixin:Spawn] Blocked by chained hook (OrbisGuard) world=%s, pos=(%d,%d,%d)",
                worldName, x, y, z);
              return true;
            }
          } catch (Throwable t) {
            Logger.debugMixin("Error invoking chained spawn hook: %s", t.getMessage());
          }
        }

        // Then check HyperFactions logic
        boolean blocked = callback.shouldBlockSpawn(worldName, x, y, z);
        Logger.debugSpawning("[Mixin:Spawn] world=%s, pos=(%d,%d,%d), blocked=%b",
          worldName, x, y, z, blocked);
        return blocked;
      } catch (Exception e) {
        Logger.debugMixin("Error in spawn check: %s", e.getMessage());
        return false; // Fail-open - don't block if check fails
      }
    }
  }

  // ========== Unregister All Hooks ==========

  /**
   * Unregisters all HyperFactions hooks from OrbisGuard-Mixins.
   * Call this during plugin shutdown.
   */
  public static void unregisterAllHooks() {
    unregisterPickupHook();
    unregisterHammerHook();
    unregisterExplosionHook();
    unregisterCommandHook();
    unregisterDeathHook();
    unregisterDurabilityHook();
    unregisterUseHook();
    unregisterSeatHook();
    unregisterHarvestHook();
    unregisterPlaceHook();
    unregisterSpawnHook();
    Logger.debugMixin("Unregistered all OrbisGuard-Mixins hooks");
  }
}
