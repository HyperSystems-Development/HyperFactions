package com.hyperfactions.gui.admin.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.backup.BackupManager;
import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminBackupsData;
import com.hyperfactions.util.AdminGuiKeys;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Backups page - lists, creates, restores, and deletes faction data backups.
 */
public class AdminBackupsPage extends InteractiveCustomUIPage<AdminBackupsData> {

  private static final int BACKUPS_PER_PAGE = 8;

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  private final HyperFactions plugin;

  private int currentPage = 0;

  private final Set<String> expandedBackups = new HashSet<>();

  private String confirmingRestore = null;

  private String confirmingDelete = null;

  private String statusMessage = "";

  private boolean creating = false;

  /** Creates a new AdminBackupsPage. */
  public AdminBackupsPage(PlayerRef playerRef, GuiManager guiManager, HyperFactions plugin) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminBackupsData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.plugin = plugin;
  }

  /** Builds the backups page. */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_BACKUPS);

    AdminNavBarHelper.setupBar(playerRef, "backups", cmd, events);

    // Static labels — set once, persist across sendUpdate(false) calls
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_TITLE_BACKUPS));
    cmd.set("#NameLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_NAME_PLACEHOLDER));

    // Dynamic content — changes on each rebuild
    buildBackupList(cmd, events);
  }

  /** Builds (or rebuilds) all dynamic content: count, status, backup entries, pagination. */
  private void buildBackupList(UICommandBuilder cmd, UIEventBuilder events) {
    BackupManager manager = plugin.getBackupManager();
    List<BackupMetadata> allBackups = manager != null ? manager.listBackups() : List.of();

    // Header with count
    cmd.set("#BackupCount.Text",
        HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TOTAL_COUNT, allBackups.size()));

    // Create button
    cmd.set("#CreateBackupBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_BTN_CREATE));
    if (!creating) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateBackupBtn",
          EventData.of("Button", "Create")
              .append("@BackupInputName", "#BackupNameInput.Value"), false);
    }

    // Status message
    cmd.set("#StatusMessage.Text", statusMessage);

    // Pagination calculation
    int totalPages = Math.max(1, (int) Math.ceil((double) allBackups.size() / BACKUPS_PER_PAGE));
    if (currentPage >= totalPages) {
      currentPage = totalPages - 1;
    }
    if (currentPage < 0) {
      currentPage = 0;
    }

    int start = currentPage * BACKUPS_PER_PAGE;
    int end = Math.min(start + BACKUPS_PER_PAGE, allBackups.size());
    List<BackupMetadata> pageBackups = allBackups.subList(start, end);

    // Clear and rebuild backup list using IndexCards pattern
    cmd.clear("#BackupListContainer");

    if (pageBackups.isEmpty()) {
      cmd.appendInline("#BackupListContainer", "Label { Text: \""
          + HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_EMPTY)
          + "\"; Style: (FontSize: 11, TextColor: #666666, HorizontalAlignment: Center); Anchor: (Height: 40); }");
    } else {
      cmd.appendInline("#BackupListContainer", "Group #IndexCards { LayoutMode: Top; }");
      for (int i = 0; i < pageBackups.size(); i++) {
        buildBackupEntry(cmd, events, i, pageBackups.get(i));
      }
    }

    // Pagination controls
    cmd.set("#PageLabel.Text", totalPages > 1
        ? (currentPage + 1) + " / " + totalPages : "");

    cmd.set("#PrevBtn.Visible", currentPage > 0);
    if (currentPage > 0) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
          EventData.of("Button", "PrevPage").append("Page", String.valueOf(currentPage - 1)), false);
    }

    cmd.set("#NextBtn.Visible", currentPage < totalPages - 1);
    if (currentPage < totalPages - 1) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
          EventData.of("Button", "NextPage").append("Page", String.valueOf(currentPage + 1)), false);
    }
  }

  private void buildBackupEntry(UICommandBuilder cmd, UIEventBuilder events,
                                int index, BackupMetadata backup) {
    String name = backup.name();
    boolean expanded = expandedBackups.contains(name);

    cmd.append("#IndexCards", UIPaths.ADMIN_BACKUP_ENTRY);
    String idx = "#IndexCards[" + index + "]";

    // Header info via indexed child selectors
    cmd.set(idx + " #BackupNameLabel.Text", formatBackupName(name));
    cmd.set(idx + " #BackupSizeLabel.Text", backup.getFormattedSize());
    cmd.set(idx + " #BackupTypeTag.Text", getTypeLabel(backup.type()));

    // Expand/collapse button
    cmd.set(idx + " #ExpandBtn.Text", expanded ? "v" : ">");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ExpandBtn",
        EventData.of("Button", "Toggle").append("BackupName", name), false);

    if (expanded) {
      cmd.set(idx + " #DetailSection.Visible", true);
      cmd.set(idx + " #DetailTypeLabel.Text",
          HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DETAIL_TYPE) + " " + getTypeLabel(backup.type()));
      cmd.set(idx + " #DetailCreatedLabel.Text",
          HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DETAIL_CREATED) + " " + backup.getFormattedTimestamp());
      cmd.set(idx + " #DetailSizeLabel.Text",
          HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DETAIL_SIZE) + " " + backup.getFormattedSize());

      // Restore button
      if (name.equals(confirmingRestore)) {
        cmd.set(idx + " #RestoreWarning.Visible", true);
        cmd.set(idx + " #RestoreWarning.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RESTORE_WARNING));
        cmd.set(idx + " #RestoreBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RESTORE_CONFIRM));
      } else {
        cmd.set(idx + " #RestoreBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_BTN_RESTORE));
      }
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #RestoreBtn",
          EventData.of("Button", "Restore").append("BackupName", name), false);

      // Delete button
      if (name.equals(confirmingDelete)) {
        cmd.set(idx + " #DeleteBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DELETE_CONFIRM));
      } else {
        cmd.set(idx + " #DeleteBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_BTN_DELETE));
      }
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("BackupName", name), false);
    }
  }

  private String formatBackupName(String name) {
    // Remove "backup_" prefix for cleaner display
    if (name.startsWith("backup_")) {
      return name.substring(7);
    }
    return name;
  }

  private String getTypeLabel(BackupType type) {
    return switch (type) {
      case HOURLY -> HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TYPE_HOURLY);
      case DAILY -> HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TYPE_DAILY);
      case WEEKLY -> HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TYPE_WEEKLY);
      case MANUAL -> HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TYPE_MANUAL);
      case MIGRATION -> HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_TYPE_MIGRATION);
    };
  }

  @Nullable
  private World resolveWorld() {
    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      return null;
    }
    return Universe.get().getWorld(worldUuid);
  }

  private void rebuildOnWorldThread() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildBackupList(cmd, events);
    sendUpdate(cmd, events, false);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminBackupsData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button == null) {
      return;
    }

    switch (data.button) {
      case "Create" -> handleCreate(data);
      case "Toggle" -> handleToggle(data);
      case "Restore" -> handleRestore(data);
      case "Delete" -> handleDelete(data);
      case "PrevPage", "NextPage" -> {
        currentPage = data.page;
        expandedBackups.clear();
        confirmingRestore = null;
        confirmingDelete = null;
        rebuildOnWorldThread();
      }
      case "Back" -> guiManager.closePage(player, ref, store);
      default -> { }
    }
  }

  private void handleCreate(AdminBackupsData data) {
    if (creating) {
      return;
    }

    BackupManager manager = plugin.getBackupManager();
    if (manager == null) {
      statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_CREATE_FAILED);
      rebuildOnWorldThread();
      return;
    }

    creating = true;
    String customName = data.backupInputName;
    statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_CREATING);
    rebuildOnWorldThread();

    manager.createBackup(BackupType.MANUAL, customName, playerRef.getUuid()).thenAccept(result -> {
      World world = resolveWorld();
      if (world == null) {
        creating = false;
        return;
      }
      world.execute(() -> {
        creating = false;
        if (result instanceof BackupManager.BackupResult.Success success) {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_CREATED,
              success.metadata().name());
          Logger.info("[Backups] %s created manual backup: %s",
              playerRef.getUsername(), success.metadata().name());
        } else if (result instanceof BackupManager.BackupResult.Failure failure) {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_CREATE_FAILED)
              + ": " + failure.error();
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleToggle(AdminBackupsData data) {
    if (data.backupName == null) {
      return;
    }
    if (expandedBackups.contains(data.backupName)) {
      expandedBackups.remove(data.backupName);
    } else {
      expandedBackups.add(data.backupName);
    }
    // Reset confirm states when toggling
    confirmingRestore = null;
    confirmingDelete = null;
    rebuildOnWorldThread();
  }

  private void handleRestore(AdminBackupsData data) {
    if (data.backupName == null) {
      return;
    }

    BackupManager manager = plugin.getBackupManager();
    if (manager == null) {
      return;
    }

    // First click — ask for confirmation
    if (!data.backupName.equals(confirmingRestore)) {
      confirmingRestore = data.backupName;
      confirmingDelete = null;
      rebuildOnWorldThread();
      return;
    }

    // Second click — confirmed, create safety backup first then restore
    confirmingRestore = null;
    statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RESTORING);
    rebuildOnWorldThread();

    // Safety backup before restore
    manager.createBackup(BackupType.MANUAL, "pre-restore-safety", playerRef.getUuid())
        .thenCompose(safetyResult -> manager.restoreBackup(data.backupName))
        .thenAccept(result -> {
          World world = resolveWorld();
          if (world == null) {
            return;
          }
          world.execute(() -> {
            if (result instanceof BackupManager.RestoreResult.Success success) {
              statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RESTORED,
                  success.backupName(), success.filesRestored());
              Logger.info("[Backups] %s restored backup: %s (%d files)",
                  playerRef.getUsername(), success.backupName(), success.filesRestored());
              // Append reload note
              statusMessage += " " + HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RELOAD_REQUIRED);
            } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
              statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_RESTORE_FAILED)
                  + ": " + failure.error();
            }
            rebuildOnWorldThread();
          });
        });
  }

  private void handleDelete(AdminBackupsData data) {
    if (data.backupName == null) {
      return;
    }

    BackupManager manager = plugin.getBackupManager();
    if (manager == null) {
      return;
    }

    // First click — ask for confirmation
    if (!data.backupName.equals(confirmingDelete)) {
      confirmingDelete = data.backupName;
      confirmingRestore = null;
      rebuildOnWorldThread();
      return;
    }

    // Second click — confirmed
    confirmingDelete = null;
    statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DELETING);
    rebuildOnWorldThread();

    manager.deleteBackup(data.backupName).thenAccept(success -> {
      World world = resolveWorld();
      if (world == null) {
        return;
      }
      world.execute(() -> {
        if (success) {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DELETED, data.backupName);
          expandedBackups.remove(data.backupName);
          Logger.info("[Backups] %s deleted backup: %s", playerRef.getUsername(), data.backupName);
        } else {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.BKP_DELETE_FAILED);
        }
        rebuildOnWorldThread();
      });
    });
  }
}
