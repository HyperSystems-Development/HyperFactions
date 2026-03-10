package com.hyperfactions.territory;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.territory.TerritoryInfo.TerritoryType;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles territory entry/exit notifications for players.
 * Displays banner notifications when players move between territories.
 */
public class TerritoryNotifier {

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  private final RelationManager relationManager;

  private final PlayerStorage playerStorage;

  // Tracks the previous territory for each player
  private final Map<UUID, TerritoryInfo> previousTerritories = new ConcurrentHashMap<>();

  // Tracks the last chunk for each player (to detect chunk changes)
  private final Map<UUID, ChunkKey> lastChunks = new ConcurrentHashMap<>();

  // Players who have disabled territory alerts (opt-out set)
  private final Set<UUID> alertsDisabledPlayers = ConcurrentHashMap.newKeySet();

  /** Creates a new TerritoryNotifier. */
  public TerritoryNotifier(
      @NotNull FactionManager factionManager,
      @NotNull ClaimManager claimManager,
      @NotNull ZoneManager zoneManager,
      @NotNull RelationManager relationManager,
      @NotNull PlayerStorage playerStorage) {
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.zoneManager = zoneManager;
    this.relationManager = relationManager;
    this.playerStorage = playerStorage;
  }

  /**
   * Called when a player's position is updated.
   * Checks if they moved to a new chunk and handles territory notifications.
   *
   * @param playerRef the player reference
   * @param world     the world name
   * @param x         the player's X coordinate
   * @param z         the player's Z coordinate
   */
  public void onPlayerMove(@NotNull PlayerRef playerRef, @NotNull String world, double x, double z) {
    if (!ConfigManager.get().isTerritoryNotificationsEnabled()) {
      return;
    }

    UUID playerUuid = playerRef.getUuid();
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    ChunkKey currentChunk = new ChunkKey(world, chunkX, chunkZ);

    // Check if chunk changed
    ChunkKey lastChunk = lastChunks.get(playerUuid);
    if (currentChunk.equals(lastChunk)) {
      return; // Same chunk, no notification needed
    }

    // Update last chunk
    lastChunks.put(playerUuid, currentChunk);

    // Get territory info for new location
    TerritoryInfo currentTerritory = getTerritoryAt(world, chunkX, chunkZ, playerUuid);
    TerritoryInfo previousTerritory = previousTerritories.get(playerUuid);

    // Check if territory changed
    if (currentTerritory.isDifferentFrom(previousTerritory)) {
      // For wilderness entries, apply context-specific config based on what the player left
      TerritoryInfo notifyTerritory = currentTerritory;
      if (currentTerritory.type() == TerritoryType.WILDERNESS && previousTerritory != null) {
        notifyTerritory = buildWildernessFromConfig(previousTerritory);
      }

      // Update stored territory
      previousTerritories.put(playerUuid, currentTerritory);

      // Send notification
      sendTerritoryNotification(playerRef, notifyTerritory);
    }
  }

  /**
   * Builds a wilderness TerritoryInfo with config-driven text/enabled based on
   * what territory the player is leaving (zone vs faction claim).
   */
  @NotNull
  private TerritoryInfo buildWildernessFromConfig(@NotNull TerritoryInfo previousTerritory) {
    ConfigManager config = ConfigManager.get();
    boolean leftZone = previousTerritory.type() == TerritoryType.SAFEZONE
        || previousTerritory.type() == TerritoryType.WARZONE;

    if (leftZone) {
      return TerritoryInfo.wilderness(
          config.isWildernessOnLeaveZoneEnabled(),
          config.getWildernessOnLeaveZoneUpper(),
          config.getWildernessOnLeaveZoneLower()
      );
    } else {
      // Left a faction claim (or wilderness→wilderness, which won't reach here)
      return TerritoryInfo.wilderness(
          config.isWildernessOnLeaveClaimEnabled(),
          config.getWildernessOnLeaveClaimUpper(),
          config.getWildernessOnLeaveClaimLower()
      );
    }
  }

  /**
   * Sends a territory notification to a player using EventTitleUtil.
   * This displays a centered banner notification on screen.
   *
   * @param playerRef the player reference
   * @param territory the territory info
   */
  private void sendTerritoryNotification(@NotNull PlayerRef playerRef, @NotNull TerritoryInfo territory) {
    // Check player preference — respect opt-out
    if (alertsDisabledPlayers.contains(playerRef.getUuid())) {
      Logger.debugTerritory("Territory notification suppressed for %s: player disabled alerts",
          playerRef.getUsername());
      return;
    }

    if (!territory.isNotificationEnabled()) {
      Logger.debugTerritory("Notification suppressed for %s: %s",
          playerRef.getUsername(), territory.getPrimaryText());
      return;
    }

    try {
      // Build primary message (territory name)
      Message primaryMessage = Message.raw(territory.getPrimaryText())
          .color(territory.getDisplayColor());

      // Build secondary message (territory type description)
      String secondaryText = territory.getSecondaryText();
      Message secondaryMessage = secondaryText != null
          ? Message.raw(secondaryText).color("#AAAAAA")
          : Message.raw("");

      // Send event title notification (centered banner)
      // Parameters: playerRef, primaryTitle, secondaryTitle, isMajor, icon, duration, fadeIn, fadeOut
      EventTitleUtil.showEventTitleToPlayer(
          playerRef,
          primaryMessage,
          secondaryMessage,
          false,  // isMajor - false for territory notifications (less intrusive)
          null,   // No icon
          2.0f,   // Display duration (seconds)
          0.3f,   // Fade in duration (seconds)
          0.5f    // Fade out duration (seconds)
      );

      Logger.debugTerritory("Sent territory notification to %s: %s",
          playerRef.getUsername(), territory.getPrimaryText());

    } catch (Exception e) {
      // Fallback to chat message if notification fails
      Logger.warn("Failed to send territory notification, falling back to chat: %s", e.getMessage());
      sendChatFallback(playerRef, territory);
    }
  }

