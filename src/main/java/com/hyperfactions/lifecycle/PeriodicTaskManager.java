package com.hyperfactions.lifecycle;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
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
    }

    /**
     * Starts the auto-save periodic task if enabled.
     */
    private void startAutoSaveTask() {
        ConfigManager config = ConfigManager.get();
        if (!config.isAutoSaveEnabled()) {
            Logger.info("Auto-save is disabled in config");
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
            Logger.info("Auto-save scheduled every %d minutes", intervalMinutes);
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
            Logger.info("Invite/request cleanup task scheduled every 5 minutes");
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
            Logger.info("Chat history retention cleanup scheduled every %d minutes", intervalMinutes);
        }
    }
}
