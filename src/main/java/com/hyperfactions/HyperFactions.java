package com.hyperfactions;

import com.hyperfactions.api.events.FactionDisbandEvent;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiUpdateService;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.lifecycle.CallbackWiring;
import com.hyperfactions.lifecycle.MembershipHistoryHandler;
import com.hyperfactions.lifecycle.PeriodicTaskManager;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.CoreConfig;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.integration.protection.ProtectionMixinBridge;
import com.hyperfactions.integration.permissions.HyperPermsIntegration;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.*;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.damage.DamageProtectionHandler;
import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.storage.ChatHistoryStorage;
import com.hyperfactions.storage.FactionStorage;
import com.hyperfactions.storage.JsonEconomyStorage;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.storage.ZoneStorage;
import com.hyperfactions.storage.json.JsonChatHistoryStorage;
import com.hyperfactions.storage.json.JsonFactionStorage;
import com.hyperfactions.storage.json.JsonPlayerStorage;
import com.hyperfactions.storage.json.JsonZoneStorage;
import com.hyperfactions.territory.TerritoryNotifier;
import com.hyperfactions.update.UpdateChecker;
import com.hyperfactions.update.UpdateNotificationListener;
import com.hyperfactions.update.UpdateNotificationPreferences;
import com.hyperfactions.util.Logger;
import com.hyperfactions.worldmap.MapPlayerFilterService;
import com.hyperfactions.worldmap.WorldMapService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Main HyperFactions core class.
 * Platform-agnostic coordinator for all faction functionality.
 */
public class HyperFactions {

    /** Plugin version from BuildInfo (auto-generated at build time) */
    public static final String VERSION = BuildInfo.VERSION;

    private final Path dataDir;
    private final com.hypixel.hytale.logger.HytaleLogger hytaleLogger;

    // Storage
    private FactionStorage factionStorage;
    private PlayerStorage playerStorage;
    private ZoneStorage zoneStorage;
    private ChatHistoryStorage chatHistoryStorage;

    // Managers
    private FactionManager factionManager;
    private ClaimManager claimManager;
    private PowerManager powerManager;
    private RelationManager relationManager;
    private CombatTagManager combatTagManager;
    private ZoneManager zoneManager;
    private TeleportManager teleportManager;
    private InviteManager inviteManager;
    private JoinRequestManager joinRequestManager;
    private ChatManager chatManager;
    private ChatHistoryManager chatHistoryManager;
    private ConfirmationManager confirmationManager;
    private SpawnSuppressionManager spawnSuppressionManager;
    private AnnouncementManager announcementManager;

    // Economy
    private VaultEconomyProvider vaultEconomyProvider;
    private EconomyManager economyManager;
    private JsonEconomyStorage economyStorage;
    private String treasuryDisabledReason;

    // Integrations
    private GravestoneIntegration gravestoneIntegration;

    // Protection
    private ProtectionChecker protectionChecker;
    private ZoneDamageProtection zoneDamageProtection;
    private ZoneInteractionProtection zoneInteractionProtection;
    private DamageProtectionHandler damageProtectionHandler;

    // GUI
    private GuiManager guiManager;
    private ActivePageTracker activePageTracker;
    private GuiUpdateService guiUpdateService;

    // Backup
    private BackupManager backupManager;

    // Update checker
    private UpdateChecker updateChecker;
    private UpdateChecker hyperProtectUpdateChecker;
    private UpdateNotificationListener updateNotificationListener;
    private UpdateNotificationPreferences notificationPreferences;

    // Territory features
    private TerritoryNotifier territoryNotifier;
    private WorldMapService worldMapService;
    private MapPlayerFilterService mapPlayerFilterService;

    // Lifecycle helpers
    private MembershipHistoryHandler membershipHistoryHandler;
    private PeriodicTaskManager periodicTaskManager;

    // Task management
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    private final Map<Integer, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

    // Admin bypass state (per-player toggle for protection bypass)
    private final Map<UUID, Boolean> adminBypassEnabled = new ConcurrentHashMap<>();

    // Platform callbacks (set by plugin)
    private Consumer<Runnable> asyncExecutor;
    private TaskSchedulerCallback taskScheduler;
    private TaskCancelCallback taskCanceller;
    private RepeatingTaskSchedulerCallback repeatingTaskScheduler;
    private java.util.function.Function<UUID, com.hypixel.hytale.server.core.universe.PlayerRef> playerLookup;
    private Supplier<Collection<com.hypixel.hytale.server.core.universe.PlayerRef>> onlinePlayersSupplier;

