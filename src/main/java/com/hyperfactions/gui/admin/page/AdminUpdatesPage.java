package com.hyperfactions.gui.admin.page;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminUpdatesData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Admin Updates page - placeholder for update management.
 */
public class AdminUpdatesPage extends InteractiveCustomUIPage<AdminUpdatesData> {

    private final PlayerRef playerRef;
    private final GuiManager guiManager;

    public AdminUpdatesPage(PlayerRef playerRef, GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminUpdatesData.CODEC);
        this.playerRef = playerRef;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        // Load the placeholder template first (nav bar elements must exist before setupBar)
        cmd.append("HyperFactions/admin/admin_updates.ui");

        // Setup admin nav bar (must be after template load)
        AdminNavBarHelper.setupBar(playerRef, "updates", cmd, events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminUpdatesData data) {
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

        // Handle other button events (placeholder for future implementation)
        if (data.button != null) {
            switch (data.button) {
                case "Back" -> guiManager.closePage(player, ref, store);
            }
        }
    }
}
