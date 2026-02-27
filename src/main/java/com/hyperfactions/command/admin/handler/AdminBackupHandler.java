package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.manager.ConfirmationManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to manage backups.", COLOR_RED)));
      return;
    }

    if (args.length == 0) {
      showBackupHelp(ctx);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "create" -> handleBackupCreate(ctx, senderUuid, subArgs);
      case "list" -> handleBackupList(ctx);
      case "restore" -> handleBackupRestore(ctx, senderUuid, subArgs);
      case "delete" -> handleBackupDelete(ctx, subArgs);
      case "help", "?" -> showBackupHelp(ctx);
      default -> {
        ctx.sendMessage(prefix().insert(msg("Unknown backup command: " + subCmd, COLOR_RED)));
        showBackupHelp(ctx);
      }
    }
  }

  private void showBackupHelp(CommandContext ctx) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin backup create [name]", "Create manual backup"));
    commands.add(new CommandHelp("/f admin backup list", "List all backups grouped by type"));
    commands.add(new CommandHelp("/f admin backup restore <name>", "Restore from backup (requires confirmation)"));
    commands.add(new CommandHelp("/f admin backup delete <name>", "Delete a backup"));
    ctx.sendMessage(HelpFormatter.buildHelp("Backup Management", "GFS rotation scheme", commands, null));
  }

  /** Handles backup create. */
  public void handleBackupCreate(CommandContext ctx, UUID senderUuid, String[] args) {
    String customName = args.length > 0 ? String.join("_", args) : null;

    ctx.sendMessage(prefix().insert(msg("Creating backup...", COLOR_YELLOW)));

    hyperFactions.getBackupManager().createBackup(BackupType.MANUAL, customName, senderUuid)
      .thenAccept(result -> {
        if (result instanceof BackupManager.BackupResult.Success success) {
          ctx.sendMessage(prefix().insert(msg("Backup created successfully!", COLOR_GREEN)));
          ctx.sendMessage(msg("  Name: " + success.metadata().name(), COLOR_GRAY));
          ctx.sendMessage(msg("  Size: " + success.metadata().getFormattedSize(), COLOR_GRAY));
        } else if (result instanceof BackupManager.BackupResult.Failure failure) {
          ctx.sendMessage(prefix().insert(msg("Backup failed: " + failure.error(), COLOR_RED)));
        }
      });
  }

  /** Handles backup list. */
  public void handleBackupList(CommandContext ctx) {
    Map<BackupType, List<BackupMetadata>> grouped = hyperFactions.getBackupManager().getBackupsGroupedByType();

    if (grouped.isEmpty()) {
      ctx.sendMessage(prefix().insert(msg("No backups found.", COLOR_GRAY)));
      return;
    }

    ctx.sendMessage(msg("=== Backups ===", COLOR_CYAN).bold(true));

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
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup restore <name>", COLOR_RED)));
      return;
    }

    String backupName = args[0];
    // Find backup by name
    BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
      .filter(b -> b.name().equalsIgnoreCase(backupName))
      .findFirst()
      .orElse(null);
    if (backup == null) {
      ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
      return;
    }

    ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
    ConfirmationManager.ConfirmationResult confirmResult = confirmManager.checkOrCreate(
      senderUuid, ConfirmationManager.ConfirmationType.RESTORE_BACKUP, null
    );

    switch (confirmResult) {
      case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
        ctx.sendMessage(prefix().insert(msg("WARNING: Restoring backup will overwrite current data!", COLOR_RED)));
        ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
          .insert(msg("/f admin backup restore " + backupName, COLOR_WHITE))
          .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
      }
      case CONFIRMED -> {
        ctx.sendMessage(prefix().insert(msg("Restoring backup...", COLOR_YELLOW)));
        hyperFactions.getBackupManager().restoreBackup(backup.name())
          .thenAccept(result -> {
            if (result instanceof BackupManager.RestoreResult.Success) {
              ctx.sendMessage(prefix().insert(msg("Backup restored successfully! Data reloaded.", COLOR_GREEN)));
            } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
              ctx.sendMessage(prefix().insert(msg("Restore failed: " + failure.error(), COLOR_RED)));
            }
          });
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm restore.", COLOR_YELLOW)));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /** Handles backup delete. */
  public void handleBackupDelete(CommandContext ctx, String[] args) {
    if (args.length < 1) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin backup delete <name>", COLOR_RED)));
      return;
    }

    String backupName = args[0];
    // Find backup by name
    BackupMetadata backup = hyperFactions.getBackupManager().listBackups().stream()
      .filter(b -> b.name().equalsIgnoreCase(backupName))
      .findFirst()
      .orElse(null);
    if (backup == null) {
      ctx.sendMessage(prefix().insert(msg("Backup '" + backupName + "' not found.", COLOR_RED)));
      return;
    }

    hyperFactions.getBackupManager().deleteBackup(backup.name())
      .thenAccept(deleted -> {
        if (deleted) {
          ctx.sendMessage(prefix().insert(msg("Deleted backup '" + backupName + "'", COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Failed to delete backup.", COLOR_RED)));
        }
      });
  }
}
