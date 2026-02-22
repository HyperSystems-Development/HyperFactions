package com.hyperfactions.config;

import com.hyperfactions.config.modules.*;
import com.hyperfactions.config.modules.WorldMapConfig;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationRunner;
import com.hyperfactions.migration.MigrationType;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Central manager for all HyperFactions configuration.
 * <p>
 * Orchestrates loading of the core config and all module configs,
 * handles migrations, and provides unified access to all settings.
 */
public class ConfigManager {

    private static ConfigManager instance;

    private Path dataDir;
    private CoreConfig coreConfig;
    private BackupConfig backupConfig;
    private ChatConfig chatConfig;
    private DebugConfig debugConfig;
    private EconomyConfig economyConfig;
    private FactionPermissionsConfig factionPermissionsConfig;
    private WorldMapConfig worldMapConfig;
    private AnnouncementConfig announcementConfig;
    private GravestoneConfig gravestoneConfig;

    private ConfigManager() {}

    /**
     * Gets the singleton config manager instance.
     *
     * @return config manager
     */
    @NotNull
    public static ConfigManager get() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Loads all configuration files.
     * <p>
     * This method:
     * <ol>
     *   <li>Runs any pending config migrations</li>
     *   <li>Loads the core config from config.json</li>
     *   <li>Loads all module configs from config/ directory</li>
     * </ol>
     *
     * @param dataDir the plugin data directory
     */
    public void loadAll(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        Logger.info("[Config] Loading configuration from: %s", dataDir.toAbsolutePath());

        // Step 1: Run pending migrations
        runMigrations();

        // Step 2: Load core config
        coreConfig = new CoreConfig(dataDir.resolve("config.json"));
        coreConfig.load();

        // Step 3: Load module configs from config/ subdirectory
        Path configDir = dataDir.resolve("config");

        backupConfig = new BackupConfig(configDir.resolve("backup.json"));
        backupConfig.load();

        chatConfig = new ChatConfig(configDir.resolve("chat.json"));
        chatConfig.load();

        debugConfig = new DebugConfig(configDir.resolve("debug.json"));
        debugConfig.load();

        economyConfig = new EconomyConfig(configDir.resolve("economy.json"));
        economyConfig.load();

        factionPermissionsConfig = new FactionPermissionsConfig(configDir.resolve("faction-permissions.json"));
        factionPermissionsConfig.load();

        worldMapConfig = new WorldMapConfig(configDir.resolve("worldmap.json"));
        worldMapConfig.load();

        announcementConfig = new AnnouncementConfig(configDir.resolve("announcements.json"));
        announcementConfig.load();

        gravestoneConfig = new GravestoneConfig(configDir.resolve("gravestones.json"));
        gravestoneConfig.load();

        // Step 4: Validate all configs and log any issues
        validateAll();

        Logger.info("[Config] Configuration loaded successfully");
    }

    /**
     * Validates all configuration files and logs any issues found.
     * <p>
     * This performs "soft" validation - invalid values are logged as warnings
     * and auto-corrected when possible, but the plugin will continue to function.
     */
    private void validateAll() {
        ValidationResult combined = new ValidationResult();

        // Validate each config and merge results
        coreConfig.validateAndLog();
        if (coreConfig.getLastValidationResult() != null) {
            combined.merge(coreConfig.getLastValidationResult());
        }

        backupConfig.validateAndLog();
        if (backupConfig.getLastValidationResult() != null) {
            combined.merge(backupConfig.getLastValidationResult());
        }

        chatConfig.validateAndLog();
        if (chatConfig.getLastValidationResult() != null) {
            combined.merge(chatConfig.getLastValidationResult());
        }

        debugConfig.validateAndLog();
        if (debugConfig.getLastValidationResult() != null) {
            combined.merge(debugConfig.getLastValidationResult());
        }

        economyConfig.validateAndLog();
        if (economyConfig.getLastValidationResult() != null) {
            combined.merge(economyConfig.getLastValidationResult());
        }

        factionPermissionsConfig.validateAndLog();
        if (factionPermissionsConfig.getLastValidationResult() != null) {
            combined.merge(factionPermissionsConfig.getLastValidationResult());
        }

        worldMapConfig.validateAndLog();
        if (worldMapConfig.getLastValidationResult() != null) {
            combined.merge(worldMapConfig.getLastValidationResult());
        }

        announcementConfig.validateAndLog();
        if (announcementConfig.getLastValidationResult() != null) {
            combined.merge(announcementConfig.getLastValidationResult());
        }

        gravestoneConfig.validateAndLog();
        if (gravestoneConfig.getLastValidationResult() != null) {
            combined.merge(gravestoneConfig.getLastValidationResult());
        }

        // Log summary
        if (combined.hasIssues()) {
            int warnings = combined.getWarnings().size();
            int errors = combined.getErrors().size();
            Logger.info("[Config] Validation complete: %d warning(s), %d error(s)", warnings, errors);
        }
    }

