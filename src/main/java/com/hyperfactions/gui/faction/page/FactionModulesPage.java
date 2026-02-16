package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionModulesData;
import com.hyperfactions.manager.FactionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

/**
 * Faction Modules page - displays available faction modules with status.
 * Currently shows placeholder cards for upcoming features.
 */
public class FactionModulesPage extends InteractiveCustomUIPage<FactionModulesData> {

    private static final String PAGE_ID = "modules";

    // Module definitions for the 2x2 grid
    private static final List<ModuleInfo> MODULES = List.of(
            new ModuleInfo("treasury", "Treasury", "Faction bank & economy system", "#fbbf24", false),
            new ModuleInfo("raids", "Raids", "Scheduled faction battles", "#ef4444", false),
            new ModuleInfo("levels", "Levels", "Faction progression & XP", "#22c55e", false),
            new ModuleInfo("war", "War", "Formal war declarations", "#a855f7", false)
    );

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;

    public FactionModulesPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              GuiManager guiManager,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionModulesData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the modules template
        cmd.append("HyperFactions/faction/faction_modules.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

        // Populate module cards
        for (int i = 0; i < MODULES.size(); i++) {
            ModuleInfo module = MODULES.get(i);
            String cardSelector = "#ModuleCard" + i;

            // Set module info
            cmd.set(cardSelector + " #ModuleName.Text", module.name);
            cmd.set(cardSelector + " #ModuleDesc.Text", module.description);

            // Set color indicator
            cmd.set(cardSelector + " #ColorBar.Background.Color", module.color);

            // Set status badge
            if (module.available) {
                cmd.set(cardSelector + " #StatusBadge.Text", "Available");
                cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#22c55e");
            } else {
                cmd.set(cardSelector + " #StatusBadge.Text", "Coming Soon");
                cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#888888");
            }

            // Bind click event (for future use when modules are available)
            if (module.available) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cardSelector + " #ModuleBtn",
                        EventData.of("Button", "OpenModule").append("ModuleId", module.id),
                        false
                );
            }
        }

        // Back button - return to settings
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionModulesData data) {
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

        switch (data.button) {
            case "Back" -> {
                guiManager.openFactionSettings(player, ref, store, playerRef, faction);
            }

            case "OpenModule" -> {
                // Future: open module configuration page
                // For now, modules are not yet implemented
                sendUpdate();
            }

            default -> sendUpdate();
        }
    }

    private record ModuleInfo(String id, String name, String description, String color, boolean available) {}
}
