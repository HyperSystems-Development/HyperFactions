package com.hyperfactions.gui.admin.page;

import com.hyperfactions.BuildInfo;
import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminUpdatesData;
import com.hyperfactions.update.UpdateChecker;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Updates page - displays version info, update checking,
 * changelog, mixin status, and rollback support.
 */
public class AdminUpdatesPage extends InteractiveCustomUIPage<AdminUpdatesData> {

  private static final DateTimeFormatter BUILD_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  private final HyperFactions plugin;

  /** Cached update info from last check. */
  @Nullable
  private UpdateChecker.UpdateInfo cachedUpdate;

  /** Cached mixin update info. */
  @Nullable
  private UpdateChecker.UpdateInfo cachedMixinUpdate;

  /** Current status message. */
  private String statusMessage = "";

  /** Current mixin status message. */
  private String mixinStatusMessage = "";

  /** Prevents double-click during download. */
  private boolean downloading = false;

  /** Prevents double-click during mixin download. */
  private boolean downloadingMixin = false;

  /** Two-click confirmation for rollback. */
  private boolean rollbackConfirm = false;

  /** Whether a restart is needed after download. */
  private boolean restartRequired = false;

  /** Creates a new AdminUpdatesPage. */
  public AdminUpdatesPage(PlayerRef playerRef, GuiManager guiManager, HyperFactions plugin) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminUpdatesData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.plugin = plugin;
  }

  /** Builds the updates page. */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_UPDATES);

    AdminNavBarHelper.setupBar(playerRef, "updates", cmd, events);

    // Static labels — set once, persist across sendUpdate(false) calls
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_TITLE_UPDATES));
    buildStaticContent(cmd, events);

    // Dynamic content — changes on each rebuild
    buildDynamicContent(cmd, events);
  }

  /** Sets labels and values that never change after initial build. */
  private void buildStaticContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Version Information section
    cmd.set("#VersionInfoLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_VERSION_INFO));
    cmd.set("#CurrentVersionLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_CURRENT_VERSION));
    cmd.set("#CurrentVersionValue.Text", "v" + BuildInfo.VERSION);
    cmd.set("#BuildDateLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BUILD_DATE));
    cmd.set("#BuildDateValue.Text", BUILD_DATE_FORMATTER.format(
        Instant.ofEpochMilli(BuildInfo.BUILD_TIMESTAMP)));
    cmd.set("#ChannelLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_CHANNEL));
    cmd.set("#ChannelValue.Text", ConfigManager.get().getReleaseChannel());

    // Section titles
    cmd.set("#UpdateStatusLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_UPDATE_STATUS));
    cmd.set("#LatestVersionLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_LATEST_VERSION));
    cmd.set("#MixinTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_TITLE));
    cmd.set("#MixinVersionLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_VERSION));

    // Button labels that don't change
    cmd.set("#CheckUpdateBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_CHECK));
    cmd.set("#CheckMixinBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_CHECK));
  }

  /** Sets only the dynamic values that change between rebuilds. */
  private void buildDynamicContent(UICommandBuilder cmd, UIEventBuilder events) {
    UpdateChecker checker = plugin.getUpdateChecker();

    // Pre-populate from cached check if available
    if (cachedUpdate == null && checker != null) {
      cachedUpdate = checker.getCachedUpdate();
    }

    if (cachedUpdate != null) {
      cmd.set("#LatestVersionValue.Text", "v" + cachedUpdate.version());
      if (cachedUpdate.isPreRelease()) {
        cmd.set("#PreReleaseTag.Visible", true);
        cmd.set("#PreReleaseTag.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_PRE_RELEASE));
      } else {
        cmd.set("#PreReleaseTag.Visible", false);
      }
      if (statusMessage.isEmpty()) {
        statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_AVAILABLE,
            cachedUpdate.version());
      }

      // Show download button if update available and not already downloading
      if (!downloading && !restartRequired) {
        cmd.set("#DownloadBtn.Visible", true);
        cmd.set("#DownloadBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_DOWNLOAD));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DownloadBtn",
            EventData.of("Button", "Download"), false);
      } else {
        cmd.set("#DownloadBtn.Visible", false);
      }

      // Show changelog if available
      if (cachedUpdate.changelog() != null && !cachedUpdate.changelog().isEmpty()) {
        cmd.set("#ChangelogSection.Visible", true);
        cmd.set("#ChangelogTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_CHANGELOG_TITLE));
        String changelog = cachedUpdate.changelog();
        if (changelog.length() > 500) {
          changelog = changelog.substring(0, 497) + "...";
        }
        cmd.set("#ChangelogText.Text", changelog);
      } else {
        cmd.set("#ChangelogSection.Visible", false);
      }
    } else {
      cmd.set("#LatestVersionValue.Text", "-");
      cmd.set("#PreReleaseTag.Visible", false);
      cmd.set("#DownloadBtn.Visible", false);
      cmd.set("#ChangelogSection.Visible", false);
      if (statusMessage.isEmpty()) {
        statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_UP_TO_DATE);
      }
    }

    cmd.set("#StatusMessage.Text", statusMessage);

    // Check for Updates button event
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CheckUpdateBtn",
        EventData.of("Button", "CheckUpdate"), false);

    // Mixin section
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();
    if (mixinChecker != null) {
      cmd.set("#MixinVersionValue.Text", "v" + mixinChecker.getCurrentVersion());
      cmd.set("#MixinStatus.Text", mixinStatusMessage);
      cmd.set("#CheckMixinBtn.Visible", true);

      events.addEventBinding(CustomUIEventBindingType.Activating, "#CheckMixinBtn",
          EventData.of("Button", "CheckMixin"), false);

      if (cachedMixinUpdate != null && !downloadingMixin && !restartRequired) {
        cmd.set("#DownloadMixinBtn.Visible", true);
        cmd.set("#DownloadMixinBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_DOWNLOAD));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DownloadMixinBtn",
            EventData.of("Button", "DownloadMixin"), false);
      } else {
        cmd.set("#DownloadMixinBtn.Visible", false);
      }
    } else {
      cmd.set("#MixinVersionValue.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_NOT_INSTALLED));
      cmd.set("#CheckMixinBtn.Visible", false);
      cmd.set("#DownloadMixinBtn.Visible", false);
    }

    // Rollback section
    if (checker != null && checker.isRollbackSafe()) {
      UpdateChecker.RollbackInfo rollbackInfo = checker.getRollbackInfo();
      if (rollbackInfo != null) {
        cmd.set("#SeparatorLine4.Visible", true);
        cmd.set("#RollbackSection.Visible", true);
        cmd.set("#RollbackTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_SECTION));

        String rollbackLabel;
        if (rollbackConfirm) {
          rollbackLabel = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_CONFIRM);
        } else {
          rollbackLabel = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_ROLLBACK)
              + " v" + rollbackInfo.fromVersion();
        }
        cmd.set("#RollbackBtn.Text", rollbackLabel);
        cmd.set("#RollbackInfo.Text",
            "v" + rollbackInfo.toVersion() + " -> v" + rollbackInfo.fromVersion());
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RollbackBtn",
            EventData.of("Button", "Rollback"), false);
      } else {
        cmd.set("#SeparatorLine4.Visible", false);
        cmd.set("#RollbackSection.Visible", false);
      }
    } else {
      cmd.set("#SeparatorLine4.Visible", false);
      cmd.set("#RollbackSection.Visible", false);
    }

    // Restart note
    if (restartRequired) {
      cmd.set("#RestartNote.Visible", true);
      cmd.set("#RestartNote.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_RESTART_REQUIRED));
    } else {
      cmd.set("#RestartNote.Visible", false);
    }
  }

  /**
   * Resolves the player's world for dispatching async results back to the world thread.
   */
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
    buildDynamicContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminUpdatesData data) {
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
      case "CheckUpdate" -> handleCheckUpdate();
      case "Download" -> handleDownload();
      case "CheckMixin" -> handleCheckMixin();
      case "DownloadMixin" -> handleDownloadMixin();
      case "Rollback" -> handleRollback(player);
      case "Back" -> guiManager.closePage(player, ref, store);
      default -> { }
    }
  }

  private void handleCheckUpdate() {
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker == null) {
      statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_FAILED);
      rebuildOnWorldThread();
      return;
    }

    statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_CHECKING);
    rebuildOnWorldThread();

    checker.checkForUpdates(true).thenAccept(info -> {
      World world = resolveWorld();
      if (world == null) {
        return;
      }
      world.execute(() -> {
        cachedUpdate = info;
        if (info != null) {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_AVAILABLE,
              info.version());
        } else {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_UP_TO_DATE);
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleDownload() {
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker == null || cachedUpdate == null || downloading) {
      return;
    }

    downloading = true;
    statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_DOWNLOADING);
    rebuildOnWorldThread();

    checker.downloadUpdate(cachedUpdate).thenAccept(path -> {
      World world = resolveWorld();
      if (world == null) {
        downloading = false;
        return;
      }
      world.execute(() -> {
        downloading = false;
        if (path != null) {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_DOWNLOADED);
          restartRequired = true;
          checker.createRollbackMarker(BuildInfo.VERSION, cachedUpdate.version());
          checker.cleanupOldBackups(BuildInfo.VERSION);
          Logger.info("[Updates] %s downloaded update v%s via admin GUI",
              playerRef.getUsername(), cachedUpdate.version());
        } else {
          statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_FAILED);
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleCheckMixin() {
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();
    if (mixinChecker == null) {
      return;
    }

    mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_CHECKING);
    rebuildOnWorldThread();

    mixinChecker.checkForUpdates(true).thenAccept(info -> {
      World world = resolveWorld();
      if (world == null) {
        return;
      }
      world.execute(() -> {
        cachedMixinUpdate = info;
        if (info != null) {
          mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_AVAILABLE,
              info.version());
        } else {
          mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_UP_TO_DATE);
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleDownloadMixin() {
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();
    if (mixinChecker == null || cachedMixinUpdate == null || downloadingMixin) {
      return;
    }

    downloadingMixin = true;
    mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_DOWNLOADING);
    rebuildOnWorldThread();

    mixinChecker.downloadUpdate(cachedMixinUpdate).thenAccept(path -> {
      World world = resolveWorld();
      if (world == null) {
        downloadingMixin = false;
        return;
      }
      world.execute(() -> {
        downloadingMixin = false;
        if (path != null) {
          mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_DOWNLOADED);
          restartRequired = true;
          Logger.info("[Updates] %s downloaded mixin update v%s via admin GUI",
              playerRef.getUsername(), cachedMixinUpdate.version());
        } else {
          mixinStatusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_FAILED);
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleRollback(Player player) {
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker == null || !checker.isRollbackSafe()) {
      statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_UNSAFE);
      rebuildOnWorldThread();
      return;
    }

    if (!rollbackConfirm) {
      rollbackConfirm = true;
      rebuildOnWorldThread();
      return;
    }

    rollbackConfirm = false;
    UpdateChecker.RollbackResult result = checker.performRollback();
    if (result.success()) {
      statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_SUCCESS,
          result.restoredVersion());
      restartRequired = true;
      Logger.info("[Updates] %s rolled back to v%s via admin GUI",
          playerRef.getUsername(), result.restoredVersion());
    } else {
      statusMessage = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_FAILED,
          result.errorMessage());
    }
    rebuildOnWorldThread();
  }
}
