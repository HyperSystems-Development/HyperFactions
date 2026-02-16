package com.hyperfactions.gui.admin.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminDashboardData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.ZoneManager;
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

import java.util.Collection;

/**
 * Admin Dashboard page - shows server-wide statistics overview.
 */
public class AdminDashboardPage extends InteractiveCustomUIPage<AdminDashboardData> {

    private final PlayerRef playerRef;
    private final HyperFactions plugin;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final ZoneManager zoneManager;
    private final GuiManager guiManager;

    public AdminDashboardPage(PlayerRef playerRef,
                              HyperFactions plugin,
                              FactionManager factionManager,
                              PowerManager powerManager,
                              ZoneManager zoneManager,
                              GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminDashboardData.CODEC);
        this.playerRef = playerRef;
        this.plugin = plugin;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.zoneManager = zoneManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the dashboard template
        cmd.append("HyperFactions/admin/admin_dashboard.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "dashboard", cmd, events);

        // Calculate server-wide statistics
        Collection<Faction> allFactions = factionManager.getAllFactions();
        int totalFactions = allFactions.size();
        int totalMembers = allFactions.stream()
                .mapToInt(f -> f.members().size())
                .sum();
        int totalClaims = allFactions.stream()
                .mapToInt(f -> f.claims().size())
                .sum();

        // Zone statistics
        Collection<Zone> allZones = zoneManager.getAllZones();
        long safeZones = allZones.stream().filter(Zone::isSafeZone).count();
        long warZones = allZones.stream().filter(Zone::isWarZone).count();

        // Power statistics
        double totalPower = 0;
        for (Faction faction : allFactions) {
            PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
            totalPower += stats.currentPower();
        }
        double avgPower = totalFactions > 0 ? totalPower / totalFactions : 0;

        // Set statistics
        cmd.set("#TotalFactions.Text", String.valueOf(totalFactions));
        cmd.set("#TotalMembers.Text", String.valueOf(totalMembers));
        cmd.set("#TotalClaims.Text", String.valueOf(totalClaims));
        cmd.set("#SafeZones.Text", String.valueOf(safeZones));
        cmd.set("#WarZones.Text", String.valueOf(warZones));
        cmd.set("#TotalPower.Text", String.format("%.0f", totalPower));
        cmd.set("#AvgPower.Text", String.format("%.1f", avgPower));

        // Setup bypass toggle
        boolean bypassEnabled = plugin.isAdminBypassEnabled(playerRef.getUuid());
        cmd.set("#BypassState.Text", bypassEnabled ? "On" : "Off");
        cmd.set("#BypassState.Style.TextColor", bypassEnabled ? "#55FF55" : "#FF5555");
        cmd.set("#ToggleBypassBtn.Text", bypassEnabled ? "Disable" : "Enable");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ToggleBypassBtn",
                EventData.of("Button", "ToggleBypass"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminDashboardData data) {
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

        // Handle bypass toggle button
        if ("ToggleBypass".equals(data.button)) {
            boolean nowEnabled = plugin.toggleAdminBypass(playerRef.getUuid());
            // Refresh the bypass section UI to show new state
            rebuildBypassSection(nowEnabled);
        }
    }

    /**
     * Rebuilds only the bypass section UI elements.
     */
    private void rebuildBypassSection(boolean bypassEnabled) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        // Update bypass state display
        cmd.set("#BypassState.Text", bypassEnabled ? "On" : "Off");
        cmd.set("#BypassState.Style.TextColor", bypassEnabled ? "#55FF55" : "#FF5555");
        cmd.set("#ToggleBypassBtn.Text", bypassEnabled ? "Disable" : "Enable");

        // Re-bind the toggle button event
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ToggleBypassBtn",
                EventData.of("Button", "ToggleBypass"),
                false
        );

        sendUpdate(cmd, events, false);
    }
}
