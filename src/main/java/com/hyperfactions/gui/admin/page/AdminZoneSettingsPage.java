package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZoneSettingsData;
import com.hyperfactions.integration.protection.ProtectionMixinBridge;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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
 * Admin Zone Settings page - configure zone flags visually.
 * Shows 46 core zone flags with toggle buttons and default indicators in 3-column layout.
 * Integration flags are on a separate sub-page (AdminZoneIntegrationFlagsPage).
 *
 * <p>Layout (indices — 46 total):
 * - Left column: Combat (0-6), Damage (7-10), Death (11-12)
 * - Middle column: Building (13-16), Interaction (17-29)
 * - Right column: Transport (30-32), Items (33-36), Spawning (37-41), Mob Clearing (42-45)
 *
 * <p>Mixin-dependent flags are disabled when no mixin system (HyperProtect or OrbisGuard) is detected.
 * Uses ProtectionMixinBridge for provider-agnostic mixin feature detection.
 */
public class AdminZoneSettingsPage extends InteractiveCustomUIPage<AdminZoneSettingsData> {

  private final PlayerRef playerRef;

  private final UUID zoneId;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private final String backTarget;

  private final String currentTab;

  private final int currentPage;

  /**
   * Creates a flags page with default back behavior (returns to zone list).
   */
  public AdminZoneSettingsPage(PlayerRef playerRef,
                UUID zoneId,
                ZoneManager zoneManager,
                GuiManager guiManager) {
    this(playerRef, zoneId, zoneManager, guiManager, null, null, 0);
  }