    /**
     * Functional interface for scheduling delayed tasks.
     */
    @FunctionalInterface
    public interface TaskSchedulerCallback {
        int schedule(int delayTicks, Runnable task);
    }

    /**
     * Functional interface for cancelling tasks.
     */
    @FunctionalInterface
    public interface TaskCancelCallback {
        void cancel(int taskId);
    }

    /**
     * Functional interface for scheduling repeating tasks.
     */
    @FunctionalInterface
    public interface RepeatingTaskSchedulerCallback {
        int schedule(int delayTicks, int periodTicks, Runnable task);
    }

    /**
     * Represents a scheduled task.
     */
    private record ScheduledTask(int id, Runnable task) {}

    /**
     * Creates a new HyperFactions instance.
     *
     * @param dataDir    the plugin data directory
     * @param hytaleLogger the Hytale logger
     */
    public HyperFactions(@NotNull Path dataDir, @NotNull com.hypixel.hytale.logger.HytaleLogger hytaleLogger) {
        this.dataDir = dataDir;
        this.hytaleLogger = hytaleLogger;
    }

    /**
     * Enables HyperFactions.
     */
    public void enable() {
        // Initialize logger
        Logger.init(hytaleLogger);
        Logger.debug("HyperFactions v%s starting (build: %d)", VERSION, BuildInfo.BUILD_TIMESTAMP);

        // Load configuration (uses new ConfigManager with migration support)
        ConfigManager.get().loadAll(dataDir);

        // Initialize HyperPerms integration (legacy, for backward compatibility)
        HyperPermsIntegration.init();

        // Initialize unified permission manager (new chain-based system)
        PermissionManager.get().init();

        // Preload Gson classes to avoid ClassNotFoundException on Timer threads
        // The Hytale PluginClassLoader doesn't properly propagate to Timer threads,
        // so we need to load all Gson inner classes on the main thread at startup.
        preloadGsonClasses();

        // Initialize storage
        factionStorage = new JsonFactionStorage(dataDir);
        playerStorage = new JsonPlayerStorage(dataDir);
        zoneStorage = new JsonZoneStorage(dataDir);

        factionStorage.init().join();
        playerStorage.init().join();
        zoneStorage.init().join();

        // Initialize managers (order matters!)
        factionManager = new FactionManager(factionStorage);
        powerManager = new PowerManager(playerStorage, factionManager);
        claimManager = new ClaimManager(factionManager, powerManager);
        relationManager = new RelationManager(factionManager);
        combatTagManager = new CombatTagManager();
        zoneManager = new ZoneManager(zoneStorage, claimManager);
        claimManager.setZoneManager(zoneManager); // Wire zone manager for zone protection checks
        spawnSuppressionManager = new SpawnSuppressionManager(zoneManager, claimManager, factionManager);
        teleportManager = new TeleportManager(factionManager);
        inviteManager = new InviteManager(dataDir);
        joinRequestManager = new JoinRequestManager(dataDir);

        // Initialize invite/request managers (loads persisted data)
        inviteManager.init();
        joinRequestManager.init();

        // Initialize confirmation manager (for text-mode command confirmations)
        confirmationManager = new ConfirmationManager();

        // Initialize backup manager
        backupManager = new BackupManager(dataDir, this);
        backupManager.init();

        // Load data
        factionManager.loadAll().join();
        powerManager.loadAll().join();
        zoneManager.loadAll().join();

        // Build claim index after loading factions
        claimManager.buildIndex();

        // Initialize treasury system (requires both config enabled AND VaultUnlocked economy)
        vaultEconomyProvider = new VaultEconomyProvider();
        vaultEconomyProvider.init();

        if (!ConfigManager.get().isEconomyEnabled()) {
            treasuryDisabledReason = "Economy features are not available on this server";
            Logger.debug("Treasury module disabled by server configuration");
        } else if (!vaultEconomyProvider.isAvailable()) {
            treasuryDisabledReason = "No economy plugin detected — install VaultUnlocked and an economy plugin";
            Logger.debug("Treasury module disabled (no economy provider)");
        } else {
            // Both conditions met — activate treasury
            economyStorage = new JsonEconomyStorage(dataDir);
            economyStorage.init().join();
            economyManager = new EconomyManager(factionManager, vaultEconomyProvider, economyStorage);
            economyManager.loadAll();
            treasuryDisabledReason = null;
            Logger.debug("Treasury module enabled (VaultUnlocked economy detected)");
        }

        // Initialize protection checker (with plugin reference for admin bypass toggle)
        protectionChecker = new ProtectionChecker(
            () -> this, factionManager, claimManager, zoneManager, relationManager, combatTagManager
        );

        // GravestonePlugin integration is initialized later by the plugin
        // via initGravestoneIntegration() once EventRegistry is available
        gravestoneIntegration = new GravestoneIntegration();
        protectionChecker.setGravestoneIntegration(gravestoneIntegration);

        // Initialize zone damage protection
        zoneDamageProtection = new ZoneDamageProtection(this);

        // Initialize zone interaction protection
        zoneInteractionProtection = new ZoneInteractionProtection(this);

        // Initialize damage protection handler (coordinates all protection systems)
        // Note: denialMessageProvider will be set by plugin after ProtectionListener is created
        damageProtectionHandler = null; // Initialized later by plugin

        // Initialize GUI manager
        guiManager = new GuiManager(
            () -> this,
            () -> factionManager,
            () -> claimManager,
            () -> powerManager,
            () -> relationManager,
            () -> zoneManager,
            () -> teleportManager,
            () -> inviteManager,
            () -> joinRequestManager,
            () -> dataDir
        );

        // Initialize real-time GUI update system
        activePageTracker = new ActivePageTracker();
        guiUpdateService = new GuiUpdateService(activePageTracker, factionManager);
        guiManager.setActivePageTracker(activePageTracker);

        // Initialize membership history handler
        membershipHistoryHandler = new MembershipHistoryHandler(playerStorage, factionManager);

        // Initialize territory notifier (for entry/exit notifications)
        territoryNotifier = new TerritoryNotifier(
            factionManager, claimManager, zoneManager, relationManager
        );

        // Initialize world map service (for claim markers on map)
        worldMapService = new WorldMapService(
            factionManager, claimManager, zoneManager, relationManager
        );

        // Initialize the world map refresh scheduler with optimized mode
        worldMapService.initializeScheduler(ConfigManager.get().worldMap());

        // Initialize map player filter service (faction-aware player visibility on map/compass)
        mapPlayerFilterService = new MapPlayerFilterService(factionManager, relationManager);

        // Initialize announcement manager (uses deferred onlinePlayersSupplier)
        announcementManager = new AnnouncementManager(
            () -> onlinePlayersSupplier != null ? onlinePlayersSupplier.get() : Collections.emptyList()
        );

        // Wire all manager callbacks (GUI updates, events, announcements, notifications)
        CallbackWiring.wireAll(this, guiUpdateService, membershipHistoryHandler);

        // Migrate existing faction members — creates active membership records
        // for players who don't have one yet (upgrade from pre-history versions)
        membershipHistoryHandler.migrateMembershipHistory();

        // Initialize chat manager (uses deferred playerLookup)
        chatManager = new ChatManager(factionManager, relationManager,
            uuid -> playerLookup != null ? playerLookup.apply(uuid) : null);

        // Initialize chat history storage and manager
        chatHistoryStorage = new JsonChatHistoryStorage(dataDir);
        chatHistoryStorage.init().join();
        chatHistoryManager = new ChatHistoryManager(chatHistoryStorage);

        // Wire chat history into chat manager and GUI
        chatManager.setChatHistoryManager(chatHistoryManager);
        chatManager.setGuiUpdateService(guiUpdateService);
        guiManager.setChatManagerSupplier(() -> chatManager);
        guiManager.setChatHistoryManagerSupplier(() -> chatHistoryManager);

        // Zone change callback is set by the platform plugin (HyperFactionsPlugin)
        // to include both world map refresh and spawn suppression updates

        // Initialize update checker if enabled
        if (ConfigManager.get().isUpdateCheckEnabled()) {
            updateChecker = new UpdateChecker(dataDir, VERSION, ConfigManager.get().getUpdateCheckUrl(),
                    ConfigManager.get().isPreReleaseChannel());

            // Clear any pending rollback marker - server has restarted, migrations may have run
            // This must happen before checkForUpdates() so rollback safety is properly tracked
            updateChecker.clearRollbackMarker();

            updateChecker.checkForUpdates();

            // Initialize notification preferences
            notificationPreferences = new UpdateNotificationPreferences(dataDir);
            notificationPreferences.load();

            // Initialize update notification listener
            updateNotificationListener = new UpdateNotificationListener(this);
        }

        // HyperProtect-Mixin auto-download / update checking
        initHyperProtectMixinLifecycle();

        // Start periodic tasks (auto-save, invite cleanup)
        // Note: These are started after platform sets callbacks via setRepeatingTaskScheduler()
        // The platform should call startPeriodicTasks() after setting up callbacks

        // Initialize PlaceholderAPI integration (after all managers are ready)
        com.hyperfactions.integration.placeholder.PlaceholderAPIIntegration.init(this);

        // Initialize WiFlow PlaceholderAPI integration (guard before class loading)
        try {
            Class.forName("com.wiflow.placeholderapi.WiFlowPlaceholderAPI");
            com.hyperfactions.integration.placeholder.WiFlowPlaceholderIntegration.init(this);
        } catch (ClassNotFoundException e) {
            Logger.debug("WiFlow PlaceholderAPI not found — WiFlow placeholders disabled");
        }
    }

