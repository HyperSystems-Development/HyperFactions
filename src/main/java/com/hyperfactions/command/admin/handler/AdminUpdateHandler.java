package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Handles /f admin update and /f admin rollback commands.
 */
public class AdminUpdateHandler {

    private final HyperFactions hyperFactions;

    private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;
    private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;
    private static final String COLOR_RED = CommandUtil.COLOR_RED;
    private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;
    private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;
    private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

    private static Message prefix() { return CommandUtil.prefix(); }
    private static Message msg(String text, String color) { return CommandUtil.msg(text, color); }

    public AdminUpdateHandler(HyperFactions hyperFactions) {
        this.hyperFactions = hyperFactions;
    }

    public void handleAdminUpdate(CommandContext ctx, UUID senderUuid) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        if (!updateChecker.hasUpdateAvailable()) {
            ctx.sendMessage(prefix().insert(msg("Checking for updates...", COLOR_YELLOW)));
            updateChecker.checkForUpdates(true).thenAccept(info -> {
                if (info == null) {
                    ctx.sendMessage(prefix().insert(msg("Plugin is already up-to-date (v" + updateChecker.getCurrentVersion() + ")", COLOR_GREEN)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Update available: v" + info.version(), COLOR_GREEN)));
                    startDownload(ctx, senderUuid, updateChecker, info);
                }
            });
            return;
        }

        var info = updateChecker.getCachedUpdate();
        if (info == null) {
            ctx.sendMessage(prefix().insert(msg("No update information available.", COLOR_RED)));
            return;
        }

        startDownload(ctx, senderUuid, updateChecker, info);
    }

    private void startDownload(CommandContext ctx, UUID senderUuid,
                               com.hyperfactions.update.UpdateChecker updateChecker,
                               com.hyperfactions.update.UpdateChecker.UpdateInfo info) {
        String currentVersion = updateChecker.getCurrentVersion();

        // Step 1: Create a data backup before downloading the update
        ctx.sendMessage(prefix().insert(msg("Creating pre-update backup...", COLOR_YELLOW)));

        hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, "pre-update-" + currentVersion, senderUuid)
            .thenCompose(backupResult -> {
                if (backupResult instanceof BackupManager.BackupResult.Success success) {
                    ctx.sendMessage(prefix().insert(msg("Backup created: " + success.metadata().name(), COLOR_GREEN)));
                } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
                    ctx.sendMessage(prefix().insert(msg("Warning: Backup failed - " + failure.error(), COLOR_YELLOW)));
                    ctx.sendMessage(msg("  Continuing with update anyway...", COLOR_GRAY));
                }

                // Step 2: Download the update
                ctx.sendMessage(prefix().insert(msg("Downloading HyperFactions v" + info.version() + "...", COLOR_YELLOW)));
                return updateChecker.downloadUpdate(info);
            })
            .thenAccept(path -> {
                if (path == null) {
                    ctx.sendMessage(prefix().insert(msg("Failed to download update. Check server logs.", COLOR_RED)));
                } else {
                    ctx.sendMessage(prefix().insert(msg("Update downloaded successfully!", COLOR_GREEN)));
                    ctx.sendMessage(msg("  File: " + path.getFileName(), COLOR_GRAY));

                    // Step 3: Clean up old JAR backups (keep only the version we just upgraded from)
                    int cleaned = updateChecker.cleanupOldBackups(currentVersion);
                    if (cleaned > 0) {
                        ctx.sendMessage(msg("  Cleanup: Removed " + cleaned + " old backup(s)", COLOR_GRAY));
                    }
                    ctx.sendMessage(msg("  Kept: HyperFactions-" + currentVersion + ".jar.backup (for rollback)", COLOR_GRAY));

                    // Step 4: Create rollback marker (safe to rollback until server restarts)
                    updateChecker.createRollbackMarker(currentVersion, info.version());

                    ctx.sendMessage(msg("  Restart the server to apply the update.", COLOR_YELLOW));
                    ctx.sendMessage(msg("  Use /f admin rollback to revert before restarting.", COLOR_GRAY));

                    // Run manual backup rotation to respect retention limits
                    hyperFactions.getBackupManager().performRotation();
                }
            });
    }

    public void handleAdminRollback(CommandContext ctx) {
        var updateChecker = hyperFactions.getUpdateChecker();
        if (updateChecker == null) {
            ctx.sendMessage(prefix().insert(msg("Update checker is not available.", COLOR_RED)));
            return;
        }

        // Check if there's a backup to rollback to
        Path latestBackup = updateChecker.findLatestBackup();
        if (latestBackup == null) {
            ctx.sendMessage(prefix().insert(msg("No backup JAR found to rollback to.", COLOR_RED)));
            return;
        }

        String backupVersion = latestBackup.getFileName().toString()
                .replace("HyperFactions-", "")
                .replace(".jar.backup", "");

        // Check if rollback is safe (server hasn't restarted since update)
        if (!updateChecker.isRollbackSafe()) {
            // Server has restarted - migrations may have run
            ctx.sendMessage(prefix().insert(msg("Cannot automatically rollback!", COLOR_RED)));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("The server has been restarted since the last update.", COLOR_YELLOW));
            ctx.sendMessage(msg("Config/data migrations may have been applied.", COLOR_YELLOW));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("To rollback safely, you must:", COLOR_WHITE));
            ctx.sendMessage(msg("  1. Stop the server", COLOR_GRAY));
            ctx.sendMessage(msg("  2. Restore from the pre-update backup:", COLOR_GRAY));
            ctx.sendMessage(msg("     /f admin backup restore <backup-name>", COLOR_CYAN));
            ctx.sendMessage(msg("  3. Manually replace the JAR file:", COLOR_GRAY));
            ctx.sendMessage(msg("     " + latestBackup.getFileName() + " -> HyperFactions-" + backupVersion + ".jar", COLOR_CYAN));
            ctx.sendMessage(msg("  4. Restart the server", COLOR_GRAY));
            ctx.sendMessage(msg("", COLOR_GRAY));
            ctx.sendMessage(msg("Use /f admin backup list to find the pre-update backup.", COLOR_YELLOW));
            return;
        }

        // Get rollback info
        var rollbackInfo = updateChecker.getRollbackInfo();
        if (rollbackInfo != null) {
            ctx.sendMessage(prefix().insert(msg("Rolling back update...", COLOR_YELLOW)));
            ctx.sendMessage(msg("  From: v" + rollbackInfo.toVersion() + " (new)", COLOR_GRAY));
            ctx.sendMessage(msg("  To: v" + rollbackInfo.fromVersion() + " (previous)", COLOR_GRAY));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rolling back to v" + backupVersion + "...", COLOR_YELLOW)));
        }

        // Perform the rollback
        var result = updateChecker.performRollback();

        if (result.success()) {
            ctx.sendMessage(prefix().insert(msg("Rollback successful!", COLOR_GREEN)));
            ctx.sendMessage(msg("  Restored: HyperFactions-" + result.restoredVersion() + ".jar", COLOR_GRAY));
            if (result.removedVersion() != null) {
                ctx.sendMessage(msg("  Removed: HyperFactions-" + result.removedVersion() + ".jar", COLOR_GRAY));
            }
            ctx.sendMessage(msg("  Restart the server to apply the rollback.", COLOR_YELLOW));
        } else {
            ctx.sendMessage(prefix().insert(msg("Rollback failed: " + result.errorMessage(), COLOR_RED)));
        }
    }
}
