package com.hyperfactions.lifecycle;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionDisbandEvent;
import com.hyperfactions.api.events.FactionMemberEvent;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiUpdateService;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.Logger;
import com.hyperfactions.worldmap.MapPlayerFilterService;
import com.hyperfactions.worldmap.WorldMapService;

import java.util.UUID;

/**
 * Static utility class that wires all manager callbacks for GUI updates,
 * events, announcements, and notifications. Called from HyperFactions.enable()
 * after all managers are initialized.
 */
public final class CallbackWiring {

    private CallbackWiring() {}

    /**
     * Wires all manager callbacks for GUI updates, events, announcements, etc.
     * Called from HyperFactions.enable() after all managers are initialized.
     *
     * @param hf                the HyperFactions instance
     * @param guiUpdateService  the GUI update service
     * @param membershipHandler the membership history handler
     */
    public static void wireAll(HyperFactions hf, GuiUpdateService guiUpdateService,
                               MembershipHistoryHandler membershipHandler) {
        wireGuiCallbacks(guiUpdateService, hf.getInviteManager(), hf.getJoinRequestManager(),
            hf.getRelationManager(), hf.getClaimManager());
        wireMemberEventBus(guiUpdateService);
        wireMembershipHistoryEventBus(membershipHandler);
        wireFactionDisbandEventBus(hf);
        wireCombatTagCallbacks(hf.getCombatTagManager(), hf.getPowerManager());
        wireWorldMapCallbacks(hf.getWorldMapService(), hf.getClaimManager());
        wireMapPlayerFilterCallbacks(hf, hf.getMapPlayerFilterService(), hf.getRelationManager());
        wireAnnouncementCallbacks(hf.getAnnouncementManager(), hf.getFactionManager(),
            hf.getClaimManager(), hf.getRelationManager());
        wireOverclaimNotification(hf);
        wireEconomyCallbacks(hf);
    }

    /**
     * Wires manager callbacks for real-time GUI updates.
     */
    private static void wireGuiCallbacks(GuiUpdateService guiUpdateService,
                                         InviteManager inviteManager,
                                         JoinRequestManager joinRequestManager,
                                         RelationManager relationManager,
                                         ClaimManager claimManager) {
        inviteManager.setOnInviteCreated(guiUpdateService::onInviteCreated);
        inviteManager.setOnInviteRemoved(guiUpdateService::onInviteRemoved);
        joinRequestManager.setOnRequestCreated(guiUpdateService::onRequestCreated);
        joinRequestManager.setOnRequestAccepted(guiUpdateService::onRequestAccepted);
        joinRequestManager.setOnRequestDeclined(guiUpdateService::onRequestDeclined);
        relationManager.setOnRelationChanged(guiUpdateService::onRelationChanged);
        relationManager.setOnAllyRequestReceived(guiUpdateService::onAllyRequestReceived);
        claimManager.setOnGuiChunkChangeCallback(guiUpdateService::onChunkClaimed);
    }

    /**
     * Wires EventBus for member changes to GUI updates.
     */
    private static void wireMemberEventBus(GuiUpdateService guiUpdateService) {
        EventBus.register(FactionMemberEvent.class, event -> {
            switch (event.type()) {
                case JOIN -> guiUpdateService.onMemberJoined(event.faction().id(), event.playerUuid());
                case LEAVE -> guiUpdateService.onMemberLeft(event.faction().id(), event.playerUuid());
                case KICK -> guiUpdateService.onMemberKicked(event.faction().id(), event.playerUuid());
                case PROMOTE, DEMOTE -> guiUpdateService.onMemberRoleChanged(event.faction().id(), event.playerUuid());
            }
        });
    }

    /**
     * Wires EventBus for membership history recording.
     */
    private static void wireMembershipHistoryEventBus(MembershipHistoryHandler membershipHandler) {
        EventBus.register(FactionMemberEvent.class, membershipHandler::handleMembershipHistory);
        EventBus.register(FactionDisbandEvent.class, membershipHandler::handleDisbandHistory);
    }

    /**
     * Wires EventBus for faction disband cleanup.
     */
    private static void wireFactionDisbandEventBus(HyperFactions hf) {
        EventBus.register(FactionDisbandEvent.class, hf::handleFactionDisband);
    }

    /**
     * Wires combat tag callbacks (combat logout power penalty).
     */
    private static void wireCombatTagCallbacks(CombatTagManager combatTagManager, PowerManager powerManager) {
        combatTagManager.setOnCombatLogout(playerUuid -> {
            double penalty = ConfigManager.get().getLogoutPowerLoss();
            powerManager.applyCombatLogoutPenalty(playerUuid, penalty);
            com.hyperfactions.util.Logger.info("Player %s combat logged - %.1f power penalty applied", playerUuid, penalty);
        });
    }

    /**
     * Wires world map service callbacks for chunk-specific and bulk claim changes.
     */
    private static void wireWorldMapCallbacks(WorldMapService worldMapService, ClaimManager claimManager) {
        claimManager.setOnChunkChangeCallback(worldMapService::queueChunkRefresh);
        claimManager.setOnClaimChangeCallback(worldMapService::triggerFactionWideRefresh);
    }

