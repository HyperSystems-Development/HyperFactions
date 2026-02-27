package com.hyperfactions.config;

import com.hyperfactions.data.FactionPermissions;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy configuration singleton for HyperFactions.
 *
 * <p>
 * <b>DEPRECATED:</b> This class is maintained for backward compatibility.
 * New code should use {@link ConfigManager} directly:
 * <pre>
 * // Instead of:
 * HyperFactionsConfig.get().getMaxMembers();
 *
 * <p>// Use:
 * ConfigManager.get().getMaxMembers();
 * // Or for more control:
 * ConfigManager.get().factions().getMaxMembers();
 * ConfigManager.get().server().isHyperProtectAutoUpdate();
 * ConfigManager.get().backup().isEnabled();
 * </pre>
 *
 * @deprecated Use {@link ConfigManager} instead. This class delegates all calls
 *             to ConfigManager and will be removed in a future version.
 */
@Deprecated(since = "0.4.0", forRemoval = true)
public class HyperFactionsConfig {

  private static HyperFactionsConfig instance;

  private HyperFactionsConfig() {}

  /**
   * Gets the singleton config instance.
   *
   * @return the config instance
   * @deprecated Use {@link ConfigManager#get()} instead
   */
  @Deprecated(since = "0.4.0", forRemoval = true)
  public static HyperFactionsConfig get() {
    if (instance == null) {
      instance = new HyperFactionsConfig();
    }
    return instance;
  }

  /**
   * Loads the configuration from file.
   *
   * @param dataDir the plugin data directory
   * @deprecated Use {@link ConfigManager#get()}.loadAll(dataDir) instead
   */
  @Deprecated(since = "0.4.0", forRemoval = true)
  public void load(@NotNull Path dataDir) {
    ConfigManager.get().loadAll(dataDir);
  }

  /**
   * Saves the configuration to file.
   *
   * @param dataDir the plugin data directory
   * @deprecated Use {@link ConfigManager#get()}.saveAll() instead
   */
  @Deprecated(since = "0.4.0", forRemoval = true)
  public void save(@NotNull Path dataDir) {
    ConfigManager.get().saveAll();
  }

  /**
   * Reloads the configuration.
   *
   * @param dataDir the plugin data directory
   * @deprecated Use {@link ConfigManager#get()}.reloadAll() instead
   */
  @Deprecated(since = "0.4.0", forRemoval = true)
  public void reload(@NotNull Path dataDir) {
    ConfigManager.get().reloadAll();
  }

  // === All getters delegate to ConfigManager ===

  // Faction settings
  /** Returns the max members. */
  public int getMaxMembers() {
    return ConfigManager.get().getMaxMembers();
  }

  /** Returns the max name length. */
  public int getMaxNameLength() {
    return ConfigManager.get().getMaxNameLength();
  }

  /** Returns the min name length. */
  public int getMinNameLength() {
    return ConfigManager.get().getMinNameLength();
  }

  public boolean isAllowColors() {
    return ConfigManager.get().isAllowColors();
  }

  // Power settings
  /** Returns the max player power. */
  public double getMaxPlayerPower() {
    return ConfigManager.get().getMaxPlayerPower();
  }

  /** Returns the starting power. */
  public double getStartingPower() {
    return ConfigManager.get().getStartingPower();
  }

  /** Returns the power per claim. */
  public double getPowerPerClaim() {
    return ConfigManager.get().getPowerPerClaim();
  }

  /** Returns the death penalty. */
  public double getDeathPenalty() {
    return ConfigManager.get().getDeathPenalty();
  }

  public double getRegenPerMinute() {
    return ConfigManager.get().getRegenPerMinute();
  }

  /** Checks if regen when offline. */
  public boolean isRegenWhenOffline() {
    return ConfigManager.get().isRegenWhenOffline();
  }

  // Claim settings
  /** Returns the max claims. */
  public int getMaxClaims() {
    return ConfigManager.get().getMaxClaims();
  }

  public boolean isOnlyAdjacent() {
    return ConfigManager.get().isOnlyAdjacent();
  }

