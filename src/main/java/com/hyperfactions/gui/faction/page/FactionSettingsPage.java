package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionSettingsData;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.TeleportManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;

/**
 * Unified Faction Settings page with two-column boxed layout.
 * Left column: General, Recruitment, Home, Modules, Danger Zone (leader only)
 * Right column: Appearance (color), Territory Permissions, Faction Settings (PvP + Officers)
 */
public class FactionSettingsPage extends InteractiveCustomUIPage<FactionSettingsData> {

  private static final String PAGE_ID = "settings";

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final GuiManager guiManager;

  private final HyperFactions hyperFactions;

  private final Faction faction;

  /** Creates a new FactionSettingsPage. */
  public FactionSettingsPage(PlayerRef playerRef,
               FactionManager factionManager,
               ClaimManager claimManager,
               GuiManager guiManager,
               HyperFactions hyperFactions,
               Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionSettingsData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.guiManager = guiManager;
    this.hyperFactions = hyperFactions;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Permission check - officer or leader only
    if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
      cmd.append(UIPaths.ERROR_PAGE);
      cmd.set("#ErrorMessage.Text", HFMessages.get(playerRef, MessageKeys.SettingsGui.OFFICERS_ONLY));
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#CloseBtn",
          EventData.of("Button", "Close"),
          false
      );
      return;
    }

    boolean isLeader = member.role() == FactionRole.LEADER;

    // Load the unified settings template
    cmd.append(UIPaths.FACTION_SETTINGS);

    // Setup navigation bar
    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

    // === LEFT COLUMN ===
    buildGeneralSettings(cmd, events);
    buildHomeSection(cmd, events);
    buildModulesSection(events);

    // === RIGHT COLUMN ===
    buildColorSection(cmd, events);
    buildPermissions(cmd, events, isLeader);

    // Danger zone for leaders
    if (isLeader) {
      cmd.set("#DangerZone.Visible", true);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#DisbandBtn",
          EventData.of("Button", "Disband"),
          false
      );
    }
  }

  // =========================================================================
  // GENERAL SETTINGS (Left Column)
  // =========================================================================

  private void buildGeneralSettings(UICommandBuilder cmd, UIEventBuilder events) {
    // Name
    cmd.set("#NameValue.Text", faction.name());
    events.addEventBinding(CustomUIEventBindingType.Activating, "#NameEditBtn",
        EventData.of("Button", "OpenRenameModal"), false);

    // Tag
    String tagDisplay = faction.tag() != null && !faction.tag().isEmpty()
        ? "[" + faction.tag().toUpperCase() + "]"
        : HFMessages.get(playerRef, MessageKeys.SettingsGui.DISPLAY_NONE);
    cmd.set("#TagValue.Text", tagDisplay);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#TagEditBtn",
        EventData.of("Button", "OpenTagModal"), false);

    // Description
    String desc = faction.description() != null && !faction.description().isEmpty()
        ? faction.description()
        : HFMessages.get(playerRef, MessageKeys.SettingsGui.DISPLAY_NONE);
    cmd.set("#DescValue.Text", desc);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#DescEditBtn",
        EventData.of("Button", "OpenDescriptionModal"), false);

    // Recruitment dropdown
    cmd.set("#RecruitmentDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN)), "OPEN"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY)), "INVITE_ONLY")
    ));
    cmd.set("#RecruitmentDropdown.Value", faction.open() ? "OPEN" : "INVITE_ONLY");
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RecruitmentDropdown",
        EventData.of("Button", "RecruitmentChanged")
            .append("@Recruitment", "#RecruitmentDropdown.Value"), false);
  }

  // =========================================================================
  // COLOR / APPEARANCE (Right Column)
  // =========================================================================

  private void buildColorSection(UICommandBuilder cmd, UIEventBuilder events) {
    String colorHex = faction.color();
    cmd.set("#ColorPreview.Background.Color", colorHex);
    cmd.set("#ColorValue.Text", colorHex);
    cmd.set("#FactionColorPicker.Value", colorHex);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FactionColorPicker",
        EventData.of("Button", "ColorChanged")
            .append("@Color", "#FactionColorPicker.Value"), false);
  }

  // =========================================================================
  // PERMISSIONS + FACTION SETTINGS (Right Column)
  // =========================================================================

  private void buildPermissions(UICommandBuilder cmd, UIEventBuilder events, boolean isLeader) {
    boolean canEdit = canEditPermissions(playerRef.getUuid(), faction);

    FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
        faction.getEffectivePermissions()
    );
    ConfigManager config = ConfigManager.get();

    // Build toggles for all 4 levels
    for (String level : FactionPermissions.ALL_LEVELS) {
      String cap = capitalize(level);
      buildToggle(cmd, events, cap + "BreakToggle", level + "Break", perms.get(level + "Break"), canEdit, config, false);
      buildToggle(cmd, events, cap + "PlaceToggle", level + "Place", perms.get(level + "Place"), canEdit, config, false);
      buildToggle(cmd, events, cap + "InteractToggle", level + "Interact", perms.get(level + "Interact"), canEdit, config, false);

      // Interaction sub-flags — disabled when parent interact is off
      boolean interactOff = !perms.get(level + "Interact");
      buildToggle(cmd, events, cap + "DoorToggle", level + "DoorUse", perms.get(level + "DoorUse"), canEdit, config, interactOff);
      buildToggle(cmd, events, cap + "ContainerToggle", level + "ContainerUse", perms.get(level + "ContainerUse"), canEdit, config, interactOff);
      buildToggle(cmd, events, cap + "BenchToggle", level + "BenchUse", perms.get(level + "BenchUse"), canEdit, config, interactOff);
      buildToggle(cmd, events, cap + "ProcessingToggle", level + "ProcessingUse", perms.get(level + "ProcessingUse"), canEdit, config, interactOff);
      buildToggle(cmd, events, cap + "SeatToggle", level + "SeatUse", perms.get(level + "SeatUse"), canEdit, config, interactOff);
      buildToggle(cmd, events, cap + "TransportToggle", level + "TransportUse", perms.get(level + "TransportUse"), canEdit, config, interactOff);

      // Standalone flags (not children of Interact)
      buildToggle(cmd, events, cap + "CrateToggle", level + "CrateUse", perms.get(level + "CrateUse"), canEdit, config, false);
      buildToggle(cmd, events, cap + "NpcTameToggle", level + "NpcTame", perms.get(level + "NpcTame"), canEdit, config, false);
      buildToggle(cmd, events, cap + "PveDamageToggle", level + "PveDamage", perms.get(level + "PveDamage"), canEdit, config, false);
    }

    // Mob spawning toggles — children disabled when master is off
    boolean mobSpawning = perms.get(FactionPermissions.MOB_SPAWNING);
    boolean mobParentOff = !mobSpawning;
    buildToggle(cmd, events, "MobSpawningToggle", "mobSpawning", mobSpawning, canEdit, config, false);
    buildToggle(cmd, events, "HostileMobToggle", "hostileMobSpawning", perms.get(FactionPermissions.HOSTILE_MOB_SPAWNING), canEdit, config, mobParentOff);
    buildToggle(cmd, events, "PassiveMobToggle", "passiveMobSpawning", perms.get(FactionPermissions.PASSIVE_MOB_SPAWNING), canEdit, config, mobParentOff);
    buildToggle(cmd, events, "NeutralMobToggle", "neutralMobSpawning", perms.get(FactionPermissions.NEUTRAL_MOB_SPAWNING), canEdit, config, mobParentOff);

    // PvP toggle
    buildToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), canEdit, config, false);
    cmd.set("#PvPStatus.Text", perms.pvpEnabled()
        ? HFMessages.get(playerRef, MessageKeys.SettingsGui.PVP_ENABLED)
        : HFMessages.get(playerRef, MessageKeys.SettingsGui.PVP_DISABLED));
    cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");

    // Officers can edit - only leader can change this
    if (isLeader) {
      buildToggle(cmd, events, "OfficersCanEditToggle", "officersCanEdit", perms.officersCanEdit(), true, config, false);
    } else {
      cmd.set("#OfficerSettingRow.Visible", false);
    }
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private void buildToggle(UICommandBuilder cmd, UIEventBuilder events,
              String elementId, String permName, boolean currentValue,
              boolean canEdit, ConfigManager config, boolean parentDisabled) {
    boolean locked = config.isPermissionLocked(permName);
    String selector = "#" + elementId;

    // When parent is disabled, show unchecked and disable the checkbox
    boolean displayValue = parentDisabled ? false : currentValue;
    boolean shouldDisable = parentDisabled || locked || !canEdit;

    cmd.set(selector + " #CheckBox.Value", displayValue);
    cmd.set(selector + " #CheckBox.Disabled", shouldDisable);

    if (!shouldDisable) {
      events.addEventBinding(
          CustomUIEventBindingType.ValueChanged,
          selector + " #CheckBox",
          EventData.of("Button", "TogglePerm").append("Perm", permName),
          false
      );
    }
  }

  private boolean canEditPermissions(UUID uuid, Faction faction) {
    FactionMember member = faction.getMember(uuid);
    if (member == null) {
      return false;
    }

    FactionRole role = member.role();

    if (role == FactionRole.LEADER) {
      return true;
    }

    if (role == FactionRole.OFFICER) {
      FactionPermissions perms = faction.getEffectivePermissions();
      return perms.officersCanEdit()
         && PermissionManager.get().hasPermission(uuid, Permissions.FACTION_PERMISSIONS);
    }

    return false;
  }

  // =========================================================================
  // HOME LOCATION (Left Column)
  // =========================================================================

  private void buildHomeSection(UICommandBuilder cmd, UIEventBuilder events) {
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
      cmd.set("#HomeLocation.Text", HFMessages.get(playerRef, MessageKeys.SettingsGui.HOME_NOT_SET));
      cmd.set("#TeleportHomeBtn.Disabled", true);
      cmd.set("#DeleteHomeBtn.Disabled", true);
    }

    events.addEventBinding(CustomUIEventBindingType.Activating, "#SetHomeBtn",
        EventData.of("Button", "SetHome"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#TeleportHomeBtn",
        EventData.of("Button", "TeleportHome"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteHomeBtn",
        EventData.of("Button", "DeleteHome"), false);
  }

  // =========================================================================
  // MODULES (Left Column)
  // =========================================================================

  private void buildModulesSection(UIEventBuilder events) {
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ModulesBtn",
        EventData.of("Button", "OpenModules"), false);
  }

  // =========================================================================
  // EVENT HANDLING
  // =========================================================================

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                FactionSettingsData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if ("Nav".equals(data.button) && data.navBar != null) {
      FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
      if (entry != null) {
        Faction currentFaction = factionManager.getFaction(faction.id());
        var page = entry.guiSupplier().create(player, ref, store, playerRef, currentFaction, guiManager);
        if (page != null) {
          player.getPageManager().openCustomPage(ref, store, page);
          return;
        }
      }
      sendUpdate();
      return;
    }

    // Handle close button (from error page)
    if ("Close".equals(data.button)) {
      guiManager.openFactionMain(player, ref, store, playerRef);
      return;
    }

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Verify permissions
    if (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel()) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.NO_PERMISSION));
      sendUpdate();
      return;
    }

    boolean isLeader = member.role() == FactionRole.LEADER;

    switch (data.button) {
      case "TogglePerm" -> handleTogglePerm(player, ref, store, data, isLeader);
      case "ColorChanged" -> handleColorChanged(player, ref, store, data);
      case "RecruitmentChanged" -> handleRecruitmentChanged(player, ref, store, data);
      case "OpenRenameModal" -> guiManager.openRenameModal(player, ref, store, playerRef, faction);
      case "OpenDescriptionModal" -> guiManager.openDescriptionModal(player, ref, store, playerRef, faction);
      case "OpenTagModal" -> guiManager.openTagModal(player, ref, store, playerRef, faction);
      case "SetHome" -> handleSetHome(player, ref, store, uuid);
      case "TeleportHome" -> handleTeleportHome(player, ref, store, uuid);
      case "DeleteHome" -> handleDeleteHome(player, ref, store, uuid);
      case "OpenModules" -> guiManager.openFactionModules(player, ref, store, playerRef, faction);
      case "Disband" -> {
        if (!isLeader) {
          player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.ONLY_LEADER_DISBAND));
          sendUpdate();
          return;
        }
        guiManager.openDisbandConfirm(player, ref, store, playerRef, faction);
      }
      default -> sendUpdate();
    }
  }

  private void handleTogglePerm(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 FactionSettingsData data, boolean isLeader) {
    String permName = data.perm;
    if (permName == null) {
      sendUpdate();
      return;
    }

    ConfigManager config = ConfigManager.get();

    if (config.isPermissionLocked(permName)) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.PERM_LOCKED));
      sendUpdate();
      return;
    }

    if (!canEditPermissions(playerRef.getUuid(), faction)) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.NO_PERM_EDIT));
      sendUpdate();
      return;
    }

    if ("officersCanEdit".equals(permName) && !isLeader) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.ONLY_LEADER_OFFICERS));
      sendUpdate();
      return;
    }

    FactionPermissions current = faction.getEffectivePermissions();
    FactionPermissions updated = current.toggle(permName);

    Faction updatedFaction = faction.withPermissions(updated);
    factionManager.updateFaction(updatedFaction);

    // Re-open page with fresh data
    Faction freshFaction = factionManager.getFaction(faction.id());
    guiManager.openFactionSettings(player, ref, store, playerRef, freshFaction);
  }

  private void handleColorChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  FactionSettingsData data) {
    String rawColor = data.color;
    if (rawColor == null || rawColor.isEmpty()) {
      sendUpdate();
      return;
    }

    // ColorPicker returns #RRGGBBAA — strip alpha to get #RRGGBB
    String hexColor = rawColor.length() >= 7 ? rawColor.substring(0, 7).toUpperCase() : rawColor.toUpperCase();

    if (!hexColor.matches("#[0-9A-F]{6}")) {
      sendUpdate();
      return;
    }

    Faction updatedFaction = faction.withColor(hexColor);
    factionManager.updateFaction(updatedFaction);

    var worldMapService = hyperFactions.getWorldMapService();
    if (worldMapService != null) {
      worldMapService.triggerFactionWideRefresh(faction.id());
    }

    Faction freshFaction = factionManager.getFaction(faction.id());
    guiManager.openFactionSettings(player, ref, store, playerRef, freshFaction);
  }

  private void handleRecruitmentChanged(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                      FactionSettingsData data) {
    String value = data.recruitment;
    if (value == null) {
      sendUpdate();
      return;
    }

    boolean isOpen = "OPEN".equals(value);
    Faction updatedFaction = faction.withOpen(isOpen);
    factionManager.updateFaction(updatedFaction);

    String status = isOpen
        ? HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN)
        : HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY);
    player.sendMessage(MessageUtil.success(playerRef, MessageKeys.SettingsGui.RECRUITMENT_SET, status));

    Faction freshFaction = factionManager.getFaction(faction.id());
    guiManager.openFactionSettings(player, ref, store, playerRef, freshFaction);
  }

  private void handleSetHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    World world = player.getWorld();
    if (transform != null && world != null) {
      var pos = transform.getPosition();
      String worldName = world.getName();

      int chunkX = ChunkUtil.toChunkCoord(pos.x);
      int chunkZ = ChunkUtil.toChunkCoord(pos.z);
      UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
      if (claimOwner == null || !claimOwner.equals(faction.id())) {
        player.sendMessage(MessageUtil.error(playerRef, MessageKeys.SettingsGui.NOT_IN_TERRITORY));
        sendUpdate();
        return;
      }

      Faction.FactionHome newHome = Faction.FactionHome.create(
          worldName, pos.x, pos.y, pos.z, 0f, 0f, uuid
      );

      Faction updatedFaction = faction.withHome(newHome);
      factionManager.updateFaction(updatedFaction);

      player.sendMessage(MessageUtil.success(playerRef, MessageKeys.SettingsGui.HOME_SET));

      guiManager.openFactionSettings(player, ref, store, playerRef,
          factionManager.getFaction(faction.id()));
    } else {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Common.LOCATION_ERROR));
      sendUpdate();
    }
  }

  private void handleTeleportHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
    if (faction.home() == null) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Home.NO_HOME));
      sendUpdate();
      return;
    }

    // Close GUI first
    guiManager.closePage(player, ref, store);

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Common.LOCATION_ERROR));
      return;
    }

    Vector3d pos = transform.getPosition();
    World world = player.getWorld();
    if (world == null) {
      player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Common.WORLD_ERROR));
      return;
    }

    TeleportManager.StartLocation startLoc = new TeleportManager.StartLocation(
        world.getName(), pos.getX(), pos.getY(), pos.getZ()
    );

    TeleportManager.TeleportResult result = hyperFactions.getTeleportManager().teleportToHome(
        uuid,
        startLoc,
        f -> executeTeleport(store, ref, world, f),
        player::sendMessage,
        () -> hyperFactions.getCombatTagManager().isTagged(uuid)
    );

    handleTeleportResult(player, result);
  }

  private TeleportManager.TeleportResult executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref,
                             World currentWorld, Faction faction) {
    Faction.FactionHome home = faction.home();
    if (home == null) {
      return TeleportManager.TeleportResult.NO_HOME;
    }

    World targetWorld;
    if (currentWorld.getName().equals(home.world())) {
      targetWorld = currentWorld;
    } else {
      targetWorld = Universe.get().getWorld(home.world());
      if (targetWorld == null) {
        return TeleportManager.TeleportResult.WORLD_NOT_FOUND;
      }
    }

    targetWorld.execute(() -> {
      Vector3d position = new Vector3d(home.x(), home.y(), home.z());
      Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
    });

    return TeleportManager.TeleportResult.SUCCESS_INSTANT;
  }

  private void handleTeleportResult(Player player, TeleportManager.TeleportResult result) {
    switch (result) {
      case NOT_IN_FACTION -> player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Common.NOT_IN_FACTION));
      case NO_HOME -> player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Home.NO_HOME));
      case COMBAT_TAGGED -> player.sendMessage(MessageUtil.error(playerRef, MessageKeys.Home.COMBAT_TAGGED));
      case SUCCESS_INSTANT -> player.sendMessage(MessageUtil.success(playerRef, MessageKeys.Home.TELEPORTED));
      case ON_COOLDOWN, SUCCESS_WARMUP -> {} // Message sent by TeleportManager
      default -> {}
    }
  }

  private void handleDeleteHome(Player player, Ref<EntityStore> ref, Store<EntityStore> store, UUID uuid) {
    if (faction.home() == null) {
      player.sendMessage(MessageUtil.info(playerRef, MessageKeys.SettingsGui.HOME_NO_SET, MessageUtil.COLOR_GOLD));
      sendUpdate();
      return;
    }

    Faction updatedFaction = faction.withHome(null);
    factionManager.updateFaction(updatedFaction);

    player.sendMessage(MessageUtil.success(playerRef, MessageKeys.SettingsGui.HOME_DELETED));

    guiManager.openFactionSettings(player, ref, store, playerRef,
        factionManager.getFaction(faction.id()));
  }
}