    /**
     * Starts periodic tasks (auto-save, invite cleanup, scheduled backups).
     * Should be called by the platform after setting up task scheduler callbacks.
     */
    public void startPeriodicTasks() {
        if (periodicTaskManager == null) {
            periodicTaskManager = new PeriodicTaskManager(this);
        }
        periodicTaskManager.startAll();
        // Start scheduled backups now that the task scheduler is available
        if (backupManager != null) {
            backupManager.startScheduledBackups();
        }
    }

    /**
     * Disables HyperFactions.
     */
    public void disable() {

        // Unregister PlaceholderAPI expansions
        try {
            Class.forName("com.wiflow.placeholderapi.WiFlowPlaceholderAPI");
            com.hyperfactions.integration.placeholder.WiFlowPlaceholderIntegration.shutdown();
        } catch (ClassNotFoundException ignored) {
            // WiFlow not present, nothing to shut down
        }
        com.hyperfactions.integration.placeholder.PlaceholderAPIIntegration.shutdown();

        // Cancel periodic tasks first
        if (periodicTaskManager != null) {
            periodicTaskManager.cancelAll();
        }

        // Save economy data
        if (economyManager != null) {
            economyManager.saveAll();
        }

        // Save all data
        saveAllData();

        // Shutdown backup manager (creates shutdown backup if configured)
        if (backupManager != null) {
            backupManager.shutdown();
        }

        // Shutdown invite/request managers (saves persisted data)
        if (inviteManager != null) {
            inviteManager.shutdown();
        }
        if (joinRequestManager != null) {
            joinRequestManager.shutdown();
        }

        // Shutdown chat history manager (flushes pending saves)
        if (chatHistoryManager != null) {
            chatHistoryManager.shutdown();
        }
        if (chatHistoryStorage != null) {
            chatHistoryStorage.shutdown().join();
        }

        // Shutdown storage
        if (factionStorage != null) {
            factionStorage.shutdown().join();
        }
        if (playerStorage != null) {
            playerStorage.shutdown().join();
        }
        if (zoneStorage != null) {
            zoneStorage.shutdown().join();
        }

        // Shutdown update notification listener
        if (updateNotificationListener != null) {
            updateNotificationListener.shutdown();
        }

        // Shutdown territory services
        if (territoryNotifier != null) {
            territoryNotifier.shutdown();
        }
        if (worldMapService != null) {
            worldMapService.shutdown();
        }

        // Clear active page tracker
        if (activePageTracker != null) {
            activePageTracker.clear();
        }

        // Cancel remaining scheduled tasks
        for (int taskId : scheduledTasks.keySet()) {
            cancelTask(taskId);
        }

    }