  /** Checks if decay enabled. */
  public boolean isDecayEnabled() {
    return ConfigManager.get().isDecayEnabled();
  }

  public int getDecayDaysInactive() {
    return ConfigManager.get().getDecayDaysInactive();
  }

  /** Returns the world whitelist. */
  public List<String> getWorldWhitelist() {
    return ConfigManager.get().getWorldWhitelist();
  }

  /** Returns the world blacklist. */
  public List<String> getWorldBlacklist() {
    return ConfigManager.get().getWorldBlacklist();
  }

  // Combat settings
  /** Returns the tag duration seconds. */
  public int getTagDurationSeconds() {
    return ConfigManager.get().getTagDurationSeconds();
  }

  public boolean isAllyDamage() {
    return ConfigManager.get().isAllyDamage();
  }

  /** Checks if faction damage. */
  public boolean isFactionDamage() {
    return ConfigManager.get().isFactionDamage();
  }

  /** Checks if tagged logout penalty. */
  public boolean isTaggedLogoutPenalty() {
    return ConfigManager.get().isTaggedLogoutPenalty();
  }

  /** Returns the logout power loss. */
  public double getLogoutPowerLoss() {
    return ConfigManager.get().getLogoutPowerLoss();
  }

  // Spawn Protection settings
  /** Checks if spawn protection enabled. */
  public boolean isSpawnProtectionEnabled() {
    return ConfigManager.get().isSpawnProtectionEnabled();
  }

  public int getSpawnProtectionDurationSeconds() {
    return ConfigManager.get().getSpawnProtectionDurationSeconds();
  }

  /** Checks if spawn protection break on attack. */
  public boolean isSpawnProtectionBreakOnAttack() {
    return ConfigManager.get().isSpawnProtectionBreakOnAttack();
  }

  /** Checks if spawn protection break on move. */
  public boolean isSpawnProtectionBreakOnMove() {
    return ConfigManager.get().isSpawnProtectionBreakOnMove();
  }

  // Relation settings
  /** Returns the max allies. */
  public int getMaxAllies() {
    return ConfigManager.get().getMaxAllies();
  }

  public int getMaxEnemies() {
    return ConfigManager.get().getMaxEnemies();
  }

  // Invite/Request settings
  /** Returns the invite expiration minutes. */
  public int getInviteExpirationMinutes() {
    return ConfigManager.get().getInviteExpirationMinutes();
  }

  /** Returns the join request expiration hours. */
  public int getJoinRequestExpirationHours() {
    return ConfigManager.get().getJoinRequestExpirationHours();
  }

  public long getInviteExpirationMs() {
    return ConfigManager.get().getInviteExpirationMs();
  }

  /** Returns the join request expiration ms. */
  public long getJoinRequestExpirationMs() {
    return ConfigManager.get().getJoinRequestExpirationMs();
  }

  // Stuck settings
  /** Returns the stuck warmup seconds. */
  public int getStuckWarmupSeconds() {
    return ConfigManager.get().getStuckWarmupSeconds();
  }

  /** Returns the stuck cooldown seconds. */
  public int getStuckCooldownSeconds() {
    return ConfigManager.get().getStuckCooldownSeconds();
  }

  // Teleport settings
  /** Returns the warmup seconds. */
  public int getWarmupSeconds() {
    return ConfigManager.get().getWarmupSeconds();
  }

  public int getCooldownSeconds() {
    return ConfigManager.get().getCooldownSeconds();
  }

  /** Checks if cancel on move. */
  public boolean isCancelOnMove() {
    return ConfigManager.get().isCancelOnMove();
  }

  /** Checks if cancel on damage. */
  public boolean isCancelOnDamage() {
    return ConfigManager.get().isCancelOnDamage();
  }

  // Update settings
  /** Checks if update check enabled. */
  public boolean isUpdateCheckEnabled() {
    return ConfigManager.get().isUpdateCheckEnabled();
  }

  /** Returns the update check url. */
  public String getUpdateCheckUrl() {
    return ConfigManager.get().getUpdateCheckUrl();
  }

  public String getReleaseChannel() {
    return ConfigManager.get().getReleaseChannel();
  }

