package com.hyperfactions.gui.page.admin;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionInfoData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.util.TimeUtil;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin Faction Info page - displays detailed information about a faction.
 * Uses admin navigation context so Back returns to AdminFactionsPage.
 */
public class AdminFactionInfoPage extends InteractiveCustomUIPage<AdminFactionInfoData> {

    private final PlayerRef playerRef;
    private final UUID factionId;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final RelationManager relationManager;
    private final GuiManager guiManager;

    public AdminFactionInfoPage(PlayerRef playerRef,
                                UUID factionId,
                                FactionManager factionManager,
                                PowerManager powerManager,
                                RelationManager relationManager,
                                GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionInfoData.CODEC);
        this.playerRef = playerRef;
        this.factionId = factionId;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.relationManager = relationManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the admin faction info template
        cmd.append("HyperFactions/admin/admin_faction_info.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

        // Get the faction
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            cmd.set("#FactionName.Text", "Faction Not Found");
            cmd.set("#FactionDescription.Text", "This faction no longer exists.");
            return;
        }

        // === Header Section ===
        cmd.set("#FactionName.Text", faction.name());

        // Tag (if set)
        String tag = faction.tag();
        if (tag != null && !tag.isEmpty()) {
            cmd.set("#FactionTag.Text", "[" + tag + "]");
        } else {
            cmd.set("#FactionTag.Text", "");
        }

        // Description
        String description = faction.description();
        cmd.set("#FactionDescription.Text",
                description != null && !description.isEmpty() ? description : "No description set.");

        // Open/Closed status indicator
        cmd.set("#StatusIndicator.Text", faction.open() ? "Open" : "Invite Only");

        // === Stats Section ===
        PowerManager.FactionPowerStats powerStats = powerManager.getFactionPowerStats(faction.id());

        // Power
        cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", powerStats.currentPower(), powerStats.maxPower()));

        // Claims
        cmd.set("#ClaimsValue.Text", String.format("%d / %d", powerStats.currentClaims(), powerStats.maxClaims()));

        // Members
        int memberCount = faction.getMemberCount();
        int maxMembers = ConfigManager.get().getMaxMembers();
        cmd.set("#MembersValue.Text", String.format("%d / %d", memberCount, maxMembers));

        // Recruitment status
        cmd.set("#RecruitmentValue.Text", faction.open() ? "Open" : "Invite Only");

        // Founded date
        cmd.set("#FoundedValue.Text", TimeUtil.formatRelative(faction.createdAt()));

        // Relations count
        int allyCount = relationManager.getAllies(faction.id()).size();
        int enemyCount = relationManager.getEnemies(faction.id()).size();
        cmd.set("#AlliesValue.Text", String.valueOf(allyCount));
        cmd.set("#EnemiesValue.Text", String.valueOf(enemyCount));

        // Raidable status
        if (powerStats.isRaidable()) {
            cmd.set("#RaidableValue.Text", "Raidable");
        } else {
            cmd.set("#RaidableValue.Text", "Protected");
        }

        // === Leadership Section ===
        FactionMember leader = faction.getLeader();
        cmd.set("#LeaderName.Text", leader != null ? leader.username() : "Unknown");

        // Officers
        List<FactionMember> officers = faction.getMembersSorted().stream()
                .filter(m -> m.role() == FactionRole.OFFICER)
                .toList();
        if (officers.isEmpty()) {
            cmd.set("#OfficersValue.Text", "None");
        } else {
            String officerNames = officers.stream()
                    .map(FactionMember::username)
                    .limit(3)
                    .collect(Collectors.joining(", "));
            if (officers.size() > 3) {
                officerNames += " +" + (officers.size() - 3) + " more";
            }
            cmd.set("#OfficersValue.Text", officerNames);
        }

        // === Event Bindings ===
        // View Members button - opens admin members page
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewMembersBtn",
                EventData.of("Button", "ViewMembers")
                        .append("FactionId", factionId.toString()),
                false
        );

        // View Relations button - opens admin relations page
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewRelationsBtn",
                EventData.of("Button", "ViewRelations")
                        .append("FactionId", factionId.toString()),
                false
        );

        // View Settings button - opens admin faction settings page
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewSettingsBtn",
                EventData.of("Button", "ViewSettings")
                        .append("FactionId", factionId.toString()),
                false
        );

        // === Power Management Buttons ===
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerSubFive",
                EventData.of("Button", "BulkPower").append("Amount", "-5").append("FactionId", factionId.toString()), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerSubOne",
                EventData.of("Button", "BulkPower").append("Amount", "-1").append("FactionId", factionId.toString()), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerAddOne",
                EventData.of("Button", "BulkPower").append("Amount", "1").append("FactionId", factionId.toString()), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerAddFive",
                EventData.of("Button", "BulkPower").append("Amount", "5").append("FactionId", factionId.toString()), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerResetAll",
                EventData.of("Button", "ResetAllPower").append("FactionId", factionId.toString()), false);

        // Back button - returns to admin factions list
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminFactionInfoData data) {
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

        switch (data.button) {
            case "ViewMembers" -> {
                guiManager.openAdminFactionMembers(player, ref, store, playerRef, factionId);
            }

            case "ViewRelations" -> {
                guiManager.openAdminFactionRelations(player, ref, store, playerRef, factionId);
            }

            case "ViewSettings" -> {
                guiManager.openAdminFactionSettings(player, ref, store, playerRef, factionId);
            }

            case "BulkPower" -> {
                Faction faction = factionManager.getFaction(factionId);
                if (faction != null && data.amount != null) {
                    try {
                        double delta = Double.parseDouble(data.amount);
                        for (UUID memberUuid : faction.members().keySet()) {
                            powerManager.adjustPlayerPower(memberUuid, delta);
                        }
                        Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER,
                                "Admin adjusted all " + faction.getMemberCount() + " members' power by " + String.format("%.1f", delta),
                                playerRef.getUuid()));
                        factionManager.updateFaction(updated);
                        // Rebuild page to show updated stats
                        guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
                    } catch (NumberFormatException ignored) {}
                }
            }

            case "ResetAllPower" -> {
                Faction faction = factionManager.getFaction(factionId);
                if (faction != null) {
                    for (UUID memberUuid : faction.members().keySet()) {
                        powerManager.resetPlayerPower(memberUuid);
                    }
                    Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER,
                            "Admin reset power for all " + faction.getMemberCount() + " members",
                            playerRef.getUuid()));
                    factionManager.updateFaction(updated);
                    guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
                }
            }

            case "Back" -> {
                guiManager.openAdminFactions(player, ref, store, playerRef);
            }
        }
    }
}
