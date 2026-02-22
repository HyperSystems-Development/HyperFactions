package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.HyperFactionsAPI;
import com.hyperfactions.chat.PublicChatListener;
import com.hyperfactions.command.FactionCommand;
import com.hyperfactions.listener.PlayerListener;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.interactions.HyperFactionsHarvestCropInteraction;
import com.hyperfactions.protection.interactions.HyperFactionsPlaceFluidInteraction;
import com.hyperfactions.protection.interactions.HyperFactionsRefillContainerInteraction;
import com.hyperfactions.util.Logger;
import com.hyperfactions.integration.PermissionRegistrar;
import com.hyperfactions.integration.protection.OrbisMixinsIntegration;
import com.hyperfactions.integration.protection.OrbisGuardIntegration;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Main Hytale plugin class for HyperFactions.
 */
public class HyperFactionsPlugin extends JavaPlugin {

    private static HyperFactionsPlugin instance;

    /**
     * Gets the plugin instance.
     */
    public static HyperFactionsPlugin getInstance() {
        return instance;
    }

    private HyperFactions hyperFactions;
    private PlayerListener playerListener;
    private ProtectionListener protectionListener;
    private PublicChatListener publicChatListener;

    // Extracted helper classes
    private EventRegistration eventRegistration;
    private WorldSetup worldSetup;
    private PlayerConnectionHandler connectionHandler;

    // Task scheduling
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, Object> scheduledTasks = new ConcurrentHashMap<>();

    // Player tracking
    private final Map<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

    // Periodic task executor
    private ScheduledExecutorService tickExecutor;
    private ScheduledFuture<?> powerRegenTask;
    private ScheduledFuture<?> combatTagTask;
    private ScheduledFuture<?> claimDecayTask;

    /**
     * Creates a new HyperFactionsPlugin instance.
     *
     * @param init the plugin initialization data
     */
    public HyperFactionsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        // Register interaction codec replacements (must be in setup, before assets load)
        registerInteractionCodecs();

        // Initialize HyperFactions core
        hyperFactions = new HyperFactions(getDataDirectory(), java.util.logging.Logger.getLogger("HyperFactions"));

        // Set API instance
        HyperFactionsAPI.setInstance(hyperFactions);

