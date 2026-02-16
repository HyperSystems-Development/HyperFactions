package com.hyperfactions.gui.shared.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.util.TimeUtil;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Faction Info page - displays detailed information about a specific faction.
 * Used for /f info [faction] command (GUI mode).
 */
public class FactionInfoPage extends InteractiveCustomUIPage<FactionPageData> {

    private final PlayerRef viewerRef;
    private final Faction targetFaction;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final RelationManager relationManager;
    private final EconomyManager economyManager;
    private final GuiManager guiManager;
    private final String sourcePage;

    // Context for returning to PlayerInfoPage (when sourcePage = "player_info")
    private final UUID sourcePlayerUuid;
    private final String sourcePlayerName;
    private final String playerInfoSourcePage;

    public FactionInfoPage(PlayerRef viewerRef,
                           Faction targetFaction,
                           FactionManager factionManager,
                           PowerManager powerManager,
                           RelationManager relationManager,
                           EconomyManager economyManager,
                           GuiManager guiManager) {
        this(viewerRef, targetFaction, factionManager, powerManager, relationManager, economyManager, guiManager, null);
    }

    /**
     * Constructor with source page tracking.
     * @param sourcePage The page to return to when back is clicked:
     *                   "browser" - FactionBrowserPage
     *                   "newplayer_browser" - NewPlayerBrowsePage
     *                   "admin_factions" - AdminFactionsPage
     *                   "relations" - FactionRelationsPage
     *                   null - just close the page
     */
    public FactionInfoPage(PlayerRef viewerRef,
                           Faction targetFaction,
                           FactionManager factionManager,
                           PowerManager powerManager,
                           RelationManager relationManager,
                           EconomyManager economyManager,
                           GuiManager guiManager,
                           String sourcePage) {
        this(viewerRef, targetFaction, factionManager, powerManager, relationManager, economyManager, guiManager,
                sourcePage, null, null, null);
    }