  /** Checks if pre release channel. */
  public boolean isPreReleaseChannel() {
    return ConfigManager.get().isPreReleaseChannel();
  }

  // Auto-save settings
  /** Checks if auto save enabled. */
  public boolean isAutoSaveEnabled() {
    return ConfigManager.get().isAutoSaveEnabled();
  }

  /** Returns the auto save interval minutes. */
  public int getAutoSaveIntervalMinutes() {
    return ConfigManager.get().getAutoSaveIntervalMinutes();
  }

  // Backup settings
  /** Checks if backup enabled. */
  public boolean isBackupEnabled() {
    return ConfigManager.get().isBackupEnabled();
  }

  /** Returns the backup hourly retention. */
  public int getBackupHourlyRetention() {
    return ConfigManager.get().getBackupHourlyRetention();
  }

  /** Returns the backup daily retention. */
  public int getBackupDailyRetention() {
    return ConfigManager.get().getBackupDailyRetention();
  }

  /** Returns the backup weekly retention. */
  public int getBackupWeeklyRetention() {
    return ConfigManager.get().getBackupWeeklyRetention();
  }

  /** Returns the backup manual retention. */
  public int getBackupManualRetention() {
    return ConfigManager.get().getBackupManualRetention();
  }

  /** Checks if backup on shutdown. */
  public boolean isBackupOnShutdown() {
    return ConfigManager.get().isBackupOnShutdown();
  }

  public int getBackupShutdownRetention() {
    return ConfigManager.get().getBackupShutdownRetention();
  }

  // Economy settings
  /** Checks if economy enabled. */
  public boolean isEconomyEnabled() {
    return ConfigManager.get().isEconomyEnabled();
  }

  /** Returns the economy currency name. */
  public String getEconomyCurrencyName() {
    return ConfigManager.get().getEconomyCurrencyName();
  }

  /** Returns the economy currency name plural. */
  public String getEconomyCurrencyNamePlural() {
    return ConfigManager.get().getEconomyCurrencyNamePlural();
  }

  /** Returns the economy currency symbol. */
  public String getEconomyCurrencySymbol() {
    return ConfigManager.get().getEconomyCurrencySymbol();
  }

  public java.math.BigDecimal getEconomyStartingBalance() {
    return ConfigManager.get().getEconomyStartingBalance();
  }

  // Message settings (v3: structured prefix)
  /** Returns the prefix text. */
  public String getPrefixText() {
    return ConfigManager.get().getPrefixText();
  }

  /** Returns the prefix color. */
  public String getPrefixColor() {
    return ConfigManager.get().getPrefixColor();
  }

  public String getPrefixBracketColor() {
    return ConfigManager.get().getPrefixBracketColor();
  }

  /** Returns the primary color. */
  public String getPrimaryColor() {
    return ConfigManager.get().getPrimaryColor();
  }

  // GUI settings
  public String getGuiTitle() {
    return ConfigManager.get().getGuiTitle();
  }

  // Territory notification settings
  /** Checks if territory notifications enabled. */
  public boolean isTerritoryNotificationsEnabled() {
    return ConfigManager.get().isTerritoryNotificationsEnabled();
  }

  // World map marker settings
  /** Checks if world map markers enabled. */
  public boolean isWorldMapMarkersEnabled() {
    return ConfigManager.get().isWorldMapMarkersEnabled();
  }

  // Debug settings
  /** Checks if debug enabled by default. */
  public boolean isDebugEnabledByDefault() {
    return ConfigManager.get().isDebugEnabledByDefault();
  }

  /** Checks if debug log to console. */
  public boolean isDebugLogToConsole() {
    return ConfigManager.get().isDebugLogToConsole();
  }

  public boolean isDebugPower() {
    return ConfigManager.get().isDebugPower();
  }

  /** Checks if debug claim. */
  public boolean isDebugClaim() {
    return ConfigManager.get().isDebugClaim();
  }

  /** Checks if debug combat. */
  public boolean isDebugCombat() {
    return ConfigManager.get().isDebugCombat();
  }

  public boolean isDebugProtection() {
    return ConfigManager.get().isDebugProtection();
  }

