package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.data.AdminDisbandConfirmData;
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
 * Admin confirmation modal for disbanding a faction.
 * Similar to DisbandConfirmPage but without leader permission check.
 */
public class AdminDisbandConfirmPage extends InteractiveCustomUIPage<AdminDisbandConfirmData> {

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final UUID factionId;
    private final String factionName;

    public AdminDisbandConfirmPage(PlayerRef playerRef,
                                   FactionManager factionManager,
                                   GuiManager guiManager,
                                   UUID factionId,
                                   String factionName) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminDisbandConfirmData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.factionId = factionId;
        this.factionName = factionName;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Reuse the shared disband confirmation template
        cmd.append("HyperFactions/shared/disband_confirm.ui");

        // Set faction name in the modal
        cmd.set("#FactionName.Text", factionName);

        // Cancel button - return to admin page
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
                                AdminDisbandConfirmData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Cancel" -> {
                // Return to admin page
                guiManager.openAdminMain(player, ref, store, playerRef);
            }

            case "Confirm" -> {
                // Re-fetch faction to verify it still exists
                Faction faction = factionManager.getFaction(factionId);
                if (faction == null) {
                    player.sendMessage(MessageUtil.errorText("Faction no longer exists."));
                    guiManager.openAdminMain(player, ref, store, playerRef);
                    return;
                }

                // Admin bypass - get the leader's UUID to disband
                UUID leaderId = faction.getLeaderId();
                if (leaderId != null) {
                    FactionManager.FactionResult result = factionManager.disbandFaction(factionId, leaderId);
                    if (result == FactionManager.FactionResult.SUCCESS) {
                        player.sendMessage(
                                Message.raw("Faction '").color("#FF5555")
                                        .insert(Message.raw(factionName).color("#AAAAAA"))
                                        .insert(Message.raw("' has been disbanded.").color("#FF5555"))
                        );
                    } else {
                        player.sendMessage(MessageUtil.errorText("Failed to disband: " + result));
                    }
                } else {
                    player.sendMessage(MessageUtil.errorText("Faction has no leader, cannot disband."));
                }

                // Return to admin page (will show updated list)
                guiManager.openAdminMain(player, ref, store, playerRef);
            }
        }
    }
}
