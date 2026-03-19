package com.hyperfactions.gui.admin.page;

import com.hyperfactions.BuildInfo;
import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminUpdatesData;
import com.hyperfactions.integration.protection.ProtectionMixinBridge;
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
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Updates page — two-column layout showing HyperFactions and HyperProtect Mixin
 * version info, update status, and download/rollback actions.
 */
public class AdminUpdatesPage extends InteractiveCustomUIPage<AdminUpdatesData> {

  private static final DateTimeFormatter BUILD_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;
  private final GuiManager guiManager;
  private final HyperFactions plugin;

  @Nullable private UpdateChecker.UpdateInfo cachedUpdate;
  @Nullable private UpdateChecker.UpdateInfo cachedMixinUpdate;
  private String hfStatus = "";
  private String hpStatus = "";
  private boolean downloading = false;
  private boolean downloadingMixin = false;
  private boolean rollbackConfirm = false;
  private boolean restartRequired = false;

  public AdminUpdatesPage(PlayerRef playerRef, GuiManager guiManager, HyperFactions plugin) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminUpdatesData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.plugin = plugin;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                     UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_UPDATES);
    AdminNavBarHelper.setupBar(playerRef, "updates", cmd, events);
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_TITLE_UPDATES));
    buildDynamicContent(cmd, events);
  }

  private void buildDynamicContent(UICommandBuilder cmd, UIEventBuilder events) {
    buildHyperFactionsColumn(cmd, events);
    buildHyperProtectColumn(cmd, events);
    buildActionBar(cmd, events);
  }

  // ── HyperFactions column ──────────────────────────────────────────────────

  private void buildHyperFactionsColumn(UICommandBuilder cmd, UIEventBuilder events) {
    UpdateChecker checker = plugin.getUpdateChecker();

    // Pre-populate from cached check
    if (cachedUpdate == null && checker != null) {
      cachedUpdate = checker.getCachedUpdate();
    }

    cmd.set("#HFCurrentVersion.Text", "v" + BuildInfo.VERSION);
    cmd.set("#HFChannel.Text", ConfigManager.get().getReleaseChannel());
    cmd.set("#HFBuildDate.Text", BUILD_DATE_FORMATTER.format(
        Instant.ofEpochMilli(BuildInfo.BUILD_TIMESTAMP)));

    if (cachedUpdate != null) {
      String latestText = "v" + cachedUpdate.version();
      if (cachedUpdate.isPreRelease()) {
        latestText += " (pre-release)";
      }
      cmd.set("#HFLatestVersion.Text", latestText);
      if (hfStatus.isEmpty()) {
        hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_AVAILABLE,
            cachedUpdate.version());
      }

      if (!downloading && !restartRequired) {
        cmd.set("#DownloadBtn.Visible", true);
        cmd.set("#DownloadBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_DOWNLOAD));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#DownloadBtn",
            EventData.of("Button", "Download"), false);
      } else {
        cmd.set("#DownloadBtn.Visible", false);
      }

      if (cachedUpdate.changelog() != null && !cachedUpdate.changelog().isEmpty()) {
        cmd.set("#ChangelogSection.Visible", true);
        String changelog = cachedUpdate.changelog();
        if (changelog.length() > 500) changelog = changelog.substring(0, 497) + "...";
        cmd.set("#ChangelogText.Text", changelog);
      } else {
        cmd.set("#ChangelogSection.Visible", false);
      }
    } else {
      cmd.set("#HFLatestVersion.Text", "v" + BuildInfo.VERSION);
      cmd.set("#DownloadBtn.Visible", false);
      cmd.set("#ChangelogSection.Visible", false);
      if (hfStatus.isEmpty()) {
        hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_UP_TO_DATE);
      }
    }

    cmd.set("#HFStatus.Text", hfStatus);
  }

  // ── HyperProtect column ───────────────────────────────────────────────────

  private void buildHyperProtectColumn(UICommandBuilder cmd, UIEventBuilder events) {
    ProtectionMixinBridge.MixinProvider provider = ProtectionMixinBridge.getProvider();
    boolean mixinInstalled = provider == ProtectionMixinBridge.MixinProvider.HYPERPROTECT
        || provider == ProtectionMixinBridge.MixinProvider.BOTH;
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();

    if (mixinInstalled) {
      String hpVersion = System.getProperty("hyperprotect.bridge.version", "unknown");
      cmd.set("#HPCurrentVersion.Text", "v" + hpVersion);

      if (cachedMixinUpdate != null) {
        cmd.set("#HPLatestVersion.Text", "v" + cachedMixinUpdate.version());
        if (hpStatus.isEmpty()) {
          hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_AVAILABLE,
              cachedMixinUpdate.version());
        }

        if (!downloadingMixin && !restartRequired) {
          cmd.set("#DownloadMixinBtn.Visible", true);
          cmd.set("#DownloadMixinBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_DOWNLOAD));
          events.addEventBinding(CustomUIEventBindingType.Activating, "#DownloadMixinBtn",
              EventData.of("Button", "DownloadMixin"), false);
        } else {
          cmd.set("#DownloadMixinBtn.Visible", false);
        }
      } else {
        cmd.set("#HPLatestVersion.Text", "v" + hpVersion);
        cmd.set("#DownloadMixinBtn.Visible", false);
        if (hpStatus.isEmpty()) {
          hpStatus = mixinChecker != null
              ? HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_UP_TO_DATE)
              : "Installed";
        }
      }
    } else {
      cmd.set("#HPCurrentVersion.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_NOT_INSTALLED));
      cmd.set("#HPLatestVersion.Text", "-");
      cmd.set("#DownloadMixinBtn.Visible", false);
      if (hpStatus.isEmpty()) {
        hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_NOT_INSTALLED);
      }
    }

    cmd.set("#HPStatus.Text", hpStatus);
  }

  // ── Action bar ────────────────────────────────────────────────────────────

  private void buildActionBar(UICommandBuilder cmd, UIEventBuilder events) {
    // Check for Updates — checks both HF and HP at once
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CheckUpdateBtn",
        EventData.of("Button", "CheckUpdate"), false);

    // Rollback
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker != null && checker.isRollbackSafe()) {
      UpdateChecker.RollbackInfo rollbackInfo = checker.getRollbackInfo();
      if (rollbackInfo != null) {
        cmd.set("#RollbackBtn.Visible", true);
        String rollbackLabel = rollbackConfirm
            ? HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_CONFIRM)
            : HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_BTN_ROLLBACK)
                + " v" + rollbackInfo.fromVersion();
        cmd.set("#RollbackBtn.Text", rollbackLabel);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RollbackBtn",
            EventData.of("Button", "Rollback"), false);
      } else {
        cmd.set("#RollbackBtn.Visible", false);
      }
    } else {
      cmd.set("#RollbackBtn.Visible", false);
    }

    // Restart note
    if (restartRequired) {
      cmd.set("#RestartNote.Visible", true);
      cmd.set("#RestartNote.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_RESTART_REQUIRED));
    } else {
      cmd.set("#RestartNote.Visible", false);
    }
  }

  // ── Event handling ────────────────────────────────────────────────────────

  @Nullable
  private World resolveWorld() {
    UUID worldUuid = playerRef.getWorldUuid();
    return worldUuid != null ? Universe.get().getWorld(worldUuid) : null;
  }

  private void refresh() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildDynamicContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                               AdminUpdatesData data) {
    super.handleDataEvent(ref, store, data);
    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || pRef == null) return;
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, pRef, guiManager)) return;
    if (data.button == null) return;

    switch (data.button) {
      case "CheckUpdate" -> handleCheckAll();
      case "Download" -> handleDownload();
      case "DownloadMixin" -> handleDownloadMixin();
      case "Rollback" -> handleRollback(player);
      case "Back" -> guiManager.closePage(player, ref, store);
      default -> { }
    }
  }

  /** Checks both HF and HP updates simultaneously. */
  private void handleCheckAll() {
    hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_CHECKING);
    hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_CHECKING);
    refresh();

    UpdateChecker checker = plugin.getUpdateChecker();
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();

    CompletableFuture<UpdateChecker.UpdateInfo> hfFuture = checker != null
        ? checker.checkForUpdates(true) : CompletableFuture.completedFuture(null);
    CompletableFuture<UpdateChecker.UpdateInfo> hpFuture = mixinChecker != null
        ? mixinChecker.checkForUpdates(true) : CompletableFuture.completedFuture(null);

    hfFuture.thenCombine(hpFuture, (hfInfo, hpInfo) -> {
      World world = resolveWorld();
      if (world == null) return null;
      world.execute(() -> {
        cachedUpdate = hfInfo;
        hfStatus = hfInfo != null
            ? HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_AVAILABLE, hfInfo.version())
            : HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_UP_TO_DATE);

        cachedMixinUpdate = hpInfo;
        if (mixinChecker != null) {
          hpStatus = hpInfo != null
              ? HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_AVAILABLE, hpInfo.version())
              : HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_UP_TO_DATE);
        } else {
          hpStatus = "";
        }
        refresh();
      });
      return null;
    });
  }

  private void handleDownload() {
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker == null || cachedUpdate == null || downloading) return;

    downloading = true;
    hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_DOWNLOADING);
    refresh();

    checker.downloadUpdate(cachedUpdate).thenAccept(path -> {
      World world = resolveWorld();
      if (world == null) { downloading = false; return; }
      world.execute(() -> {
        downloading = false;
        if (path != null) {
          hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_DOWNLOADED);
          restartRequired = true;
          checker.createRollbackMarker(BuildInfo.VERSION, cachedUpdate.version());
          checker.cleanupOldBackups(BuildInfo.VERSION);
          Logger.info("[Updates] %s downloaded update v%s via admin GUI",
              playerRef.getUsername(), cachedUpdate.version());
        } else {
          hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_STATUS_FAILED);
        }
        refresh();
      });
    });
  }

  private void handleDownloadMixin() {
    UpdateChecker mixinChecker = plugin.getHyperProtectUpdateChecker();
    if (mixinChecker == null || cachedMixinUpdate == null || downloadingMixin) return;

    downloadingMixin = true;
    hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_DOWNLOADING);
    refresh();

    mixinChecker.downloadUpdate(cachedMixinUpdate).thenAccept(path -> {
      World world = resolveWorld();
      if (world == null) { downloadingMixin = false; return; }
      world.execute(() -> {
        downloadingMixin = false;
        if (path != null) {
          hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_DOWNLOADED);
          restartRequired = true;
          Logger.info("[Updates] %s downloaded mixin update v%s via admin GUI",
              playerRef.getUsername(), cachedMixinUpdate.version());
        } else {
          hpStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_MIXIN_FAILED);
        }
        refresh();
      });
    });
  }

  private void handleRollback(Player player) {
    UpdateChecker checker = plugin.getUpdateChecker();
    if (checker == null || !checker.isRollbackSafe()) {
      hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_UNSAFE);
      refresh();
      return;
    }

    if (!rollbackConfirm) {
      rollbackConfirm = true;
      refresh();
      return;
    }

    rollbackConfirm = false;
    UpdateChecker.RollbackResult result = checker.performRollback();
    if (result.success()) {
      hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_SUCCESS,
          result.restoredVersion());
      restartRequired = true;
      Logger.info("[Updates] %s rolled back to v%s via admin GUI",
          playerRef.getUsername(), result.restoredVersion());
    } else {
      hfStatus = HFMessages.get(playerRef, AdminGuiKeys.AdminGui.UPD_ROLLBACK_FAILED,
          result.errorMessage());
    }
    refresh();
  }
}