    /**
     * Runs any pending config migrations.
     */
    private void runMigrations() {
        List<MigrationResult> results = MigrationRunner.runPendingMigrations(dataDir, MigrationType.CONFIG);

        for (MigrationResult result : results) {
            if (result.success()) {
                Logger.info("[Config] Migration '%s' completed: v%d -> v%d",
                        result.migrationId(), result.fromVersion(), result.toVersion());
            } else {
                Logger.severe("[Config] Migration '%s' failed: %s",
                        result.migrationId(), result.errorMessage());
                if (result.rolledBack()) {
                    Logger.info("[Config] Rolled back to previous config version");
                }
            }
        }
    }

    /**
     * Reloads all configuration files.
     */
    public void reloadAll() {
        Logger.info("[Config] Reloading configuration...");

        coreConfig.reload();
        backupConfig.reload();
        chatConfig.reload();
        debugConfig.reload();
        economyConfig.reload();
        factionPermissionsConfig.reload();
        worldMapConfig.reload();
        announcementConfig.reload();
        gravestoneConfig.reload();

        // Re-validate after reload
        validateAll();

        Logger.info("[Config] Configuration reloaded");
    }

    /**
     * Saves all configuration files.
     */
    public void saveAll() {
        coreConfig.save();
        backupConfig.save();
        chatConfig.save();
        debugConfig.save();
        economyConfig.save();
        factionPermissionsConfig.save();
        worldMapConfig.save();
        announcementConfig.save();
        gravestoneConfig.save();
    }

    // === Config Accessors ===

    /**
     * Gets the core configuration.
     *
     * @return core config
     */
    @NotNull
    public CoreConfig core() {
        return coreConfig;
    }

    /**
     * Gets the backup module configuration.
     *
     * @return backup config
     */
    @NotNull
    public BackupConfig backup() {
        return backupConfig;
    }

    /**
     * Gets the chat module configuration.
     *
     * @return chat config
     */
    @NotNull
    public ChatConfig chat() {
        return chatConfig;
    }

    /**
     * Gets the debug module configuration.
     *
     * @return debug config
     */
    @NotNull
    public DebugConfig debug() {
        return debugConfig;
    }

    /**
     * Gets the economy module configuration.
     *
     * @return economy config
     */
    @NotNull
    public EconomyConfig economy() {
        return economyConfig;
    }

    /**
     * Gets the faction permissions module configuration.
     *
     * @return faction permissions config
     */
    @NotNull
    public FactionPermissionsConfig factionPermissions() {
        return factionPermissionsConfig;
    }

    /**
     * Gets the world map module configuration.
     *
     * @return world map config
     */
    @NotNull
    public WorldMapConfig worldMap() {
        return worldMapConfig;
    }

    /**
     * Gets the announcement module configuration.
     *
     * @return announcement config
     */
    @NotNull
    public AnnouncementConfig announcements() {
        return announcementConfig;
    }

    /**
     * Gets the gravestone integration module configuration.
     *
     * @return gravestone config
     */
    @NotNull
    public GravestoneConfig gravestones() {
        return gravestoneConfig;
    }

    // === Convenience Methods (for backward compatibility) ===

    // Faction
    public int getMaxMembers() { return coreConfig.getMaxMembers(); }
    public int getMaxMembershipHistory() { return coreConfig.getMaxMembershipHistory(); }
    public int getMaxNameLength() { return coreConfig.getMaxNameLength(); }
    public int getMinNameLength() { return coreConfig.getMinNameLength(); }
    public boolean isAllowColors() { return coreConfig.isAllowColors(); }

