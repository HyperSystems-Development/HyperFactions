package com.hyperfactions.worldmap;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.WorldMapConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Service that controls which players are visible on the world map and compass
 * based on faction relations. Uses the Hytale {@code playerMapFilter} predicate
 * on {@link WorldMapTracker} to filter player markers per-viewer.
 *
 * <p>
 * When disabled (default), all players are visible (vanilla behavior).
 * When enabled, visibility is determined by faction membership and relation type.
 *
 * <p><h3>Filter Semantics</h3>
 * The Hytale {@code PlayerIconMarkerProvider} evaluates the predicate per-target:
 * <ul>
 *   <li>{@code predicate.test(target) == true} → player marker is <b>skipped</b> (hidden)</li>
 *   <li>{@code predicate.test(target) == false} → player marker is <b>sent</b> (visible)</li>
 *   <li>{@code predicate == null} → no filtering, all players visible (vanilla)</li>
 * </ul>
 * This is confirmed by the decompiled server code in {@code PlayerIconMarkerProvider}:
 * {@code if (playerMapFilter != null && playerMapFilter.test(otherPlayer)) continue;}
 * where {@code continue} skips sending the marker.
 *
 * <p><h3>Thread Safety</h3>
 * <ul>
 *   <li>{@link #applyFilter(Player)} must be called on the player's world thread.</li>
 *   <li>{@link #applyToAll()}, {@link #updateForFaction(Faction)},
 *       {@link #updateForAllPlayers()}, and {@link #resetAll()} dispatch via
 *       {@code world.execute()} — safe to call from any thread.</li>
 *   <li>Config values are snapshotted at predicate creation time to avoid tearing.</li>
 * </ul>
 */
public class MapPlayerFilterService {

  private final FactionManager factionManager;

  private final RelationManager relationManager;

  /** Per-player admin bypass check — returns true if the player has admin bypass toggled on. */
  private final Predicate<UUID> adminBypassCheck;

  /** Creates a new MapPlayerFilterService. */
  public MapPlayerFilterService(@NotNull FactionManager factionManager,
                 @NotNull RelationManager relationManager,
                 @NotNull Predicate<UUID> adminBypassCheck) {
    this.factionManager = factionManager;
    this.relationManager = relationManager;
    this.adminBypassCheck = adminBypassCheck;
  }

  /**
   * Applies the player map filter to a single player.
   *
   * <p>
   * <b>Must be called on the player's world thread.</b>
   * For cross-thread usage, use {@link #applyToAll()} which dispatches
   * via {@code world.execute()}.
   *
   * @param player the player to apply the filter to
   */
  public void applyFilter(@NotNull Player player) {
    try {
      PlayerRef viewerRef = player.getPlayerRef();
      if (viewerRef == null) {
        return;
      }

      WorldMapTracker tracker = player.getWorldMapTracker();
      if (tracker == null) {
        return;
      }

      WorldMapConfig config = ConfigManager.get().worldMap();

      // Feature disabled → clear filter (vanilla: show all)
      if (!config.isPlayerVisibilityEnabled()) {
        tracker.setPlayerMapFilter(null);
        return;
      }

      UUID viewerUuid = viewerRef.getUuid();

      // Admin bypass: viewer sees all players (requires permission AND toggle on)
      boolean hasPermBypass = PermissionManager.get().hasPermission(viewerUuid, Permissions.BYPASS_MAP_FILTER);
      boolean toggleOn = adminBypassCheck.test(viewerUuid);
      if (hasPermBypass && toggleOn) {
        tracker.setPlayerMapFilter(null);
        return;
      }

      // Snapshot viewer state and config at predicate creation time.
      UUID viewerFactionId = factionManager.getPlayerFactionId(viewerUuid);
      boolean cfgShowOwn = config.isShowOwnFaction();
      boolean cfgShowAllies = config.isShowAllies();
      boolean cfgShowNeutrals = config.isShowNeutrals();
      boolean cfgShowEnemies = config.isShowEnemies();
      boolean cfgShowFactionless = config.isShowFactionlessPlayers();
      boolean cfgShowFactionlessToFactionless = config.isShowFactionlessToFactionless();

      // Set the filter predicate.
      // IMPORTANT: true = HIDE (skip marker), false = SHOW (send marker).
      // This matches the decompiled OtherPlayersMarkerProvider logic:
      //   if (playerMapFilter.test(otherPlayer)) continue; // continue = skip = hide
      tracker.setPlayerMapFilter(targetRef -> {
        if (targetRef == null) { // null → hide
          return true;
        }

        UUID targetUuid = targetRef.getUuid();
        if (viewerUuid.equals(targetUuid)) { // Always see yourself → show
          return false;
        }

        // Admin bypass: target is always visible when they have permission AND toggle on
        if (PermissionManager.get().hasPermission(targetUuid, Permissions.BYPASS_MAP_VISIBILITY)
            && adminBypassCheck.test(targetUuid)) {
          return false; // show
        }

        UUID targetFactionId = factionManager.getPlayerFactionId(targetUuid);

        // Both factionless
        if (viewerFactionId == null && targetFactionId == null) {
          return !cfgShowFactionlessToFactionless;
        // Viewer is factionless, target is in a faction → hide
        } else if (viewerFactionId == null) {
          return true;
        // Target is factionless
        } else if (targetFactionId == null) {
          return !cfgShowFactionless;
        // Same faction
        } else if (viewerFactionId.equals(targetFactionId)) {
          return !cfgShowOwn;
        // Check relation
        } else {
          RelationType relation = relationManager.getEffectiveRelation(viewerFactionId, targetFactionId);
          return switch (relation) {
            case ALLY, OWN -> !cfgShowAllies;
            case ENEMY -> !cfgShowEnemies;
            case NEUTRAL -> !cfgShowNeutrals;
          };
        }
      });
    } catch (Exception e) {
      Logger.warn("Failed to apply map player filter: %s", e.getMessage());
    }
  }

  /**
   * Applies filters to all online players across all worlds.
   * Dispatches to each world's thread via {@code world.execute()}.
   * Safe to call from any thread.
   */
  public void applyToAll() {
    WorldMapConfig config = ConfigManager.get().worldMap();
    if (!config.isPlayerVisibilityEnabled()) {
      Logger.debugWorldMap("[MapFilter] applyToAll: feature disabled, resetting all");
      resetAll();
      return;
    }

    Logger.debugWorldMap("[MapFilter] applyToAll: applying filters to all worlds");
    try {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        try {
          world.execute(() -> {
            try {
              @SuppressWarnings("unchecked")
              List<Player> players = world.getPlayers();
              Logger.debugWorldMap("[MapFilter] applyToAll: world=%s players=%d",
                  world.getName(), players.size());
              for (Player player : players) {
                applyFilter(player);
              }
            } catch (Exception e) {
              Logger.warn("Error applying map filters in world %s: %s",
                  world.getName(), e.getMessage());
              ErrorHandler.report("[MapFilter] Error applying filters in world " + world.getName(), e);
            }
          });
        } catch (Exception e) {
          // World thread not accepting tasks (e.g., dungeon instances shutting down) — safe to skip
          Logger.debugWorldMap("[MapFilter] Skipping world '%s': %s", world.getName(), e.getMessage());
        }
      }
    } catch (Exception e) {
      Logger.warn("Error applying map filters to all worlds: %s", e.getMessage());
      ErrorHandler.report("[MapFilter] Error applying filters to all worlds", e);
    }
  }

  /**
   * Updates filters after a faction membership change (join/leave/kick).
   * Safe to call from any thread.
   *
   * @param faction the faction whose membership changed
   */
  public void updateForFaction(@NotNull Faction faction) {
    if (!ConfigManager.get().worldMap().isPlayerVisibilityEnabled()) {
      return;
    }
    Logger.debugWorldMap("[MapFilter] updateForFaction: %s", faction.name());
    applyToAll();
  }

  /**
   * Updates all online players' filters.
   * Called when relations change or a faction is disbanded.
   * Safe to call from any thread.
   */
  public void updateForAllPlayers() {
    Logger.debugWorldMap("[MapFilter] updateForAllPlayers");
    applyToAll();
  }

  /**
   * Clears all filters (shows all players). Used when the feature is disabled.
   * Safe to call from any thread.
   */
  public void resetAll() {
    Logger.debugWorldMap("[MapFilter] resetAll: clearing all filters");
    try {
      for (World world : Universe.get().getWorlds().values()) {
        if (world == null) {
          continue;
        }
        try {
          world.execute(() -> {
            try {
              @SuppressWarnings("unchecked")
              List<Player> players = world.getPlayers();
              for (Player player : players) {
                WorldMapTracker tracker = player.getWorldMapTracker();
                if (tracker != null) {
                  tracker.setPlayerMapFilter(null);
                }
              }
              Logger.debugWorldMap("[MapFilter] resetAll: cleared filters for %d players in %s",
                  players.size(), world.getName());
            } catch (Exception e) {
              Logger.warn("Error resetting map filters in world: %s", e.getMessage());
              ErrorHandler.report("[MapFilter] Error resetting filters in world " + world.getName(), e);
            }
          });
        } catch (Exception e) {
          // World thread not accepting tasks (e.g., dungeon instances shutting down) — safe to skip
          Logger.debugWorldMap("[MapFilter] Skipping world '%s' during reset: %s", world.getName(), e.getMessage());
        }
      }
    } catch (Exception e) {
      Logger.warn("Error resetting map filters: %s", e.getMessage());
      ErrorHandler.report("[MapFilter] Error resetting filters across all worlds", e);
    }
  }
}