    /**
     * Constructor with source page tracking and player context.
     * Used when navigating from PlayerInfoPage → FactionInfoPage so back returns to PlayerInfoPage.
     *
     * @param sourcePage          "player_info" when coming from PlayerInfoPage
     * @param sourcePlayerUuid    The UUID of the player being viewed in PlayerInfoPage
     * @param sourcePlayerName    The name of the player being viewed in PlayerInfoPage
     * @param playerInfoSourcePage The sourcePage that PlayerInfoPage itself was opened with
     */
    public FactionInfoPage(PlayerRef viewerRef,
                           Faction targetFaction,
                           FactionManager factionManager,
                           PowerManager powerManager,
                           RelationManager relationManager,
                           EconomyManager economyManager,
                           GuiManager guiManager,
                           String sourcePage,
                           UUID sourcePlayerUuid,
                           String sourcePlayerName,
                           String playerInfoSourcePage) {
        super(viewerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
        this.viewerRef = viewerRef;
        this.targetFaction = targetFaction;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.relationManager = relationManager;
        this.economyManager = economyManager;
        this.guiManager = guiManager;
        this.sourcePage = sourcePage;
        this.sourcePlayerUuid = sourcePlayerUuid;
        this.sourcePlayerName = sourcePlayerName;
        this.playerInfoSourcePage = playerInfoSourcePage;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the faction info template
        cmd.append("HyperFactions/shared/faction_info.ui");

        // Check if viewer is viewing their own faction
        Faction viewerFaction = factionManager.getPlayerFaction(viewerRef.getUuid());
        boolean isOwnFaction = viewerFaction != null && viewerFaction.id().equals(targetFaction.id());

        // === Header Section ===
        // Faction name
        cmd.set("#FactionName.Text", targetFaction.name());
        // Note: Cannot dynamically set text color via cmd.set()

        // Tag (if set)
        String tag = targetFaction.tag();
        if (tag != null && !tag.isEmpty()) {
            cmd.set("#FactionTag.Text", "[" + tag + "]");
        } else {
            cmd.set("#FactionTag.Text", "");
        }

        // Description
        String description = targetFaction.description();
        cmd.set("#FactionDescription.Text",
                description != null && !description.isEmpty() ? description : "No description set.");

        // Open/Closed status indicator
        cmd.set("#StatusIndicator.Text", targetFaction.open() ? "Open" : "Invite Only");
        // Note: Cannot dynamically set text color via cmd.set()

        // === Stats Section ===
        PowerManager.FactionPowerStats powerStats = powerManager.getFactionPowerStats(targetFaction.id());

        // Power
        cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", powerStats.currentPower(), powerStats.maxPower()));

        // Claims
        cmd.set("#ClaimsValue.Text", String.format("%d / %d", powerStats.currentClaims(), powerStats.maxClaims()));

        // Members
        int memberCount = targetFaction.getMemberCount();
        int maxMembers = ConfigManager.get().getMaxMembers();
        cmd.set("#MembersValue.Text", String.format("%d / %d", memberCount, maxMembers));

        // Recruitment status
        cmd.set("#RecruitmentValue.Text", targetFaction.open() ? "Open" : "Invite Only");
        // Note: Cannot dynamically set text color via cmd.set()

        // Founded date
        cmd.set("#FoundedValue.Text", TimeUtil.formatRelative(targetFaction.createdAt()));

        // Relations count
        int allyCount = relationManager.getAllies(targetFaction.id()).size();
        int enemyCount = relationManager.getEnemies(targetFaction.id()).size();
        cmd.set("#AlliesValue.Text", String.valueOf(allyCount));
        cmd.set("#EnemiesValue.Text", String.valueOf(enemyCount));

        // Raidable status
        if (powerStats.isRaidable()) {
            cmd.set("#RaidableValue.Text", "Raidable");
            // Note: Cannot dynamically set text color via cmd.set()
        } else {
            cmd.set("#RaidableValue.Text", "Protected");
            // Note: Cannot dynamically set text color via cmd.set()
        }

        // Treasury balance (visible when economy enabled)
        if (economyManager != null) {
            cmd.set("#TreasuryCard.Visible", true);
            double balance = economyManager.getFactionBalance(targetFaction.id());
            cmd.set("#TreasuryValue.Text", economyManager.formatCurrencyCompact(balance));
        }

        // === Leadership Section ===
        // Leader
        FactionMember leader = targetFaction.getLeader();
        cmd.set("#LeaderName.Text", leader != null ? leader.username() : "Unknown");

        // Officers
        List<FactionMember> officers = targetFaction.getMembersSorted().stream()
                .filter(m -> m.role() == FactionRole.OFFICER)
                .toList();
        if (officers.isEmpty()) {
            cmd.set("#OfficersValue.Text", "None");
        } else {
            String officerNames = officers.stream()
                    .map(FactionMember::username)
                    .limit(3) // Show max 3 names
                    .collect(Collectors.joining(", "));
            if (officers.size() > 3) {
                officerNames += " +" + (officers.size() - 3) + " more";
            }
            cmd.set("#OfficersValue.Text", officerNames);
        }

        // === Event Bindings ===
        // View Members and Relations buttons - only show for own faction
        if (isOwnFaction) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ViewMembersBtn",
                    EventData.of("Button", "ViewMembers")
                            .append("FactionId", targetFaction.id().toString()),
                    false
            );

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#ViewRelationsBtn",
                    EventData.of("Button", "ViewRelations")
                            .append("FactionId", targetFaction.id().toString()),
                    false
            );
        } else {
            // Hide buttons when viewing another faction
            cmd.set("#ViewMembersBtn.Visible", false);
            cmd.set("#ViewRelationsBtn.Visible", false);
        }

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionPageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "ViewMembers" -> {
                // Check if viewer is in this faction
                Faction viewerFaction = factionManager.getPlayerFaction(viewerRef.getUuid());
                if (viewerFaction != null && viewerFaction.id().equals(targetFaction.id())) {
                    // Own faction - open full members page
                    guiManager.openFactionMembers(player, ref, store, playerRef, targetFaction);
                } else {
                    // Other faction - just show member list in browser
                    guiManager.openFactionBrowser(player, ref, store, playerRef);
                }
            }

            case "ViewRelations" -> {
                // Check if viewer is in this faction
                Faction viewerFaction = factionManager.getPlayerFaction(viewerRef.getUuid());
                if (viewerFaction != null && viewerFaction.id().equals(targetFaction.id())) {
                    // Own faction - open full relations page
                    guiManager.openFactionRelations(player, ref, store, playerRef, targetFaction);
                } else {
                    // Other faction - open set relation modal to interact
                    guiManager.openSetRelationModal(player, ref, store, playerRef, viewerFaction);
                }
            }

            case "Back" -> handleBack(player, ref, store, playerRef);
        }
    }

    private void handleBack(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
        if (sourcePage == null) {
            guiManager.closePage(player, ref, store);
            return;
        }

        switch (sourcePage) {
            case "browser" -> guiManager.openFactionBrowser(player, ref, store, playerRef);
            case "newplayer_browser" -> guiManager.openNewPlayerBrowse(player, ref, store, playerRef, 0, null, "");
            case "admin_factions" -> guiManager.openAdminFactions(player, ref, store, playerRef);
            case "player_info" -> {
                if (sourcePlayerUuid != null && sourcePlayerName != null) {
                    guiManager.openPlayerInfo(player, ref, store, playerRef,
                            sourcePlayerUuid, sourcePlayerName, playerInfoSourcePage);
                } else {
                    guiManager.closePage(player, ref, store);
                }
            }
            case "relations" -> {
                Faction viewerFaction = factionManager.getPlayerFaction(viewerRef.getUuid());
                if (viewerFaction != null) {
                    guiManager.openFactionRelations(player, ref, store, playerRef, viewerFaction);
                } else {
                    guiManager.closePage(player, ref, store);
                }
            }
            default -> guiManager.closePage(player, ref, store);
        }
    }
}
