package com.hyperfactions.integration.protection;

import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration with OrbisGuard-Mixins for enhanced protection coverage.
 *
 * OrbisGuard-Mixins uses System.getProperties() for cross-classloader communication,
 * allowing plugins to register hooks without a direct dependency.
 *
 * When OrbisGuard-Mixins is installed (requires Hyxin mixin loader), it sets
 * flags in system properties and provides a hook registry. Plugins can register
 * callbacks to control various behaviors that are not accessible via normal events.
 *
 * Available Hooks:
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
 * If OrbisGuard-Mixins is not available, this integration is gracefully disabled
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
     * Detection strategy: OrbisGuard-Mixins sets system properties in mixin class
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
                Logger.info("OrbisGuard-Mixins detected - enhanced protection hooks available");
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
            Logger.warn("Error checking OrbisGuard-Mixins availability: %s", e.getMessage());
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
                Logger.info("OrbisGuard-Mixins now detected (late load)");
                initError = null;
            }
        }
        if (durabilityMixinLoaded != oldDurability || pickupMixinLoaded != oldPickup ||
            deathMixinLoaded != oldDeath || seatingMixinLoaded != oldSeating) {
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
        if (durabilityMixinLoaded) { sb.append("durability"); first = false; }
        if (pickupMixinLoaded) { sb.append(first ? "" : ", ").append("pickup"); first = false; }
        if (deathMixinLoaded) { sb.append(first ? "" : ", ").append("death"); first = false; }
        if (seatingMixinLoaded) { sb.append(first ? "" : ", ").append("seating"); first = false; }
        if (first) { sb.append("general"); }
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
    public static boolean isDurabilityMixinLoaded() { return durabilityMixinLoaded; }

    /**
     * Checks if the seating mixin is loaded.
     */
    public static boolean isSeatingMixinLoaded() { return seatingMixinLoaded; }

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

    // ========== Pickup Protection Hook ==========

    /**
     * Registers a pickup protection hook with OrbisGuard-Mixins.
     *
     * The callback will be invoked whenever OrbisGuard-Mixins processes an
     * F-key or auto pickup event. If the callback returns false, the pickup
     * is blocked.
     *
     * Note: Hooks are registered unconditionally. The mixin will find and use
     * the hook if it's loaded. Detection is only for logging purposes.
     *
     * @param callback the callback to check if pickup is allowed
     * @return true if hook was registered successfully
     */
    public static boolean registerPickupHook(@NotNull PickupCheckCallback callback) {
        try {
            PickupHookWrapper wrapper = new PickupHookWrapper(callback);
            registerHookInRegistry(OG_PICKUP_HOOK, wrapper);
            Logger.info("Registered pickup protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register pickup hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterPickupHook() {
        unregisterHookFromRegistry(OG_PICKUP_HOOK);
        Logger.debugMixin("Unregistered pickup hook");
    }

    @FunctionalInterface
    public interface PickupCheckCallback {
        boolean isPickupAllowed(@NotNull UUID playerUuid, @NotNull String worldName,
                                double x, double y, double z, @NotNull String mode);
    }

    public static final class PickupHookWrapper {
        private final PickupCheckCallback callback;

        public PickupHookWrapper(@NotNull PickupCheckCallback callback) {
            this.callback = callback;
        }

        /**
         * Called by OrbisGuard-Mixins via reflection.
         * Method name and signature must match exactly:
         * check(UUID, String, double, double, double, String) -> boolean
         */
        public boolean check(UUID playerUuid, String worldName, double x, double y, double z, String mode) {
            try {
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
            HammerHookWrapper wrapper = new HammerHookWrapper(callback);
            registerHookInRegistry(OG_HAMMER_HOOK, wrapper);
            Logger.info("Registered hammer protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register hammer hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterHammerHook() {
        unregisterHookFromRegistry(OG_HAMMER_HOOK);
    }

    @FunctionalInterface
    public interface HammerCheckCallback {
        boolean isHammerAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class HammerHookWrapper {
        private final HammerCheckCallback callback;

        public HammerHookWrapper(@NotNull HammerCheckCallback callback) {
            this.callback = callback;
        }

        public boolean isHammerAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
                boolean allowed = callback.isHammerAllowed(playerUuid, worldName, x, y, z);
                Logger.debugInteraction("[Mixin:Hammer] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
                    playerUuid, worldName, x, y, z, allowed);
                Logger.debugProtection("[Mixin:Hammer] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
                    playerUuid, worldName, x, y, z, allowed);
                return allowed;
            } catch (Exception e) {
                Logger.debugMixin("Error in hammer check: %s", e.getMessage());
                return true; // Fail-open
            }
        }
    }

    // ========== Explosion Protection Hook ==========

    /**
     * Registers an explosion protection hook for explosion block damage.
     */
    public static boolean registerExplosionHook(@NotNull ExplosionCheckCallback callback) {
        try {
            ExplosionHookWrapper wrapper = new ExplosionHookWrapper(callback);
            registerHookInRegistry(OG_EXPLOSION_HOOK, wrapper);
            Logger.info("Registered explosion protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register explosion hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterExplosionHook() {
        unregisterHookFromRegistry(OG_EXPLOSION_HOOK);
    }

    @FunctionalInterface
    public interface ExplosionCheckCallback {
        boolean shouldBlockExplosion(@NotNull String worldName, int x, int y, int z);
    }

    public static final class ExplosionHookWrapper {
        private final ExplosionCheckCallback callback;

        public ExplosionHookWrapper(@NotNull ExplosionCheckCallback callback) {
            this.callback = callback;
        }

        public boolean shouldBlockExplosion(World world, int x, int y, int z) {
            try {
                String worldName = world != null ? world.getName() : "";
                boolean blocked = callback.shouldBlockExplosion(worldName, x, y, z);
                Logger.debugProtection("[Mixin:Explosion] world=%s, pos=(%d,%d,%d), blocked=%b",
                    worldName, x, y, z, blocked);
                return blocked;
            } catch (Exception e) {
                Logger.debugMixin("Error in explosion check: %s", e.getMessage());
                return false; // Fail-open - don't block if check fails
            }
        }
    }

    // ========== Command Protection Hook ==========

    /**
     * Registers a command protection hook for blocking commands in zones.
     */
    public static boolean registerCommandHook(@NotNull CommandCheckCallback callback) {
        try {
            CommandHookWrapper wrapper = new CommandHookWrapper(callback);
            registerHookInRegistry(OG_COMMAND_HOOK, wrapper);
            Logger.info("Registered command protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register command hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterCommandHook() {
        unregisterHookFromRegistry(OG_COMMAND_HOOK);
    }

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

    public record CommandCheckResult(boolean block, @Nullable String denialMessage) {
        public static CommandCheckResult allow() { return new CommandCheckResult(false, null); }
        public static CommandCheckResult deny(@Nullable String message) { return new CommandCheckResult(true, message); }
    }

    public static final class CommandHookWrapper {
        private final CommandCheckCallback callback;
        private volatile CommandCheckResult lastResult = CommandCheckResult.allow();

        public CommandHookWrapper(@NotNull CommandCheckCallback callback) {
            this.callback = callback;
        }

        public boolean shouldBlockCommand(Player player, String command) {
            try {
                if (player == null || command == null) return false;

                UUID uuid = player.getUuid();
                // Note: OrbisGuard-Mixins passes the Player directly; we can get UUID and world from it
                // Position lookup is complex; for now we pass 0,0,0 and let the callback handle it
                String worldName = "";
                int x = 0, y = 0, z = 0;

                lastResult = callback.shouldBlockCommand(uuid, worldName, x, y, z, command);
                return lastResult.block();
            } catch (Exception e) {
                Logger.debugMixin("Error in command check: %s", e.getMessage());
                return false; // Fail-open
            }
        }

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
            DeathHookWrapper wrapper = new DeathHookWrapper(callback);
            registerHookInRegistry(OG_DEATH_HOOK, wrapper);
            Logger.info("Registered death (keep inventory) hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register death hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterDeathHook() {
        unregisterHookFromRegistry(OG_DEATH_HOOK);
    }

    @FunctionalInterface
    public interface DeathCheckCallback {
        boolean shouldKeepInventory(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class DeathHookWrapper {
        private final DeathCheckCallback callback;

        public DeathHookWrapper(@NotNull DeathCheckCallback callback) {
            this.callback = callback;
        }

        public boolean shouldKeepInventory(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
                boolean keepInventory = callback.shouldKeepInventory(playerUuid, worldName, x, y, z);
                Logger.debugCombat("[Mixin:Death] player=%s, world=%s, pos=(%d,%d,%d), keepInventory=%b",
                    playerUuid, worldName, x, y, z, keepInventory);
                return keepInventory;
            } catch (Exception e) {
                Logger.debugMixin("Error in death check: %s", e.getMessage());
                return false; // Fail-open - don't keep inventory if check fails
            }
        }
    }

    // ========== Durability Protection Hook ==========

    /**
     * Registers a durability hook to prevent tool/armor durability loss.
     */
    public static boolean registerDurabilityHook(@NotNull DurabilityCheckCallback callback) {
        try {
            DurabilityHookWrapper wrapper = new DurabilityHookWrapper(callback);
            registerHookInRegistry(OG_DURABILITY_HOOK, wrapper);
            Logger.info("Registered durability protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register durability hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterDurabilityHook() {
        unregisterHookFromRegistry(OG_DURABILITY_HOOK);
    }

    @FunctionalInterface
    public interface DurabilityCheckCallback {
        boolean shouldPreventDurabilityLoss(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class DurabilityHookWrapper {
        private final DurabilityCheckCallback callback;

        public DurabilityHookWrapper(@NotNull DurabilityCheckCallback callback) {
            this.callback = callback;
        }

        public boolean shouldPreventDurabilityLoss(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
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
            UseHookWrapper wrapper = new UseHookWrapper(callback);
            registerHookInRegistry(OG_USE_HOOK, wrapper);
            Logger.info("Registered use protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register use hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterUseHook() {
        unregisterHookFromRegistry(OG_USE_HOOK);
    }

    @FunctionalInterface
    public interface UseCheckCallback {
        boolean isUseAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class UseHookWrapper {
        private final UseCheckCallback callback;

        public UseHookWrapper(@NotNull UseCheckCallback callback) {
            this.callback = callback;
        }

        public boolean isUseAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
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
            SeatHookWrapper wrapper = new SeatHookWrapper(callback);
            registerHookInRegistry(OG_SEAT_HOOK, wrapper);
            Logger.info("Registered seat protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register seat hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterSeatHook() {
        unregisterHookFromRegistry(OG_SEAT_HOOK);
    }

    @FunctionalInterface
    public interface SeatCheckCallback {
        boolean isSeatAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class SeatHookWrapper {
        private final SeatCheckCallback callback;

        public SeatHookWrapper(@NotNull SeatCheckCallback callback) {
            this.callback = callback;
        }

        public boolean isSeatAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
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
            HarvestHookWrapper wrapper = new HarvestHookWrapper(callback);
            registerHookInRegistry(OG_HARVEST_HOOK, wrapper);
            Logger.info("Registered harvest protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register harvest hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterHarvestHook() {
        unregisterHookFromRegistry(OG_HARVEST_HOOK);
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

        public HarvestHookWrapper(@NotNull HarvestCheckCallback callback) {
            this.callback = callback;
        }

        /**
         * Called by BlockHarvestUtilsMixin for block break permission.
         * This sets the 'denied' ThreadLocal flag in the mixin.
         * If we return non-null, the block removal is prevented entirely,
         * which means no item drops and no pickup to intercept.
         *
         * @return null to allow, denial message to block
         */
        public String check(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
                // Check if manual pickup would be allowed - if not, block the harvest entirely
                // This prevents the item from dropping in the first place
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
         * This is where we check ITEM_PICKUP_MANUAL flag.
         * @return null if allowed, denial message if blocked
         */
        public String checkPickup(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
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
    }

    // ========== Place Protection Hook ==========

    /**
     * Registers a place hook for bucket/fluid placement protection.
     */
    public static boolean registerPlaceHook(@NotNull PlaceCheckCallback callback) {
        try {
            PlaceHookWrapper wrapper = new PlaceHookWrapper(callback);
            registerHookInRegistry(OG_PLACE_HOOK, wrapper);
            Logger.info("Registered place protection hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register place hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterPlaceHook() {
        unregisterHookFromRegistry(OG_PLACE_HOOK);
    }

    @FunctionalInterface
    public interface PlaceCheckCallback {
        boolean isPlaceAllowed(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z);
    }

    public static final class PlaceHookWrapper {
        private final PlaceCheckCallback callback;

        public PlaceHookWrapper(@NotNull PlaceCheckCallback callback) {
            this.callback = callback;
        }

        public boolean isPlaceAllowed(UUID playerUuid, String worldName, int x, int y, int z) {
            try {
                boolean allowed = callback.isPlaceAllowed(playerUuid, worldName, x, y, z);
                Logger.debugInteraction("[Mixin:Place] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
                    playerUuid, worldName, x, y, z, allowed);
                Logger.debugProtection("[Mixin:Place] player=%s, world=%s, pos=(%d,%d,%d), allowed=%b",
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
            SpawnHookWrapper wrapper = new SpawnHookWrapper(callback);
            registerHookInRegistry(OG_SPAWN_HOOK, wrapper);
            Logger.info("Registered spawn control hook");
            return true;
        } catch (Exception e) {
            Logger.warn("Failed to register spawn hook: %s", e.getMessage());
            return false;
        }
    }

    public static void unregisterSpawnHook() {
        unregisterHookFromRegistry(OG_SPAWN_HOOK);
    }

    @FunctionalInterface
    public interface SpawnCheckCallback {
        /**
         * Checks if a mob spawn should be blocked.
         * Note: OrbisGuard-Mixins does not pass the NPC type.
         */
        boolean shouldBlockSpawn(@NotNull String worldName, int x, int y, int z);
    }

    public static final class SpawnHookWrapper {
        private final SpawnCheckCallback callback;

        public SpawnHookWrapper(@NotNull SpawnCheckCallback callback) {
            this.callback = callback;
        }

        /**
         * Called by OrbisGuard-Mixins via reflection.
         * Method signature must match: shouldBlockSpawn(String, int, int, int) -> boolean
         */
        public boolean shouldBlockSpawn(String worldName, int x, int y, int z) {
            try {
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