    /**
     * Preloads Gson classes to avoid ClassNotFoundException on Timer threads.
     * The Hytale PluginClassLoader doesn't properly propagate class visibility
     * to Timer threads, so we load all needed Gson inner classes at startup.
     */
    private void preloadGsonClasses() {
        try {
            // Create a test object and serialize/deserialize it
            // This forces all Gson internal classes to be loaded
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            // Create a JsonObject with various types to trigger class loading
            com.google.gson.JsonObject testObj = new com.google.gson.JsonObject();
            testObj.addProperty("string", "test");
            testObj.addProperty("number", 42);
            testObj.addProperty("boolean", true);

            com.google.gson.JsonArray testArray = new com.google.gson.JsonArray();
            testArray.add("item1");
            testArray.add("item2");
            testObj.add("array", testArray);

            // Serialize to JSON string (triggers LinkedTreeMap$EntrySet and related classes)
            String json = gson.toJson(testObj);

            // Also iterate over entries (triggers entrySet inner classes)
            for (var entry : testObj.entrySet()) {
                // Force class loading
                @SuppressWarnings("unused")
                String key = entry.getKey();
            }

            // Parse back (triggers other internal classes)
            gson.fromJson(json, com.google.gson.JsonObject.class);

            Logger.debug("Gson classes preloaded successfully");
        } catch (Exception e) {
            Logger.warn("Failed to preload Gson classes: %s", e.getMessage());
        }
    }

