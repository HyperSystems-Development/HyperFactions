package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZoneSettingsData;
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

/**
 * Admin Zone Integration Flags page - configure integration-specific zone flags.
 * Shows integration flags (e.g., GRAVESTONE_ACCESS) with toggle buttons.
 * Reuses AdminZoneSettingsData codec since it has the same field structure.
 */
public class AdminZoneIntegrationFlagsPage extends InteractiveCustomUIPage<AdminZoneSettingsData> {

  private final PlayerRef playerRef;

  private final UUID zoneId;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private final GravestoneIntegration gravestoneIntegration;

  /** Creates a new AdminZoneIntegrationFlagsPage. */
  public AdminZoneIntegrationFlagsPage(PlayerRef playerRef,
                     UUID zoneId,
                     ZoneManager zoneManager,
                     GuiManager guiManager,
                     GravestoneIntegration gravestoneIntegration) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneSettingsData.CODEC);
    this.playerRef = playerRef;
    this.zoneId = zoneId;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.gravestoneIntegration = gravestoneIntegration;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the integration flags template
    cmd.append(UIPaths.ADMIN_ZONE_INTEGRATION_FLAGS);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

    // Get the zone
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      cmd.set("#ZoneName.Text", "Zone Not Found");
      cmd.set("#FlagsContainer.Visible", false);
      return;
    }

    // Zone info header
    cmd.set("#ZoneName.Text", zone.name());
    cmd.set("#ZoneType.Text", zone.type().name());
    String typeColor = zone.isSafeZone() ? "#55FF55" : "#FF5555";
    cmd.set("#ZoneType.Style.TextColor", typeColor);

    // Build integration flag toggles
    buildFlagCategory(cmd, events, zone, ZoneFlags.INTEGRATION_FLAGS, 0);

    // Map visibility selection (only visible when SHOW_ON_MAP is enabled)
    buildMapVisibilityControl(cmd, events, zone);

    // Reset to Defaults button — only enable if zone has any integration flags set
    boolean hasIntegrationFlags = hasCustomIntegrationFlags(zone);
    if (hasIntegrationFlags) {
      cmd.set("#ResetBtn.Disabled", false);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ResetBtn",
          EventData.of("Button", "ResetDefaults")
              .append("ZoneId", zoneId.toString()),
          false
      );
    } else {
      cmd.set("#ResetBtn.Disabled", true);
    }

    // Back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back")
            .append("ZoneId", zoneId.toString()),
        false
    );
  }

  private void buildFlagCategory(UICommandBuilder cmd, UIEventBuilder events,
                  Zone zone, String[] flags, int startIndex) {
    for (int i = 0; i < flags.length; i++) {
      String flagName = flags[i];
      int index = startIndex + i;
      buildFlagToggle(cmd, events, index, flagName, zone);
    }
  }

  private void buildFlagToggle(UICommandBuilder cmd, UIEventBuilder events,
                 int index, String flagName, Zone zone) {
    String idx = "#Flag" + index;

    boolean currentValue = zone.getEffectiveFlag(flagName);
    boolean isDefault = !zone.hasFlagSet(flagName);

    // Check if the integration for this flag is available
    boolean integrationUnavailable = !isIntegrationAvailable(flagName);

    // Flag name (display name from ZoneFlags)
    String displayName = ZoneFlags.getDisplayName(flagName);
    cmd.set(idx + "Name.Text", displayName);

    // Set checkbox value via child selector
    // When integration is unavailable, show as unchecked
    boolean displayValue = integrationUnavailable ? false : currentValue;
    cmd.set(idx + "Toggle #CheckBox.Value", displayValue);
    cmd.set(idx + "Toggle #CheckBox.Disabled", integrationUnavailable);

    // Default indicator (shows "(default)", "(custom)", or "(no plugin)")
    if (integrationUnavailable) {
      cmd.set(idx + "Default.Text", "(no plugin)");
      cmd.set(idx + "Default.Style.TextColor", "#FF5555");
    } else if (isDefault) {
      cmd.set(idx + "Default.Text", "(default)");
      cmd.set(idx + "Default.Style.TextColor", "#555555");
    } else {
      cmd.set(idx + "Default.Text", "(custom)");
      cmd.set(idx + "Default.Style.TextColor", "#FFAA00");
    }

    // ValueChanged event (disabled checkboxes won't fire)
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        idx + "Toggle #CheckBox",
        EventData.of("Button", "ToggleFlag")
            .append("Flag", flagName)
            .append("ZoneId", zoneId.toString()),
        false
    );
  }

  private void buildMapVisibilityControl(UICommandBuilder cmd, UIEventBuilder events, Zone zone) {
    boolean showOnMapEnabled = zone.getEffectiveFlag(ZoneFlags.SHOW_ON_MAP);
    String visibility = zone.getEffectiveSetting(ZoneFlags.MAP_VISIBILITY);
    boolean isDefault = !zone.hasSettingSet(ZoneFlags.MAP_VISIBILITY);

    // Hide/show the visibility row based on toggle state
    cmd.set("#MapVisibilityRow.Visible", showOnMapEnabled);

    if (showOnMapEnabled) {
      // Set button text to current selection
      String displayText = ZoneFlags.getSettingValueDisplay(ZoneFlags.MAP_VISIBILITY, visibility);
      cmd.set("#MapVisibilityBtn.Text", displayText);

      // Default indicator
      if (isDefault) {
        cmd.set("#MapVisibilityDefault.Text", "(default)");
        cmd.set("#MapVisibilityDefault.Style.TextColor", "#555555");
      } else {
        cmd.set("#MapVisibilityDefault.Text", "(custom)");
        cmd.set("#MapVisibilityDefault.Style.TextColor", "#FFAA00");
      }

      // Cycle button event
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#MapVisibilityBtn",
          EventData.of("Button", "CycleMapVisibility")
              .append("ZoneId", zoneId.toString()),
          false
      );
    }
  }

  /**
   * Checks if the integration required for a given flag is available.
   */
  private boolean isIntegrationAvailable(String flagName) {
    return switch (flagName) {
      case ZoneFlags.GRAVESTONE_ACCESS ->
          gravestoneIntegration != null && gravestoneIntegration.isAvailable()
              && ConfigManager.get().gravestones().isEnabled();
      default -> true;
    };
  }

  /**
   * Checks if the zone has any custom integration flags set.
   */
  private boolean hasCustomIntegrationFlags(Zone zone) {
    for (String flag : ZoneFlags.INTEGRATION_FLAGS) {
      if (zone.hasFlagSet(flag)) {
        return true;
      }
    }
    for (String setting : ZoneFlags.ALL_SETTINGS) {
      if (zone.hasSettingSet(setting)) {
        return true;
      }
    }
    return false;
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminZoneSettingsData data) {
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
      case "ToggleFlag" -> handleToggleFlag(player, data);
      case "CycleMapVisibility" -> handleCycleMapVisibility(player);
      case "ResetDefaults" -> handleResetDefaults(player);
      case "Back" -> guiManager.openAdminZoneSettings(player, ref, store, playerRef, zoneId);
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleToggleFlag(Player player, AdminZoneSettingsData data) {
    String flagName = data.flag;
    if (flagName == null || !ZoneFlags.isValidFlag(flagName)) {
      player.sendMessage(MessageUtil.adminError("Invalid flag."));
      sendUpdate();
      return;
    }

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.adminError("Zone not found."));
      sendUpdate();
      return;
    }

    // Toggle the flag
    boolean currentValue = zone.getEffectiveFlag(flagName);
    boolean newValue = !currentValue;
    boolean defaultValue = ZoneFlags.getDefault(flagName, zone.type());

    if (newValue == defaultValue) {
      // Clear the flag to use default
      zoneManager.clearZoneFlag(zoneId, flagName);
    } else {
      // Set custom value
      zoneManager.setZoneFlag(zoneId, flagName, newValue);
    }

    rebuildPage();
  }

  private void handleCycleMapVisibility(Player player) {
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.adminError("Zone not found."));
      sendUpdate();
      return;
    }

    String current = zone.getEffectiveSetting(ZoneFlags.MAP_VISIBILITY);
    String[] options = ZoneFlags.MAP_VISIBILITY_OPTIONS;

    // Find current index and advance to next
    int currentIdx = 0;
    for (int i = 0; i < options.length; i++) {
      if (options[i].equals(current)) {
        currentIdx = i;
        break;
      }
    }
    String next = options[(currentIdx + 1) % options.length];

    String defaultVal = ZoneFlags.getSettingDefault(ZoneFlags.MAP_VISIBILITY, zone.type());
    if (next.equals(defaultVal)) {
      zoneManager.clearZoneSetting(zoneId, ZoneFlags.MAP_VISIBILITY);
    } else {
      zoneManager.setZoneSetting(zoneId, ZoneFlags.MAP_VISIBILITY, next);
    }

    rebuildPage();
  }

  private void handleResetDefaults(Player player) {
    // Clear only integration flags and settings, not all zone flags
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.adminError("Zone not found."));
      sendUpdate();
      return;
    }

    for (String flag : ZoneFlags.INTEGRATION_FLAGS) {
      if (zone.hasFlagSet(flag)) {
        zoneManager.clearZoneFlag(zoneId, flag);
      }
    }
    for (String setting : ZoneFlags.ALL_SETTINGS) {
      if (zone.hasSettingSet(setting)) {
        zoneManager.clearZoneSetting(zoneId, setting);
      }
    }

    player.sendMessage(MessageUtil.adminSuccess("Reset integration flags to defaults."));
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

    // Rebuild integration flag toggles
    buildFlagCategory(cmd, events, zone, ZoneFlags.INTEGRATION_FLAGS, 0);

    // Rebuild map visibility control
    buildMapVisibilityControl(cmd, events, zone);

    // Update reset button state
    boolean hasIntegrationFlags = hasCustomIntegrationFlags(zone);
    if (hasIntegrationFlags) {
      cmd.set("#ResetBtn.Disabled", false);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ResetBtn",
          EventData.of("Button", "ResetDefaults")
              .append("ZoneId", zoneId.toString()),
          false
      );
    } else {
      cmd.set("#ResetBtn.Disabled", true);
    }

    sendUpdate(cmd, events, false);
  }
}
