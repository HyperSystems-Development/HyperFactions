package com.hyperfactions.config;

import com.hyperfactions.config.modules.*;
import com.hyperfactions.config.modules.WorldMapConfig;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationRunner;
import com.hyperfactions.migration.MigrationType;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Central manager for all HyperFactions configuration.
 *
 * <p>
 * Orchestrates loading of the core config and all module configs,
 * handles migrations, and provides unified access to all settings.
 */
public class ConfigManager {

  private static ConfigManager instance;

  private Path dataDir;

  private CoreConfig coreConfig;

  private FactionsConfig factionsConfig;

  private ServerConfig serverConfig;

  private BackupConfig backupConfig;

  private ChatConfig chatConfig;

  private DebugConfig debugConfig;

  private EconomyConfig economyConfig;

  private FactionPermissionsConfig factionPermissionsConfig;

  private WorldMapConfig worldMapConfig;

  private AnnouncementConfig announcementConfig;

  private GravestoneConfig gravestoneConfig;

  private WorldsConfig worldsConfig;

  private final WorldSettingsResolver worldSettingsResolver = new WorldSettingsResolver();

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
   * Initializes ConfigManager with default values for all configs.
   * Uses a temporary directory so no files are actually read from or written to disk.
   * Intended for unit tests that need ConfigManager to be non-null.
   *
   * @return the initialized ConfigManager
   */
  @NotNull
  public static ConfigManager initTestDefaults() {
    ConfigManager cm = get();
    Path tempDir;
    try {
      tempDir = java.nio.file.Files.createTempDirectory("hf-test-config");
      tempDir.toFile().deleteOnExit();
    } catch (java.io.IOException e) {
      throw new RuntimeException("Failed to create temp config dir for tests", e);
    }
    cm.loadAll(tempDir);
    // In test environments without a permission mod, allow all user-level permissions
    cm.server().setAllowWithoutPermissionMod(true);
    return cm;
  }

  /**
   * Resets the singleton instance. For testing only.
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Loads all configuration files.
   *
   * <p>
   * This method:
   * <ol>
   *   <li>Runs any pending config migrations</li>
   *   <li>Loads factions + server configs (new structure)</li>
   *   <li>Falls back to legacy config.json if new files are missing</li>
   *   <li>Loads all module configs from config/ directory</li>
   * </ol>
   *
   * @param dataDir the plugin data directory
   */
  public void loadAll(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    Logger.debug("[Config] Loading configuration from: %s", dataDir.toAbsolutePath());

    // Step 1: Run pending migrations
    runMigrations();

    // Step 2: Load factions + server configs (post-V6 structure)
    Path configDir = dataDir.resolve("config");

    factionsConfig = new FactionsConfig(configDir.resolve("factions.json"));
    factionsConfig.load();

    serverConfig = new ServerConfig(configDir.resolve("server.json"));
    serverConfig.load();

    // Step 2b: Legacy fallback — if config.json still exists (migration incomplete),
    // load it and use its values to populate the new configs in-memory
    Path legacyConfigPath = dataDir.resolve("config.json");
    if (java.nio.file.Files.exists(legacyConfigPath)) {
      Logger.info("[Config] Legacy config.json found — loading as fallback");
      coreConfig = new CoreConfig(legacyConfigPath);
      coreConfig.load();
    }

    // Step 3: Load remaining module configs
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

    worldsConfig = new WorldsConfig(configDir.resolve("worlds.json"));
    worldsConfig.load();

    // Build the world settings resolver from loaded config
    worldSettingsResolver.rebuild(worldsConfig);

    // Step 4: Validate all configs and log any issues
    validateAll();

    Logger.debug("[Config] Configuration loaded successfully");
  }