    /**
     * Wires map player filter callbacks for faction membership and relation changes.
     * Updates player visibility on the world map/compass when faction state changes.
     */
    private static void wireMapPlayerFilterCallbacks(HyperFactions hf,
                                                      MapPlayerFilterService filterService,
                                                      RelationManager relationManager) {
        // Member join/leave/kick → update all players' filters
        EventBus.register(FactionMemberEvent.class, event -> {
            switch (event.type()) {
                case JOIN, LEAVE, KICK -> filterService.updateForFaction(event.faction());
                default -> {} // PROMOTE/DEMOTE don't affect visibility
            }
        });

        // Faction disband → update all players (former members are now factionless)
        EventBus.register(FactionDisbandEvent.class, event -> filterService.updateForAllPlayers());

        // Relation changes → update all players (both factions' members' views change)
        relationManager.setOnRelationChangedForMapFilter((factionId, targetFactionId) ->
            filterService.updateForAllPlayers());
    }

    /**
     * Wires announcement manager callbacks for faction events.
     */
    private static void wireAnnouncementCallbacks(AnnouncementManager announcementManager,
                                                  FactionManager factionManager,
                                                  ClaimManager claimManager,
                                                  RelationManager relationManager) {
        factionManager.setOnFactionCreated((name, leader) ->
            announcementManager.announceFactionCreated(name, leader));
        factionManager.setOnFactionDisbanded(name ->
            announcementManager.announceFactionDisbanded(name));
        factionManager.setOnLeadershipTransferred((faction, oldLeader, newLeader) ->
            announcementManager.announceLeadershipTransfer(faction, oldLeader, newLeader));
        claimManager.setOnOverclaimCallback((attacker, defender) ->
            announcementManager.announceOverclaim(attacker, defender));
        relationManager.setOnWarDeclared((declaring, target) ->
            announcementManager.announceWarDeclared(declaring, target));
        relationManager.setOnAllianceFormed((f1, f2) ->
            announcementManager.announceAllianceFormed(f1, f2));
        relationManager.setOnAllianceBroken((f1, f2) ->
            announcementManager.announceAllianceBroken(f1, f2));
    }

    /**
     * Wires economy callbacks for faction creation and disband.
     * On creation: initializes faction economy with starting balance.
     * On disband: refunds balance to leader (if configured) or destroys it.
     */
    private static void wireEconomyCallbacks(HyperFactions hf) {
        if (!hf.isTreasuryEnabled()) return;

        EconomyManager econ = hf.getEconomyManager();
        if (econ == null) return;

        // Faction creation → initialize economy
        hf.getFactionManager().setOnFactionCreatedForEconomy(faction -> {
            econ.initializeFaction(faction.id());
        });

        // Faction disband → refund or destroy balance
        EventBus.register(FactionDisbandEvent.class, event -> {
            Faction faction = event.faction();
            double balance = econ.getFactionBalance(faction.id());

            if (balance > 0 && ConfigManager.get().isEconomyDisbandRefundToLeader()) {
                // Refund balance to leader
                UUID leaderId = faction.getLeaderId();
                VaultEconomyProvider vault = econ.getVaultProvider();
                if (leaderId != null && vault.deposit(leaderId, balance)) {
                    Logger.info("Refunded %s to leader %s on faction %s disband",
                            econ.formatCurrency(balance), leaderId, faction.name());
                } else {
                    Logger.warn("Failed to refund %s to leader on faction %s disband",
                            econ.formatCurrency(balance), faction.name());
                }
            } else if (balance > 0) {
                Logger.info("Destroyed %s balance on faction %s disband (refund disabled)",
                        econ.formatCurrency(balance), faction.name());
            }

            econ.removeFaction(faction.id());
        });
    }

    /**
     * Wires overclaim notification callback for alerting faction members.
     * Uses deferred playerLookup via hf.lookupPlayer().
     */
    private static void wireOverclaimNotification(HyperFactions hf) {
        hf.getClaimManager().setNotificationCallback((factionId, message, hexColor) -> {
            Faction faction = hf.getFactionManager().getFaction(factionId);
            if (faction == null) return;

            ConfigManager cfg = ConfigManager.get();
            com.hypixel.hytale.server.core.Message formatted =
                com.hypixel.hytale.server.core.Message.raw("[").color(cfg.getPrefixBracketColor())
                    .insert(com.hypixel.hytale.server.core.Message.raw(cfg.getPrefixText()).color(cfg.getPrefixColor()))
                    .insert(com.hypixel.hytale.server.core.Message.raw("] ").color(cfg.getPrefixBracketColor()))
                    .insert(com.hypixel.hytale.server.core.Message.raw(message).color(hexColor));

            for (UUID memberUuid : faction.members().keySet()) {
                com.hypixel.hytale.server.core.universe.PlayerRef member = hf.lookupPlayer(memberUuid);
                if (member != null) {
                    member.sendMessage(formatted);
                }
            }
        });
    }
}
