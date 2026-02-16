package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.shared.data.DisbandConfirmData;
import com.hyperfactions.manager.FactionManager;
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
 * Confirmation modal for disbanding a faction.
 * Shows a warning and requires explicit confirmation.
 */
public class DisbandConfirmPage extends InteractiveCustomUIPage<DisbandConfirmData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public DisbandConfirmPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              GuiManager guiManager,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, DisbandConfirmData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the disband confirmation template
        cmd.append("HyperFactions/shared/disband_confirm.ui");

        // Set faction name in the modal
        cmd.set("#FactionName.Text", faction.name());

        // Cancel button - return to settings
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("Button", "Cancel"),
                false
        );

        // Confirm button - actually disband
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmBtn",
                EventData.of("Button", "Confirm"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                DisbandConfirmData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        FactionMember member = faction.getMember(uuid);

        // Verify leader permission
        if (member == null || member.role() != FactionRole.LEADER) {
            player.sendMessage(MessageUtil.errorText("Only the leader can disband the faction."));
            guiManager.openFactionSettings(player, ref, store, playerRef,
                    factionManager.getFaction(faction.id()));
            return;
        }

        switch (data.button) {
            case "Cancel" -> {
                // Return to settings page
                guiManager.openFactionSettings(player, ref, store, playerRef,
                        factionManager.getFaction(faction.id()));
            }

            case "Confirm" -> {
                // Actually disband the faction
                // Note: FactionManager.disbandFaction fires FactionDisbandEvent which
                // triggers cleanup of claims, invites, requests, and relations in HyperFactions
                String factionName = faction.name();
                FactionManager.FactionResult result = factionManager.disbandFaction(faction.id(), uuid);

                if (result == FactionManager.FactionResult.SUCCESS) {
                    player.sendMessage(
                            Message.raw("Faction '").color("#FF5555")
                                    .insert(Message.raw(factionName).color("#AAAAAA"))
                                    .insert(Message.raw("' has been disbanded.").color("#FF5555"))
                    );
                } else {
                    player.sendMessage(MessageUtil.errorText("Failed to disband faction."));
                }

                guiManager.openFactionMain(player, ref, store, playerRef);
            }
        }
    }
}