  /**
   * Validates all configuration files and logs any issues found.
   *
   * <p>
   * This performs "soft" validation - invalid values are logged as warnings
   * and auto-corrected when possible, but the plugin will continue to function.
   */
  private void validateAll() {
    ValidationResult combined = new ValidationResult();

    // Validate each config and merge results
    factionsConfig.validateAndLog();
    if (factionsConfig.getLastValidationResult() != null) {
      combined.merge(factionsConfig.getLastValidationResult());
    }

    serverConfig.validateAndLog();
    if (serverConfig.getLastValidationResult() != null) {
      combined.merge(serverConfig.getLastValidationResult());
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

    worldsConfig.validateAndLog();
    if (worldsConfig.getLastValidationResult() != null) {
      combined.merge(worldsConfig.getLastValidationResult());
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
        ErrorHandler.report(String.format("[Config] Migration '%s' failed: %s",
            result.migrationId(), result.errorMessage()), (Exception) null);
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

    factionsConfig.reload();
    serverConfig.reload();
    backupConfig.reload();
    chatConfig.reload();
    debugConfig.reload();
    economyConfig.reload();
    factionPermissionsConfig.reload();
    worldMapConfig.reload();
    announcementConfig.reload();
    gravestoneConfig.reload();
    worldsConfig.reload();

    // Rebuild world settings resolver
    worldSettingsResolver.rebuild(worldsConfig);

    // Re-validate after reload
    validateAll();

    Logger.info("[Config] Configuration reloaded");
  }

  /**
   * Resets all configuration files to factory defaults and saves.
   */
  public void resetAllDefaults() {
    Logger.info("[Config] Resetting all configuration to defaults...");

    factionsConfig.resetDefaults();
    serverConfig.resetDefaults();
    backupConfig.resetDefaults();
    chatConfig.resetDefaults();
    debugConfig.resetDefaults();
    economyConfig.resetDefaults();
    factionPermissionsConfig.resetDefaults();
    worldMapConfig.resetDefaults();
    announcementConfig.resetDefaults();
    gravestoneConfig.resetDefaults();
    worldsConfig.resetDefaults();

    worldSettingsResolver.rebuild(worldsConfig);
    validateAll();

    Logger.info("[Config] Configuration reset to defaults");
  }

  /**
   * Saves all configuration files.
   */
  public void saveAll() {
    factionsConfig.save();
    serverConfig.save();
    backupConfig.save();
    chatConfig.save();
    debugConfig.save();
    economyConfig.save();
    factionPermissionsConfig.save();
    worldMapConfig.save();
    announcementConfig.save();
    gravestoneConfig.save();
    worldsConfig.save();
  }

  // === Config Accessors ===

  /**
   * Gets the factions gameplay configuration.
   *
   * @return factions config
   */
  @NotNull
  public FactionsConfig factions() {
    return factionsConfig;
  }

  /**
   * Gets the server behavior configuration.
   *
   * @return server config
   */
  @NotNull
  public ServerConfig server() {
    return serverConfig;
  }

  /**
   * Gets the legacy core configuration (for backward compatibility).
   *
   * @return core config, or null if config.json doesn't exist
   * @deprecated Use {@link #factions()} or {@link #server()} instead
   */
  @Deprecated(since = "0.4.0", forRemoval = true)
  @org.jetbrains.annotations.Nullable
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

  /**
   * Gets the worlds module configuration.
   *
   * @return worlds config
   */
  @NotNull
  public WorldsConfig worlds() {
    return worldsConfig;
  }

  /**
   * Gets the world settings resolver for per-world behavior queries.
   *
   * @return world settings resolver
   */
  @NotNull
  public WorldSettingsResolver getWorldSettingsResolver() {
    return worldSettingsResolver;
  }

  // === Convenience Methods ===

  // Roles (from factions config)
  /** Returns the role display name. */
  @NotNull public String getRoleDisplayName(@NotNull FactionRole role) {
    return factionsConfig.getRoleDisplayName(role);
  }

  /** Returns the role short name. */
  @NotNull public String getRoleShortName(@NotNull FactionRole role) {
    return factionsConfig.getRoleShortName(role);
  }

  // Faction (from factions config)
  /** Returns the max members. */
  public int getMaxMembers() {
    return factionsConfig.getMaxMembers();
  }

  public int getMaxMembershipHistory() {
    return factionsConfig.getMaxMembershipHistory();
  }

  /** Returns the max name length. */
  public int getMaxNameLength() {
    return factionsConfig.getMaxNameLength();
  }

  /** Returns the min name length. */
  public int getMinNameLength() {
    return factionsConfig.getMinNameLength();
  }

  /** Checks if allow colors. */
  public boolean isAllowColors() {
    return factionsConfig.isAllowColors();
  }

  // Power (from factions config)
  /** Returns the max player power. */
  public double getMaxPlayerPower() {
    return factionsConfig.getMaxPlayerPower();
  }

  /** Returns the starting power. */
  public double getStartingPower() {
    return factionsConfig.getStartingPower();
  }

  /** Returns the power per claim. */
  public double getPowerPerClaim() {
    return factionsConfig.getPowerPerClaim();
  }

  /** Returns the death penalty. */
  public double getDeathPenalty() {
    return factionsConfig.getDeathPenalty();
  }

  /** Returns the kill reward. */
  public double getKillReward() {
    return factionsConfig.getKillReward();
  }

  /** Checks if kill reward requires faction. */
  public boolean isKillRewardRequiresFaction() {
    return factionsConfig.isKillRewardRequiresFaction();
  }

  public boolean isPowerLossOnMobDeath() {
    return factionsConfig.isPowerLossOnMobDeath();
  }

  /** Checks if power loss on environmental death. */
  public boolean isPowerLossOnEnvironmentalDeath() {
    return factionsConfig.isPowerLossOnEnvironmentalDeath();
  }

  /** Returns the regen per minute. */
  public double getRegenPerMinute() {
    return factionsConfig.getRegenPerMinute();
  }

  public boolean isRegenWhenOffline() {
    return factionsConfig.isRegenWhenOffline();
  }

  /** Checks if hardcore mode. */
  public boolean isHardcoreMode() {
    return factionsConfig.isHardcoreMode();
  }

  // Claims (from factions config)
  /** Returns the max claims. */
  public int getMaxClaims() {
    return factionsConfig.getMaxClaims();
  }

  public boolean isOnlyAdjacent() {
    return factionsConfig.isOnlyAdjacent();
  }

  /** Checks if prevent disconnect. */
  public boolean isPreventDisconnect() {
    return factionsConfig.isPreventDisconnect();
  }

  /** Checks if decay enabled. */
  public boolean isDecayEnabled() {
    return factionsConfig.isDecayEnabled();
  }

  /** Returns the decay days inactive. */
  public int getDecayDaysInactive() {
    return factionsConfig.getDecayDaysInactive();
  }

  /** Returns the world whitelist. */
  @NotNull public List<String> getWorldWhitelist() {
    return factionsConfig.getWorldWhitelist();
  }

  /** Returns the world blacklist. */
  @NotNull public List<String> getWorldBlacklist() {
    return factionsConfig.getWorldBlacklist();
  }

  /** Checks if world allowed. */
  public boolean isWorldAllowed(@NotNull String worldName) {
    if (worldsConfig != null && worldsConfig.isEnabled()) {
      return worldSettingsResolver.isClaimingAllowed(worldName);
    }
    return factionsConfig.isWorldAllowed(worldName);
  }

  public int calculateMaxClaims(double totalPower) {
    return factionsConfig.calculateMaxClaims(totalPower);
  }

  // Claim protection overrides (from factions config)
  /**
   * Checks if explosions are allowed in claims (legacy combined check).
   * Returns true only if ALL explosion source types are allowed.
   *
   * @deprecated Use the individual explosion config methods instead
   */
  @Deprecated(since = "0.9.0", forRemoval = true)
  public boolean isAllowExplosionsInClaims() {
    return factionsConfig.isFactionlessExplosionsAllowed()
        && factionsConfig.isEnemyExplosionsAllowed()
        && factionsConfig.isNeutralExplosionsAllowed();
  }

  /** Checks if outsider pickup allowed. */
  public boolean isOutsiderPickupAllowed() {
    return factionsConfig.isOutsiderPickupAllowed();
  }

  /** Checks if outsider drop allowed. */
  public boolean isOutsiderDropAllowed() {
    return factionsConfig.isOutsiderDropAllowed();
  }

  /** Checks if factionless explosions allowed. */
  public boolean isFactionlessExplosionsAllowed() {
    return factionsConfig.isFactionlessExplosionsAllowed();
  }

  /** Checks if enemy explosions allowed. */
  public boolean isEnemyExplosionsAllowed() {
    return factionsConfig.isEnemyExplosionsAllowed();
  }

  /** Checks if neutral explosions allowed. */
  public boolean isNeutralExplosionsAllowed() {
    return factionsConfig.isNeutralExplosionsAllowed();
  }

  /** Checks if fire spread allowed. */
  public boolean isFireSpreadAllowed() {
    return factionsConfig.isFireSpreadAllowed();
  }

  /** Checks if factionless damage allowed. */
  public boolean isFactionlessDamageAllowed() {
    return factionsConfig.isFactionlessDamageAllowed();
  }

  /** Checks if enemy damage allowed. */
  public boolean isEnemyDamageAllowed() {
    return factionsConfig.isEnemyDamageAllowed();
  }

  /** Checks if neutral damage allowed. */
  public boolean isNeutralDamageAllowed() {
    return factionsConfig.isNeutralDamageAllowed();
  }

  // Per-world settings
  /** Checks if power loss enabled in world. */
  public boolean isPowerLossEnabledInWorld(@NotNull String worldName) {
    if (worldsConfig != null && worldsConfig.isEnabled()) {
      return worldSettingsResolver.isPowerLossEnabled(worldName);
    }
    return true;
  }

  // Combat (from factions config)
  /** Returns the tag duration seconds. */
  public int getTagDurationSeconds() {
    return factionsConfig.getTagDurationSeconds();
  }

  /** Checks if ally damage. */
  public boolean isAllyDamage() {
    return factionsConfig.isAllyDamage();
  }

  public boolean isFactionDamage() {
    return factionsConfig.isFactionDamage();
  }

  /** Checks if faction damage. */
  public boolean isFactionDamage(@org.jetbrains.annotations.Nullable String worldName) {
    if (worldName != null && worldsConfig != null && worldsConfig.isEnabled()) {
      Boolean override = worldSettingsResolver.isFriendlyFireFactionAllowed(worldName);
      if (override != null) {
        return override;
      }
    }
    return factionsConfig.isFactionDamage();
  }

  public boolean isAllyDamage(@org.jetbrains.annotations.Nullable String worldName) {
    if (worldName != null && worldsConfig != null && worldsConfig.isEnabled()) {
      Boolean override = worldSettingsResolver.isFriendlyFireAllyAllowed(worldName);
      if (override != null) {
        return override;
      }
    }
    return factionsConfig.isAllyDamage();
  }

  /** Checks if tagged logout penalty. */
  public boolean isTaggedLogoutPenalty() {
    return factionsConfig.isTaggedLogoutPenalty();
  }

  /** Returns the logout power loss. */
  public double getLogoutPowerLoss() {
    return factionsConfig.getLogoutPowerLoss();
  }

  public double getNeutralAttackPenalty() {
    return factionsConfig.getNeutralAttackPenalty();
  }

  // Spawn Protection (from factions config)
  /** Checks if spawn protection enabled. */
  public boolean isSpawnProtectionEnabled() {
    return factionsConfig.isSpawnProtectionEnabled();
  }

  /** Returns the spawn protection duration seconds. */
  public int getSpawnProtectionDurationSeconds() {
    return factionsConfig.getSpawnProtectionDurationSeconds();
  }

  public boolean isSpawnProtectionBreakOnAttack() {
    return factionsConfig.isSpawnProtectionBreakOnAttack();
  }

  /** Checks if spawn protection break on move. */
  public boolean isSpawnProtectionBreakOnMove() {
    return factionsConfig.isSpawnProtectionBreakOnMove();
  }

  // Relations (from factions config)
  /** Returns the max allies. */
  public int getMaxAllies() {
    return factionsConfig.getMaxAllies();
  }

  /** Returns the max enemies. */
  public int getMaxEnemies() {
    return factionsConfig.getMaxEnemies();
  }

  // Invites (from factions config)
  /** Returns the invite expiration minutes. */
  public int getInviteExpirationMinutes() {
    return factionsConfig.getInviteExpirationMinutes();
  }

  public int getJoinRequestExpirationHours() {
    return factionsConfig.getJoinRequestExpirationHours();
  }

  /** Returns the invite expiration ms. */
  public long getInviteExpirationMs() {
    return factionsConfig.getInviteExpirationMs();
  }

  /** Returns the join request expiration ms. */
  public long getJoinRequestExpirationMs() {
    return factionsConfig.getJoinRequestExpirationMs();
  }

  // Stuck (from factions config)
  /** Returns the stuck min radius. */
  public int getStuckMinRadius() {
    return factionsConfig.getStuckMinRadius();
  }

  /** Returns the stuck radius increase. */
  public int getStuckRadiusIncrease() {
    return factionsConfig.getStuckRadiusIncrease();
  }

  public int getStuckMaxAttempts() {
    return factionsConfig.getStuckMaxAttempts();
  }

  /** Returns the stuck warmup seconds. */
  public int getStuckWarmupSeconds() {
    return factionsConfig.getStuckWarmupSeconds();
  }

  /** Returns the stuck cooldown seconds. */
  public int getStuckCooldownSeconds() {
    return factionsConfig.getStuckCooldownSeconds();
  }

  // Teleport (from server config)
  /** Returns the warmup seconds. */
  public int getWarmupSeconds() {
    return serverConfig.getWarmupSeconds();
  }

  /** Returns the cooldown seconds. */
  public int getCooldownSeconds() {
    return serverConfig.getCooldownSeconds();
  }

  public boolean isCancelOnMove() {
    return serverConfig.isCancelOnMove();
  }

  /** Checks if cancel on damage. */
  public boolean isCancelOnDamage() {
    return serverConfig.isCancelOnDamage();
  }

  // Updates (from server config)
  /** Checks if update check enabled. */
  public boolean isUpdateCheckEnabled() {
    return serverConfig.isUpdateCheckEnabled();
  }

  /** Returns the update check url. */
  @NotNull public String getUpdateCheckUrl() {
    return serverConfig.getUpdateCheckUrl();
  }

  /** Returns the release channel. */
  @NotNull public String getReleaseChannel() {
    return serverConfig.getReleaseChannel();
  }

  public boolean isPreReleaseChannel() {
    return serverConfig.isPreReleaseChannel();
  }

  // Auto-save (from server config)
  /** Checks if auto save enabled. */
  public boolean isAutoSaveEnabled() {
    return serverConfig.isAutoSaveEnabled();
  }

  /** Returns the auto save interval minutes. */
  public int getAutoSaveIntervalMinutes() {
    return serverConfig.getAutoSaveIntervalMinutes();
  }

  // Mob clearing
  /** Whether periodic mob clearing is enabled. */
  public boolean isMobClearEnabled() {
    return serverConfig.isMobClearEnabled();
  }

  /** Returns the mob clear sweep interval in seconds. */
  public int getMobClearIntervalSeconds() {
    return serverConfig.getMobClearIntervalSeconds();
  }

  // Backup (from module)
  /** Checks if backup enabled. */
  public boolean isBackupEnabled() {
    return backupConfig.isEnabled();
  }

  /** Returns the backup hourly retention. */
  public int getBackupHourlyRetention() {
    return backupConfig.getHourlyRetention();
  }

  /** Returns the backup daily retention. */
  public int getBackupDailyRetention() {
    return backupConfig.getDailyRetention();
  }

  /** Returns the backup weekly retention. */
  public int getBackupWeeklyRetention() {
    return backupConfig.getWeeklyRetention();
  }

  /** Returns the backup manual retention. */
  public int getBackupManualRetention() {
    return backupConfig.getManualRetention();
  }

  /** Checks if backup on shutdown. */
  public boolean isBackupOnShutdown() {
    return backupConfig.isOnShutdown();
  }

  public int getBackupShutdownRetention() {
    return backupConfig.getShutdownRetention();
  }

  // Economy (from module)
  /** Checks if economy enabled. */
  public boolean isEconomyEnabled() {
    return economyConfig.isEnabled();
  }

  /** Returns the economy currency name. */
  @NotNull public String getEconomyCurrencyName() {
    return economyConfig.getCurrencyName();
  }

  /** Returns the economy currency name plural. */
  @NotNull public String getEconomyCurrencyNamePlural() {
    return economyConfig.getCurrencyNamePlural();
  }

  /** Returns the economy currency symbol. */
  @NotNull public String getEconomyCurrencySymbol() {
    return economyConfig.getCurrencySymbol();
  }

  /** Checks if the economy currency symbol should be placed on the right side. */
  public boolean isEconomyCurrencySymbolRight() {
    return economyConfig.isSymbolRight();
  }

  @NotNull public java.math.BigDecimal getEconomyStartingBalance() {
    return economyConfig.getStartingBalance();
  }

  /** Checks if economy disband refund to leader. */
  public boolean isEconomyDisbandRefundToLeader() {
    return economyConfig.isDisbandRefundToLeader();
  }

  @NotNull public com.hyperfactions.data.FactionEconomy.TreasuryLimits getDefaultTreasuryLimits() {
    return economyConfig.getDefaultTreasuryLimits();
  }

  @NotNull public com.hyperfactions.config.modules.EconomyConfig getEconomyConfig() {
    return economyConfig;
  }

  @NotNull public java.math.BigDecimal getDepositFeePercent() {
    return economyConfig.getDepositFeePercent();
  }

  @NotNull public java.math.BigDecimal getWithdrawFeePercent() {
    return economyConfig.getWithdrawFeePercent();
  }

  @NotNull public java.math.BigDecimal getTransferFeePercent() {
    return economyConfig.getTransferFeePercent();
  }

  /** Checks if upkeep enabled. */
  public boolean isUpkeepEnabled() {
    return economyConfig.isUpkeepEnabled();
  }

  @NotNull public java.math.BigDecimal getUpkeepCostPerChunk() {
    return economyConfig.getUpkeepCostPerChunk();
  }

  public int getUpkeepIntervalHours() {
    return economyConfig.getUpkeepIntervalHours();
  }

  /** Returns the upkeep grace period hours. */
  public int getUpkeepGracePeriodHours() {
    return economyConfig.getUpkeepGracePeriodHours();
  }

  /** Checks if upkeep auto pay default. */
  public boolean isUpkeepAutoPayDefault() {
    return economyConfig.isUpkeepAutoPayDefault();
  }

  /** Returns the number of chunks exempt from upkeep cost. */
  public int getUpkeepFreeChunks() {
    return economyConfig.getUpkeepFreeChunks();
  }

  /** Returns the number of claims lost per failed cycle after grace. */
  public int getUpkeepClaimLossPerCycle() {
    return economyConfig.getUpkeepClaimLossPerCycle();
  }

  /** Returns the hours before upkeep to warn online members. */
  public int getUpkeepWarningHours() {
    return economyConfig.getUpkeepWarningHours();
  }

  /** Returns the max cost cap per cycle (0 = unlimited). */
  @NotNull public java.math.BigDecimal getUpkeepMaxCostCap() {
    return economyConfig.getUpkeepMaxCostCap();
  }

  /** Returns the scaling mode ("flat" or "progressive"). */
  @NotNull public String getUpkeepScalingMode() {
    return economyConfig.getUpkeepScalingMode();
  }

  /** Returns the progressive scaling tiers. */
  @NotNull public java.util.List<com.hyperfactions.config.modules.EconomyConfig.ScalingTier> getUpkeepScalingTiers() {
    return economyConfig.getUpkeepScalingTiers();
  }

  // Messages (from server config)
  /** Returns the prefix text. */
  @NotNull public String getPrefixText() {
    return serverConfig.getPrefixText();
  }

  /** Returns the prefix color. */
  @NotNull public String getPrefixColor() {
    return serverConfig.getPrefixColor();
  }

  /** Returns the prefix bracket color. */
  @NotNull public String getPrefixBracketColor() {
    return serverConfig.getPrefixBracketColor();
  }

  /** Returns the primary color. */
  @NotNull public String getPrimaryColor() {
    return serverConfig.getPrimaryColor();
  }

  // GUI (from server config)
  @NotNull public String getGuiTitle() {
    return serverConfig.getGuiTitle();
  }

  /** Checks if terrain map enabled. */
  public boolean isTerrainMapEnabled() {
    return serverConfig.isTerrainMapEnabled();
  }

  // Territory Notifications (from announcements config)
  public boolean isTerritoryNotificationsEnabled() {
    return announcementConfig.isTerritoryNotificationsEnabled();
  }

  public boolean isWildernessOnLeaveZoneEnabled() {
    return announcementConfig.isWildernessOnLeaveZoneEnabled();
  }

  public String getWildernessOnLeaveZoneUpper() {
    return announcementConfig.getWildernessOnLeaveZoneUpper();
  }

  public String getWildernessOnLeaveZoneLower() {
    return announcementConfig.getWildernessOnLeaveZoneLower();
  }

  public boolean isWildernessOnLeaveClaimEnabled() {
    return announcementConfig.isWildernessOnLeaveClaimEnabled();
  }

  public String getWildernessOnLeaveClaimUpper() {
    return announcementConfig.getWildernessOnLeaveClaimUpper();
  }

  public String getWildernessOnLeaveClaimLower() {
    return announcementConfig.getWildernessOnLeaveClaimLower();
  }

  // World Map (from worldmap.json module config)
  public boolean isWorldMapMarkersEnabled() {
    return worldMapConfig.isEnabled();
  }

  // Debug (from module)
  /** Checks if debug enabled by default. */
  public boolean isDebugEnabledByDefault() {
    return debugConfig.isEnabledByDefault();
  }

  /** Checks if debug log to console. */
  public boolean isDebugLogToConsole() {
    return debugConfig.isLogToConsole();
  }

  /** Checks if debug power. */
  public boolean isDebugPower() {
    return debugConfig.isPower();
  }

  /** Checks if debug claim. */
  public boolean isDebugClaim() {
    return debugConfig.isClaim();
  }

  /** Checks if debug combat. */
  public boolean isDebugCombat() {
    return debugConfig.isCombat();
  }

  /** Checks if debug protection. */
  public boolean isDebugProtection() {
    return debugConfig.isProtection();
  }

  /** Checks if debug relation. */
  public boolean isDebugRelation() {
    return debugConfig.isRelation();
  }

  /** Checks if debug territory. */
  public boolean isDebugTerritory() {
    return debugConfig.isTerritory();
  }

  /** Checks if debug worldmap. */
  public boolean isDebugWorldmap() {
    return debugConfig.isWorldmap();
  }

  /** Checks if debug integration. */
  public boolean isDebugIntegration() {
    return debugConfig.isIntegration();
  }

  // Debug setters
  /** Sets the debug power. */
  public void setDebugPower(boolean enabled) {
    debugConfig.setPower(enabled);
  }

  /** Sets the debug claim. */
  public void setDebugClaim(boolean enabled) {
    debugConfig.setClaim(enabled);
  }

  /** Sets the debug combat. */
  public void setDebugCombat(boolean enabled) {
    debugConfig.setCombat(enabled);
  }

  public void setDebugProtection(boolean enabled) {
    debugConfig.setProtection(enabled);
  }

  public void setDebugRelation(boolean enabled) {
    debugConfig.setRelation(enabled);
  }

  /** Sets the debug territory. */
  public void setDebugTerritory(boolean enabled) {
    debugConfig.setTerritory(enabled);
  }

  /** Sets the debug worldmap. */
  public void setDebugWorldmap(boolean enabled) {
    debugConfig.setWorldmap(enabled);
  }

  public void setDebugIntegration(boolean enabled) {
    debugConfig.setIntegration(enabled);
  }

  /** Enables this component. */
  public void enableAllDebug() {
    debugConfig.enableAll();
  }

  /** Disables this component. */
  public void disableAllDebug() {
    debugConfig.disableAll();
  }

  /** Applies debug settings. */
  public void applyDebugSettings() {
    debugConfig.applyToLogger();
  }

  // Chat (from module)
  /** Checks if chat formatting enabled. */
  public boolean isChatFormattingEnabled() {
    return chatConfig.isEnabled();
  }

  /** Returns the chat format. */
  @NotNull public String getChatFormat() {
    return chatConfig.getFormat();
  }

  /** Returns the chat tag display. */
  @NotNull public String getChatTagDisplay() {
    return chatConfig.getTagDisplay();
  }

  /** Returns the chat tag format. */
  @NotNull public String getChatTagFormat() {
    return chatConfig.getTagFormat();
  }

  /** Returns the chat no faction tag. */
  @NotNull public String getChatNoFactionTag() {
    return chatConfig.getNoFactionTag();
  }

  /** Returns the chat no faction tag color. */
  @NotNull public String getChatNoFactionTagColor() {
    return chatConfig.getNoFactionTagColor();
  }

  @NotNull public String getChatPlayerNameColor() {
    return chatConfig.getPlayerNameColor();
  }

  /** Returns the chat event priority. */
  @NotNull public String getChatEventPriority() {
    return chatConfig.getPriority();
  }

  /** Returns the chat relation color own. */
  @NotNull public String getChatRelationColorOwn() {
    return chatConfig.getRelationColorOwn();
  }

  /** Returns the chat relation color ally. */
  @NotNull public String getChatRelationColorAlly() {
    return chatConfig.getRelationColorAlly();
  }

  @NotNull public String getChatRelationColorNeutral() {
    return chatConfig.getRelationColorNeutral();
  }

  /** Returns the chat relation color enemy. */
  @NotNull public String getChatRelationColorEnemy() {
    return chatConfig.getRelationColorEnemy();
  }

  // Faction Chat (from chat module factionChat section)
  @NotNull public String getFactionChatColor() {
    return chatConfig.getFactionChatColor();
  }

  /** Returns the faction chat prefix. */
  @NotNull public String getFactionChatPrefix() {
    return chatConfig.getFactionChatPrefix();
  }

  /** Returns the ally chat color. */
  @NotNull public String getAllyChatColor() {
    return chatConfig.getAllyChatColor();
  }

  /** Returns the ally chat prefix. */
  @NotNull public String getAllyChatPrefix() {
    return chatConfig.getAllyChatPrefix();
  }

  /** Returns the sender name color. */
  @NotNull public String getSenderNameColor() {
    return chatConfig.getSenderNameColor();
  }

  /** Returns the message color. */
  @NotNull public String getMessageColor() {
    return chatConfig.getMessageColor();
  }

  public boolean isChatHistoryEnabled() {
    return chatConfig.isHistoryEnabled();
  }

  /** Returns the chat history max messages. */
  public int getChatHistoryMaxMessages() {
    return chatConfig.getHistoryMaxMessages();
  }

  /** Returns the chat history retention days. */
  public int getChatHistoryRetentionDays() {
    return chatConfig.getHistoryRetentionDays();
  }

  /** Returns the chat history cleanup interval minutes. */
  public int getChatHistoryCleanupIntervalMinutes() {
    return chatConfig.getHistoryCleanupIntervalMinutes();
  }

  // Language / i18n (from server config)
  /** Returns the default server language code (e.g. "en-US"). */
  @NotNull public String getDefaultLanguage() {
    return serverConfig.getDefaultLanguage();
  }

  /** Whether to respect each player's client language for translations. */
  public boolean isUsePlayerLanguage() {
    return serverConfig.isUsePlayerLanguage();
  }

  // Permissions (from server config)
  public boolean isAdminRequiresOp() {
    return serverConfig.isAdminRequiresOp();
  }

  /** Checks if allow without permission mod. */
  public boolean isAllowWithoutPermissionMod() {
    return serverConfig.isAllowWithoutPermissionMod();
  }

  // Faction Permissions (from module)
  /** Returns the default faction permissions. */
  @NotNull public FactionPermissions getDefaultFactionPermissions() {
    return factionPermissionsConfig.getDefaultFactionPermissions();
  }

  @NotNull public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
    return factionPermissionsConfig.getEffectiveFactionPermissions(factionPerms);
  }

  /** Checks if permission locked. */
  public boolean isPermissionLocked(@NotNull String permissionName) {
    return factionPermissionsConfig.isPermissionLocked(permissionName);
  }
}