    // Power
    public double getMaxPlayerPower() { return coreConfig.getMaxPlayerPower(); }
    public double getStartingPower() { return coreConfig.getStartingPower(); }
    public double getPowerPerClaim() { return coreConfig.getPowerPerClaim(); }
    public double getDeathPenalty() { return coreConfig.getDeathPenalty(); }
    public double getKillReward() { return coreConfig.getKillReward(); }

    public boolean isKillRewardRequiresFaction() { return coreConfig.isKillRewardRequiresFaction(); }
    public boolean isPowerLossOnMobDeath() { return coreConfig.isPowerLossOnMobDeath(); }
    public boolean isPowerLossOnEnvironmentalDeath() { return coreConfig.isPowerLossOnEnvironmentalDeath(); }
    public double getRegenPerMinute() { return coreConfig.getRegenPerMinute(); }
    public boolean isRegenWhenOffline() { return coreConfig.isRegenWhenOffline(); }

    // Claims
    public int getMaxClaims() { return coreConfig.getMaxClaims(); }
    public boolean isOnlyAdjacent() { return coreConfig.isOnlyAdjacent(); }
    public boolean isPreventDisconnect() { return coreConfig.isPreventDisconnect(); }
    public boolean isDecayEnabled() { return coreConfig.isDecayEnabled(); }
    public int getDecayDaysInactive() { return coreConfig.getDecayDaysInactive(); }
    @NotNull public List<String> getWorldWhitelist() { return coreConfig.getWorldWhitelist(); }
    @NotNull public List<String> getWorldBlacklist() { return coreConfig.getWorldBlacklist(); }
    public boolean isWorldAllowed(@NotNull String worldName) { return coreConfig.isWorldAllowed(worldName); }
    public int calculateMaxClaims(double totalPower) { return coreConfig.calculateMaxClaims(totalPower); }

    // Combat
    public int getTagDurationSeconds() { return coreConfig.getTagDurationSeconds(); }
    public boolean isAllyDamage() { return coreConfig.isAllyDamage(); }
    public boolean isFactionDamage() { return coreConfig.isFactionDamage(); }
    public boolean isTaggedLogoutPenalty() { return coreConfig.isTaggedLogoutPenalty(); }
    public double getLogoutPowerLoss() { return coreConfig.getLogoutPowerLoss(); }
    public double getNeutralAttackPenalty() { return coreConfig.getNeutralAttackPenalty(); }

    // Spawn Protection
    public boolean isSpawnProtectionEnabled() { return coreConfig.isSpawnProtectionEnabled(); }
    public int getSpawnProtectionDurationSeconds() { return coreConfig.getSpawnProtectionDurationSeconds(); }
    public boolean isSpawnProtectionBreakOnAttack() { return coreConfig.isSpawnProtectionBreakOnAttack(); }
    public boolean isSpawnProtectionBreakOnMove() { return coreConfig.isSpawnProtectionBreakOnMove(); }

    // Relations
    public int getMaxAllies() { return coreConfig.getMaxAllies(); }
    public int getMaxEnemies() { return coreConfig.getMaxEnemies(); }

    // Invites
    public int getInviteExpirationMinutes() { return coreConfig.getInviteExpirationMinutes(); }
    public int getJoinRequestExpirationHours() { return coreConfig.getJoinRequestExpirationHours(); }
    public long getInviteExpirationMs() { return coreConfig.getInviteExpirationMs(); }
    public long getJoinRequestExpirationMs() { return coreConfig.getJoinRequestExpirationMs(); }

    // Stuck
    public int getStuckMinRadius() { return coreConfig.getStuckMinRadius(); }
    public int getStuckRadiusIncrease() { return coreConfig.getStuckRadiusIncrease(); }
    public int getStuckMaxAttempts() { return coreConfig.getStuckMaxAttempts(); }
    public int getStuckWarmupSeconds() { return coreConfig.getStuckWarmupSeconds(); }
    public int getStuckCooldownSeconds() { return coreConfig.getStuckCooldownSeconds(); }

    // Teleport
    public int getWarmupSeconds() { return coreConfig.getWarmupSeconds(); }
    public int getCooldownSeconds() { return coreConfig.getCooldownSeconds(); }
    public boolean isCancelOnMove() { return coreConfig.isCancelOnMove(); }
    public boolean isCancelOnDamage() { return coreConfig.isCancelOnDamage(); }

