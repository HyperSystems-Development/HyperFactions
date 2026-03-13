package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.manager.ConfirmationManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.HelpKeys;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin backup commands (create, list, restore, delete).
 */
public class AdminBackupHandler {

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

  private boolean hasPermission(@Nullable PlayerRef player, String permission) {
    if (player == null) {
      return true;
    }
    return CommandUtil.hasPermission(player, permission);
  }

  /** Creates a new AdminBackupHandler. */
  public AdminBackupHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles admin backup. */
  public void handleAdminBackup(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN_BACKUP)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.BACKUP_NO_PERMISSION), COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      showBackupHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "create" -> handleBackupCreate(ctx, senderUuid, subArgs);
      case "list" -> handleBackupList(ctx);
      case "restore" -> handleBackupRestore(ctx, senderUuid, subArgs);
      case "delete" -> handleBackupDelete(ctx, subArgs);
      case "help", "?" -> showBackupHelp(ctx, player);
      default -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.BACKUP_UNKNOWN_CMD), COLOR_RED)));
        showBackupHelp(ctx, player);
      }
    }
  }

  private void showBackupHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin backup create [name]", HelpKeys.Help.BACKUP_CMD_CREATE));
    commands.add(new CommandHelp("/f admin backup list", HelpKeys.Help.BACKUP_CMD_LIST));
    commands.add(new CommandHelp("/f admin backup restore <name>", HelpKeys.Help.BACKUP_CMD_RESTORE));
    commands.add(new CommandHelp("/f admin backup delete <name>", HelpKeys.Help.BACKUP_CMD_DELETE));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.BACKUP_TITLE, HelpKeys.Help.BACKUP_DESCRIPTION, commands, null, player));
  }

  /** Handles backup create. */
  public void handleBackupCreate(CommandContext ctx, UUID senderUuid, String[] args) {
    String customName = args.length > 0 ? String.join("_", args) : null;

    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_CREATING), COLOR_YELLOW)));

    hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, customName, senderUuid)
      .thenAccept(result -> {
        if (result instanceof BackupManager.BackupResult.Success success) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_CREATED), COLOR_GREEN)));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_NAME, success.metadata().name()), COLOR_GRAY));
          ctx.sendMessage(msg("  " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_SIZE, success.metadata().getFormattedSize()), COLOR_GRAY));
        } else if (result instanceof BackupManager.BackupResult.Failure failure) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_FAILED, failure.error()), COLOR_RED)));
        }
      });
  }

  /** Handles backup list. */
  public void handleBackupList(CommandContext ctx) {
    Map<BackupType, List<BackupMetadata>> grouped = hyperFactions.getBackupManager().getBackupsGroupedByType();

    if (grouped.isEmpty()) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_NONE), COLOR_GRAY)));
      return;
    }

    ctx.sendMessage(msg("=== " + HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_HEADER) + " ===", COLOR_CYAN).bold(true));

    for (BackupType type : BackupType.values()) {
      List<BackupMetadata> backups = grouped.getOrDefault(type, List.of());
      if (backups.isEmpty()) {
        continue;
      }

      ctx.sendMessage(msg(type.getDisplayName() + " (" + backups.size() + "):", COLOR_YELLOW));
      for (BackupMetadata backup : backups) {
        ctx.sendMessage(msg("  " + backup.name(), COLOR_WHITE)
          .insert(msg(" - " + backup.getFormattedTimestamp() + " (" + backup.getFormattedSize() + ")", COLOR_GRAY)));
      }
    }
  }

  /** Handles backup restore. */
  public void handleBackupRestore(CommandContext ctx, UUID senderUuid, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_USAGE_RESTORE), COLOR_RED)));
      return;
    }

    String backupName = args[0];
    // Find backup by name
    BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
      .filter(b -> b.name().equalsIgnoreCase(backupName))
      .findFirst()
      .orElse(null);
    if (backup == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_NOT_FOUND, backupName), COLOR_RED)));
      return;
    }

    ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
    ConfirmationManager.ConfirmationResult confirmResult = confirmManager.checkOrCreate(
      senderUuid, ConfirmationManager.ConfirmationType.RESTORE_BACKUP, null
    );

    switch (confirmResult) {
      case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_RESTORE_WARNING), COLOR_RED)));
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_RESTORE_CONFIRM), COLOR_YELLOW)));
      }
      case CONFIRMED -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_RESTORING), COLOR_YELLOW)));
        hyperFactions.getBackupManager().restoreBackup(backup.name())
          .thenAccept(result -> {
            if (result instanceof BackupManager.RestoreResult.Success) {
              ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_RESTORED, String.valueOf(hyperFactions.getFactionManager().getAllFactions().size())), COLOR_GREEN)));
            } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
              ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_RESTORE_FAILED, failure.error()), COLOR_RED)));
            }
          });
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_CONFIRM_CANCEL), COLOR_YELLOW)));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /** Handles backup delete. */
  public void handleBackupDelete(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_USAGE_DELETE), COLOR_RED)));
      return;
    }

    String backupName = args[0];
    // Find backup by name
    BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
      .filter(b -> b.name().equalsIgnoreCase(backupName))
      .findFirst()
      .orElse(null);
    if (backup == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_NOT_FOUND, backupName), COLOR_RED)));
      return;
    }

    hyperFactions.getBackupManager().deleteBackup(backup.name())
      .thenAccept(deleted -> {
        if (deleted) {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_DELETED, backupName), COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BACKUP_DELETE_FAILED, "unknown error"), COLOR_RED)));
        }
      });
  }
}