        getLogger().at(Level.INFO).log("HyperFactions setup complete");
    }

    @Override
    protected void start() {
        // Configure platform callbacks
        configurePlatformCallbacks();

        // Enable core
        hyperFactions.enable();

        // Initialize GravestonePlugin integration (v2 direct API — needs EventRegistry)
        hyperFactions.initGravestoneIntegration(getEventRegistry());

        // Create helper classes
        worldSetup = new WorldSetup(this, hyperFactions);
        eventRegistration = new EventRegistration(this, hyperFactions);
        connectionHandler = new PlayerConnectionHandler(hyperFactions, trackedPlayers);

        // Register world map provider CODEC (must be before worlds load)
        worldSetup.registerWorldMapProvider();

        // Register commands
        registerCommands();

        // Register event listeners (world, player, chat, protection systems)
        EventRegistration.RegistrationResult result = eventRegistration.registerAll(worldSetup, connectionHandler);
        playerListener = result.playerListener();
        protectionListener = result.protectionListener();
        publicChatListener = result.publicChatListener();

        // Register teleport systems
        eventRegistration.registerTeleportSystems();

        // Register territory tracking systems
        eventRegistration.registerTerritorySystems();

        // Apply world map provider to already-loaded worlds
        // (AddWorldEvent may have fired before our listener was registered)
        worldSetup.applyToExistingWorlds();

        // Start periodic tasks
        startPeriodicTasks();

        // Initialize spawn suppression manager (must be after TagSetPlugin is ready)
        worldSetup.initializeSpawnSuppression();

        // Initialize OrbisGuard integrations and register pickup hooks
        initializeOrbisIntegrations();

        // Log protection coverage summary
        worldSetup.logProtectionCoverage();

        // Register permission nodes with LuckPerms on BootEvent (after all plugins loaded)
        getEventRegistry().registerGlobal(BootEvent.class, e -> PermissionRegistrar.registerWithLuckPerms());

        getLogger().at(Level.INFO).log("HyperFactions v%s enabled!", getManifest().getVersion());
    }

    @Override
    protected void shutdown() {
        // Stop periodic tasks
        stopPeriodicTasks();

        // Handle combat logout for all tagged players
        for (UUID playerUuid : trackedPlayers.keySet()) {
            hyperFactions.getCombatTagManager().handleDisconnect(playerUuid);
        }

        // Unregister all OrbisGuard-Mixins hooks
        OrbisMixinsIntegration.unregisterAllHooks();

        // Clean up territory ticking system
        if (eventRegistration != null) {
            eventRegistration.shutdownTerritory();
        }

        // Clear instances
        instance = null;
        HyperFactionsAPI.setInstance(null);

        // Disable core
        if (hyperFactions != null) {
            hyperFactions.disable();
        }

        // Clear tracked players
        trackedPlayers.clear();

        getLogger().at(Level.INFO).log("HyperFactions disabled");
    }

    /**
     * Configures platform-specific callbacks for HyperFactions core.
     */
    private void configurePlatformCallbacks() {
        // Async executor
        hyperFactions.setAsyncExecutor(task -> {
            java.util.concurrent.CompletableFuture.runAsync(task);
        });

        // Task scheduler (for one-shot delayed tasks)
        hyperFactions.setTaskScheduler((delayTicks, task) -> {
            int id = taskIdCounter.incrementAndGet();
            java.util.Timer timer = new java.util.Timer();
            long delayMs = delayTicks * 50L;
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    scheduledTasks.remove(id);
                    task.run();
                }
            }, delayMs);
            scheduledTasks.put(id, timer);
            return id;
        });

        // Repeating task scheduler (for periodic tasks like auto-save)
        hyperFactions.setRepeatingTaskScheduler((delayTicks, periodTicks, task) -> {
            int id = taskIdCounter.incrementAndGet();
            java.util.Timer timer = new java.util.Timer();
            long delayMs = delayTicks * 50L;
            long periodMs = periodTicks * 50L;
            timer.scheduleAtFixedRate(new java.util.TimerTask() {
                @Override
                public void run() {
                    task.run();
                }
            }, delayMs, periodMs);
            scheduledTasks.put(id, timer);
            return id;
        });

        // Task canceller
        hyperFactions.setTaskCanceller(taskId -> {
            Object task = scheduledTasks.remove(taskId);
            if (task instanceof java.util.Timer timer) {
                timer.cancel();
            }
        });

        // Player lookup (for chat manager)
        hyperFactions.setPlayerLookup(this::getTrackedPlayer);

        // Online players supplier (for announcements)
        hyperFactions.setOnlinePlayersSupplier(() -> trackedPlayers.values());
    }

    /**
     * Registers commands with Hytale.
     */
    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new FactionCommand(hyperFactions, this));
            getLogger().at(Level.INFO).log("Registered command: /faction (/f, /hf)");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).withCause(e).log("Failed to register commands");
        }
    }

    /**
     * Registers custom interaction codec replacements for protection.
     * Must be called in setup() before assets are loaded.
     *
     * Replaced interactions:
     * - PlaceFluid: Prevents fluid placement (water/lava buckets) in protected territory
     * - RefillContainer: Prevents fluid pickup from containers in protected territory
     * - HarvestCrop: Prevents crop harvesting (F-key on mature crops) in protected territory
     */
    private void registerInteractionCodecs() {
        try {
            var registry = this.getCodecRegistry(Interaction.CODEC);
            registry.register("PlaceFluid",
                    HyperFactionsPlaceFluidInteraction.class,
                    HyperFactionsPlaceFluidInteraction.CODEC);
            registry.register("RefillContainer",
                    HyperFactionsRefillContainerInteraction.class,
                    HyperFactionsRefillContainerInteraction.CODEC);
            registry.register("HarvestCrop",
                    HyperFactionsHarvestCropInteraction.class,
                    HyperFactionsHarvestCropInteraction.CODEC);
            getLogger().at(Level.INFO).log("Registered interaction protection codecs (fluid place/pickup + crop harvest)");
        } catch (Exception e) {
            getLogger().at(Level.WARNING).log("Failed to register interaction codecs: %s", e.getMessage());
        }
    }

    /**
     * Initializes OrbisGuard integrations and registers pickup protection hooks.
     *
     * OrbisGuard-Mixins provides hooks for F-key and auto item pickup protection.
     * OrbisGuard provides region protection to prevent claiming in protected areas.
     */
    private void initializeOrbisIntegrations() {
        // Initialize OrbisGuard API detection (for claim conflict checking)
        OrbisGuardIntegration.init();

        // Register mixin hooks unconditionally - the mixin will find them if loaded
        // This matches how OrbisGuard registers its hooks
        OrbisMixinsIntegration.registerPickupHook(
                (playerUuid, worldName, x, y, z, mode) -> {
                    // Delegate to ProtectionChecker with mode awareness
                    // mode = "manual" for F-key pickup, "auto" for walking over items
                    return hyperFactions.getProtectionChecker().canPickupItem(
                            playerUuid, worldName, x, y, z, mode);
                });

        // Harvest hook is registered in CallbackWiring.wireHarvestProtection()
        // (handles both zone and claim protection in one hook)

        // Register spawn protection hook
        OrbisMixinsIntegration.registerSpawnHook(
                (worldName, x, y, z) -> {
                    // Delegate to ProtectionChecker - returns true if spawn should be BLOCKED
                    return hyperFactions.getProtectionChecker().shouldBlockSpawn(worldName, x, y, z);
                });

        // Note: Fluid placement protection is handled via interaction codec replacement
        // (HyperFactionsPlaceFluidInteraction), not via mixin hooks.
    }

    /**
     * Starts periodic tasks (power regen, combat tag decay, auto-save, invite cleanup).
     */
    private void startPeriodicTasks() {
        tickExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HyperFactions-Ticker");
            t.setDaemon(true);
            return t;
        });

        // Power regeneration every minute
        powerRegenTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getPowerManager().tickPowerRegen();
                } catch (Exception e) {
                    Logger.severe("Error in power regen tick", e);
                }
            },
            60, 60, TimeUnit.SECONDS
        );

        // Combat tag decay every second
        combatTagTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getCombatTagManager().tickDecay();
                } catch (Exception e) {
                    Logger.severe("Error in combat tag tick", e);
                }
            },
            1, 1, TimeUnit.SECONDS
        );

        // Claim decay for inactive factions - runs hourly
        // Uses thread-safe ConcurrentHashMap operations internally
        claimDecayTask = tickExecutor.scheduleAtFixedRate(
            () -> {
                try {
                    hyperFactions.getClaimManager().tickClaimDecay();
                } catch (Exception e) {
                    Logger.severe("Error in claim decay tick", e);
                }
            },
            1, 1, TimeUnit.HOURS  // Initial delay of 1 hour, then every hour
        );

        // Territory tracking is now handled by TerritoryTickingSystem (ECS)
        // which ticks reliably every game tick for all player entities

        // Start core periodic tasks (auto-save, invite cleanup)
        hyperFactions.startPeriodicTasks();

        getLogger().at(Level.INFO).log("Started periodic tasks (including claim decay every hour)");
    }

    /**
     * Stops periodic tasks.
     */
    private void stopPeriodicTasks() {
        if (powerRegenTask != null) {
            powerRegenTask.cancel(false);
        }
        if (combatTagTask != null) {
            combatTagTask.cancel(false);
        }
        if (claimDecayTask != null) {
            claimDecayTask.cancel(false);
        }
        if (tickExecutor != null) {
            tickExecutor.shutdown();
        }
    }

    /**
     * Gets a tracked player by UUID.
     *
     * @param uuid the player's UUID
     * @return the PlayerRef, or null if not online
     */
    public PlayerRef getTrackedPlayer(UUID uuid) {
        return trackedPlayers.get(uuid);
    }

    /**
     * Gets all tracked players.
     *
     * @return map of UUID to PlayerRef
     */
    public Map<UUID, PlayerRef> getTrackedPlayers() {
        return trackedPlayers;
    }

    /**
     * Reloads the configuration.
     */
    public void reloadConfig() {
        hyperFactions.reloadConfig();
    }

    /**
     * Gets the HyperFactions instance.
     *
     * @return the HyperFactions instance
     */
    public HyperFactions getHyperFactions() {
        return hyperFactions;
    }

    /**
     * Gets the player listener.
     *
     * @return the player listener
     */
    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    /**
     * Gets the protection listener.
     *
     * @return the protection listener
     */
    public ProtectionListener getProtectionListener() {
        return protectionListener;
    }

}