    /**
     * Initializes HyperProtect-Mixin lifecycle management.
     * - If HP is running: check for updates
     * - If HP JAR exists on disk but didn't initialize: warn + check for updates
     * - If no JAR on disk + autoDownload enabled: download latest release
     * - If no JAR on disk + autoDownload disabled: log instructions for manual install
     */
    private void initHyperProtectMixinLifecycle() {
        CoreConfig config = ConfigManager.get().core();
        Path earlyPluginsDir = dataDir.toAbsolutePath().getParent().getParent().resolve("earlyplugins");
        boolean hpDetected = "true".equalsIgnoreCase(System.getProperty("hyperprotect.bridge.active"))
                || "true".equalsIgnoreCase(System.getProperty("hyperprotect.intercept.block_break"))
                || ProtectionMixinBridge.getProvider() == ProtectionMixinBridge.MixinProvider.HYPERPROTECT;

        if (hpDetected) {
            // HP is installed and running — check for updates if enabled
            if (config.isHyperProtectAutoUpdate()) {
                String hpVersion = System.getProperty("hyperprotect.bridge.version", "0.0.0");
                hyperProtectUpdateChecker = new UpdateChecker(
                        dataDir, hpVersion, config.getHyperProtectUpdateUrl(),
                        false, "HyperProtect-Mixin", earlyPluginsDir);
                hyperProtectUpdateChecker.checkForUpdates(false).thenAccept(info -> {
                    if (info != null) {
                        Logger.info("[Update] HyperProtect-Mixin update available: v%s -> v%s", hpVersion, info.version());
                        Logger.info("[Update] Run '/f admin update mixin' to download, then restart.");
                    }
                });
            }
            return;
        }

        // HP is NOT running — check if a JAR exists on disk
        String diskVersion = null;
        try (var stream = java.nio.file.Files.newDirectoryStream(earlyPluginsDir, "HyperProtect-Mixin-*.jar")) {
            for (var jar : stream) {
                String name = jar.getFileName().toString();
                String v = name.substring("HyperProtect-Mixin-".length(), name.length() - ".jar".length());
                if (diskVersion == null || v.compareTo(diskVersion) > 0) {
                    diskVersion = v;
                }
            }
        } catch (Exception ignored) {}

        if (diskVersion != null) {
            // JAR exists but didn't initialize — warn and check for updates
            final String foundVersion = diskVersion;
            Logger.warn("[Update] HyperProtect-Mixin v%s found in earlyplugins/ but did not initialize. Check for errors above.", foundVersion);
            if (config.isHyperProtectAutoUpdate()) {
                hyperProtectUpdateChecker = new UpdateChecker(
                        dataDir, foundVersion, config.getHyperProtectUpdateUrl(),
                        false, "HyperProtect-Mixin", earlyPluginsDir);
                hyperProtectUpdateChecker.checkForUpdates(false).thenAccept(info -> {
                    if (info != null) {
                        Logger.info("[Update] HyperProtect-Mixin update available: v%s -> v%s", foundVersion, info.version());
                        Logger.info("[Update] Run '/f admin update mixin' to download, then restart.");
                    }
                });
            }
        } else if (config.isHyperProtectAutoDownload()) {
            // No JAR on disk + auto-download enabled — download latest release
            Logger.info("[Update] HyperProtect-Mixin not detected. Attempting auto-download...");
            hyperProtectUpdateChecker = new UpdateChecker(
                    dataDir, "0.0.0", config.getHyperProtectUpdateUrl(),
                    false, "HyperProtect-Mixin", earlyPluginsDir);
            hyperProtectUpdateChecker.checkForUpdates(false).thenAccept(info -> {
                if (info == null) {
                    Logger.info("[Update] No HyperProtect-Mixin releases available yet. Will check again next restart.");
                    return;
                }
                hyperProtectUpdateChecker.downloadUpdate(info).thenAccept(path -> {
                    if (path != null) {
                        Logger.info("[Update] HyperProtect-Mixin v%s downloaded to earlyplugins/. Restart server to activate.", info.version());
                    } else {
                        Logger.warn("Failed to download HyperProtect-Mixin. Install manually from GitHub.");
                    }
                });
            });
        } else {
            // No JAR on disk + auto-download disabled — inform the user
            Logger.info("[HyperProtect] HyperProtect-Mixin is not installed. For full protection features (explosions, fire spread, builder tools, etc.), install it:");
            Logger.info("[HyperProtect]   - Run '/f admin update mixin' to download, or");
            Logger.info("[HyperProtect]   - Set updates.hyperProtect.autoDownload=true in config.json, or");
            Logger.info("[HyperProtect]   - Download manually from GitHub and place in earlyplugins/");
        }
    }