  /**
   * Creates a flags page with configurable back behavior.
   *
   * @param backTarget  "settings" to return to properties page, null/other for zone list
   * @param currentTab  The current tab filter in AdminZonePage
   * @param currentPage The current page number in AdminZonePage
   */
  public AdminZoneSettingsPage(PlayerRef playerRef,
                UUID zoneId,
                ZoneManager zoneManager,
                GuiManager guiManager,
                String backTarget,
                String currentTab,
                int currentPage) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneSettingsData.CODEC);
    this.playerRef = playerRef;
    this.zoneId = zoneId;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.backTarget = backTarget;
    this.currentTab = currentTab;
    this.currentPage = currentPage;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the admin zone settings template
    cmd.append(UIPaths.ADMIN_ZONE_SETTINGS);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

    // Localize labels
    cmd.set("#Title.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_ZONE_SETTINGS));
    cmd.set("#CatCombat.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_COMBAT));
    cmd.set("#CatDamage.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_DAMAGE));
    cmd.set("#CatDeath.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_DEATH));
    cmd.set("#CatBuilding.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_BUILDING));
    cmd.set("#CatInteraction.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_INTERACTION));
    cmd.set("#CatTransport.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_TRANSPORT));
    cmd.set("#CatItems.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_ITEMS));
    cmd.set("#CatSpawning.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_SPAWNING));
    cmd.set("#CatMobClear.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CAT_MOB_CLEAR));
    String childrenHint = HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_CHILDREN_HINT);
    cmd.set("#CatCombatSub.Text", childrenHint);
    cmd.set("#CatBuildingSub.Text", childrenHint);
    cmd.set("#CatInteractionSub.Text", childrenHint);
    cmd.set("#CatSpawningSub.Text", childrenHint);
    cmd.set("#CatMobClearSub.Text", childrenHint);
    cmd.set("#ResetBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_RESET_DEFAULTS));
    cmd.set("#IntegrationFlagsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_INTEGRATION_FLAGS));
    cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZSET_BACK_TO_ZONES));

    // Get the zone
    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      cmd.set("#ZoneName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZINT_ZONE_NOT_FOUND));
      cmd.set("#FlagsContainer.Visible", false);
      return;
    }

    // Zone info header
    cmd.set("#ZoneName.Text", zone.name());
    cmd.set("#ZoneType.Text", zone.type().name());
    cmd.set("#ZoneChunks.Text", zone.getChunkCount() + " chunks");

    // Type indicator color
    String typeColor = zone.isSafeZone() ? "#55FF55" : "#FF5555";
    cmd.set("#ZoneType.Style.TextColor", typeColor);

    // Build flag toggles by category (matching 3-column UI layout)
    // Left column: Combat (0-6), Damage (7-10), Death (11-12)
    buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
    buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 7);
    buildFlagCategory(cmd, events, zone, "Death", ZoneFlags.DEATH_FLAGS, 11);
    // Middle column: Building (13-16), Interaction (17-29)
    buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 13);
    buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 17);
    // Right column: Transport (30-32), Items (33-36), Spawning (37-41)
    buildFlagCategory(cmd, events, zone, "Transport", ZoneFlags.TRANSPORT_FLAGS, 30);
    buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 33);
    buildFlagCategory(cmd, events, zone, "Spawning", ZoneFlags.SPAWNING_FLAGS, 37);
    buildFlagCategory(cmd, events, zone, "Mob Clearing", ZoneFlags.MOB_CLEAR_FLAGS, 42);

    // Reset to Defaults button
    if (!zone.getFlags().isEmpty()) {
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

    // Integration Flags button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#IntegrationFlagsBtn",
        EventData.of("Button", "IntegrationFlags")
            .append("ZoneId", zoneId.toString()),
        false
    );

    // Back button - text depends on back target
    if ("settings".equals(backTarget)) {
      cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZFLAGS_BACK_TO_SETTINGS));
    }
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );

  }

  private void buildFlagCategory(UICommandBuilder cmd, UIEventBuilder events,
                  Zone zone, String categoryName, String[] flags, int startIndex) {
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

    // Check if any ancestor in the parent chain is OFF (recursive)
    boolean parentDisabled = false;
    String ancestor = ZoneFlags.getParentFlag(flagName);
    while (ancestor != null) {
      if (!zone.getEffectiveFlag(ancestor)) {
        parentDisabled = true;
        break;
      }
      ancestor = ZoneFlags.getParentFlag(ancestor);
    }

    // Check if this flag requires a mixin that isn't available
    boolean mixinUnavailable = false;
    String mixinType = ZoneFlags.getMixinType(flagName);
    if (mixinType != null) {
      mixinUnavailable = !isMixinAvailable(mixinType);
    }

    // Check if this flag is a mob clear child conflicting with its spawn counterpart
    boolean spawnConflict = false;
    String conflictingSpawn = ZoneFlags.getConflictingSpawnFlag(flagName);
    if (conflictingSpawn != null && zone.getEffectiveFlag(conflictingSpawn)) {
      spawnConflict = true;
    }

    // Flag name (display name from ZoneFlags)
    String displayName = ZoneFlags.getDisplayName(flagName);
    cmd.set(idx + "Name.Text", displayName);

    // Set checkbox value via child selector
    // When parent is off, show children as unchecked for clearer visual state
    boolean shouldDisable = parentDisabled || mixinUnavailable || spawnConflict;
    boolean displayValue = (parentDisabled || spawnConflict) ? false : currentValue;
    cmd.set(idx + "Toggle #CheckBox.Value", displayValue);
    cmd.set(idx + "Toggle #CheckBox.Disabled", shouldDisable);

    // Default indicator (shows "(default)" or "(custom)" or "(mixin)" or "(conflict)")
    if (spawnConflict) {
      cmd.set(idx + "Default.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZFLAGS_CONFLICT));
      cmd.set(idx + "Default.Style.TextColor", "#FF5555");
    } else if (mixinUnavailable) {
      cmd.set(idx + "Default.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZFLAGS_MIXIN));
      cmd.set(idx + "Default.Style.TextColor", "#FF5555");
    } else if (isDefault) {
      cmd.set(idx + "Default.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZINT_DEFAULT));
      cmd.set(idx + "Default.Style.TextColor", "#555555");
    } else {
      cmd.set(idx + "Default.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZINT_CUSTOM));
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

  /**
   * Checks if a specific mixin feature is available via the active mixin provider.
   * Maps ZoneFlags mixin type constants to bridge feature names.
   */
  private boolean isMixinAvailable(String mixinType) {
    // Map ZoneFlags mixin type to bridge feature name
    String featureName = switch (mixinType) {
      case ZoneFlags.MIXIN_PICKUP -> "item_pickup";
      case ZoneFlags.MIXIN_DEATH -> "death_drop";
      case ZoneFlags.MIXIN_DURABILITY -> "durability";
      case ZoneFlags.MIXIN_SEATING -> "seat";
      case ZoneFlags.MIXIN_SPAWN -> "mob_spawn";
      case ZoneFlags.MIXIN_EXPLOSION -> "explosion";
      case ZoneFlags.MIXIN_FIRE_SPREAD -> "fire_spread";
      case ZoneFlags.MIXIN_BUILDER_TOOLS -> "builder_tools";
      case ZoneFlags.MIXIN_TELEPORTER -> "teleporter";
      case ZoneFlags.MIXIN_PORTAL -> "portal";
      case ZoneFlags.MIXIN_HAMMER -> "hammer";
      case ZoneFlags.MIXIN_PLACE -> "block_place";
      case ZoneFlags.MIXIN_USE -> "use";
      default -> mixinType;
    };
    return ProtectionMixinBridge.isMixinFeatureAvailable(featureName);
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
      case "ResetDefaults" -> handleResetDefaults(player, data);
      case "IntegrationFlags" -> {
        UUID zId = UUID.fromString(data.zoneId);
        guiManager.openAdminZoneIntegrationFlags(player, ref, store, playerRef, zId);
      }
      case "Back" -> {
        if ("settings".equals(backTarget)) {
          guiManager.openAdminZoneProperties(player, ref, store, playerRef,
              zoneId, currentTab, currentPage);
        } else {
          guiManager.openAdminZone(player, ref, store, playerRef);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleToggleFlag(Player player, AdminZoneSettingsData data) {
    String flagName = data.flag;
    if (flagName == null || !ZoneFlags.isValidFlag(flagName)) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZFLAGS_INVALID_FLAG));
      sendUpdate();
      return;
    }

    Zone zone = zoneManager.getZoneById(zoneId);
    if (zone == null) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZFLAGS_ZONE_NOT_FOUND));
      sendUpdate();
      return;
    }

    // Toggle the flag
    boolean currentValue = zone.getEffectiveFlag(flagName);
    boolean newValue = !currentValue;
    boolean defaultValue = ZoneFlags.getDefault(flagName, zone.type());

    ZoneManager.ZoneResult result;
    if (newValue == defaultValue) {
      // Clear the flag to use default
      result = zoneManager.clearZoneFlag(zoneId, flagName);
    } else {
      // Set custom value
      result = zoneManager.setZoneFlag(zoneId, flagName, newValue);
    }

    // No chat message - UI feedback is sufficient

    rebuildPage();
  }

  private void handleResetDefaults(Player player, AdminZoneSettingsData data) {
    ZoneManager.ZoneResult result = zoneManager.clearAllZoneFlags(zoneId);

    if (result == ZoneManager.ZoneResult.SUCCESS) {
      player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.ZFLAGS_RESET_ALL));
    } else {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.ZFLAGS_RESET_FAILED, result));
    }

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

    // Rebuild flag toggles (matching 3-column UI layout)
    // Left column: Combat (0-6), Damage (7-10), Death (11-12)
    buildFlagCategory(cmd, events, zone, "Combat", ZoneFlags.COMBAT_FLAGS, 0);
    buildFlagCategory(cmd, events, zone, "Damage", ZoneFlags.DAMAGE_FLAGS, 7);
    buildFlagCategory(cmd, events, zone, "Death", ZoneFlags.DEATH_FLAGS, 11);
    // Middle column: Building (13-16), Interaction (17-29)
    buildFlagCategory(cmd, events, zone, "Building", ZoneFlags.BUILDING_FLAGS, 13);
    buildFlagCategory(cmd, events, zone, "Interaction", ZoneFlags.INTERACTION_FLAGS, 17);
    // Right column: Transport (30-32), Items (33-36), Spawning (37-41)
    buildFlagCategory(cmd, events, zone, "Transport", ZoneFlags.TRANSPORT_FLAGS, 30);
    buildFlagCategory(cmd, events, zone, "Items", ZoneFlags.ITEM_FLAGS, 33);
    buildFlagCategory(cmd, events, zone, "Spawning", ZoneFlags.SPAWNING_FLAGS, 37);
    buildFlagCategory(cmd, events, zone, "Mob Clearing", ZoneFlags.MOB_CLEAR_FLAGS, 42);

    // Update reset button state
    if (!zone.getFlags().isEmpty()) {
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
