package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
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

/**
 * Create Faction page — two-column form combining name, tag, color,
 * description, recruitment, territory permissions, and preview into
 * one 950x650 screen.
 */
public class CreateFactionPage extends InteractiveCustomUIPage<NewPlayerPageData> {

  private static final String PAGE_ID = "create";

  private static final int MIN_NAME_LENGTH = 3;

  private static final int MAX_NAME_LENGTH = 20;

  private static final int MIN_TAG_LENGTH = 2;

  private static final int MAX_TAG_LENGTH = 4;

  private static final int MAX_DESCRIPTION_LENGTH = 200;

  private static final String DEFAULT_COLOR = "#55FFFF";

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  // Mutable state — updated by toggles via sendUpdate()
  private boolean openRecruitment;

  private FactionPermissions permissions = FactionPermissions.defaults();

  /** Creates a new CreateFactionPage. */
  public CreateFactionPage(PlayerRef playerRef,
              FactionManager factionManager,
              GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.openRecruitment = false;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.NEWPLAYER_CREATE_FACTION);

    // Setup navigation bar
    NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

    // Localize static labels — page title and section headers
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.TITLE));
    cmd.set("#SectionPreview.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.SECTION_PREVIEW));
    cmd.set("#NamePrefix.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.NAME_PREFIX));
    cmd.set("#SectionBasicInfo.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.SECTION_BASIC_INFO));
    cmd.set("#FactionNameLabel.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.FACTION_NAME_LABEL));
    cmd.set("#TagLabel.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.TAG_LABEL));
    cmd.set("#SectionDetails.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.SECTION_DETAILS));
    cmd.set("#DescLabel.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.DESC_LABEL));
    cmd.set("#RecruitmentLabel.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.RECRUITMENT_LABEL));

    // Localize middle column — territory permissions (reuse SettingsGui keys)
    cmd.set("#LockHint.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.LOCK_HINT));
    cmd.set("#TerritoryPermissionsLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.TERRITORY_PERMISSIONS));
    cmd.set("#ColOut.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.COL_OUT));
    cmd.set("#ColAlly.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.COL_ALLY));
    cmd.set("#ColMem.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.COL_MEM));
    cmd.set("#ColOff.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.COL_OFF));
    cmd.set("#CatBuilding.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.CAT_BUILDING));
    cmd.set("#PermBreak.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_BREAK));
    cmd.set("#PermPlace.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_PLACE));
    cmd.set("#CatInteraction.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.CAT_INTERACTION));
    cmd.set("#InteractionHint.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.INTERACTION_HINT));
    cmd.set("#PermAll.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_ALL));
    cmd.set("#PermDoor.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_DOOR));
    cmd.set("#PermChest.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_CHEST));
    cmd.set("#PermBench.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_BENCH));
    cmd.set("#PermProcessing.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_PROCESSING));
    cmd.set("#PermSeat.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_SEAT));
    cmd.set("#PermTransport.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_TRANSPORT));
    cmd.set("#CatOther.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.CAT_OTHER));
    cmd.set("#PermCrate.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_CRATE));
    cmd.set("#PermNpcTame.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_NPC_TAME));
    cmd.set("#PermPve.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PERM_PVE));

    // Localize right column — faction color, mob spawning, combat
    cmd.set("#SectionFactionColor.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.SECTION_FACTION_COLOR));
    cmd.set("#SectionMobSpawning.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.MOB_SPAWNING));
    cmd.set("#MobSpawningHint.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.MOB_SPAWNING_HINT));
    cmd.set("#MobSpawningLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.MOB_SPAWNING_LABEL));
    cmd.set("#HostileMobsLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.HOSTILE_MOBS));
    cmd.set("#PassiveMobsLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PASSIVE_MOBS));
    cmd.set("#NeutralMobsLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.NEUTRAL_MOBS));
    cmd.set("#SectionCombat.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.SECTION_COMBAT));
    cmd.set("#PvPLabel.Text", HFMessages.get(playerRef, GuiKeys.SettingsGui.PVP_IN_TERRITORY));
    cmd.set("#CreateBtn.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.CREATE_BTN));

    // Set default ColorPicker value (cyan)
    cmd.set("#FactionColorPicker.Value", DEFAULT_COLOR);

    // Set preview defaults
    cmd.set("#PreviewName.TextSpans", Message.raw(HFMessages.get(playerRef, GuiKeys.CreateGui.PREVIEW_NAME)).color(DEFAULT_COLOR));
    cmd.set("#PreviewLeader.Text", HFMessages.get(playerRef, GuiKeys.CreateGui.LEADER_PREFIX, playerRef.getUsername()));

    // Recruitment dropdown
    cmd.set("#RecruitmentDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.FactionInfoGui.STATUS_INVITE_ONLY)), "INVITE_ONLY"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.FactionInfoGui.STATUS_OPEN)), "OPEN")
    ));
    cmd.set("#RecruitmentDropdown.Value", openRecruitment ? "OPEN" : "INVITE_ONLY");

    // Bind ColorPicker ValueChanged for real-time preview
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#FactionColorPicker",
        EventData.of("Button", "ColorChanged")
            .append("@Color", "#FactionColorPicker.Value")
            .append("@Name", "#NameInput.Value")
            .append("@Tag", "#TagInput.Value"),
        false
    );

    // Bind recruitment dropdown
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#RecruitmentDropdown",
        EventData.of("Button", "SetRecruitment")
            .append("@Recruitment", "#RecruitmentDropdown.Value"),
        false
    );

    // Setup permission toggle buttons
    buildPermissionToggles(cmd, events);

    // Bind CREATE button — captures all inputs
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CreateBtn",
        EventData.of("Button", "Create")
            .append("@Name", "#NameInput.Value")
            .append("@Color", "#FactionColorPicker.Value")
            .append("@Tag", "#TagInput.Value")
            .append("@Description", "#DescInput.Value"),
        false
    );
  }

  private void buildPermissionToggles(UICommandBuilder cmd, UIEventBuilder events) {
    ConfigManager config = ConfigManager.get();

    // Apply server locks to get effective values for display
    FactionPermissions perms = config.getEffectiveFactionPermissions(permissions);

    // Build toggles for all 4 levels
    for (String level : FactionPermissions.ALL_LEVELS) {
      String cap = capitalize(level);
      buildPermissionToggle(cmd, events, cap + "BreakToggle", level + "Break", perms.get(level + "Break"), config, false);
      buildPermissionToggle(cmd, events, cap + "PlaceToggle", level + "Place", perms.get(level + "Place"), config, false);
      buildPermissionToggle(cmd, events, cap + "InteractToggle", level + "Interact", perms.get(level + "Interact"), config, false);

      // Interaction sub-flags — disabled when parent interact is off
      boolean interactOff = !perms.get(level + "Interact");
      buildPermissionToggle(cmd, events, cap + "DoorToggle", level + "DoorUse", perms.get(level + "DoorUse"), config, interactOff);
      buildPermissionToggle(cmd, events, cap + "ContainerToggle", level + "ContainerUse", perms.get(level + "ContainerUse"), config, interactOff);
      buildPermissionToggle(cmd, events, cap + "BenchToggle", level + "BenchUse", perms.get(level + "BenchUse"), config, interactOff);
      buildPermissionToggle(cmd, events, cap + "ProcessingToggle", level + "ProcessingUse", perms.get(level + "ProcessingUse"), config, interactOff);
      buildPermissionToggle(cmd, events, cap + "SeatToggle", level + "SeatUse", perms.get(level + "SeatUse"), config, interactOff);
      buildPermissionToggle(cmd, events, cap + "TransportToggle", level + "TransportUse", perms.get(level + "TransportUse"), config, interactOff);

      // Standalone flags (not children of Interact)
      buildPermissionToggle(cmd, events, cap + "CrateToggle", level + "CrateUse", perms.get(level + "CrateUse"), config, false);
      buildPermissionToggle(cmd, events, cap + "NpcTameToggle", level + "NpcTame", perms.get(level + "NpcTame"), config, false);
      buildPermissionToggle(cmd, events, cap + "PveDamageToggle", level + "PveDamage", perms.get(level + "PveDamage"), config, false);
    }

    // Mob spawning toggles — children disabled when master is off
    boolean mobSpawning = perms.get(FactionPermissions.MOB_SPAWNING);
    boolean mobParentOff = !mobSpawning;
    buildPermissionToggle(cmd, events, "MobSpawningToggle", "mobSpawning", mobSpawning, config, false);
    buildPermissionToggle(cmd, events, "HostileMobToggle", "hostileMobSpawning", perms.get(FactionPermissions.HOSTILE_MOB_SPAWNING), config, mobParentOff);
    buildPermissionToggle(cmd, events, "PassiveMobToggle", "passiveMobSpawning", perms.get(FactionPermissions.PASSIVE_MOB_SPAWNING), config, mobParentOff);
    buildPermissionToggle(cmd, events, "NeutralMobToggle", "neutralMobSpawning", perms.get(FactionPermissions.NEUTRAL_MOB_SPAWNING), config, mobParentOff);

    // PvP toggle
    buildPermissionToggle(cmd, events, "PvPToggle", "pvpEnabled", perms.pvpEnabled(), config, false);
    cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? HFMessages.get(playerRef, GuiKeys.SettingsGui.PVP_ENABLED) : HFMessages.get(playerRef, GuiKeys.SettingsGui.PVP_DISABLED));
    cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");
  }

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private void buildPermissionToggle(UICommandBuilder cmd, UIEventBuilder events,
                   String elementId, String permName, boolean value,
                   ConfigManager config, boolean parentDisabled) {
    boolean locked = config.isPermissionLocked(permName);
    String selector = "#" + elementId;

    // When parent is disabled, show unchecked and disable the checkbox
    boolean displayValue = parentDisabled ? false : value;
    boolean shouldDisable = parentDisabled || locked;

    cmd.set(selector + " #CheckBox.Value", displayValue);
    cmd.set(selector + " #CheckBox.Disabled", shouldDisable);

    if (!shouldDisable) {
      events.addEventBinding(
          CustomUIEventBindingType.ValueChanged,
          selector + " #CheckBox",
          EventData.of("Button", "TogglePerm")
              .append("Perm", permName),
          false
      );
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                NewPlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    switch (data.button) {
      case "ColorChanged" -> handleColorChanged(data);
      case "SetRecruitment" -> handleRecruitmentToggle(data);
      case "TogglePerm" -> handleTogglePerm(data);
      case "Create" -> handleCreate(player, ref, store, playerRef, data);
      default -> sendUpdate();
    }
  }

  private void handleColorChanged(NewPlayerPageData data) {
    String hex = extractHex(data.inputColor);
    String name = data.inputName != null ? data.inputName : "";
    String tag = data.inputTag != null ? data.inputTag : "";
    String previewText = !name.isEmpty() ? name : HFMessages.get(playerRef, GuiKeys.CreateGui.PREVIEW_NAME);
    if (!tag.isEmpty()) {
      previewText += " [" + tag + "]";
    }

    UICommandBuilder updateCmd = new UICommandBuilder();
    updateCmd.set("#PreviewName.TextSpans", Message.raw(previewText).color(hex));
    sendUpdate(updateCmd);
  }

  private void handleRecruitmentToggle(NewPlayerPageData data) {
    String value = data.inputRecruitment;
    if (value == null) {
      sendUpdate();
      return;
    }
    this.openRecruitment = "OPEN".equals(value);
    sendUpdate();
  }

  private void handleTogglePerm(NewPlayerPageData data) {
    String permName = data.perm;
    if (permName == null) {
      sendUpdate();
      return;
    }

    // Don't allow toggling locked permissions
    if (ConfigManager.get().isPermissionLocked(permName)) {
      sendUpdate();
      return;
    }

    // Toggle in-memory (no database — faction doesn't exist yet)
    this.permissions = this.permissions.toggle(permName);

    // Rebuild permission toggles to update parent/child disable state
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildPermissionToggles(cmd, events);
    sendUpdate(cmd, events, false);
  }

  private void handleCreate(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
               PlayerRef playerRef, NewPlayerPageData data) {
    String name = data.inputName != null ? data.inputName.trim() : "";
    String tag = data.inputTag != null ? data.inputTag.trim() : "";
    String description = data.inputDescription != null ? data.inputDescription.trim() : "";

    // Extract hex color from ColorPicker value
    String color = extractHex(data.inputColor);

    // Validate faction name
    if (name.isEmpty()) {
      player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.ENTER_NAME));
      sendUpdate();
      return;
    }

    if (name.length() < MIN_NAME_LENGTH) {
      player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.NAME_TOO_SHORT, MIN_NAME_LENGTH));
      sendUpdate();
      return;
    }

    if (name.length() > MAX_NAME_LENGTH) {
      player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.NAME_TOO_LONG, MAX_NAME_LENGTH));
      sendUpdate();
      return;
    }

    // Check if name is already taken
    if (factionManager.getFactionByName(name) != null) {
      player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.NAME_TAKEN));
      sendUpdate();
      return;
    }

    // Validate tag format if provided
    if (!tag.isEmpty()) {
      if (tag.length() < MIN_TAG_LENGTH || tag.length() > MAX_TAG_LENGTH) {
        player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.TAG_LENGTH, MIN_TAG_LENGTH, MAX_TAG_LENGTH));
        sendUpdate();
        return;
      }

      if (!tag.matches("^[a-zA-Z0-9]+$")) {
        player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.TAG_FORMAT));
        sendUpdate();
        return;
      }
    }

    // Auto-generate tag if empty
    if (tag.isEmpty()) {
      tag = generateTag(name);
    }

    // Validate description length
    if (description.length() > MAX_DESCRIPTION_LENGTH) {
      player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.DESC_TOO_LONG, MAX_DESCRIPTION_LENGTH));
      sendUpdate();
      return;
    }

    // Check if player is already in a faction
    if (factionManager.isInFaction(playerRef.getUuid())) {
      player.sendMessage(MessageUtil.errorText(playerRef, CommonKeys.Common.ALREADY_IN_FACTION));
      sendUpdate();
      return;
    }

    // Create the faction
    FactionManager.FactionResult result = factionManager.createFaction(
        name,
        playerRef.getUuid(),
        playerRef.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        Faction faction = factionManager.getPlayerFaction(playerRef.getUuid());
        if (faction != null) {
          // Apply settings using immutable pattern
          Faction updated = faction;

          if (color != null && !color.isEmpty()) {
            updated = updated.withColor(color);
          }

          if (!tag.isEmpty()) {
            updated = updated.withTag(tag);
          }

          if (!description.isEmpty()) {
            updated = updated.withDescription(description);
          }

          updated = updated.withOpen(openRecruitment);
          updated = updated.withPermissions(this.permissions);

          factionManager.updateFaction(updated);

          player.sendMessage(MessageUtil.successText(playerRef, GuiKeys.CreateGui.CREATED, name));

          // Open faction dashboard
          Faction freshFaction = factionManager.getFaction(updated.id());
          if (freshFaction != null) {
            guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
          } else {
            guiManager.openFactionMain(player, ref, store, playerRef);
          }
        } else {
          player.sendMessage(MessageUtil.text(playerRef, GuiKeys.CreateGui.CREATED_NO_DASHBOARD, MessageUtil.COLOR_GOLD));
          guiManager.openFactionMain(player, ref, store, playerRef);
        }
      }

      case ALREADY_IN_FACTION -> {
        player.sendMessage(MessageUtil.errorText(playerRef, CommonKeys.Common.ALREADY_IN_FACTION));
        sendUpdate();
      }

      case NAME_TAKEN -> {
        player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.NAME_TAKEN));
        sendUpdate();
      }

      case NAME_TOO_SHORT, NAME_TOO_LONG -> {
        player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.INVALID_NAME));
        sendUpdate();
      }

      default -> {
        player.sendMessage(MessageUtil.errorText(playerRef, GuiKeys.CreateGui.CREATE_FAILED));
        sendUpdate();
      }
    }
  }

  // ── Utility methods ──────────────────────────────────────────────

  /**
   * Extracts a #RRGGBB hex string from the ColorPicker value.
   * The ColorPicker returns #RRGGBBAA (hex with alpha), so we strip the alpha suffix.
   */
  private String extractHex(String pickerValue) {
    if (pickerValue != null && pickerValue.length() >= 7) {
      return pickerValue.substring(0, 7);
    }
    return DEFAULT_COLOR;
  }

  /**
   * Auto-generates a faction tag from the faction name.
   * Takes the first 3-4 characters of the name (uppercase).
   */
  private static String generateTag(String factionName) {
    if (factionName == null || factionName.isEmpty()) {
      return "FACT";
    }
    String cleaned = factionName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
    if (cleaned.isEmpty()) {
      return "FACT";
    }
    int length = Math.min(cleaned.length(), 4);
    if (length < 2) {
      return (cleaned + cleaned + cleaned + cleaned).substring(0, 3);
    }
    return cleaned.substring(0, length);
  }
}