    /**
     * Reloads the configuration and reinitializes managers that depend on config values.
     */
    public void reloadConfig() {
        ConfigManager.get().reloadAll();

        // Reinitialize world map scheduler in case refresh mode changed
        if (worldMapService != null) {
            worldMapService.initializeScheduler(ConfigManager.get().worldMap());
        }

        // Re-evaluate map player filters in case visibility settings changed
        if (mapPlayerFilterService != null) {
            mapPlayerFilterService.applyToAll();
        }

        // Restart backup scheduler in case backup settings changed
        if (backupManager != null) {
            backupManager.restartScheduledBackups();
        }

        // Cleanup expired time-sensitive items with potentially new expiration settings
        if (inviteManager != null) {
            inviteManager.cleanupExpired();
        }
        if (joinRequestManager != null) {
            joinRequestManager.cleanupExpired();
        }
        if (confirmationManager != null) {
            confirmationManager.cleanupExpired();
        }

        Logger.info("[Config] Configuration reloaded");
    }

    /**
     * Initializes the GravestonePlugin integration (v2 direct API).
     * Must be called by the plugin after enable() once EventRegistry is available.
     *
     * @param eventRegistry the event registry for gravestone event listeners
     */
    public void initGravestoneIntegration(@NotNull com.hypixel.hytale.event.EventRegistry eventRegistry) {
        if (gravestoneIntegration != null) {
            gravestoneIntegration.init(() -> this, protectionChecker, eventRegistry);
        }
    }

    // === Platform callbacks ===

    public void setAsyncExecutor(@NotNull Consumer<Runnable> executor) {
        this.asyncExecutor = executor;
    }

    public void setTaskScheduler(@NotNull TaskSchedulerCallback scheduler) {
        this.taskScheduler = scheduler;
    }

    public void setTaskCanceller(@NotNull TaskCancelCallback canceller) {
        this.taskCanceller = canceller;
    }

    public void setRepeatingTaskScheduler(@NotNull RepeatingTaskSchedulerCallback scheduler) {
        this.repeatingTaskScheduler = scheduler;
    }

    public void setPlayerLookup(@NotNull java.util.function.Function<UUID, com.hypixel.hytale.server.core.universe.PlayerRef> lookup) {
        this.playerLookup = lookup;
        // Also set up the permission manager's player lookup for OP checks
        PermissionManager.get().setPlayerLookup(lookup);
    }

    public void setOnlinePlayersSupplier(@NotNull Supplier<Collection<com.hypixel.hytale.server.core.universe.PlayerRef>> supplier) {
        this.onlinePlayersSupplier = supplier;
    }

    /**
     * Gets all online player references.
     *
     * @return collection of online PlayerRef, or empty if not available
     */
    @NotNull
    public Collection<com.hypixel.hytale.server.core.universe.PlayerRef> getOnlinePlayerRefs() {
        return onlinePlayersSupplier != null ? onlinePlayersSupplier.get() : Collections.emptyList();
    }

    /**
     * Looks up an online player by UUID.
     *
     * @param uuid the player's UUID
     * @return the PlayerRef, or null if not online or lookup unavailable
     */
    @Nullable
    public com.hypixel.hytale.server.core.universe.PlayerRef lookupPlayer(@NotNull UUID uuid) {
        return playerLookup != null ? playerLookup.apply(uuid) : null;
    }

    /**
     * Resolves a player UUID by username.
     * Delegates to {@link com.hyperfactions.util.PlayerResolver} for centralized resolution:
     * online players -> faction members -> PlayerDB API.
     *
     * @param name the player username (case-insensitive)
     * @return the player's UUID, or null if not found
     */
    @Nullable
    public UUID resolvePlayerByName(@NotNull String name) {
        var resolved = com.hyperfactions.util.PlayerResolver.resolve(this, name);
        return resolved != null ? resolved.uuid() : null;
    }

