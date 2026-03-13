package com.hyperfactions.platform;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles player connect, disconnect, and chat events for HyperFactions.
 * Extracted from HyperFactionsPlugin to reduce class complexity.
 */
public class PlayerConnectionHandler {

  private final HyperFactions hyperFactions;

  private final Map<UUID, PlayerRef> trackedPlayers;

  /** Creates a new PlayerConnectionHandler. */
  public PlayerConnectionHandler(HyperFactions hyperFactions, Map<UUID, PlayerRef> trackedPlayers) {
    this.hyperFactions = hyperFactions;
    this.trackedPlayers = trackedPlayers;
  }

  /**
   * Handles player connect event.
   */
  public void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    UUID uuid = playerRef.getUuid();
    String username = playerRef.getUsername();

    Logger.debug("Player connecting: %s (%s) [username null=%s]", username, uuid, username == null);

    // Track the player
    trackedPlayers.put(uuid, playerRef);
    Logger.debug("Tracked players after connect: %d (contains %s=%s)",
        trackedPlayers.size(), uuid, trackedPlayers.containsKey(uuid));

    // Cache username, track first join and last online, load preferences
    ErrorHandler.guard("Player connect: load/save player data for " + username,
      hyperFactions.getPlayerStorage().loadPlayerData(uuid).thenAccept(opt -> {
        com.hyperfactions.data.PlayerData data = opt.orElseGet(() -> new com.hyperfactions.data.PlayerData(uuid));
        data.setUsername(username);
        long now = System.currentTimeMillis();
        if (data.getFirstJoined() == 0) {
          data.setFirstJoined(now);
        }
        data.setLastOnline(now);
        hyperFactions.getPlayerStorage().savePlayerData(data);

        // Cache language preference for i18n resolution
        if (data.getLanguagePreference() != null) {
          HFMessages.setLanguageOverride(uuid, data.getLanguagePreference());
        }
      }));

    // Load player power
    hyperFactions.getPowerManager().playerOnline(uuid);

    // Restore persistent admin bypass if saved and player still has permission
    ErrorHandler.guard("Player connect: restore admin bypass for " + username,
      hyperFactions.getPlayerStorage().loadPlayerData(uuid).thenAccept(opt -> {
        if (opt.isPresent()) {
          com.hyperfactions.data.PlayerData data = opt.get();
          if (data.isAdminBypassEnabled()) {
            if (PermissionManager.get().hasPermission(uuid, Permissions.ADMIN)) {
              hyperFactions.setAdminBypass(uuid, true);
              Logger.debug("Restored admin bypass for %s", username);
            } else {
              // Player lost admin permission — clear the persisted flag
              data.setAdminBypassEnabled(false);
              hyperFactions.getPlayerStorage().savePlayerData(data);
              Logger.debug("Cleared stale admin bypass for %s (no permission)", username);
            }
          }
        }
      }));

    // Update faction member last online
    hyperFactions.getFactionManager().updateLastOnline(uuid);

    // Pre-warm chat history cache if player is in a faction
    com.hyperfactions.data.Faction playerFaction = hyperFactions.getFactionManager().getPlayerFaction(uuid);
    if (playerFaction != null && hyperFactions.getChatHistoryManager() != null) {
      hyperFactions.getChatHistoryManager().preWarmCache(playerFaction.id());
    }

    // Initialize territory tracking and send initial notification
    // World map provider is now registered via AddWorldEvent, not here
    try {
      UUID worldUuid = playerRef.getWorldUuid();
      if (worldUuid != null) {
        com.hypixel.hytale.server.core.universe.world.World world =
          com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
        if (world != null) {
          // Get spawn position for initial territory notification
          com.hypixel.hytale.math.vector.Transform transform = playerRef.getTransform();
          if (transform != null) {
            com.hypixel.hytale.math.vector.Vector3d position = transform.getPosition();
            hyperFactions.getTerritoryNotifier().onPlayerConnect(
              playerRef, world.getName(), position.getX(), position.getZ()
            );
          }
        }
      }
    } catch (Exception e) {
      Logger.debugTerritory("Failed to initialize territory tracking for %s: %s", username, e.getMessage());
    }
  }

  /**
   * Handles player disconnect event.
   */
  public void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    UUID uuid = playerRef.getUuid();
    String username = playerRef.getUsername();

    Logger.debug("Player disconnecting: %s", username);

    // Handle combat logout
    boolean wasCombatTagged = hyperFactions.getCombatTagManager().handleDisconnect(uuid);
    if (wasCombatTagged) {
      Logger.info("[Combat] Player %s combat logged!", username);
    }

    // Cancel pending teleport
    hyperFactions.getTeleportManager().cancelPending(uuid, hyperFactions::cancelTask);

    // Mark player offline
    hyperFactions.getPowerManager().playerOffline(uuid);

    // Update last online timestamp
    ErrorHandler.guard("Player disconnect: update last online for " + username,
      hyperFactions.getPlayerStorage().loadPlayerData(uuid).thenAccept(opt -> {
        if (opt.isPresent()) {
          com.hyperfactions.data.PlayerData data = opt.get();
          data.setLastOnline(System.currentTimeMillis());
          hyperFactions.getPlayerStorage().savePlayerData(data);
        }
      }));

    // Update faction member last online
    hyperFactions.getFactionManager().updateLastOnline(uuid);

    // Reset chat channel
    hyperFactions.getChatManager().resetChannel(uuid);

    // Evict chat history cache if no online faction members remain
    com.hyperfactions.data.Faction dcFaction = hyperFactions.getFactionManager().getPlayerFaction(uuid);
    if (dcFaction != null && hyperFactions.getChatHistoryManager() != null) {
      boolean anyOnline = dcFaction.members().keySet().stream()
          .filter(id -> !id.equals(uuid))
          .anyMatch(id -> trackedPlayers.containsKey(id));
      if (!anyOnline) {
        hyperFactions.getChatHistoryManager().evictCache(dcFaction.id());
      }
    }

    // Clean up territory tracking
    hyperFactions.getTerritoryNotifier().onPlayerDisconnect(uuid);

    // Clear cached language preference
    HFMessages.clearLanguageOverride(uuid);

    // Unregister from active page tracker (GUI real-time updates)
    if (hyperFactions.getActivePageTracker() != null) {
      hyperFactions.getActivePageTracker().unregister(uuid);
    }

    // Untrack the player
    trackedPlayers.remove(uuid);
  }

  /**
   * Handles player chat event for faction/ally chat channels (async handler).
   */
  public CompletableFuture<PlayerChatEvent> onPlayerChatAsync(
      CompletableFuture<PlayerChatEvent> futureEvent) {
    return futureEvent.thenApply(event -> {
      try {
        if (event.isCancelled()) {
          return event;
        }

        PlayerRef sender = event.getSender();
        String message = event.getContent();

        // Check if player is in faction/ally chat mode
        boolean handled = hyperFactions.getChatManager().processChatMessage(sender, message);
        if (handled) {
          // Cancel the normal chat broadcast
          event.setCancelled(true);
        }
      } catch (Exception e) {
        ErrorHandler.report("Player chat: faction/ally chat processing", e);
      }
      return event;
    });
  }
}
