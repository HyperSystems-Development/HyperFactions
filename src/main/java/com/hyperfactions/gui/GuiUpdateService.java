package com.hyperfactions.gui;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Bridges manager change events to real-time GUI updates.
 * Receives notifications from managers when data changes, looks up affected
 * viewers via {@link ActivePageTracker}, and dispatches {@link RefreshablePage#refreshContent()}
 * on the appropriate world thread.
 */
public class GuiUpdateService {

  private final ActivePageTracker tracker;

  private final FactionManager factionManager;

  /** Creates a new GuiUpdateService. */
  public GuiUpdateService(@NotNull ActivePageTracker tracker, @NotNull FactionManager factionManager) {
    this.tracker = tracker;
    this.factionManager = factionManager;
  }

  // ============================================================
  // Invite Events
  // ============================================================

  /**
   * Called when an invite is created.
   * Refreshes: recipient's invites page + faction's invites page.
   */
  public void onInviteCreated(@NotNull PendingInvite invite) {
    // Refresh the recipient's new player invites page
    dispatchRefresh(invite.playerUuid());
    // Refresh faction's invites page (officers viewing it)
    refreshFactionPage("invites", invite.factionId());
  }

  /**
   * Called when an invite is removed (cancelled, expired, or accepted).
   * Refreshes: player's invites page + faction's invites page.
   */
  public void onInviteRemoved(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid);
    refreshFactionPage("invites", factionId);
  }

  // ============================================================
  // Join Request Events
  // ============================================================

  /**
   * Called when a join request is created.
   * Refreshes: faction's invites page (where officers see requests).
   */
  public void onRequestCreated(@NotNull JoinRequest request) {
    refreshFactionPage("invites", request.factionId());
  }

  /**
   * Called when a join request is accepted.
   * Refreshes: faction members + dashboard + invites pages, and the player's page.
   */
  public void onRequestAccepted(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid);
    refreshFactionPage("invites", factionId);
    refreshFactionPage("members", factionId);
    refreshFactionPage("dashboard", factionId);
  }

  /**
   * Called when a join request is declined.
   * Refreshes: faction invites + player's invites page.
   */
  public void onRequestDeclined(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid);
    refreshFactionPage("invites", factionId);
  }

  // ============================================================
  // Member Events
  // ============================================================

  /**
   * Called when a member joins a faction.
   * Refreshes: faction members + dashboard pages.
   */
  public void onMemberJoined(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    refreshFactionPage("members", factionId);
    refreshFactionPage("dashboard", factionId);
  }

  /**
   * Called when a member leaves a faction.
   * Refreshes: faction members + dashboard pages.
   */
  public void onMemberLeft(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    refreshFactionPage("members", factionId);
    refreshFactionPage("dashboard", factionId);
  }

  /**
   * Called when a member is kicked from a faction.
   * Refreshes: faction members + dashboard pages.
   */
  public void onMemberKicked(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    refreshFactionPage("members", factionId);
    refreshFactionPage("dashboard", factionId);
  }

  /**
   * Called when a member's role changes (promote/demote).
   * Refreshes: faction members page.
   */
  public void onMemberRoleChanged(@NotNull UUID factionId, @NotNull UUID playerUuid) {
    refreshFactionPage("members", factionId);
  }

  // ============================================================
  // Relation Events
  // ============================================================

  /**
   * Called when a relation changes between two factions.
   * Refreshes: both factions' relations + dashboard pages.
   */
  public void onRelationChanged(@NotNull UUID factionId, @NotNull UUID targetFactionId) {
    refreshFactionPage("relations", factionId);
    refreshFactionPage("relations", targetFactionId);
    refreshFactionPage("dashboard", factionId);
    refreshFactionPage("dashboard", targetFactionId);
  }

  /**
   * Called when an ally request is received.
   * Refreshes: target faction's relations page.
   */
  public void onAllyRequestReceived(@NotNull UUID targetFactionId, @NotNull UUID fromFactionId) {
    refreshFactionPage("relations", targetFactionId);
  }

  // ============================================================
  // Chat Events
  // ============================================================

  /**
   * Called when a chat message is sent in a faction/ally channel.
   * Refreshes chat pages for faction members.
   */
  public void onChatMessage(@NotNull UUID factionId) {
    refreshFactionPage("chat", factionId);
  }

  // ============================================================
  // Territory Events
  // ============================================================

  /**
   * Called when a chunk is claimed, unclaimed, or overclaimed.
   * Refreshes ALL players viewing any map page.
   */
  public void onChunkClaimed(@NotNull String worldName, int chunkX, int chunkZ) {
    // Refresh all map viewers (faction map, new player map, admin zone map)
    refreshAllOnPage("map");
    refreshAllOnPage("admin_zone_map");
  }

  // ============================================================
  // Internal Dispatch
  // ============================================================

  /**
   * Dispatches a refresh to a specific player if they have a RefreshablePage open.
   */
  private void dispatchRefresh(@NotNull UUID playerUuid) {
    ActivePageTracker.ActivePageInfo info = tracker.get(playerUuid);
    if (info == null) {
      return;
    }

    if (!(info.page() instanceof RefreshablePage)) {
      return;
    }

    PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
    if (playerRef == null) {
      tracker.unregister(playerUuid);
      return;
    }

    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) {
      tracker.unregister(playerUuid);
      return;
    }

    World world = Universe.get().getWorld(worldUuid);
    if (world == null) {
      tracker.unregister(playerUuid);
      return;
    }

    world.execute(() -> {
      // Re-check after thread dispatch
      ActivePageTracker.ActivePageInfo fresh = tracker.get(playerUuid);
      if (fresh == null || fresh.page() != info.page()) {
        return;
      }

      if (fresh.page() instanceof RefreshablePage r) {
        try {
          r.refreshContent();
        } catch (Exception e) {
          ErrorHandler.report("[GuiUpdate] Error refreshing page for " + playerUuid, e);
        }
      }
    });
  }

  /**
   * Refreshes all players viewing a specific page for a specific faction.
   */
  private void refreshFactionPage(@NotNull String pageId, @NotNull UUID factionId) {
    // Get all members of the faction who might be viewing this page
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      return;
    }

    for (UUID memberUuid : faction.members().keySet()) {
      ActivePageTracker.ActivePageInfo info = tracker.get(memberUuid);
      if (info != null && info.pageId().equals(pageId)
          && factionId.equals(info.factionId())) {
        dispatchRefresh(memberUuid);
      }
    }
  }

  /**
   * Refreshes ALL players viewing a specific page (any faction context).
   * Used for global events like chunk changes that affect all map viewers.
   */
  private void refreshAllOnPage(@NotNull String pageId) {
    List<UUID> viewers = tracker.getPlayersOnPage(pageId);
    for (UUID playerUuid : viewers) {
      dispatchRefresh(playerUuid);
    }
  }
}
