package com.hyperfactions.gui.admin;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.*;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.math.BigDecimal;

/**
 * Helper for the admin config editor GUI.
 *
 * <p>
 * Provides methods to apply setting changes by key to the appropriate
 * config class, using the setters on each module config.
 */
public final class ConfigSnapshot {

  private ConfigSnapshot() {}

  /**
   * The type of a config setting for UI rendering.
   */
  public enum SettingType {
    BOOLEAN,
    INT,
    DOUBLE,
    STRING,
    COLOR
  }

  /**
   * Applies a changed value to the appropriate config field.
   *
   * @param key   the dotted setting key (e.g. "server.warmupSeconds")
   * @param value the new value
   */
  public static void applyChange(String key, Object value) {
    ConfigManager cfg = ConfigManager.get();
    try {
      switch (key) {
        // === ServerConfig ===
        case "server.warmupSeconds" -> cfg.server().setWarmupSeconds(toInt(value));
        case "server.cooldownSeconds" -> cfg.server().setCooldownSeconds(toInt(value));
        case "server.cancelOnMove" -> cfg.server().setCancelOnMove(toBool(value));
        case "server.cancelOnDamage" -> cfg.server().setCancelOnDamage(toBool(value));
        case "server.autoSaveEnabled" -> cfg.server().setAutoSaveEnabled(toBool(value));
        case "server.autoSaveIntervalMinutes" -> cfg.server().setAutoSaveIntervalMinutes(toInt(value));
        case "server.prefixText" -> cfg.server().setPrefixText(toStr(value));
        case "server.prefixColor" -> cfg.server().setPrefixColor(toStr(value));
        case "server.prefixBracketColor" -> cfg.server().setPrefixBracketColor(toStr(value));
        case "server.primaryColor" -> cfg.server().setPrimaryColor(toStr(value));
        case "server.guiTitle" -> cfg.server().setGuiTitle(toStr(value));
        case "server.terrainMapEnabled" -> cfg.server().setTerrainMapEnabled(toBool(value));
        case "server.leaderboardKdRefreshSeconds" -> cfg.server().setLeaderboardKdRefreshSeconds(toInt(value));
        case "server.adminRequiresOp" -> cfg.server().setAdminRequiresOp(toBool(value));
        case "server.allowWithoutPermissionMod" -> cfg.server().setAllowWithoutPermissionMod(toBool(value));
        case "server.updateCheckEnabled" -> cfg.server().setUpdateCheckEnabled(toBool(value));
        case "server.releaseChannel" -> cfg.server().setReleaseChannel(toStr(value));
        case "server.mobClearEnabled" -> cfg.server().setMobClearEnabled(toBool(value));
        case "server.mobClearIntervalSeconds" -> cfg.server().setMobClearIntervalSeconds(toInt(value));
        case "server.defaultLanguage" -> cfg.server().setDefaultLanguage(toStr(value));
        case "server.usePlayerLanguage" -> cfg.server().setUsePlayerLanguage(toBool(value));
        case "server.hyperProtectAutoDownload" -> cfg.server().setHyperProtectAutoDownload(toBool(value));
        case "server.hyperProtectAutoUpdate" -> cfg.server().setHyperProtectAutoUpdate(toBool(value));

        // === FactionsConfig ===
        case "factions.maxMembers" -> cfg.factions().setMaxMembers(toInt(value));
        case "factions.maxNameLength" -> cfg.factions().setMaxNameLength(toInt(value));
        case "factions.minNameLength" -> cfg.factions().setMinNameLength(toInt(value));
        case "factions.allowColors" -> cfg.factions().setAllowColors(toBool(value));
        case "factions.maxMembershipHistory" -> cfg.factions().setMaxMembershipHistory(toInt(value));
        case "factions.maxPlayerPower" -> cfg.factions().setMaxPlayerPower(toDouble(value));
        case "factions.startingPower" -> cfg.factions().setStartingPower(toDouble(value));
        case "factions.powerPerClaim" -> cfg.factions().setPowerPerClaim(toDouble(value));
        case "factions.deathPenalty" -> cfg.factions().setDeathPenalty(toDouble(value));
        case "factions.killReward" -> cfg.factions().setKillReward(toDouble(value));
        case "factions.killRewardRequiresFaction" -> cfg.factions().setKillRewardRequiresFaction(toBool(value));
        case "factions.powerLossOnMobDeath" -> cfg.factions().setPowerLossOnMobDeath(toBool(value));
        case "factions.powerLossOnEnvironmentalDeath" -> cfg.factions().setPowerLossOnEnvironmentalDeath(toBool(value));
        case "factions.regenPerMinute" -> cfg.factions().setRegenPerMinute(toDouble(value));
        case "factions.regenWhenOffline" -> cfg.factions().setRegenWhenOffline(toBool(value));
        case "factions.hardcoreMode" -> cfg.factions().setHardcoreMode(toBool(value));
        case "factions.maxClaims" -> cfg.factions().setMaxClaims(toInt(value));
        case "factions.onlyAdjacent" -> cfg.factions().setOnlyAdjacent(toBool(value));
        case "factions.preventDisconnect" -> cfg.factions().setPreventDisconnect(toBool(value));
        case "factions.decayEnabled" -> cfg.factions().setDecayEnabled(toBool(value));
        case "factions.decayDaysInactive" -> cfg.factions().setDecayDaysInactive(toInt(value));
        case "factions.decayClaimsPerCycle" -> cfg.factions().setDecayClaimsPerCycle(toInt(value));
        case "factions.outsiderPickupAllowed" -> cfg.factions().setOutsiderPickupAllowed(toBool(value));
        case "factions.outsiderDropAllowed" -> cfg.factions().setOutsiderDropAllowed(toBool(value));
        case "factions.factionlessExplosionsAllowed" -> cfg.factions().setFactionlessExplosionsAllowed(toBool(value));
        case "factions.enemyExplosionsAllowed" -> cfg.factions().setEnemyExplosionsAllowed(toBool(value));
        case "factions.neutralExplosionsAllowed" -> cfg.factions().setNeutralExplosionsAllowed(toBool(value));
        case "factions.fireSpreadAllowed" -> cfg.factions().setFireSpreadAllowed(toBool(value));
        case "factions.factionlessDamageAllowed" -> cfg.factions().setFactionlessDamageAllowed(toBool(value));
        case "factions.enemyDamageAllowed" -> cfg.factions().setEnemyDamageAllowed(toBool(value));
        case "factions.neutralDamageAllowed" -> cfg.factions().setNeutralDamageAllowed(toBool(value));
        case "factions.tagDurationSeconds" -> cfg.factions().setTagDurationSeconds(toInt(value));
        case "factions.allyDamage" -> cfg.factions().setAllyDamage(toBool(value));
        case "factions.factionDamage" -> cfg.factions().setFactionDamage(toBool(value));
        case "factions.taggedLogoutPenalty" -> cfg.factions().setTaggedLogoutPenalty(toBool(value));
        case "factions.logoutPowerLoss" -> cfg.factions().setLogoutPowerLoss(toDouble(value));
        case "factions.neutralAttackPenalty" -> cfg.factions().setNeutralAttackPenalty(toDouble(value));
        case "factions.spawnProtectionEnabled" -> cfg.factions().setSpawnProtectionEnabled(toBool(value));
        case "factions.spawnProtectionDurationSeconds" -> cfg.factions().setSpawnProtectionDurationSeconds(toInt(value));
        case "factions.spawnProtectionBreakOnAttack" -> cfg.factions().setSpawnProtectionBreakOnAttack(toBool(value));
        case "factions.spawnProtectionBreakOnMove" -> cfg.factions().setSpawnProtectionBreakOnMove(toBool(value));
        case "factions.maxAllies" -> cfg.factions().setMaxAllies(toInt(value));
        case "factions.maxEnemies" -> cfg.factions().setMaxEnemies(toInt(value));
        case "factions.inviteExpirationMinutes" -> cfg.factions().setInviteExpirationMinutes(toInt(value));
        case "factions.joinRequestExpirationHours" -> cfg.factions().setJoinRequestExpirationHours(toInt(value));
        case "factions.stuckMinRadius" -> cfg.factions().setStuckMinRadius(toInt(value));
        case "factions.stuckRadiusIncrease" -> cfg.factions().setStuckRadiusIncrease(toInt(value));
        case "factions.stuckMaxAttempts" -> cfg.factions().setStuckMaxAttempts(toInt(value));
        case "factions.stuckWarmupSeconds" -> cfg.factions().setStuckWarmupSeconds(toInt(value));
        case "factions.stuckCooldownSeconds" -> cfg.factions().setStuckCooldownSeconds(toInt(value));

        // === BackupConfig ===
        case "backup.enabled" -> cfg.backup().setEnabled(toBool(value));
        case "backup.hourlyRetention" -> cfg.backup().setHourlyRetention(toInt(value));
        case "backup.dailyRetention" -> cfg.backup().setDailyRetention(toInt(value));
        case "backup.weeklyRetention" -> cfg.backup().setWeeklyRetention(toInt(value));
        case "backup.manualRetention" -> cfg.backup().setManualRetention(toInt(value));
        case "backup.onShutdown" -> cfg.backup().setOnShutdown(toBool(value));
        case "backup.shutdownRetention" -> cfg.backup().setShutdownRetention(toInt(value));

        // === ChatConfig ===
        case "chat.enabled" -> cfg.chat().setEnabled(toBool(value));
        case "chat.format" -> cfg.chat().setFormat(toStr(value));
        case "chat.tagDisplay" -> cfg.chat().setTagDisplay(toStr(value));
        case "chat.tagFormat" -> cfg.chat().setTagFormat(toStr(value));
        case "chat.noFactionTag" -> cfg.chat().setNoFactionTag(toStr(value));
        case "chat.noFactionTagColor" -> cfg.chat().setNoFactionTagColor(toStr(value));
        case "chat.playerNameColor" -> cfg.chat().setPlayerNameColor(toStr(value));
        case "chat.relationColorOwn" -> cfg.chat().setRelationColorOwn(toStr(value));
        case "chat.relationColorAlly" -> cfg.chat().setRelationColorAlly(toStr(value));
        case "chat.relationColorNeutral" -> cfg.chat().setRelationColorNeutral(toStr(value));
        case "chat.relationColorEnemy" -> cfg.chat().setRelationColorEnemy(toStr(value));
        case "chat.factionChatColor" -> cfg.chat().setFactionChatColor(toStr(value));
        case "chat.factionChatPrefix" -> cfg.chat().setFactionChatPrefix(toStr(value));
        case "chat.allyChatColor" -> cfg.chat().setAllyChatColor(toStr(value));
        case "chat.allyChatPrefix" -> cfg.chat().setAllyChatPrefix(toStr(value));
        case "chat.senderNameColor" -> cfg.chat().setSenderNameColor(toStr(value));
        case "chat.messageColor" -> cfg.chat().setMessageColor(toStr(value));
        case "chat.historyEnabled" -> cfg.chat().setHistoryEnabled(toBool(value));
        case "chat.historyMaxMessages" -> cfg.chat().setHistoryMaxMessages(toInt(value));
        case "chat.historyRetentionDays" -> cfg.chat().setHistoryRetentionDays(toInt(value));
        case "chat.historyCleanupIntervalMinutes" -> cfg.chat().setHistoryCleanupIntervalMinutes(toInt(value));

        // === AnnouncementConfig ===
        case "announce.territoryNotificationsEnabled" -> cfg.announcements().setTerritoryNotificationsEnabled(toBool(value));
        case "announce.factionCreated" -> cfg.announcements().setFactionCreated(toBool(value));
        case "announce.factionDisbanded" -> cfg.announcements().setFactionDisbanded(toBool(value));
        case "announce.leadershipTransfer" -> cfg.announcements().setLeadershipTransfer(toBool(value));
        case "announce.overclaim" -> cfg.announcements().setOverclaim(toBool(value));
        case "announce.warDeclared" -> cfg.announcements().setWarDeclared(toBool(value));
        case "announce.allianceFormed" -> cfg.announcements().setAllianceFormed(toBool(value));
        case "announce.allianceBroken" -> cfg.announcements().setAllianceBroken(toBool(value));
        case "announce.wildernessOnLeaveZoneEnabled" -> cfg.announcements().setWildernessOnLeaveZoneEnabled(toBool(value));
        case "announce.wildernessOnLeaveZoneUpper" -> cfg.announcements().setWildernessOnLeaveZoneUpper(toStr(value));
        case "announce.wildernessOnLeaveZoneLower" -> cfg.announcements().setWildernessOnLeaveZoneLower(toStr(value));
        case "announce.wildernessOnLeaveClaimEnabled" -> cfg.announcements().setWildernessOnLeaveClaimEnabled(toBool(value));
        case "announce.wildernessOnLeaveClaimUpper" -> cfg.announcements().setWildernessOnLeaveClaimUpper(toStr(value));
        case "announce.wildernessOnLeaveClaimLower" -> cfg.announcements().setWildernessOnLeaveClaimLower(toStr(value));

        // === EconomyConfig ===
        case "economy.enabled" -> cfg.economy().setEnabled(toBool(value));
        case "economy.currencyName" -> cfg.economy().setCurrencyName(toStr(value));
        case "economy.currencyNamePlural" -> cfg.economy().setCurrencyNamePlural(toStr(value));
        case "economy.currencySymbol" -> cfg.economy().setCurrencySymbol(toStr(value));
        case "economy.currencySymbolPosition" -> cfg.economy().setCurrencySymbolPosition(toStr(value));
        case "economy.startingBalance" -> cfg.economy().setStartingBalance(toBigDecimal(value));
        case "economy.disbandRefundToLeader" -> cfg.economy().setDisbandRefundToLeader(toBool(value));
        case "economy.defaultMaxWithdrawAmount" -> cfg.economy().setDefaultMaxWithdrawAmount(toBigDecimal(value));
        case "economy.defaultMaxWithdrawPerPeriod" -> cfg.economy().setDefaultMaxWithdrawPerPeriod(toBigDecimal(value));
        case "economy.defaultMaxTransferAmount" -> cfg.economy().setDefaultMaxTransferAmount(toBigDecimal(value));
        case "economy.defaultMaxTransferPerPeriod" -> cfg.economy().setDefaultMaxTransferPerPeriod(toBigDecimal(value));
        case "economy.defaultLimitPeriodHours" -> cfg.economy().setDefaultLimitPeriodHours(toInt(value));
        case "economy.depositFeePercent" -> cfg.economy().setDepositFeePercent(toBigDecimal(value));
        case "economy.withdrawFeePercent" -> cfg.economy().setWithdrawFeePercent(toBigDecimal(value));
        case "economy.transferFeePercent" -> cfg.economy().setTransferFeePercent(toBigDecimal(value));
        case "economy.upkeepEnabled" -> cfg.economy().setUpkeepEnabled(toBool(value));
        case "economy.upkeepCostPerChunk" -> cfg.economy().setUpkeepCostPerChunk(toBigDecimal(value));
        case "economy.upkeepIntervalHours" -> cfg.economy().setUpkeepIntervalHours(toInt(value));
        case "economy.upkeepGracePeriodHours" -> cfg.economy().setUpkeepGracePeriodHours(toInt(value));
        case "economy.upkeepAutoPayDefault" -> cfg.economy().setUpkeepAutoPayDefault(toBool(value));
        case "economy.upkeepFreeChunks" -> cfg.economy().setUpkeepFreeChunks(toInt(value));
        case "economy.upkeepClaimLossPerCycle" -> cfg.economy().setUpkeepClaimLossPerCycle(toInt(value));
        case "economy.upkeepWarningHours" -> cfg.economy().setUpkeepWarningHours(toInt(value));
        case "economy.upkeepMaxCostCap" -> cfg.economy().setUpkeepMaxCostCap(toBigDecimal(value));
        case "economy.upkeepScalingMode" -> cfg.economy().setUpkeepScalingMode(toStr(value));

        // === WorldMapConfig ===
        case "worldmap.enabled" -> cfg.worldMap().setEnabled(toBool(value));
        case "worldmap.refreshMode" -> cfg.worldMap().setRefreshMode(WorldMapConfig.RefreshMode.fromString(toStr(value)));
        case "worldmap.showFactionTags" -> cfg.worldMap().setShowFactionTags(toBool(value));
        case "worldmap.playerVisibilityEnabled" -> cfg.worldMap().setPlayerVisibilityEnabled(toBool(value));
        case "worldmap.showOwnFaction" -> cfg.worldMap().setShowOwnFaction(toBool(value));
        case "worldmap.showAllies" -> cfg.worldMap().setShowAllies(toBool(value));
        case "worldmap.showNeutrals" -> cfg.worldMap().setShowNeutrals(toBool(value));
        case "worldmap.showEnemies" -> cfg.worldMap().setShowEnemies(toBool(value));
        case "worldmap.showFactionlessPlayers" -> cfg.worldMap().setShowFactionlessPlayers(toBool(value));
        case "worldmap.showFactionlessToFactionless" -> cfg.worldMap().setShowFactionlessToFactionless(toBool(value));
        case "worldmap.autoFallbackOnError" -> cfg.worldMap().setAutoFallbackOnError(toBool(value));
        case "worldmap.proximityChunkRadius" -> cfg.worldMap().setProximityChunkRadius(toInt(value));
        case "worldmap.proximityBatchIntervalTicks" -> cfg.worldMap().setProximityBatchIntervalTicks(toInt(value));
        case "worldmap.proximityMaxChunksPerBatch" -> cfg.worldMap().setProximityMaxChunksPerBatch(toInt(value));
        case "worldmap.incrementalBatchIntervalTicks" -> cfg.worldMap().setIncrementalBatchIntervalTicks(toInt(value));
        case "worldmap.incrementalMaxChunksPerBatch" -> cfg.worldMap().setIncrementalMaxChunksPerBatch(toInt(value));
        case "worldmap.debouncedDelaySeconds" -> cfg.worldMap().setDebouncedDelaySeconds(toInt(value));
        case "worldmap.factionWideRefreshThreshold" -> cfg.worldMap().setFactionWideRefreshThreshold(toInt(value));
        case "worldmap.respectWorldConfig" -> cfg.worldMap().setRespectWorldConfig(toBool(value));
        case "worldmap.betterMapCompat" -> cfg.worldMap().setBetterMapCompat(toStr(value));
        case "worldmap.overrideDefaultScale" -> cfg.worldMap().setOverrideDefaultScale(toInt(value));
        case "worldmap.overrideMinScale" -> cfg.worldMap().setOverrideMinScale(toInt(value));
        case "worldmap.overrideMaxScale" -> cfg.worldMap().setOverrideMaxScale(toInt(value));
        case "worldmap.overrideImageScale" -> cfg.worldMap().setOverrideImageScale(toInt(value));
        case "worldmap.overrideAllowTeleportToCoordinates" -> cfg.worldMap().setOverrideAllowTeleportToCoordinates(toStr(value));
        case "worldmap.overrideAllowTeleportToMarkers" -> cfg.worldMap().setOverrideAllowTeleportToMarkers(toStr(value));
        case "worldmap.overrideAllowCreatingMapMarkers" -> cfg.worldMap().setOverrideAllowCreatingMapMarkers(toStr(value));

        // === DebugConfig ===
        case "debug.enabledByDefault" -> cfg.debug().setEnabledByDefault(toBool(value));
        case "debug.logToConsole" -> cfg.debug().setLogToConsole(toBool(value));
        case "debug.power" -> cfg.debug().setPower(toBool(value));
        case "debug.claim" -> cfg.debug().setClaim(toBool(value));
        case "debug.combat" -> cfg.debug().setCombat(toBool(value));
        case "debug.protection" -> cfg.debug().setProtection(toBool(value));
        case "debug.relation" -> cfg.debug().setRelation(toBool(value));
        case "debug.territory" -> cfg.debug().setTerritory(toBool(value));
        case "debug.worldmap" -> cfg.debug().setWorldmap(toBool(value));
        case "debug.interaction" -> cfg.debug().setInteraction(toBool(value));
        case "debug.mixin" -> cfg.debug().setMixin(toBool(value));
        case "debug.spawning" -> cfg.debug().setSpawning(toBool(value));
        case "debug.integration" -> cfg.debug().setIntegration(toBool(value));
        case "debug.economy" -> cfg.debug().setEconomy(toBool(value));
        case "debug.sentryEnabled" -> cfg.debug().setSentryEnabled(toBool(value));
        case "debug.sentryDebug" -> cfg.debug().setSentryDebug(toBool(value));
        case "debug.sentryTracesSampleRate" -> cfg.debug().setSentryTracesSampleRate(toDouble(value));

        // === GravestoneConfig ===
        case "gravestone.protectInOwnTerritory" -> cfg.gravestones().setProtectInOwnTerritory(toBool(value));
        case "gravestone.factionMembersCanAccess" -> cfg.gravestones().setFactionMembersCanAccess(toBool(value));
        case "gravestone.alliesCanAccess" -> cfg.gravestones().setAlliesCanAccess(toBool(value));
        case "gravestone.protectInSafeZone" -> cfg.gravestones().setProtectInSafeZone(toBool(value));
        case "gravestone.protectInWarZone" -> cfg.gravestones().setProtectInWarZone(toBool(value));
        case "gravestone.protectInWilderness" -> cfg.gravestones().setProtectInWilderness(toBool(value));
        case "gravestone.announceDeathLocation" -> cfg.gravestones().setAnnounceDeathLocation(toBool(value));
        case "gravestone.protectInEnemyTerritory" -> cfg.gravestones().setProtectInEnemyTerritory(toBool(value));
        case "gravestone.protectInNeutralTerritory" -> cfg.gravestones().setProtectInNeutralTerritory(toBool(value));
        case "gravestone.enemiesCanLootInOwnTerritory" -> cfg.gravestones().setEnemiesCanLootInOwnTerritory(toBool(value));
        case "gravestone.allowLootDuringRaid" -> cfg.gravestones().setAllowLootDuringRaid(toBool(value));
        case "gravestone.allowLootDuringWar" -> cfg.gravestones().setAllowLootDuringWar(toBool(value));

        // === WorldsConfig ===
        case "worlds.defaultPolicy" -> cfg.worlds().setDefaultPolicy(toStr(value));

        // === FactionPermissionsConfig ===
        default -> {
          if (key.startsWith("facperm.default.")) {
            String flag = key.substring("facperm.default.".length());
            cfg.factionPermissions().setDefault(flag, toBool(value));
          } else if (key.startsWith("facperm.lock.")) {
            String flag = key.substring("facperm.lock.".length());
            cfg.factionPermissions().setLocked(flag, toBool(value));
          } else {
            Logger.warn("[ConfigEditor] Unknown setting key: %s", key);
          }
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[ConfigEditor] Failed to apply change for key '" + key + "'", e);
    }
  }

  /**
   * Returns the step size for an integer setting key.
   *
   * @param key the setting key
   * @return the step size
   */
  public static int getIntStep(String key) {
    return switch (key) {
      case "server.cooldownSeconds", "factions.stuckCooldownSeconds" -> 10;
      case "server.leaderboardKdRefreshSeconds" -> 30;
      case "chat.historyMaxMessages" -> 10;
      case "worldmap.proximityBatchIntervalTicks", "worldmap.incrementalBatchIntervalTicks" -> 5;
      case "worldmap.proximityMaxChunksPerBatch", "worldmap.incrementalMaxChunksPerBatch" -> 10;
      case "worldmap.factionWideRefreshThreshold" -> 50;
      case "worldmap.overrideDefaultScale", "worldmap.overrideMinScale",
           "worldmap.overrideMaxScale", "worldmap.overrideImageScale" -> 8;
      default -> 1;
    };
  }

  /**
   * Returns the step size for a double setting key.
   *
   * @param key the setting key
   * @return the step size
   */
  public static double getDoubleStep(String key) {
    return switch (key) {
      case "factions.regenPerMinute", "debug.sentryTracesSampleRate" -> 0.1;
      case "factions.powerPerClaim", "factions.deathPenalty", "factions.killReward",
           "factions.logoutPowerLoss", "factions.neutralAttackPenalty" -> 0.5;
      default -> 1.0;
    };
  }

  private static boolean toBool(Object value) {
    if (value instanceof Boolean b) return b;
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static int toInt(Object value) {
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(String.valueOf(value));
  }

  private static double toDouble(Object value) {
    if (value instanceof Number n) return n.doubleValue();
    return Double.parseDouble(String.valueOf(value));
  }

  private static String toStr(Object value) {
    return String.valueOf(value);
  }

  private static BigDecimal toBigDecimal(Object value) {
    if (value instanceof BigDecimal bd) return bd;
    if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
    return new BigDecimal(String.valueOf(value));
  }
}
