package com.hyperfactions.gui.admin;

/**
 * Input validation for the admin config editor.
 * Provides per-field min/max bounds and type checking.
 */
public final class ConfigValidator {

  private ConfigValidator() {}

  /**
   * Parses and clamps an integer input string within bounds.
   *
   * @param input   the raw string input
   * @param current the current value (returned if input is invalid)
   * @param min     minimum allowed value
   * @param max     maximum allowed value
   * @return the clamped integer value
   */
  public static int clampInt(String input, int current, int min, int max) {
    if (input == null || input.isBlank()) return current;
    try {
      int val = Integer.parseInt(input.trim());
      return Math.max(min, Math.min(max, val));
    } catch (NumberFormatException e) {
      return current;
    }
  }

  /**
   * Parses and clamps a double input string within bounds.
   *
   * @param input   the raw string input
   * @param current the current value (returned if input is invalid)
   * @param min     minimum allowed value
   * @param max     maximum allowed value
   * @return the clamped double value
   */
  public static double clampDouble(String input, double current, double min, double max) {
    if (input == null || input.isBlank()) return current;
    try {
      double val = Double.parseDouble(input.trim());
      if (Double.isNaN(val) || Double.isInfinite(val)) return current;
      val = Math.max(min, Math.min(max, val));
      return Math.round(val * 100.0) / 100.0;
    } catch (NumberFormatException e) {
      return current;
    }
  }

  /**
   * Returns the min bound for an integer setting key.
   */
  public static int getIntMin(String key) {
    return switch (key) {
      case "factions.maxMembers", "factions.minNameLength", "factions.maxNameLength" -> 1;
      case "factions.maxClaims" -> 0;
      case "factions.tagDurationSeconds", "factions.spawnProtectionDurationSeconds" -> 0;
      case "server.warmupSeconds", "server.cooldownSeconds" -> 0;
      case "server.autoSaveIntervalMinutes" -> 1;
      case "server.mobClearIntervalSeconds" -> 5;
      case "server.leaderboardKdRefreshSeconds" -> 30;
      case "factions.stuckMinRadius", "factions.stuckRadiusIncrease", "factions.stuckMaxAttempts" -> 1;
      case "factions.stuckWarmupSeconds", "factions.stuckCooldownSeconds" -> 0;
      case "factions.inviteExpirationMinutes", "factions.joinRequestExpirationHours" -> 1;
      case "factions.maxAllies", "factions.maxEnemies" -> -1;
      case "factions.decayDaysInactive" -> 1;
      case "factions.decayClaimsPerCycle" -> 1;
      case "factions.maxMembershipHistory" -> 1;
      case "backup.hourlyRetention", "backup.dailyRetention", "backup.weeklyRetention",
           "backup.manualRetention", "backup.shutdownRetention" -> 0;
      case "chat.historyMaxMessages" -> 10;
      case "chat.historyRetentionDays" -> 1;
      case "chat.historyCleanupIntervalMinutes" -> 1;
      case "economy.upkeepIntervalHours" -> 1;
      case "economy.upkeepGracePeriodHours" -> 0;
      case "economy.upkeepFreeChunks" -> 0;
      case "economy.upkeepClaimLossPerCycle" -> 1;
      case "economy.upkeepWarningHours" -> 0;
      case "economy.defaultLimitPeriodHours" -> 1;
      case "worldmap.proximityChunkRadius" -> 1;
      case "worldmap.proximityBatchIntervalTicks", "worldmap.incrementalBatchIntervalTicks" -> 1;
      case "worldmap.proximityMaxChunksPerBatch", "worldmap.incrementalMaxChunksPerBatch" -> 1;
      case "worldmap.debouncedDelaySeconds" -> 1;
      case "worldmap.factionWideRefreshThreshold" -> 10;
      case "worldmap.overrideDefaultScale", "worldmap.overrideMinScale",
           "worldmap.overrideMaxScale", "worldmap.overrideImageScale" -> 0; // 0 = inherit
      default -> 0;
    };
  }