    // Updates
    public boolean isUpdateCheckEnabled() { return coreConfig.isUpdateCheckEnabled(); }
    @NotNull public String getUpdateCheckUrl() { return coreConfig.getUpdateCheckUrl(); }
    @NotNull public String getReleaseChannel() { return coreConfig.getReleaseChannel(); }
    public boolean isPreReleaseChannel() { return coreConfig.isPreReleaseChannel(); }

    // Auto-save
    public boolean isAutoSaveEnabled() { return coreConfig.isAutoSaveEnabled(); }
    public int getAutoSaveIntervalMinutes() { return coreConfig.getAutoSaveIntervalMinutes(); }

    // Backup (from module)
    public boolean isBackupEnabled() { return backupConfig.isEnabled(); }
    public int getBackupHourlyRetention() { return backupConfig.getHourlyRetention(); }
    public int getBackupDailyRetention() { return backupConfig.getDailyRetention(); }
    public int getBackupWeeklyRetention() { return backupConfig.getWeeklyRetention(); }
    public int getBackupManualRetention() { return backupConfig.getManualRetention(); }
    public boolean isBackupOnShutdown() { return backupConfig.isOnShutdown(); }
    public int getBackupShutdownRetention() { return backupConfig.getShutdownRetention(); }

    // Economy (from module)
    public boolean isEconomyEnabled() { return economyConfig.isEnabled(); }
    @NotNull public String getEconomyCurrencyName() { return economyConfig.getCurrencyName(); }
    @NotNull public String getEconomyCurrencyNamePlural() { return economyConfig.getCurrencyNamePlural(); }
    @NotNull public String getEconomyCurrencySymbol() { return economyConfig.getCurrencySymbol(); }
    public double getEconomyStartingBalance() { return economyConfig.getStartingBalance(); }
    public boolean isEconomyDisbandRefundToLeader() { return economyConfig.isDisbandRefundToLeader(); }
    @NotNull public com.hyperfactions.data.FactionEconomy.TreasuryLimits getDefaultTreasuryLimits() { return economyConfig.getDefaultTreasuryLimits(); }
    @NotNull public com.hyperfactions.config.modules.EconomyConfig getEconomyConfig() { return economyConfig; }
    public double getDepositFeePercent() { return economyConfig.getDepositFeePercent(); }
    public double getWithdrawFeePercent() { return economyConfig.getWithdrawFeePercent(); }
    public double getTransferFeePercent() { return economyConfig.getTransferFeePercent(); }
    public boolean isUpkeepEnabled() { return economyConfig.isUpkeepEnabled(); }
    public double getUpkeepCostPerChunk() { return economyConfig.getUpkeepCostPerChunk(); }
    public int getUpkeepIntervalHours() { return economyConfig.getUpkeepIntervalHours(); }
    public int getUpkeepGracePeriodHours() { return economyConfig.getUpkeepGracePeriodHours(); }
    public boolean isUpkeepAutoPayDefault() { return economyConfig.isUpkeepAutoPayDefault(); }

    // Messages (v3 structured prefix)
    @NotNull public String getPrefixText() { return coreConfig.getPrefixText(); }
    @NotNull public String getPrefixColor() { return coreConfig.getPrefixColor(); }
    @NotNull public String getPrefixBracketColor() { return coreConfig.getPrefixBracketColor(); }
    @NotNull public String getPrimaryColor() { return coreConfig.getPrimaryColor(); }

    // GUI
    @NotNull public String getGuiTitle() { return coreConfig.getGuiTitle(); }
    public boolean isTerrainMapEnabled() { return coreConfig.isTerrainMapEnabled(); }

    // Territory Notifications
    public boolean isTerritoryNotificationsEnabled() { return coreConfig.isTerritoryNotificationsEnabled(); }

    // World Map (from worldmap.json module config)
    public boolean isWorldMapMarkersEnabled() { return worldMapConfig.isEnabled(); }