    // === Task scheduling ===

    /**
     * Schedules a delayed task.
     *
     * @param delayTicks the delay in ticks
     * @param task       the task
     * @return the task ID
     */
    public int scheduleDelayedTask(int delayTicks, @NotNull Runnable task) {
        if (taskScheduler != null) {
            int id = taskIdCounter.incrementAndGet();
            int platformId = taskScheduler.schedule(delayTicks, () -> {
                scheduledTasks.remove(id);
                task.run();
            });
            scheduledTasks.put(id, new ScheduledTask(platformId, task));
            return id;
        }
        // Fallback: run immediately
        task.run();
        return -1;
    }

    /**
     * Cancels a task.
     *
     * @param taskId the task ID
     */
    public void cancelTask(int taskId) {
        ScheduledTask task = scheduledTasks.remove(taskId);
        if (task != null && taskCanceller != null) {
            taskCanceller.cancel(task.id());
        }
    }

    /**
     * Schedules a repeating task.
     *
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period in ticks
     * @param task        the task
     * @return the task ID
     */
    public int scheduleRepeatingTask(int delayTicks, int periodTicks, @NotNull Runnable task) {
        if (repeatingTaskScheduler != null) {
            int id = taskIdCounter.incrementAndGet();
            int platformId = repeatingTaskScheduler.schedule(delayTicks, periodTicks, task);
            scheduledTasks.put(id, new ScheduledTask(platformId, task));
            return id;
        }
        return -1;
    }

    /**
     * Performs a save of all data.
     * Called periodically by auto-save and on shutdown.
     */
    public void saveAllData() {
        Logger.info("[AutoSave] Saving data...");
        if (factionManager != null) {
            factionManager.saveAll().join();
        }
        if (powerManager != null) {
            powerManager.saveAll().join();
        }
        if (zoneManager != null) {
            zoneManager.saveAll().join();
        }
        Logger.info("[AutoSave] Save complete");
    }

    // === Getters ===

    @NotNull
    public Path getDataDir() {
        return dataDir;
    }

    @NotNull
    public FactionManager getFactionManager() {
        return factionManager;
    }

    @NotNull
    public ClaimManager getClaimManager() {
        return claimManager;
    }

    @NotNull
    public PowerManager getPowerManager() {
        return powerManager;
    }

    @NotNull
    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    @NotNull
    public RelationManager getRelationManager() {
        return relationManager;
    }

    @NotNull
    public CombatTagManager getCombatTagManager() {
        return combatTagManager;
    }

    @NotNull
    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    @NotNull
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    @NotNull
    public InviteManager getInviteManager() {
        return inviteManager;
    }

    @NotNull
    public JoinRequestManager getJoinRequestManager() {
        return joinRequestManager;
    }

    @NotNull
    public ChatManager getChatManager() {
        return chatManager;
    }

    @Nullable
    public ChatHistoryManager getChatHistoryManager() {
        return chatHistoryManager;
    }

    public ConfirmationManager getConfirmationManager() {
        return confirmationManager;
    }

    @NotNull
    public ProtectionChecker getProtectionChecker() {
        return protectionChecker;
    }

    @NotNull
    public ZoneDamageProtection getZoneDamageProtection() {
        return zoneDamageProtection;
    }

    @NotNull
    public ZoneInteractionProtection getZoneInteractionProtection() {
        return zoneInteractionProtection;
    }

    /**
     * Gets the damage protection handler that coordinates all protection systems.
     *
     * @return the damage protection handler, or null if not yet initialized
     */
    @Nullable
    public DamageProtectionHandler getDamageProtectionHandler() {
        return damageProtectionHandler;
    }

    /**
     * Sets the damage protection handler. Called by the plugin after creating ProtectionListener.
     *
     * @param handler the damage protection handler
     */
    public void setDamageProtectionHandler(@NotNull DamageProtectionHandler handler) {
        this.damageProtectionHandler = handler;
    }

    @NotNull
    public GuiManager getGuiManager() {
        return guiManager;
    }

    /**
     * Gets the active page tracker for GUI real-time updates.
     *
     * @return the active page tracker
     */
    @Nullable
    public ActivePageTracker getActivePageTracker() {
        return activePageTracker;
    }

    /**
     * Gets the backup manager.
     *
     * @return the backup manager
     */
    @NotNull
    public BackupManager getBackupManager() {
        return backupManager;
    }

