package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.AnnouncementConfig;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Broadcasts server-wide announcements for significant faction events.
 * Checks {@link AnnouncementConfig} to determine which events are enabled.
 */
public class AnnouncementManager {

    private final Supplier<Collection<PlayerRef>> onlinePlayersSupplier;

    /**
     * Creates a new announcement manager.
     *
     * @param onlinePlayersSupplier supplies the collection of currently online players
     */
    public AnnouncementManager(@NotNull Supplier<Collection<PlayerRef>> onlinePlayersSupplier) {
        this.onlinePlayersSupplier = onlinePlayersSupplier;
    }

    /**
     * Announces that a new faction has been created.
     *
     * @param factionName the faction name
     * @param leaderName  the leader's username
     */
    public void announceFactionCreated(@NotNull String factionName, @NotNull String leaderName) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isFactionCreated()) return;

        broadcast(MessageUtil.info(leaderName + " has founded the faction " + factionName + "!", MessageUtil.COLOR_GREEN));
    }

    /**
     * Announces that a faction has been disbanded.
     *
     * @param factionName the faction name
     */
    public void announceFactionDisbanded(@NotNull String factionName) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isFactionDisbanded()) return;

        broadcast(MessageUtil.error("The faction " + factionName + " has been disbanded!"));
    }

    /**
     * Announces a leadership transfer.
     *
     * @param factionName the faction name
     * @param oldLeader   the old leader's username
     * @param newLeader   the new leader's username
     */
    public void announceLeadershipTransfer(@NotNull String factionName,
                                           @NotNull String oldLeader, @NotNull String newLeader) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isLeadershipTransfer()) return;

        broadcast(MessageUtil.info(newLeader + " is now the leader of " + factionName + "!", MessageUtil.COLOR_GOLD));
    }

    /**
     * Announces an overclaim.
     *
     * @param attackerFaction the attacking faction name
     * @param defenderFaction the defending faction name
     */
    public void announceOverclaim(@NotNull String attackerFaction, @NotNull String defenderFaction) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isOverclaim()) return;

        broadcast(MessageUtil.error(attackerFaction + " has overclaimed territory from " + defenderFaction + "!"));
    }

    /**
     * Announces a war declaration.
     *
     * @param declaringFaction the declaring faction name
     * @param targetFaction    the target faction name
     */
    public void announceWarDeclared(@NotNull String declaringFaction, @NotNull String targetFaction) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isWarDeclared()) return;

        broadcast(MessageUtil.error(declaringFaction + " has declared war on " + targetFaction + "!"));
    }

    /**
     * Announces an alliance formation.
     *
     * @param faction1 the first faction name
     * @param faction2 the second faction name
     */
    public void announceAllianceFormed(@NotNull String faction1, @NotNull String faction2) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isAllianceFormed()) return;

        broadcast(MessageUtil.info(faction1 + " and " + faction2 + " are now allies!", MessageUtil.COLOR_GREEN));
    }

    /**
     * Announces an alliance being broken.
     *
     * @param faction1 the faction breaking the alliance
     * @param faction2 the other faction
     */
    public void announceAllianceBroken(@NotNull String faction1, @NotNull String faction2) {
        AnnouncementConfig config = ConfigManager.get().announcements();
        if (!config.isEnabled() || !config.isAllianceBroken()) return;

        broadcast(MessageUtil.info(faction1 + " and " + faction2 + " are no longer allies!", MessageUtil.COLOR_GOLD));
    }

    /**
     * Builds a formatted announcement message using the configured prefix from config.json.
     */
    private Message buildMessage(@NotNull String text, @NotNull String color) {
        return MessageUtil.info(text, color);
    }

    /**
     * Broadcasts a message to all online players.
     */
    private void broadcast(@NotNull Message message) {
        try {
            Collection<PlayerRef> players = onlinePlayersSupplier.get();
            if (players == null) return;

            for (PlayerRef player : players) {
                player.sendMessage(message);
            }
        } catch (Exception e) {
            Logger.warn("Failed to broadcast announcement: %s", e.getMessage());
        }
    }
}