  /**
   * Sends a chat message as fallback if notification fails.
   *
   * @param playerRef the player reference
   * @param territory the territory info
   */
  private void sendChatFallback(@NotNull PlayerRef playerRef, @NotNull TerritoryInfo territory) {
    try {
      String secondaryText = territory.getSecondaryText();
      Message message = Message.raw("~ " + territory.getPrimaryText())
          .color(territory.getDisplayColor());

      if (secondaryText != null) {
        message = message.join(Message.raw(" - " + secondaryText).color("#AAAAAA"));
      }

      playerRef.sendMessage(message);
    } catch (Exception e) {
      Logger.warn("Failed to send territory chat fallback: %s", e.getMessage());
    }
  }

  /**
   * Gets territory information at a specific chunk location.
   *
   * @param world      the world name
   * @param chunkX     the chunk X
   * @param chunkZ     the chunk Z
   * @param playerUuid the player's UUID (for relation calculation)
   * @return the territory info
   */
  @NotNull
  public TerritoryInfo getTerritoryAt(@NotNull String world, int chunkX, int chunkZ, @NotNull UUID playerUuid) {
    // Check for zone first (SafeZone/WarZone take priority)
    Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
    if (zone != null) {
      return zone.isSafeZone()
          ? TerritoryInfo.safeZone(zone)
          : TerritoryInfo.warZone(zone);
    }

    // Check for faction claim
    UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);
    if (claimOwner != null) {
      Faction faction = factionManager.getFaction(claimOwner);
      if (faction != null) {
        RelationType relation = getPlayerRelation(playerUuid, claimOwner);
        return TerritoryInfo.factionClaim(claimOwner, faction.name(), faction.tag(), relation);
      }
    }

    // Default to wilderness
    return TerritoryInfo.wilderness();
  }

  /**
   * Gets the player's relation to a faction.
   *
   * @param playerUuid the player's UUID
   * @param factionId  the faction ID
   * @return the relation type
   */
  @NotNull
  private RelationType getPlayerRelation(@NotNull UUID playerUuid, @NotNull UUID factionId) {
    UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);

    // Player is in this faction (their own territory)
    if (factionId.equals(playerFactionId)) {
      return RelationType.OWN;
    }

    // Player has no faction (treat as neutral)
    if (playerFactionId == null) {
      return RelationType.NEUTRAL;
    }

    // Get actual relation between factions
    return relationManager.getRelation(playerFactionId, factionId);
  }

  /**
   * Called when a player connects.
   * Initializes their territory tracking.
   *
   * @param playerRef the player reference
   * @param world     the world name
   * @param x         the spawn X coordinate
   * @param z         the spawn Z coordinate
   */
  public void onPlayerConnect(@NotNull PlayerRef playerRef, @NotNull String world, double x, double z) {
    if (!ConfigManager.get().isTerritoryNotificationsEnabled()) {
      return;
    }

    UUID playerUuid = playerRef.getUuid();

    // Load territory alert preference
    playerStorage.loadPlayerData(playerUuid).thenAccept(opt -> {
      opt.ifPresent(data -> {
        if (!data.isTerritoryAlertsEnabled()) {
          alertsDisabledPlayers.add(playerUuid);
        }
      });
    });

    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Set initial chunk and territory
    ChunkKey currentChunk = new ChunkKey(world, chunkX, chunkZ);
    lastChunks.put(playerUuid, currentChunk);

    TerritoryInfo territory = getTerritoryAt(world, chunkX, chunkZ, playerUuid);
    previousTerritories.put(playerUuid, territory);

    // Send initial notification
    sendTerritoryNotification(playerRef, territory);
  }

  /**
   * Called when a player disconnects.
   * Cleans up their territory tracking data.
   *
   * @param playerUuid the player's UUID
   */
  public void onPlayerDisconnect(@NotNull UUID playerUuid) {
    previousTerritories.remove(playerUuid);
    lastChunks.remove(playerUuid);
    alertsDisabledPlayers.remove(playerUuid);
  }

  /**
   * Gets the last known territory for a player.
   *
   * @param playerUuid the player's UUID
   * @return the last known territory, or null if not tracked
   */
  @Nullable
  public TerritoryInfo getLastTerritory(@NotNull UUID playerUuid) {
    return previousTerritories.get(playerUuid);
  }

  /**
   * Gets the last known chunk for a player.
   *
   * @param playerUuid the player's UUID
   * @return the last known chunk, or null if not tracked
   */
  @Nullable
  public ChunkKey getLastChunk(@NotNull UUID playerUuid) {
    return lastChunks.get(playerUuid);
  }

  /**
   * Updates the cached territory alerts preference for a player.
   * Called from PlayerSettingsPage when the preference is toggled.
   *
   * @param playerUuid the player's UUID
   * @param enabled    whether territory alerts are enabled
   */
  public void setTerritoryAlertsEnabled(@NotNull UUID playerUuid, boolean enabled) {
    if (enabled) {
      alertsDisabledPlayers.remove(playerUuid);
    } else {
      alertsDisabledPlayers.add(playerUuid);
    }
  }

  /**
   * Clears all tracking data.
   * Called on plugin shutdown.
   */
  public void shutdown() {
    previousTerritories.clear();
    lastChunks.clear();
    alertsDisabledPlayers.clear();
  }
}
