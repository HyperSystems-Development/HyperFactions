package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZonePropertiesData;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

/**
 * Admin Zone Properties page - configure zone name, type, and notification settings.
 * Separate from the flags page (AdminZoneSettingsPage).
 *
 * <p>Layout:
 * - General section: zone name editing, zone type with change button
 * - Notifications section: entry notification toggle, upper/lower title customization
 * - Footer: Edit Flags button, Back to Zones button
 */
public class AdminZonePropertiesPage extends InteractiveCustomUIPage<AdminZonePropertiesData> {

  private final PlayerRef playerRef;

  private final UUID zoneId;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private final String currentTab;

  private final int currentPage;

  /** Transient error message to display after a failed operation. */
  private String nameError;

  /** Creates a new AdminZonePropertiesPage. */
  public AdminZonePropertiesPage(PlayerRef playerRef,
                 UUID zoneId,
                 ZoneManager zoneManager,
                 GuiManager guiManager,
                 String currentTab,
                 int currentPage) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminZonePropertiesData.CODEC);
    this.playerRef = playerRef;
    this.zoneId = zoneId;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.currentTab = currentTab;
    this.currentPage = currentPage;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    // Load the template
    cmd.append(UIPaths.ADMIN_ZONE_PROPERTIES);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

    // Get the zone
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      cmd.set("#ZoneName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZINT_ZONE_NOT_FOUND));
      cmd.set("#GeneralBox.Visible", false);
      cmd.set("#NotificationsBox.Visible", false);
      return;
    }

    // Header
    buildHeader(cmd, zone);

    // Sections
    buildGeneral(cmd, events, zone);
    buildNotifications(cmd, events, zone);

    // Footer
    buildFooter(cmd, events);
  }

  private void buildHeader(UICommandBuilder cmd, Zone zone) {
    cmd.set("#ZoneName.Text", zone.name());
    cmd.set("#TypeBadge.Text", zone.type().name());
    cmd.set("#TypeBadge.Style.TextColor", zone.isSafeZone() ? "#55FF55" : "#FF5555");
    cmd.set("#ChunkCount.Text", zone.getChunkCount() + " chunks");
  }

  private void buildGeneral(UICommandBuilder cmd, UIEventBuilder events, Zone zone) {
    // Name input pre-filled with current name
    cmd.set("#NameInput.Value", zone.name());

    // Save Name button - reads @Name from #NameInput.Value
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveNameBtn",
        EventData.of("Button", "SaveName")
            .append("ZoneId", zoneId.toString())
            .append("@Name", "#NameInput.Value"),
        false
    );

    // Name error display
    if (nameError != null) {
      cmd.set("#NameError.Visible", true);
      cmd.set("#NameError.Text", nameError);
    } else {
      cmd.set("#NameError.Visible", false);
    }

    // Current type display
    cmd.set("#CurrentType.Text", zone.type().getDisplayName());
    cmd.set("#CurrentType.Style.TextColor", zone.isSafeZone() ? "#55FF55" : "#FF5555");

    // Change Type button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ChangeTypeBtn",
        EventData.of("Button", "ChangeType")
            .append("ZoneId", zoneId.toString()),
        false
    );
  }

  private void buildNotifications(UICommandBuilder cmd, UIEventBuilder events, Zone zone) {
    // Notify on entry toggle
    boolean notifyEnabled = zone.notifyOnEntry() == null || zone.notifyOnEntry();
    cmd.set("#NotifyToggle #CheckBox.Value", notifyEnabled);

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#NotifyToggle #CheckBox",
        EventData.of("Button", "ToggleNotify")
            .append("ZoneId", zoneId.toString()),
        false
    );

    // Upper title
    String upperCustom = zone.notifyTitleUpper();
    if (upperCustom != null && !upperCustom.isEmpty()) {
      cmd.set("#UpperCurrent.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_CURRENT_CUSTOM, upperCustom));
      cmd.set("#UpperTitleInput.Value", upperCustom);
    } else {
      String defaultUpper = zone.isSafeZone() ? HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_PVP_DISABLED) : HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_PVP_ENABLED);
      cmd.set("#UpperCurrent.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_CURRENT_DEFAULT, defaultUpper));
    }

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveUpperBtn",
        EventData.of("Button", "SaveUpper")
            .append("ZoneId", zoneId.toString())
            .append("@UpperTitle", "#UpperTitleInput.Value"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ClearUpperBtn",
        EventData.of("Button", "ClearUpper")
            .append("ZoneId", zoneId.toString()),
        false
    );

    // Lower title
    String lowerCustom = zone.notifyTitleLower();
    if (lowerCustom != null && !lowerCustom.isEmpty()) {
      cmd.set("#LowerCurrent.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_CURRENT_CUSTOM, lowerCustom));
      cmd.set("#LowerTitleInput.Value", lowerCustom);
    } else {
      cmd.set("#LowerCurrent.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_CURRENT_DEFAULT, zone.name()));
    }

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveLowerBtn",
        EventData.of("Button", "SaveLower")
            .append("ZoneId", zoneId.toString())
            .append("@LowerTitle", "#LowerTitleInput.Value"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ClearLowerBtn",
        EventData.of("Button", "ClearLower")
            .append("ZoneId", zoneId.toString()),
        false
    );
  }

  private void buildFooter(UICommandBuilder cmd, UIEventBuilder events) {
    // Edit Flags button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#FlagsBtn",
        EventData.of("Button", "Flags"),
        false
    );

    // Back to Zones button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminZonePropertiesData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle admin nav bar navigation
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button == null) {
      return;
    }

    switch (data.button) {
      case "SaveName" -> handleSaveName(player, data);
      case "ChangeType" -> {
        guiManager.openZoneChangeTypeModal(player, ref, store, playerRef,
            zoneId, true, currentTab, currentPage);
      }
      case "ToggleNotify" -> handleToggleNotify(player);
      case "SaveUpper" -> handleSaveUpper(player, data);
      case "ClearUpper" -> handleClearUpper(player);
      case "SaveLower" -> handleSaveLower(player, data);
      case "ClearLower" -> handleClearLower(player);
      case "Flags" -> {
        guiManager.openAdminZoneSettings(player, ref, store, playerRef,
            zoneId, "settings", currentTab, currentPage);
      }
      case "Back" -> {
        guiManager.openAdminZone(player, ref, store, playerRef, currentTab, currentPage);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleSaveName(Player player, AdminZonePropertiesData data) {
    String newName = data.name;
    if (newName == null || newName.isBlank()) {
      nameError = HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_NAME_EMPTY);
      rebuildPage();
      return;
    }

    newName = newName.trim();
    ZoneManager.ZoneResult result = zoneManager.renameZone(zoneId, newName);

    switch (result) {
      case SUCCESS -> {
        nameError = null;
        player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZPROP_RENAMED, newName));
      }
      case NAME_TAKEN -> nameError = HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_NAME_TAKEN);
      case INVALID_NAME -> nameError = HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_NAME_INVALID);
      default -> nameError = HFMessages.get(playerRef, MessageKeys.AdminGui.ZPROP_RENAME_FAILED, result);
    }

    rebuildPage();
  }

  private void handleToggleNotify(Player player) {
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      sendUpdate();
      return;
    }

    // Toggle: if currently enabled (null or true), set to false; if false, set to null (default=true)
    boolean currentlyEnabled = zone.notifyOnEntry() == null || zone.notifyOnEntry();
    Boolean newValue = currentlyEnabled ? Boolean.FALSE : null;

    zoneManager.setZoneNotifyOnEntry(zoneId, newValue);
    rebuildPage();
  }

  private void handleSaveUpper(Player player, AdminZonePropertiesData data) {
    String upper = data.upperTitle;
    if (upper == null || upper.isBlank()) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZPROP_UPPER_EMPTY));
      sendUpdate();
      return;
    }

    zoneManager.setZoneNotifyTitle(zoneId, upper.trim(), null);
    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZPROP_UPPER_SET));
    rebuildPage();
  }

  private void handleClearUpper(Player player) {
    zoneManager.setZoneNotifyTitle(zoneId, "clear", null);
    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZPROP_UPPER_RESET));
    rebuildPage();
  }

  private void handleSaveLower(Player player, AdminZonePropertiesData data) {
    String lower = data.lowerTitle;
    if (lower == null || lower.isBlank()) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZPROP_LOWER_EMPTY));
      sendUpdate();
      return;
    }

    zoneManager.setZoneNotifyTitle(zoneId, null, lower.trim());
    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZPROP_LOWER_SET));
    rebuildPage();
  }

  private void handleClearLower(Player player) {
    zoneManager.setZoneNotifyTitle(zoneId, null, "clear");
    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZPROP_LOWER_RESET));
    rebuildPage();
  }

  private void rebuildPage() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      sendUpdate();
      return;
    }

    // Rebuild all dynamic content
    buildHeader(cmd, zone);
    buildGeneral(cmd, events, zone);
    buildNotifications(cmd, events, zone);
    buildFooter(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
