package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionSettingsData;
import com.hyperfactions.manager.FactionManager;
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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;

/**
 * Admin Faction Settings page - allows admins to edit any faction's settings and permissions.
 * Two-column layout: General Settings | Territory Permissions
 * Bypasses all role checks - admins have full control.
 */
public class AdminFactionSettingsPage extends InteractiveCustomUIPage<AdminFactionSettingsData> {

  private final PlayerRef playerRef;

  private final UUID factionId;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  /** Creates a new AdminFactionSettingsPage. */
  public AdminFactionSettingsPage(PlayerRef playerRef,
                  UUID factionId,
                  FactionManager factionManager,
                  GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionSettingsData.CODEC);
    this.playerRef = playerRef;
    this.factionId = factionId;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the admin faction settings template
    cmd.append(UIPaths.ADMIN_FACTION_SETTINGS);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

    // Localize page title and labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_FACTION_SETTINGS));
    cmd.set("#EditingLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_EDITING));
    cmd.set("#AdminOverrideLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_ADMIN_OVERRIDE));
    cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_BACK_TO_INFO));

    // Left column section headers and row labels
    cmd.set("#SectionGeneral.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_GENERAL));
    cmd.set("#NameLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_NAME_LABEL));
    cmd.set("#TagLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_TAG_LABEL));
    cmd.set("#DescLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_DESC_LABEL));
    String editText = HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_EDIT);
    cmd.set("#NameEditBtn.Text", editText);
    cmd.set("#TagEditBtn.Text", editText);
    cmd.set("#DescEditBtn.Text", editText);
    cmd.set("#SectionRecruitment.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_RECRUITMENT));
    cmd.set("#StatusLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_STATUS_LABEL));
    cmd.set("#SectionHome.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_HOME));
    cmd.set("#LocationLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_LOCATION_LABEL));
    cmd.set("#ClearHomeBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_CLEAR_HOME));
    cmd.set("#SectionDangerZone.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_DANGER_ZONE));
    cmd.set("#IrreversibleWarning.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_IRREVERSIBLE));
    cmd.set("#DisbandBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_DISBAND_FACTION));

    // Middle column - territory permissions
    cmd.set("#LockHint.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_LOCK_HINT));
    cmd.set("#SectionTerritoryPerms.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_TERRITORY_PERMS));
    cmd.set("#ColOutsider.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_COL_OUT));
    cmd.set("#ColAlly.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_COL_ALLY));
    cmd.set("#ColMember.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_COL_MEM));
    cmd.set("#ColOfficer.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_COL_OFF));
    cmd.set("#CatBuilding.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_CAT_BUILDING));
    cmd.set("#PermBreak.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_BREAK));
    cmd.set("#PermPlace.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_PLACE));
    cmd.set("#CatInteraction.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_CAT_INTERACTION));
    cmd.set("#CatInteractionSub.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_CAT_INTERACT_SUB));
    cmd.set("#PermAll.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_ALL));
    cmd.set("#PermDoor.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_DOOR));
    cmd.set("#PermChest.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_CHEST));
    cmd.set("#PermBench.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_BENCH));
    cmd.set("#PermProcessing.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_PROCESSING));
    cmd.set("#PermSeat.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_SEAT));
    cmd.set("#PermTransport.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_TRANSPORT));
    cmd.set("#CatOther.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_CAT_OTHER));
    cmd.set("#PermCrateUse.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_CRATE_USE));
    cmd.set("#PermNpcTame.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_NPC_TAME));
    cmd.set("#PermPveDamage.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_PVE_DAMAGE));

    // Right column - appearance, mob spawning, faction settings
    cmd.set("#SectionAppearance.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_APPEARANCE));
    cmd.set("#ColorLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_COLOR_LABEL));
    cmd.set("#SectionMobSpawning.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_MOB_SPAWNING));
    cmd.set("#SectionMobSpawningSub.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_MOB_SUB));
    cmd.set("#PermMobSpawning.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_MOB_SPAWNING));
    cmd.set("#PermHostile.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_HOSTILE));
    cmd.set("#PermPassive.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_PASSIVE));
    cmd.set("#PermNeutral.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_NEUTRAL));
    cmd.set("#SectionFactionSettings.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_FACTION_SETTINGS));
    cmd.set("#PermPvP.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_PVP));
    cmd.set("#PermOfficersEdit.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SET_PERM_OFFICERS_EDIT));

    // Get the faction
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      cmd.set("#FactionName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTION_NOT_FOUND_LABEL));
      return;
    }

    // Set faction name in header
    cmd.set("#FactionName.Text", faction.name());

    // === LEFT COLUMN: General Settings ===
    buildGeneralSettings(cmd, events, faction);

    // === RIGHT COLUMN ===
    buildColorSection(cmd, events, faction);
    buildPermissions(cmd, events, faction);

    // Back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back")
            .append("FactionId", factionId.toString()),
        false
    );
  }

  private void buildGeneralSettings(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    // Name
    cmd.set("#NameValue.Text", faction.name());
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#NameEditBtn",
        EventData.of("Button", "OpenRenameModal"),
        false
    );

    // Tag
    String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
        ? "[" + faction.tag().toUpperCase() + "]"
        : HFMessages.get(playerRef, MessageKeys.AdminGui.NONE_PAREN);
    cmd.set("#TagValue.Text", tagDisplay);
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TagEditBtn",
        EventData.of("Button", "OpenTagModal"),
        false
    );

    // Description
    String desc = faction.description() != null && !faction.description().isEmpty()
        ? faction.description()
        : HFMessages.get(playerRef, MessageKeys.AdminGui.NONE_PAREN);
    cmd.set("#DescValue.Text", desc);
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DescEditBtn",
        EventData.of("Button", "OpenDescriptionModal"),
        false
    );

    // Recruitment dropdown
    cmd.set("#RecruitmentDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN)), "OPEN"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY)), "INVITE_ONLY")
    ));
    cmd.set("#RecruitmentDropdown.Value", faction.open() ? "OPEN" : "INVITE_ONLY");
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#RecruitmentDropdown",
        EventData.of("Button", "RecruitmentChanged")
            .append("@Recruitment", "#RecruitmentDropdown.Value"),
        false
    );

    // Home location
    if (faction.home() != null) {
      Faction.FactionHome home = faction.home();
      String worldName = home.world();
      if (worldName.contains("/")) {
        worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
      }
      String homeText = String.format("%s (%.0f, %.0f, %.0f)",
          worldName, home.x(), home.y(), home.z());
      cmd.set("#HomeLocation.Text", homeText);
    } else {
      cmd.set("#HomeLocation.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.NOT_SET));
    }
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ClearHomeBtn",
        EventData.of("Button", "ClearHome"),
        false
    );

    // Disband
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DisbandBtn",
        EventData.of("Button", "Disband"),
        false
    );
  }

  private void buildColorSection(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    String colorHex = faction.color();
    cmd.set("#ColorPreview.Background.Color", colorHex);
    cmd.set("#ColorValue.Text", colorHex);
    cmd.set("#FactionColorPicker.Value", colorHex);
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#FactionColorPicker",
        EventData.of("Button", "ColorChanged")
            .append("@Color", "#FactionColorPicker.Value"),
        false
    );
  }

  private void buildPermissions(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
        faction.getEffectivePermissions()
    );
    ConfigManager config = ConfigManager.get();

    // Build toggles for all 4 levels
    for (String level : FactionPermissions.ALL_LEVELS) {
      String cap = capitalize(level);
      buildToggle(cmd, events, cap + "BreakToggle", level + "Break", perms.get(level + "Break"), config, false);
      buildToggle(cmd, events, cap + "PlaceToggle", level + "Place", perms.get(level + "Place"), config, false);
      buildToggle(cmd, events, cap + "InteractToggle", level + "Interact", perms.get(level + "Interact"), config, false);

      // Interaction sub-flags — disabled when parent interact is off
      boolean interactOff = !perms.get(level + "Interact");
      buildToggle(cmd, events, cap + "DoorToggle", level + "DoorUse", perms.get(level + "DoorUse"), config, interactOff);
      buildToggle(cmd, events, cap + "ContainerToggle", level + "ContainerUse", perms.get(level + "ContainerUse"), config, interactOff);
      buildToggle(cmd, events, cap + "BenchToggle", level + "BenchUse", perms.get(level + "BenchUse"), config, interactOff);
      buildToggle(cmd, events, cap + "ProcessingToggle", level + "ProcessingUse", perms.get(level + "ProcessingUse"), config, interactOff);
      buildToggle(cmd, events, cap + "SeatToggle", level + "SeatUse", perms.get(level + "SeatUse"), config, interactOff);
      buildToggle(cmd, events, cap + "TransportToggle", level + "TransportUse", perms.get(level + "TransportUse"), config, interactOff);

      // Standalone flags (not children of Interact)
      buildToggle(cmd, events, cap + "CrateToggle", level + "CrateUse", perms.get(level + "CrateUse"), config, false);
      buildToggle(cmd, events, cap + "NpcTameToggle", level + "NpcTame", perms.get(level + "NpcTame"), config, false);
      buildToggle(cmd, events, cap + "PveDamageToggle", level + "PveDamage", perms.get(level + "PveDamage"), config, false);
    }

    // Mob spawning toggles — children disabled when master is off
    boolean mobSpawning = perms.get(FactionPermissions.MOB_SPAWNING);
    boolean mobParentOff = !mobSpawning;
    buildToggle(cmd, events, "MobSpawningToggle", "mobSpawning", mobSpawning, config, false);
    buildToggle(cmd, events, "HostileMobToggle", "hostileMobSpawning", perms.get(FactionPermissions.HOSTILE_MOB_SPAWNING), config, mobParentOff);
    buildToggle(cmd, events, "PassiveMobToggle", "passiveMobSpawning", perms.get(FactionPermissions.PASSIVE_MOB_SPAWNING), config, mobParentOff);
    buildToggle(cmd, events, "NeutralMobToggle", "neutralMobSpawning", perms.get(FactionPermissions.NEUTRAL_MOB_SPAWNING), config, mobParentOff);

    // PvP toggle
    buildToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), config, false);
    cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? HFMessages.get(playerRef, MessageKeys.SettingsGui.PVP_ENABLED) : HFMessages.get(playerRef, MessageKeys.SettingsGui.PVP_DISABLED));
    cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");

    // Officers can edit
    buildToggle(cmd, events, "OfficersCanEditToggle", "officersCanEdit", perms.officersCanEdit(), config, false);
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
              String elementId, String permName, boolean currentValue,
              ConfigManager config, boolean parentDisabled) {
    boolean locked = config.isPermissionLocked(permName);
    String selector = "#" + elementId;

    // When parent is disabled, show unchecked and disable the checkbox
    boolean displayValue = parentDisabled ? false : currentValue;
    boolean shouldDisable = parentDisabled || locked;

    cmd.set(selector + " #CheckBox.Value", displayValue);
    cmd.set(selector + " #CheckBox.Disabled", shouldDisable);

    if (!shouldDisable) {
      // Admin can toggle - bind ValueChanged event
      events.addEventBinding(
          CustomUIEventBindingType.ValueChanged,
          selector + " #CheckBox",
          EventData.of("Button", "TogglePerm")
              .append("Perm", permName)
              .append("FactionId", factionId.toString()),
          false
      );
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminFactionSettingsData data) {
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

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null && !data.button.equals("Back")) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.Common.FACTION_NOT_FOUND));
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "TogglePerm" -> handleTogglePerm(player, ref, store, data);

      // Admin modal pages - bypass permission checks
      case "OpenRenameModal" -> guiManager.openAdminRenameModal(player, ref, store, playerRef, faction);

      case "OpenTagModal" -> guiManager.openAdminTagModal(player, ref, store, playerRef, faction);

      case "OpenDescriptionModal" -> guiManager.openAdminDescriptionModal(player, ref, store, playerRef, faction);

      case "ColorChanged" -> handleColorChanged(player, ref, store, data, faction);

      case "RecruitmentChanged" -> handleRecruitmentChanged(player, ref, store, data, faction);

      case "ClearHome" -> handleClearHome(player, ref, store, faction);

      case "Disband" -> guiManager.openAdminDisbandConfirm(player, ref, store, playerRef, faction.id(), faction.name());

      case "Back" -> guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleTogglePerm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 AdminFactionSettingsData data) {
    String permName = data.perm;
    if (permName == null) {
      sendUpdate();
      return;
    }

    ConfigManager config = ConfigManager.get();

    // Check if server has locked this setting
    if (config.isPermissionLocked(permName)) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.SET_LOCKED));
      sendUpdate();
      return;
    }

    // Get current faction
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.Common.FACTION_NOT_FOUND));
      sendUpdate();
      return;
    }

    // Toggle the permission
    FactionPermissions current = faction.getEffectivePermissions();
    FactionPermissions updated = current.toggle(permName);

    // Save to faction
    Faction updatedFaction = faction.withPermissions(updated);
    factionManager.updateFaction(updatedFaction);

    // Format permission name nicely
    String displayName = formatPermissionName(permName);
    boolean newValue = updated.get(permName);

    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.SET_PERM_TOGGLED, displayName, newValue ? HFMessages.get(playerRef, MessageKeys.AdminGui.ON) : HFMessages.get(playerRef, MessageKeys.AdminGui.OFF)));

    // Rebuild page with fresh data
    rebuildPage();
  }

  private void handleColorChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  AdminFactionSettingsData data, Faction faction) {
    String rawColor = data.color;
    if (rawColor == null || rawColor.isEmpty()) {
      sendUpdate();
      return;
    }

    // ColorPicker returns #RRGGBBAA — strip alpha to get #RRGGBB
    String hexColor = rawColor.length() >= 7 ? rawColor.substring(0, 7).toUpperCase() : rawColor.toUpperCase();

    // Validate hex format
    if (!hexColor.matches("#[0-9A-F]{6}")) {
      sendUpdate();
      return;
    }

    Faction updatedFaction = faction.withColor(hexColor);
    factionManager.updateFaction(updatedFaction);

    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.SET_COLOR_CHANGED, hexColor));

    rebuildPage();
  }

  private void handleRecruitmentChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                      AdminFactionSettingsData data, Faction faction) {
    String value = data.recruitment;
    if (value == null) {
      sendUpdate();
      return;
    }

    boolean isOpen = "OPEN".equals(value);
    Faction updatedFaction = faction.withOpen(isOpen);
    factionManager.updateFaction(updatedFaction);

    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.SET_RECRUITMENT_SET, isOpen ? HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN) : HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY)));

    rebuildPage();
  }

  private void handleClearHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, Faction faction) {
    if (faction.home() == null) {
      player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.SET_NO_HOME, MessageUtil.COLOR_GOLD));
      sendUpdate();
      return;
    }

    Faction updatedFaction = faction.withHome(null);
    factionManager.updateFaction(updatedFaction);

    player.sendMessage(MessageUtil.adminSuccess(playerRef, MessageKeys.AdminGui.SET_HOME_CLEARED, faction.name()));

    // Rebuild page with fresh data
    rebuildPage();
  }

  private String formatPermissionName(String permName) {
    String display = FactionPermissions.getDisplayName(permName);
    return display != null ? display : permName;
  }

  private void rebuildPage() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    // Get fresh faction data
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      cmd.set("#FactionName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTION_NOT_FOUND_LABEL));
      sendUpdate(cmd, events, false);
      return;
    }

    // Update header
    cmd.set("#FactionName.Text", faction.name());

    // Rebuild general settings
    rebuildGeneralSettings(cmd, events, faction);

    // Rebuild color section
    rebuildColorSection(cmd, events, faction);

    // Rebuild permissions
    rebuildPermissions(cmd, events, faction);

    sendUpdate(cmd, events, false);
  }

  private void rebuildGeneralSettings(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    // Name
    cmd.set("#NameValue.Text", faction.name());

    // Tag
    String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
        ? "[" + faction.tag().toUpperCase() + "]"
        : HFMessages.get(playerRef, MessageKeys.AdminGui.NONE_PAREN);
    cmd.set("#TagValue.Text", tagDisplay);

    // Description
    String desc = faction.description() != null && !faction.description().isEmpty()
        ? faction.description()
        : HFMessages.get(playerRef, MessageKeys.AdminGui.NONE_PAREN);
    cmd.set("#DescValue.Text", desc);

    // Recruitment dropdown
    cmd.set("#RecruitmentDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN)), "OPEN"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY)), "INVITE_ONLY")
    ));
    cmd.set("#RecruitmentDropdown.Value", faction.open() ? "OPEN" : "INVITE_ONLY");
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#RecruitmentDropdown",
        EventData.of("Button", "RecruitmentChanged")
            .append("@Recruitment", "#RecruitmentDropdown.Value"),
        false
    );

    // Home location
    if (faction.home() != null) {
      Faction.FactionHome home = faction.home();
      String worldName = home.world();
      if (worldName.contains("/")) {
        worldName = worldName.substring(worldName.lastIndexOf('/') + 1);
      }
      String homeText = String.format("%s (%.0f, %.0f, %.0f)",
          worldName, home.x(), home.y(), home.z());
      cmd.set("#HomeLocation.Text", homeText);
    } else {
      cmd.set("#HomeLocation.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.NOT_SET));
    }
  }

  private void rebuildColorSection(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    String colorHex = faction.color();
    cmd.set("#ColorPreview.Background.Color", colorHex);
    cmd.set("#ColorValue.Text", colorHex);
    cmd.set("#FactionColorPicker.Value", colorHex);
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#FactionColorPicker",
        EventData.of("Button", "ColorChanged")
            .append("@Color", "#FactionColorPicker.Value"),
        false
    );
  }

  private void rebuildPermissions(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    // Reuse the same build logic
    buildPermissions(cmd, events, faction);
  }

}