    /**
     * Gets the update checker.
     *
     * @return the update checker, or null if update checking is disabled
     */
    @Nullable
    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    /**
     * Gets the HyperProtect-Mixin update checker.
     *
     * @return the mixin update checker, or null if not initialized
     */
    @Nullable
    public UpdateChecker getHyperProtectUpdateChecker() {
        return hyperProtectUpdateChecker;
    }

    /**
     * Gets the update notification listener.
     *
     * @return the update notification listener, or null if update checking is disabled
     */
    @Nullable
    public UpdateNotificationListener getUpdateNotificationListener() {
        return updateNotificationListener;
    }

    /**
     * Gets the notification preferences manager.
     *
     * @return the notification preferences, or null if update checking is disabled
     */
    @Nullable
    public UpdateNotificationPreferences getNotificationPreferences() {
        return notificationPreferences;
    }

    /**
     * Gets the data directory.
     *
     * @return the plugin data directory
     */
    @NotNull
    public Path getDataDirectory() {
        return dataDir;
    }

    /**
     * Gets the territory notifier.
     *
     * @return the territory notifier
     */
    @NotNull
    public TerritoryNotifier getTerritoryNotifier() {
        return territoryNotifier;
    }

    /**
     * Gets the world map service.
     *
     * @return the world map service
     */
    @NotNull
    public WorldMapService getWorldMapService() {
        return worldMapService;
    }

    /**
     * Gets the map player filter service.
     *
     * @return the map player filter service
     */
    @NotNull
    public MapPlayerFilterService getMapPlayerFilterService() {
        return mapPlayerFilterService;
    }

    /**
     * Gets the spawn suppression manager.
     *
     * @return the spawn suppression manager
     */
    @NotNull
    public SpawnSuppressionManager getSpawnSuppressionManager() {
        return spawnSuppressionManager;
    }

    /**
     * Gets the announcement manager.
     *
     * @return the announcement manager
     */
    @NotNull
    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }

    // === Economy ===

    /**
     * Gets the economy manager, or null if treasury is disabled.
     *
     * @return the economy manager, or null
     */
    @Nullable
    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    /**
     * Gets the VaultUnlocked economy provider, or null if not initialized.
     *
     * @return the vault economy provider, or null
     */
    @Nullable
    public VaultEconomyProvider getVaultEconomyProvider() {
        return vaultEconomyProvider;
    }

    /**
     * Checks if the treasury system is fully enabled and operational.
     *
     * @return true if economy manager is active
     */
    public boolean isTreasuryEnabled() {
        return economyManager != null;
    }

    /**
     * Gets a human-readable reason why treasury is disabled.
     *
     * @return the reason string, or null if treasury is enabled
     */
    @Nullable
    public String getTreasuryDisabledReason() {
        return treasuryDisabledReason;
    }

    // === Admin Bypass Toggle ===

    /**
     * Checks if admin bypass is enabled for a player.
     * When enabled, the player can bypass protection checks in claimed territory.
     *
     * @param playerUuid the player's UUID
     * @return true if admin bypass is enabled for this player
     */
    public boolean isAdminBypassEnabled(@NotNull UUID playerUuid) {
        return adminBypassEnabled.getOrDefault(playerUuid, false);
    }

    /**
     * Toggles admin bypass state for a player.
     * When toggled on, the player can bypass protection checks in claimed territory.
     *
     * @param playerUuid the player's UUID
     * @return true if bypass is now enabled, false if now disabled
     */
    public boolean toggleAdminBypass(@NotNull UUID playerUuid) {
        return adminBypassEnabled.compute(playerUuid, (k, v) -> v == null || !v);
    }

    /**
     * Handles faction disband event by cleaning up all associated data.
     * This is called by the EventBus when any faction is disbanded.
     *
     * @param event the disband event
     */
    public void handleFactionDisband(@NotNull FactionDisbandEvent event) {
        UUID factionId = event.faction().id();
        Logger.info("[Faction] Cleaning up data for disbanded faction '%s' (ID: %s)",
                event.faction().name(), factionId);

        // Clean up claims
        claimManager.unclaimAll(factionId);

        // Clean up invites
        inviteManager.clearFactionInvites(factionId);

        // Clean up join requests
        joinRequestManager.clearFactionRequests(factionId);

        // Clean up relations
        relationManager.clearAllRelations(factionId);

        // Clean up chat history
        if (chatHistoryManager != null) {
            chatHistoryManager.deleteHistory(factionId);
        }
    }

}
