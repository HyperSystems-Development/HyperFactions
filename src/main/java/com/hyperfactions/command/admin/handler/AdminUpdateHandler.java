package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.ServerConfig;
import com.hyperfactions.update.UpdateChecker;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.AdminKeys;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Handles /f admin update, /f admin update mixin, and /f admin rollback commands.
 */
public class AdminUpdateHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  /** Creates a new AdminUpdateHandler. */
  public AdminUpdateHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Dispatches /f admin update [subcommand].
   * <ul>
   *   <li>/f admin update — check/download HyperFactions update</li>
   *   <li>/f admin update mixin — check/download HyperProtect-Mixin update</li>
   *   <li>/f admin update toggle-mixin-download — toggle HP-Mixin auto-download on/off</li>
   * </ul>
   */
  public void handleAdminUpdate(CommandContext ctx, UUID senderUuid, String[] subArgs) {
    if (subArgs.length == 0) {
      handleHyperFactionsUpdate(ctx, senderUuid);
      return;
    }

    switch (subArgs[0].toLowerCase()) {
      case "mixin" -> handleMixinUpdate(ctx);
      case "toggle-mixin-download" -> handleToggleMixinDownload(ctx);
      // Legacy alias
      case "disable-mixin-download" -> handleToggleMixinDownload(ctx);
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_UNKNOWN_TARGET, subArgs[0]), COLOR_RED)));
        ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_USAGE_HF), COLOR_GRAY));
        ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_USAGE_MIXIN), COLOR_GRAY));
        ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_USAGE_TOGGLE), COLOR_GRAY));
      }
    }
  }

  // === HyperFactions Update (existing behavior) ===

  private void handleHyperFactionsUpdate(CommandContext ctx, UUID senderUuid) {
    var updateChecker = hyperFactions.getUpdateChecker();
    if (updateChecker == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_NOT_AVAILABLE), COLOR_RED)));
      return;
    }

    if (!updateChecker.hasUpdateAvailable()) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_CHECKING), COLOR_YELLOW)));
      updateChecker.checkForUpdates(true).thenAccept(info -> {
        if (info == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_UP_TO_DATE, updateChecker.getCurrentVersion()), COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_AVAILABLE, info.version()), COLOR_GREEN)));
          startHyperFactionsDownload(ctx, senderUuid, updateChecker, info);
        }
      });
      return;
    }

    var info = updateChecker.getCachedUpdate();
    if (info == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_NO_INFO), COLOR_RED)));
      return;
    }

    startHyperFactionsDownload(ctx, senderUuid, updateChecker, info);
  }

  private void startHyperFactionsDownload(CommandContext ctx, UUID senderUuid,
                      UpdateChecker updateChecker,
                      UpdateChecker.UpdateInfo info) {
    String currentVersion = updateChecker.getCurrentVersion();

    // Step 1: Create a data backup before downloading the update
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_CREATING_BACKUP), COLOR_YELLOW)));

    hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, "pre-update-" + currentVersion, senderUuid)
      .thenCompose(backupResult -> {
        if (backupResult instanceof BackupManager.BackupResult.Success success) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_BACKUP_CREATED, success.metadata().name()), COLOR_GREEN)));
        } else if (backupResult instanceof BackupManager.BackupResult.Failure failure) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_BACKUP_WARNING, failure.error()), COLOR_YELLOW)));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_BACKUP_CONTINUE), COLOR_GRAY));
        }

        // Step 2: Download the update
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_DOWNLOADING, info.version()), COLOR_YELLOW)));
        return updateChecker.downloadUpdate(info);
      })
      .thenAccept(path -> {
        if (path == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_DOWNLOAD_FAILED), COLOR_RED)));
        } else {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_DOWNLOADED), COLOR_GREEN)));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_FILE_LABEL, path.getFileName()), COLOR_GRAY));

          // Step 3: Clean up old JAR backups (keep only the version we just upgraded from)
          int cleaned = updateChecker.cleanupOldBackups(currentVersion);
          if (cleaned > 0) {
            ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_CLEANUP, cleaned), COLOR_GRAY));
          }
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_KEPT_BACKUP, updateChecker.getArtifactName() + "-" + currentVersion + ".jar.backup"), COLOR_GRAY));

          // Step 4: Create rollback marker (safe to rollback until server restarts)
          updateChecker.createRollbackMarker(currentVersion, info.version());

          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_RESTART), COLOR_YELLOW));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_USE_ROLLBACK), COLOR_GRAY));

          // Run manual backup rotation to respect retention limits
          hyperFactions.getBackupManager().performRotation();
        }
      });
  }

  // === HyperProtect-Mixin Update ===

  private void handleMixinUpdate(CommandContext ctx) {
    var hpChecker = hyperFactions.getHyperProtectUpdateChecker();

    if (hpChecker == null) {
      // No update checker — either auto-download is disabled and HP isn't installed,
      // or updates config is disabled entirely. Create one on-the-fly.
      ServerConfig config = ConfigManager.get().server();
      Path earlyPluginsDir = hyperFactions.getDataDir().getParent().getParent().resolve("earlyplugins");
      String hpVersion = System.getProperty("hyperprotect.bridge.version", "0.0.0");

      hpChecker = new UpdateChecker(
          hyperFactions.getDataDir(), hpVersion, config.getHyperProtectUpdateUrl(),
          false, "HyperProtect-Mixin", earlyPluginsDir);
    }

    boolean hpDetected = "true".equalsIgnoreCase(System.getProperty("hyperprotect.bridge.active"));
    String currentVersion = hpDetected
        ? System.getProperty("hyperprotect.bridge.version", "unknown")
        : "not installed";

    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_CURRENT, currentVersion), COLOR_CYAN)));
    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_CHECKING), COLOR_YELLOW)));

    final var checker = hpChecker;
    checker.checkForUpdates(true).thenAccept(info -> {
      if (info == null) {
        if (hpDetected) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_UP_TO_DATE), COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_NONE), COLOR_YELLOW)));
        }
        return;
      }

      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_AVAILABLE, info.version()), COLOR_GREEN)));
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_DOWNLOADING, info.version()), COLOR_YELLOW)));

      checker.downloadUpdate(info).thenAccept(path -> {
        if (path == null) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_FAILED), COLOR_RED)));
        } else {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_DOWNLOADED), COLOR_GREEN)));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_FILE_LABEL, path.getFileName()), COLOR_GRAY));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_LOCATION), COLOR_GRAY));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_RESTART), COLOR_YELLOW));
        }
      });
    });
  }

  // === Toggle Mixin Auto-Download ===

  private void handleToggleMixinDownload(CommandContext ctx) {
    ServerConfig config = ConfigManager.get().server();
    boolean newValue = !config.isHyperProtectAutoDownload();

    config.setHyperProtectAutoDownload(newValue);
    ConfigManager.get().saveAll();

    if (newValue) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_AUTO_ON), COLOR_GREEN)));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_AUTO_ON_DESC), COLOR_GRAY));
    } else {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_AUTO_OFF), COLOR_GREEN)));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_MIXIN_AUTO_OFF_DESC), COLOR_GRAY));
    }
  }

  // === Rollback ===

  /** Handles admin rollback. */
  public void handleAdminRollback(CommandContext ctx) {
    var updateChecker = hyperFactions.getUpdateChecker();
    if (updateChecker == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.UPDATE_NOT_AVAILABLE), COLOR_RED)));
      return;
    }

    // Check if there's a backup to rollback to
    Path latestBackup = updateChecker.findLatestBackup();
    if (latestBackup == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_NO_BACKUP), COLOR_RED)));
      return;
    }

    String artifactName = updateChecker.getArtifactName();
    String backupVersion = latestBackup.getFileName().toString()
        .replace(artifactName + "-", "")
        .replace(".jar.backup", "");

    // Check if rollback is safe (server hasn't restarted since update)
    if (!updateChecker.isRollbackSafe()) {
      // Server has restarted - migrations may have run
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_UNSAFE), COLOR_RED)));
      ctx.sendMessage(msg("", COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_UNSAFE_REASON), COLOR_YELLOW));
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_UNSAFE_MIGRATION), COLOR_YELLOW));
      ctx.sendMessage(msg("", COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_INSTRUCTIONS), COLOR_WHITE));
      ctx.sendMessage(msg("  1. Stop the server", COLOR_GRAY));
      ctx.sendMessage(msg("  2. Restore from the pre-update backup:", COLOR_GRAY));
      ctx.sendMessage(msg("     /f admin backup restore <backup-name>", COLOR_CYAN));
      ctx.sendMessage(msg("  3. Manually replace the JAR file:", COLOR_GRAY));
      ctx.sendMessage(msg("     " + latestBackup.getFileName() + " -> " + artifactName + "-" + backupVersion + ".jar", COLOR_CYAN));
      ctx.sendMessage(msg("  4. Restart the server", COLOR_GRAY));
      ctx.sendMessage(msg("", COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_FIND_BACKUP), COLOR_YELLOW));
      return;
    }

    // Get rollback info
    var rollbackInfo = updateChecker.getRollbackInfo();
    if (rollbackInfo != null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_ROLLING), COLOR_YELLOW)));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_FROM, rollbackInfo.toVersion()), COLOR_GRAY));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_TO, rollbackInfo.fromVersion()), COLOR_GRAY));
    } else {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_VERSION, backupVersion), COLOR_YELLOW)));
    }

    // Perform the rollback
    var result = updateChecker.performRollback();

    if (result.success()) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_SUCCESS), COLOR_GREEN)));
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_RESTORED, artifactName + "-" + result.restoredVersion() + ".jar"), COLOR_GRAY));
      if (result.removedVersion() != null) {
        ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_REMOVED, artifactName + "-" + result.removedVersion() + ".jar"), COLOR_GRAY));
      }
      ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_RESTART), COLOR_YELLOW));
    } else {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ROLLBACK_FAILED, result.errorMessage()), COLOR_RED)));
    }
  }
}