  /**
   * Returns the max bound for an integer setting key.
   */
  public static int getIntMax(String key) {
    return switch (key) {
      case "factions.maxMembers" -> 1000;
      case "factions.minNameLength" -> 32;
      case "factions.maxNameLength" -> 64;
      case "factions.maxClaims" -> 10000;
      case "factions.tagDurationSeconds" -> 600;
      case "factions.spawnProtectionDurationSeconds" -> 600;
      case "server.warmupSeconds" -> 3600;
      case "server.cooldownSeconds" -> 86400;
      case "server.autoSaveIntervalMinutes" -> 60;
      case "server.mobClearIntervalSeconds" -> 3600;
      case "server.leaderboardKdRefreshSeconds" -> 86400;
      case "factions.stuckMinRadius" -> 100;
      case "factions.stuckRadiusIncrease" -> 50;
      case "factions.stuckMaxAttempts" -> 100;
      case "factions.stuckWarmupSeconds" -> 600;
      case "factions.stuckCooldownSeconds" -> 86400;
      case "factions.inviteExpirationMinutes" -> 1440;
      case "factions.joinRequestExpirationHours" -> 168;
      case "factions.maxAllies", "factions.maxEnemies" -> 100;
      case "factions.decayDaysInactive" -> 365;
      case "factions.decayClaimsPerCycle" -> 100;
      case "factions.maxMembershipHistory" -> 100;
      case "backup.hourlyRetention" -> 168;
      case "backup.dailyRetention" -> 90;
      case "backup.weeklyRetention" -> 52;
      case "backup.manualRetention" -> 100;
      case "backup.shutdownRetention" -> 100;
      case "chat.historyMaxMessages" -> 10000;
      case "chat.historyRetentionDays" -> 365;
      case "chat.historyCleanupIntervalMinutes" -> 1440;
      case "economy.upkeepIntervalHours" -> 168;
      case "economy.upkeepGracePeriodHours" -> 720;
      case "economy.upkeepFreeChunks" -> 1000;
      case "economy.upkeepClaimLossPerCycle" -> 100;
      case "economy.upkeepWarningHours" -> 168;
      case "economy.defaultLimitPeriodHours" -> 720;
      case "worldmap.proximityChunkRadius" -> 128;
      case "worldmap.proximityBatchIntervalTicks", "worldmap.incrementalBatchIntervalTicks" -> 200;
      case "worldmap.proximityMaxChunksPerBatch", "worldmap.incrementalMaxChunksPerBatch" -> 500;
      case "worldmap.debouncedDelaySeconds" -> 60;
      case "worldmap.factionWideRefreshThreshold" -> 10000;
      case "worldmap.overrideDefaultScale", "worldmap.overrideMinScale",
           "worldmap.overrideMaxScale" -> 512;
      case "worldmap.overrideImageScale" -> 10;
      default -> 999999;
    };
  }

  /**
   * Returns the min bound for a double setting key.
   */
  public static double getDoubleMin(String key) {
    return switch (key) {
      case "factions.maxPlayerPower", "factions.startingPower", "factions.powerPerClaim" -> 0.0;
      case "factions.deathPenalty", "factions.killReward" -> 0.0;
      case "factions.logoutPowerLoss", "factions.neutralAttackPenalty" -> 0.0;
      case "factions.regenPerMinute" -> 0.0;
      case "debug.sentryTracesSampleRate" -> 0.0;
      case "economy.startingBalance", "economy.upkeepCostPerChunk", "economy.upkeepMaxCostCap" -> 0.0;
      case "economy.depositFeePercent", "economy.withdrawFeePercent", "economy.transferFeePercent" -> 0.0;
      case "economy.defaultMaxWithdrawAmount", "economy.defaultMaxWithdrawPerPeriod",
           "economy.defaultMaxTransferAmount", "economy.defaultMaxTransferPerPeriod" -> 0.0;
      default -> 0.0;
    };
  }

  /**
   * Returns the max bound for a double setting key.
   */
  public static double getDoubleMax(String key) {
    return switch (key) {
      case "factions.maxPlayerPower" -> 10000.0;
      case "factions.startingPower" -> 10000.0;
      case "factions.powerPerClaim" -> 1000.0;
      case "factions.deathPenalty", "factions.killReward" -> 1000.0;
      case "factions.logoutPowerLoss", "factions.neutralAttackPenalty" -> 1000.0;
      case "factions.regenPerMinute" -> 100.0;
      case "debug.sentryTracesSampleRate" -> 1.0;
      case "economy.startingBalance" -> 999999999.0;
      case "economy.upkeepCostPerChunk" -> 999999.0;
      case "economy.upkeepMaxCostCap" -> 999999999.0;
      case "economy.depositFeePercent", "economy.withdrawFeePercent", "economy.transferFeePercent" -> 100.0;
      case "economy.defaultMaxWithdrawAmount", "economy.defaultMaxWithdrawPerPeriod",
           "economy.defaultMaxTransferAmount", "economy.defaultMaxTransferPerPeriod" -> 999999999.0;
      default -> 999999.0;
    };
  }

  /**
   * Validates a color hex string. Must match #RRGGBB format.
   *
   * @param input   the raw input
   * @param current the current value (returned if invalid)
   * @return the validated color string
   */
  public static String validateColor(String input, String current) {
    if (input == null || input.isBlank()) return current;
    String trimmed = input.trim();
    if (trimmed.matches("#[0-9A-Fa-f]{6}")) return trimmed;
    return current;
  }

  /**
   * Validates a string input with max length.
   *
   * @param input  the raw input
   * @param maxLen maximum allowed length
   * @return the validated string
   */
  public static String validateString(String input, int maxLen) {
    if (input == null) return "";
    String trimmed = input.strip();
    if (trimmed.length() > maxLen) return trimmed.substring(0, maxLen);
    return trimmed;
  }
}
