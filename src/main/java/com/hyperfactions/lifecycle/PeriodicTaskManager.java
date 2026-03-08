package com.hyperfactions.lifecycle;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;

/**
 * Manages periodic tasks for HyperFactions: auto-save, invite cleanup,
 * and chat history retention cleanup.
 */
public class PeriodicTaskManager {

  private final HyperFactions hyperFactions;

  // Task IDs for cancellation
  private int autoSaveTaskId = -1;

  private int inviteCleanupTaskId = -1;

  private int chatHistoryCleanupTaskId = -1;

  private int upkeepTaskId = -1;

  private int mobClearTaskId = -1;

  /** Creates a new PeriodicTaskManager. */
  public PeriodicTaskManager(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Starts all periodic tasks (auto-save, invite cleanup, chat history cleanup).
   */
  public void startAll() {
    startAutoSaveTask();
    startInviteCleanupTask();
    startChatHistoryCleanupTask();
    startUpkeepTask();
    startMobClearTask();
  }

  /**
   * Cancels all running periodic tasks.
   */
  public void cancelAll() {
    if (autoSaveTaskId > 0) {
      hyperFactions.cancelTask(autoSaveTaskId);
      autoSaveTaskId = -1;
    }
    if (inviteCleanupTaskId > 0) {
      hyperFactions.cancelTask(inviteCleanupTaskId);
      inviteCleanupTaskId = -1;
    }
    if (chatHistoryCleanupTaskId > 0) {
      hyperFactions.cancelTask(chatHistoryCleanupTaskId);
      chatHistoryCleanupTaskId = -1;
    }
    if (upkeepTaskId > 0) {
      hyperFactions.cancelTask(upkeepTaskId);
      upkeepTaskId = -1;
    }
    if (mobClearTaskId > 0) {
      hyperFactions.cancelTask(mobClearTaskId);
      mobClearTaskId = -1;
    }
  }

  /**
   * Starts the auto-save periodic task if enabled.
   */
  private void startAutoSaveTask() {
    ConfigManager config = ConfigManager.get();
    if (!config.isAutoSaveEnabled()) {
      Logger.debug("Auto-save is disabled in config");
      return;
    }

    int intervalMinutes = config.getAutoSaveIntervalMinutes();
    if (intervalMinutes <= 0) {
      Logger.warn("Invalid auto-save interval: %d minutes, using default 5 minutes", intervalMinutes);
      intervalMinutes = 5;
    }

    int periodTicks = intervalMinutes * 60 * 20; // Convert minutes to ticks (20 ticks per second)
    autoSaveTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, hyperFactions::saveAllData);

    if (autoSaveTaskId > 0) {
      Logger.debug("Auto-save scheduled every %d minutes", intervalMinutes);
    }
  }

  /**
   * Starts the invite cleanup periodic task.
   * Also cleans up expired join requests.
   */
  private void startInviteCleanupTask() {
    // Run every 5 minutes (6000 ticks)
    int periodTicks = 5 * 60 * 20;
    inviteCleanupTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, () -> {
      if (hyperFactions.getInviteManager() != null) {
        hyperFactions.getInviteManager().cleanupExpired();
      }
      if (hyperFactions.getJoinRequestManager() != null) {
        hyperFactions.getJoinRequestManager().cleanupExpired();
      }
    });

    if (inviteCleanupTaskId > 0) {
      Logger.debug("Invite/request cleanup task scheduled every 5 minutes");
    }
  }

  /**
   * Starts the chat history retention cleanup task if enabled.
   */
  private void startChatHistoryCleanupTask() {
    if (!ConfigManager.get().isChatHistoryEnabled()) {
      Logger.debug("Chat history disabled, skipping retention cleanup task");
      return;
    }

    int intervalMinutes = ConfigManager.get().getChatHistoryCleanupIntervalMinutes();
    if (intervalMinutes <= 0) {
      Logger.debug("Chat history cleanup interval is 0, skipping retention cleanup task");
      return;
    }

    int periodTicks = intervalMinutes * 60 * 20; // Convert minutes to ticks
    chatHistoryCleanupTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, () -> {
      if (hyperFactions.getChatHistoryManager() != null) {
        hyperFactions.getChatHistoryManager().pruneExpired();
      }
    });

    if (chatHistoryCleanupTaskId > 0) {
      Logger.debug("Chat history retention cleanup scheduled every %d minutes", intervalMinutes);
    }
  }

  /**
   * Starts the upkeep collection task if enabled.
   * This is a skeleton — logs what it would collect but doesn't actually deduct.
   */
  private void startUpkeepTask() {
    ConfigManager config = ConfigManager.get();
    if (!config.isUpkeepEnabled()) {
      Logger.debug("Upkeep is disabled in config");
      return;
    }

    int intervalHours = config.getUpkeepIntervalHours();
    int periodTicks = intervalHours * 3600 * 20; // Convert hours to ticks

    upkeepTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, () -> {
      var economyManager = hyperFactions.getEconomyManager();
      var factionManager = hyperFactions.getFactionManager();
      if (economyManager == null || factionManager == null) {
        return;
      }

      java.math.BigDecimal costPerChunk = config.getUpkeepCostPerChunk();
      int factionsProcessed = 0;

      for (var faction : factionManager.getAllFactions()) {
        int claimCount = faction.getClaimCount();
        if (claimCount <= 0) {
          continue;
        }

        java.math.BigDecimal cost = costPerChunk.multiply(java.math.BigDecimal.valueOf(claimCount));
        var economy = economyManager.getEconomy(faction.id());
        if (economy == null) {
          continue;
        }

        // Skeleton: log what would happen but don't deduct
        Logger.info("[Upkeep] Faction '%s': %d claims x %s = %s (skeleton — not deducted)",
            faction.name(), claimCount, costPerChunk.toPlainString(), cost.toPlainString());
        factionsProcessed++;
      }

      Logger.info("[Upkeep] Upkeep collection task ran — %d factions processed (skeleton mode)",
          factionsProcessed);
    });

    if (upkeepTaskId > 0) {
      Logger.debug("Upkeep collection scheduled every %d hours", intervalHours);
    }
  }

  /**
   * Starts the periodic mob clearing task if enabled.
   */
  private void startMobClearTask() {
    ConfigManager config = ConfigManager.get();
    if (!config.isMobClearEnabled()) {
      Logger.debug("Mob clearing is disabled in config");
      return;
    }

    int intervalSeconds = config.getMobClearIntervalSeconds();
    if (intervalSeconds <= 0) {
      Logger.warn("Invalid mob clear interval: %d seconds, using default 10 seconds", intervalSeconds);
      intervalSeconds = 10;
    }

    int periodTicks = intervalSeconds * 20; // Convert seconds to ticks
    mobClearTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks,
      ErrorHandler.wrapTask("Mob clear sweep", () -> hyperFactions.getZoneMobClearManager().sweep()));

    if (mobClearTaskId > 0) {
      Logger.debug("Mob clear sweep scheduled every %d seconds", intervalSeconds);
    }
  }
}
