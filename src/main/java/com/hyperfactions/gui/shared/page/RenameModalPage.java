package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.shared.data.RenameModalData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.worldmap.WorldMapService;
import org.jetbrains.annotations.Nullable;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
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
 * Modal for renaming a faction.
 * Validates name uniqueness before saving.
 */
public class RenameModalPage extends InteractiveCustomUIPage<RenameModalData> {

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 32;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;
    @Nullable
    private final WorldMapService worldMapService;
    private final boolean adminMode;

    public RenameModalPage(PlayerRef playerRef,
                           FactionManager factionManager,
                           GuiManager guiManager,
                           Faction faction,
                           @Nullable WorldMapService worldMapService) {
        this(playerRef, factionManager, guiManager, faction, worldMapService, false);
    }

    public RenameModalPage(PlayerRef playerRef,
                           FactionManager factionManager,
                           GuiManager guiManager,
                           Faction faction,
                           @Nullable WorldMapService worldMapService,
                           boolean adminMode) {
        super(playerRef, CustomPageLifetime.CanDismiss, RenameModalData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
        this.worldMapService = worldMapService;
        this.adminMode = adminMode;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the modal template
        cmd.append("HyperFactions/shared/rename_modal.ui");

        // Show current name
        cmd.set("#CurrentName.Text", faction.name());

        // Cancel button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("Button", "Cancel"),
                false
        );

        // Save button - captures text input value
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveBtn",
                EventData.of("Button", "Save").append("@Name", "#NameInput.Value"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                RenameModalData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify officer permission (skip in admin mode)
        if (!adminMode && (member == null || member.role().getLevel() < FactionRole.OFFICER.getLevel())) {
            player.sendMessage(MessageUtil.errorText("You don't have permission to rename the faction."));
            guiManager.openFactionSettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
            return;
        }

        switch (data.button) {
            case "Cancel" -> {
                if (adminMode) {
                    guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
                } else {
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }

            case "Save" -> {
                String newName = data.name;

                // Validation
                if (newName == null || newName.trim().isEmpty()) {
                    player.sendMessage(MessageUtil.errorText("Please enter a faction name."));
                    sendUpdate();
                    return;
                }

                newName = newName.trim();

                if (newName.length() < MIN_NAME_LENGTH) {
                    player.sendMessage(MessageUtil.errorText("Faction name must be at least " + MIN_NAME_LENGTH + " characters."));
                    sendUpdate();
                    return;
                }

                if (newName.length() > MAX_NAME_LENGTH) {
                    player.sendMessage(MessageUtil.errorText("Faction name cannot exceed " + MAX_NAME_LENGTH + " characters."));
                    sendUpdate();
                    return;
                }

                // Check if name is the same
                if (newName.equalsIgnoreCase(faction.name())) {
                    player.sendMessage(MessageUtil.text("That's already your faction's name.", MessageUtil.COLOR_GOLD));
                    sendUpdate();
                    return;
                }

                // Check uniqueness
                Faction existing = factionManager.getFactionByName(newName);
                if (existing != null) {
                    player.sendMessage(MessageUtil.errorText("A faction with that name already exists."));
                    sendUpdate();
                    return;
                }

                // Update the faction
                String oldName = faction.name();
                Faction updatedFaction = faction.withName(newName);
                factionManager.updateFaction(updatedFaction);

                // Refresh world maps to show new faction name (respects configured refresh mode)
                if (worldMapService != null) {
                    worldMapService.triggerFactionWideRefresh(faction.id());
                }

                String prefix = adminMode ? "[Admin] " : "";
                player.sendMessage(
                        Message.raw(prefix + "Faction renamed from ").color("#AAAAAA")
                                .insert(Message.raw(oldName).color("#888888"))
                                .insert(Message.raw(" to ").color("#AAAAAA"))
                                .insert(Message.raw(newName).color("#00FFFF"))
                                .insert(Message.raw("!").color("#AAAAAA"))
                );

                if (adminMode) {
                    guiManager.openAdminFactionSettings(player, ref, store, playerRef, faction.id());
                } else {
                    guiManager.openFactionSettings(player, ref, store, playerRef,
                            factionManager.getFaction(faction.id()));
                }
            }
        }
    }
}