    // Debug (from module)
    public boolean isDebugEnabledByDefault() { return debugConfig.isEnabledByDefault(); }
    public boolean isDebugLogToConsole() { return debugConfig.isLogToConsole(); }
    public boolean isDebugPower() { return debugConfig.isPower(); }
    public boolean isDebugClaim() { return debugConfig.isClaim(); }
    public boolean isDebugCombat() { return debugConfig.isCombat(); }
    public boolean isDebugProtection() { return debugConfig.isProtection(); }
    public boolean isDebugRelation() { return debugConfig.isRelation(); }
    public boolean isDebugTerritory() { return debugConfig.isTerritory(); }
    public boolean isDebugWorldmap() { return debugConfig.isWorldmap(); }
    public boolean isDebugIntegration() { return debugConfig.isIntegration(); }

    // Debug setters
    public void setDebugPower(boolean enabled) { debugConfig.setPower(enabled); }
    public void setDebugClaim(boolean enabled) { debugConfig.setClaim(enabled); }
    public void setDebugCombat(boolean enabled) { debugConfig.setCombat(enabled); }
    public void setDebugProtection(boolean enabled) { debugConfig.setProtection(enabled); }
    public void setDebugRelation(boolean enabled) { debugConfig.setRelation(enabled); }
    public void setDebugTerritory(boolean enabled) { debugConfig.setTerritory(enabled); }
    public void setDebugWorldmap(boolean enabled) { debugConfig.setWorldmap(enabled); }
    public void setDebugIntegration(boolean enabled) { debugConfig.setIntegration(enabled); }
    public void enableAllDebug() { debugConfig.enableAll(); }
    public void disableAllDebug() { debugConfig.disableAll(); }
    public void applyDebugSettings() { debugConfig.applyToLogger(); }

    // Chat (from module)
    public boolean isChatFormattingEnabled() { return chatConfig.isEnabled(); }
    @NotNull public String getChatFormat() { return chatConfig.getFormat(); }
    @NotNull public String getChatTagDisplay() { return chatConfig.getTagDisplay(); }
    @NotNull public String getChatTagFormat() { return chatConfig.getTagFormat(); }
    @NotNull public String getChatNoFactionTag() { return chatConfig.getNoFactionTag(); }
    @NotNull public String getChatNoFactionTagColor() { return chatConfig.getNoFactionTagColor(); }
    @NotNull public String getChatPlayerNameColor() { return chatConfig.getPlayerNameColor(); }
    @NotNull public String getChatEventPriority() { return chatConfig.getPriority(); }
    @NotNull public String getChatRelationColorOwn() { return chatConfig.getRelationColorOwn(); }
    @NotNull public String getChatRelationColorAlly() { return chatConfig.getRelationColorAlly(); }
    @NotNull public String getChatRelationColorNeutral() { return chatConfig.getRelationColorNeutral(); }
    @NotNull public String getChatRelationColorEnemy() { return chatConfig.getRelationColorEnemy(); }

    // Faction Chat (from chat module factionChat section)
    @NotNull public String getFactionChatColor() { return chatConfig.getFactionChatColor(); }
    @NotNull public String getFactionChatPrefix() { return chatConfig.getFactionChatPrefix(); }
    @NotNull public String getAllyChatColor() { return chatConfig.getAllyChatColor(); }
    @NotNull public String getAllyChatPrefix() { return chatConfig.getAllyChatPrefix(); }
    @NotNull public String getSenderNameColor() { return chatConfig.getSenderNameColor(); }
    @NotNull public String getMessageColor() { return chatConfig.getMessageColor(); }
    public boolean isChatHistoryEnabled() { return chatConfig.isHistoryEnabled(); }
    public int getChatHistoryMaxMessages() { return chatConfig.getHistoryMaxMessages(); }
    public int getChatHistoryRetentionDays() { return chatConfig.getHistoryRetentionDays(); }
    public int getChatHistoryCleanupIntervalMinutes() { return chatConfig.getHistoryCleanupIntervalMinutes(); }

    // Permissions
    public boolean isAdminRequiresOp() { return coreConfig.isAdminRequiresOp(); }
    public boolean isAllowWithoutPermissionMod() { return coreConfig.isAllowWithoutPermissionMod(); }

    // Faction Permissions (from module)
    @NotNull public FactionPermissions getDefaultFactionPermissions() {
        return factionPermissionsConfig.getDefaultFactionPermissions();
    }
    @NotNull public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
        return factionPermissionsConfig.getEffectiveFactionPermissions(factionPerms);
    }
    public boolean isPermissionLocked(@NotNull String permissionName) {
        return factionPermissionsConfig.isPermissionLocked(permissionName);
    }
}
