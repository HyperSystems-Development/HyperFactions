package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
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

    public CreateFactionPage(PlayerRef playerRef,
                             FactionManager factionManager,
                             GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.openRecruitment = false;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/newplayer/create_faction.ui");

        // Setup navigation bar
        NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

        // Set default ColorPicker value (cyan)
        cmd.set("#FactionColorPicker.Value", DEFAULT_COLOR);

        // Set preview defaults
        cmd.set("#PreviewName.TextSpans", Message.raw("Your Faction Name").color(DEFAULT_COLOR));
        cmd.set("#PreviewLeader.Text", "Leader: " + playerRef.getUsername());

        // Recruitment dropdown
        cmd.set("#RecruitmentDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Invite Only"), "INVITE_ONLY"),
                new DropdownEntryInfo(LocalizableString.fromString("Open"), "OPEN")
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
        cmd.set("#PvPStatus.Text", perms.pvpEnabled() ? "Enabled" : "Disabled");
        cmd.set("#PvPStatus.Style.TextColor", perms.pvpEnabled() ? "#55FF55" : "#FF5555");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
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

        if (shouldDisable) {
            cmd.set(selector + " #CheckBox.Disabled", true);
        } else {
            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    selector + " #CheckBox",
                    EventData.of("Button", "TogglePerm")
                            .append("Perm", permName),
                    false
            );
        }
    }

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
        String previewText = !name.isEmpty() ? name : "Your Faction Name";
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
        sendUpdate();
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
            player.sendMessage(MessageUtil.errorText("Please enter a faction name."));
            sendUpdate();
            return;
        }

        if (name.length() < MIN_NAME_LENGTH) {
            player.sendMessage(MessageUtil.errorText("Faction name must be at least " + MIN_NAME_LENGTH + " characters."));
            sendUpdate();
            return;
        }

        if (name.length() > MAX_NAME_LENGTH) {
            player.sendMessage(MessageUtil.errorText("Faction name cannot exceed " + MAX_NAME_LENGTH + " characters."));
            sendUpdate();
            return;
        }

        // Check if name is already taken
        if (factionManager.getFactionByName(name) != null) {
            player.sendMessage(MessageUtil.errorText("A faction with this name already exists."));
            sendUpdate();
            return;
        }

        // Validate tag format if provided
        if (!tag.isEmpty()) {
            if (tag.length() < MIN_TAG_LENGTH || tag.length() > MAX_TAG_LENGTH) {
                player.sendMessage(MessageUtil.errorText("Faction tag must be " + MIN_TAG_LENGTH + "-" + MAX_TAG_LENGTH + " characters."));
                sendUpdate();
                return;
            }

            if (!tag.matches("^[a-zA-Z0-9]+$")) {
                player.sendMessage(MessageUtil.errorText("Faction tag can only contain letters and numbers."));
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
            player.sendMessage(MessageUtil.errorText("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters."));
            sendUpdate();
            return;
        }

        // Check if player is already in a faction
        if (factionManager.isInFaction(playerRef.getUuid())) {
            player.sendMessage(MessageUtil.errorText("You are already in a faction."));
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

                    player.sendMessage(
                            Message.raw("Faction ").color("#55FF55")
                                    .insert(Message.raw(name).color("#00FFFF"))
                                    .insert(Message.raw(" created successfully!").color("#55FF55"))
                    );

                    // Open faction dashboard
                    Faction freshFaction = factionManager.getFaction(updated.id());
                    if (freshFaction != null) {
                        guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
                    } else {
                        guiManager.openFactionMain(player, ref, store, playerRef);
                    }
                } else {
                    player.sendMessage(MessageUtil.text("Faction created but could not open dashboard.", MessageUtil.COLOR_GOLD));
                    guiManager.openFactionMain(player, ref, store, playerRef);
                }
            }

            case ALREADY_IN_FACTION -> {
                player.sendMessage(MessageUtil.errorText("You are already in a faction."));
                sendUpdate();
            }

            case NAME_TAKEN -> {
                player.sendMessage(MessageUtil.errorText("A faction with this name already exists."));
                sendUpdate();
            }

            case NAME_TOO_SHORT, NAME_TOO_LONG -> {
                player.sendMessage(MessageUtil.errorText("Invalid faction name."));
                sendUpdate();
            }

            default -> {
                player.sendMessage(MessageUtil.errorText("Could not create faction."));
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