  /** Checks if debug relation. */
  public boolean isDebugRelation() {
    return ConfigManager.get().isDebugRelation();
  }

  /** Checks if debug territory. */
  public boolean isDebugTerritory() {
    return ConfigManager.get().isDebugTerritory();
  }

  // Chat formatting settings
  public boolean isChatFormattingEnabled() {
    return ConfigManager.get().isChatFormattingEnabled();
  }

  /** Returns the chat format. */
  public String getChatFormat() {
    return ConfigManager.get().getChatFormat();
  }

  /** Returns the chat tag display. */
  public String getChatTagDisplay() {
    return ConfigManager.get().getChatTagDisplay();
  }

  /** Returns the chat tag format. */
  public String getChatTagFormat() {
    return ConfigManager.get().getChatTagFormat();
  }

  /** Returns the chat relation color own. */
  public String getChatRelationColorOwn() {
    return ConfigManager.get().getChatRelationColorOwn();
  }

  /** Returns the chat relation color ally. */
  public String getChatRelationColorAlly() {
    return ConfigManager.get().getChatRelationColorAlly();
  }

  /** Returns the chat relation color neutral. */
  public String getChatRelationColorNeutral() {
    return ConfigManager.get().getChatRelationColorNeutral();
  }

  /** Returns the chat relation color enemy. */
  public String getChatRelationColorEnemy() {
    return ConfigManager.get().getChatRelationColorEnemy();
  }

  /** Returns the chat no faction tag. */
  public String getChatNoFactionTag() {
    return ConfigManager.get().getChatNoFactionTag();
  }

  public String getChatEventPriority() {
    return ConfigManager.get().getChatEventPriority();
  }

  // Permission settings
  /** Checks if admin requires op. */
  public boolean isAdminRequiresOp() {
    return ConfigManager.get().isAdminRequiresOp();
  }

  /** Checks if allow without permission mod. */
  public boolean isAllowWithoutPermissionMod() {
    return ConfigManager.get().isAllowWithoutPermissionMod();
  }

  // Faction permission methods
  /** Returns the default faction permissions. */
  @NotNull
  public FactionPermissions getDefaultFactionPermissions() {
    return ConfigManager.get().getDefaultFactionPermissions();
  }

  /** Returns the effective faction permissions. */
  @NotNull
  public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
    return ConfigManager.get().getEffectiveFactionPermissions(factionPerms);
  }

  /** Checks if permission locked. */
  public boolean isPermissionLocked(@NotNull String permissionName) {
    return ConfigManager.get().isPermissionLocked(permissionName);
  }

  // Debug setters
  /** Sets the debug power. */
  public void setDebugPower(boolean enabled) {
    ConfigManager.get().setDebugPower(enabled);
  }

  public void setDebugClaim(boolean enabled) {
    ConfigManager.get().setDebugClaim(enabled);
  }

  /** Sets the debug combat. */
  public void setDebugCombat(boolean enabled) {
    ConfigManager.get().setDebugCombat(enabled);
  }

  /** Sets the debug protection. */
  public void setDebugProtection(boolean enabled) {
    ConfigManager.get().setDebugProtection(enabled);
  }

  /** Sets the debug relation. */
  public void setDebugRelation(boolean enabled) {
    ConfigManager.get().setDebugRelation(enabled);
  }

  public void setDebugTerritory(boolean enabled) {
    ConfigManager.get().setDebugTerritory(enabled);
  }

  public void applyDebugSettings() {
    ConfigManager.get().applyDebugSettings();
  }

  /** Enables this component. */
  public void enableAllDebug() {
    ConfigManager.get().enableAllDebug();
  }

  /** Disables this component. */
  public void disableAllDebug() {
    ConfigManager.get().disableAllDebug();
  }

  // Utility methods
  /** Checks if world allowed. */
  public boolean isWorldAllowed(@NotNull String worldName) {
    return ConfigManager.get().isWorldAllowed(worldName);
  }

  /** Calculate Max Claims. */
  public int calculateMaxClaims(double totalPower) {
    return ConfigManager.get().calculateMaxClaims(totalPower);
  }
}
