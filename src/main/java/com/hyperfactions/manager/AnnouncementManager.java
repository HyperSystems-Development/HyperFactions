package com.hyperfactions.manager;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.AnnouncementConfig;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Collection;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

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
    if (!config.isEnabled() || !config.isFactionCreated()) {
      return;
    }

    broadcastSuccess(CommonKeys.ServerAnnounce.FACTION_CREATED, leaderName, factionName);
  }

  /**
   * Announces that a faction has been disbanded.
   *
   * @param factionName the faction name
   */
  public void announceFactionDisbanded(@NotNull String factionName) {
    AnnouncementConfig config = ConfigManager.get().announcements();
    if (!config.isEnabled() || !config.isFactionDisbanded()) {
      return;
    }

    broadcastError(CommonKeys.ServerAnnounce.FACTION_DISBANDED, factionName);
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
    if (!config.isEnabled() || !config.isLeadershipTransfer()) {
      return;
    }

    broadcastInfo(CommonKeys.ServerAnnounce.LEADERSHIP_TRANSFER, MessageUtil.COLOR_GOLD, newLeader, factionName);
  }

  /**
   * Announces an overclaim.
   *
   * @param attackerFaction the attacking faction name
   * @param defenderFaction the defending faction name
   */
  public void announceOverclaim(@NotNull String attackerFaction, @NotNull String defenderFaction) {
    AnnouncementConfig config = ConfigManager.get().announcements();
    if (!config.isEnabled() || !config.isOverclaim()) {
      return;
    }

    broadcastError(CommonKeys.ServerAnnounce.OVERCLAIM, attackerFaction, defenderFaction);
  }

  /**
   * Announces a war declaration.
   *
   * @param declaringFaction the declaring faction name
   * @param targetFaction    the target faction name
   */
  public void announceWarDeclared(@NotNull String declaringFaction, @NotNull String targetFaction) {
    AnnouncementConfig config = ConfigManager.get().announcements();
    if (!config.isEnabled() || !config.isWarDeclared()) {
      return;
    }

    broadcastError(CommonKeys.ServerAnnounce.WAR_DECLARED, declaringFaction, targetFaction);
  }

  /**
   * Announces an alliance formation.
   *
   * @param faction1 the first faction name
   * @param faction2 the second faction name
   */
  public void announceAllianceFormed(@NotNull String faction1, @NotNull String faction2) {
    AnnouncementConfig config = ConfigManager.get().announcements();
    if (!config.isEnabled() || !config.isAllianceFormed()) {
      return;
    }

    broadcastSuccess(CommonKeys.ServerAnnounce.ALLIANCE_FORMED, faction1, faction2);
  }

  /**
   * Announces an alliance being broken.
   *
   * @param faction1 the faction breaking the alliance
   * @param faction2 the other faction
   */
  public void announceAllianceBroken(@NotNull String faction1, @NotNull String faction2) {
    AnnouncementConfig config = ConfigManager.get().announcements();
    if (!config.isEnabled() || !config.isAllianceBroken()) {
      return;
    }

    broadcastInfo(CommonKeys.ServerAnnounce.ALLIANCE_BROKEN, MessageUtil.COLOR_GOLD, faction1, faction2);
  }

  /**
   * Broadcasts a success-styled message to all online players, resolving i18n per-player.
   */
  private void broadcastSuccess(@NotNull String key, Object... args) {
    broadcast(player -> MessageUtil.success(player, key, args));
  }

  /**
   * Broadcasts an error-styled message to all online players, resolving i18n per-player.
   */
  private void broadcastError(@NotNull String key, Object... args) {
    broadcast(player -> MessageUtil.error(player, key, args));
  }

  /**
   * Broadcasts an info-styled message to all online players, resolving i18n per-player.
   */
  private void broadcastInfo(@NotNull String key, @NotNull String color, Object... args) {
    broadcast(player -> MessageUtil.info(player, key, color, args));
  }

  /**
   * Broadcasts a per-player resolved message to all online players.
   */
  private void broadcast(@NotNull java.util.function.Function<PlayerRef, com.hypixel.hytale.server.core.Message> messageFactory) {
    try {
      Collection<PlayerRef> players = onlinePlayersSupplier.get();
      if (players == null) {
        return;
      }

      for (PlayerRef player : players) {
        player.sendMessage(messageFactory.apply(player));
      }
    } catch (Exception e) {
      ErrorHandler.report("Failed to broadcast announcement", e);
    }
  }
}
