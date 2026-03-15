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
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Service that controls which players are visible on the world map and compass
 * based on faction relations. Uses two complementary mechanisms:
 *
 * <ol>
 *   <li>{@code WorldMapTracker.setPlayerMapFilter()} — filters the native
 *       {@code OtherPlayersMarkerProvider} (vanilla player icons). Note: this API
 *       is {@code @Deprecated} in recent Hytale builds.</li>
 *   <li>{@code HiddenPlayersManager.hidePlayer()/showPlayer()} — the native Hytale
 *       per-player visibility system. Checked by BetterMap's {@code PlayerRadarProvider}
 *       and future-proof against the deprecated filter removal.</li>
 * </ol>
 *
 * <p>Both mechanisms are applied together so faction-based visibility works with
 * vanilla player icons AND third-party map mods (e.g., BetterMap's radar).
 *
 * <p><h3>HiddenPlayersManager Ownership</h3>
 * Other systems (admin commands, other mods) may also use {@code HiddenPlayersManager}.
 * To avoid accidentally unhiding players that were hidden by other systems, this service
 * tracks which viewer→target pairs it has hidden in {@link #factionHiddenPairs}. Only
 * pairs in this set are eligible for {@code showPlayer()} when faction rules change.
 *
 * <p><h3>Filter Semantics</h3>
 * The Hytale {@code OtherPlayersMarkerProvider} evaluates the predicate per-target:
 * <ul>
 *   <li>{@code predicate.test(target) == true} → player marker is <b>skipped</b> (hidden)</li>
 *   <li>{@code predicate.test(target) == false} → player marker is <b>sent</b> (visible)</li>
 *   <li>{@code predicate == null} → no filtering, all players visible (vanilla)</li>
 * </ul>
 *
 * <p><h3>Thread Safety</h3>
 * <ul>
 *   <li>{@link #applyFilter(Player)} must be called on the player's world thread.</li>
 *   <li>{@link #applyToAll()}, {@link #updateForFaction(Faction)},
 *       {@link #updateForAllPlayers()}, and {@link #resetAll()} dispatch via
 *       {@code world.execute()} — safe to call from any thread.</li>
 *   <li>Config values are snapshotted at predicate creation time to avoid tearing.</li>
 *   <li>{@link #factionHiddenPairs} uses ConcurrentHashMap for thread-safe access.</li>
 * </ul>
 */
public class MapPlayerFilterService {

  private final FactionManager factionManager;

  private final RelationManager relationManager;

  /** Per-player admin bypass check — returns true if the player has admin bypass toggled on. */
  private final Predicate<UUID> adminBypassCheck;

  /**
   * Tracks which viewer→target pairs we have hidden via HiddenPlayersManager.
   * Key: viewer UUID, Value: set of target UUIDs we hid for this viewer.
   * Only pairs in this map are eligible for showPlayer() when rules change,
   * preventing us from unhiding players hidden by other systems.
   */
  private final ConcurrentHashMap<UUID, Set<UUID>> factionHiddenPairs = new ConcurrentHashMap<>();

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
   * <p>Sets both the deprecated {@code setPlayerMapFilter} predicate (for vanilla icons)
   * and updates {@code HiddenPlayersManager} entries (for BetterMap radar compatibility).
   *
   * <p><b>Must be called on the player's world thread.</b>
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
        clearHiddenPlayers(viewerRef);
        return;
      }

      UUID viewerUuid = viewerRef.getUuid();

      // Admin bypass: viewer sees all players (requires permission AND toggle on)
      boolean hasPermBypass = PermissionManager.get().hasPermission(viewerUuid, Permissions.BYPASS_MAP_FILTER);
      boolean toggleOn = adminBypassCheck.test(viewerUuid);
      if (hasPermBypass && toggleOn) {
        tracker.setPlayerMapFilter(null);
        clearHiddenPlayers(viewerRef);
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

      // Set the filter predicate (for vanilla OtherPlayersMarkerProvider).
      // IMPORTANT: true = HIDE (skip marker), false = SHOW (send marker).
      tracker.setPlayerMapFilter(targetRef -> {
        if (targetRef == null) {
          return true;
        }

        UUID targetUuid = targetRef.getUuid();
        if (viewerUuid.equals(targetUuid)) {
          return false;
        }

        // Admin bypass: target is always visible when they have permission AND toggle on
        if (PermissionManager.get().hasPermission(targetUuid, Permissions.BYPASS_MAP_VISIBILITY)
            && adminBypassCheck.test(targetUuid)) {
          return false;
        }

        return shouldHideTarget(viewerUuid, viewerFactionId, targetUuid,
            cfgShowOwn, cfgShowAllies, cfgShowNeutrals, cfgShowEnemies,
            cfgShowFactionless, cfgShowFactionlessToFactionless);
      });

      // Update HiddenPlayersManager for BetterMap radar compatibility.
      // This ensures faction visibility rules are respected by any system that
      // checks HiddenPlayersManager (BetterMap's PlayerRadarProvider, future Hytale APIs).
      updateHiddenPlayers(player, viewerRef, viewerUuid, viewerFactionId,
          cfgShowOwn, cfgShowAllies, cfgShowNeutrals, cfgShowEnemies,
          cfgShowFactionless, cfgShowFactionlessToFactionless);

    } catch (Exception e) {
      ErrorHandler.report("[MapFilter] Failed to apply map player filter", e);
    }
  }

  /**
   * Determines whether a target should be hidden from a viewer based on faction rules.
   *
   * @return true if the target should be HIDDEN
   */
  private boolean shouldHideTarget(UUID viewerUuid, UUID viewerFactionId, UUID targetUuid,
                                    boolean showOwn, boolean showAllies, boolean showNeutrals,
                                    boolean showEnemies, boolean showFactionless,
                                    boolean showFactionlessToFactionless) {
    UUID targetFactionId = factionManager.getPlayerFactionId(targetUuid);

    // Both factionless
    if (viewerFactionId == null && targetFactionId == null) {
      return !showFactionlessToFactionless;
    // Viewer is factionless, target is in a faction → hide
    } else if (viewerFactionId == null) {
      return true;
    // Target is factionless
    } else if (targetFactionId == null) {
      return !showFactionless;
    // Same faction
    } else if (viewerFactionId.equals(targetFactionId)) {
      return !showOwn;
    // Check relation
    } else {
      RelationType relation = relationManager.getEffectiveRelation(viewerFactionId, targetFactionId);
      return switch (relation) {
        case ALLY, OWN -> !showAllies;
        case ENEMY -> !showEnemies;
        case NEUTRAL -> !showNeutrals;
      };
    }
  }

  /**
   * Updates HiddenPlayersManager entries for a viewer based on faction visibility rules.
   * Tracks which pairs we hide so we don't accidentally unhide players hidden by other systems.
   *
   * <p>Must be called on the player's world thread.
   */
  private void updateHiddenPlayers(Player player, PlayerRef viewerRef, UUID viewerUuid,
                                    UUID viewerFactionId,
                                    boolean showOwn, boolean showAllies, boolean showNeutrals,
                                    boolean showEnemies, boolean showFactionless,
                                    boolean showFactionlessToFactionless) {
    try {
      World world = player.getWorld();
      if (world == null) {
        return;
      }

      HiddenPlayersManager hiddenManager = viewerRef.getHiddenPlayersManager();
      Set<UUID> previouslyHidden = factionHiddenPairs.getOrDefault(viewerUuid, Set.of());
      Set<UUID> nowHidden = ConcurrentHashMap.newKeySet();

      for (PlayerRef targetRef : world.getPlayerRefs()) {
        UUID targetUuid = targetRef.getUuid();
        if (targetUuid.equals(viewerUuid)) {
          continue;
        }

        // Admin bypass: target is always visible
        if (PermissionManager.get().hasPermission(targetUuid, Permissions.BYPASS_MAP_VISIBILITY)
            && adminBypassCheck.test(targetUuid)) {
          // If we previously hid this target, unhide them
          if (previouslyHidden.contains(targetUuid)) {
            hiddenManager.showPlayer(targetUuid);
          }
          continue;
        }

        boolean shouldHide = shouldHideTarget(viewerUuid, viewerFactionId, targetUuid,
            showOwn, showAllies, showNeutrals, showEnemies,
            showFactionless, showFactionlessToFactionless);

        if (shouldHide) {
          hiddenManager.hidePlayer(targetUuid);
          nowHidden.add(targetUuid);
        } else if (previouslyHidden.contains(targetUuid)) {
          // Only unhide if WE were the ones who hid them
          hiddenManager.showPlayer(targetUuid);
        }
      }

      // Update tracking
      if (nowHidden.isEmpty()) {
        factionHiddenPairs.remove(viewerUuid);
      } else {
        factionHiddenPairs.put(viewerUuid, nowHidden);
      }
    } catch (Exception e) {
      ErrorHandler.report("[MapFilter] Failed to update HiddenPlayersManager for viewer", e);
    }
  }

  /**
   * Clears all faction-based hidden player entries for a viewer.
   * Only unhides players that WE previously hid.
   */
  private void clearHiddenPlayers(PlayerRef viewerRef) {
    try {
      UUID viewerUuid = viewerRef.getUuid();
      Set<UUID> previouslyHidden = factionHiddenPairs.remove(viewerUuid);
      if (previouslyHidden == null || previouslyHidden.isEmpty()) {
        return;
      }

      HiddenPlayersManager hiddenManager = viewerRef.getHiddenPlayersManager();
      for (UUID targetUuid : previouslyHidden) {
        hiddenManager.showPlayer(targetUuid);
      }
    } catch (Exception e) {
      ErrorHandler.report("[MapFilter] Failed to clear hidden players for viewer", e);
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
              ErrorHandler.report("[MapFilter] Error applying filters in world " + world.getName(), e);
            }
          });
        } catch (Exception e) {
          // World thread not accepting tasks (e.g., dungeon instances shutting down) — safe to skip
          Logger.debugWorldMap("[MapFilter] Skipping world '%s': %s", world.getName(), e.getMessage());
        }
      }
    } catch (Exception e) {
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
   * Clears all filters and hidden player entries (shows all players).
   * Used when the feature is disabled.
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
                PlayerRef ref = player.getPlayerRef();
                WorldMapTracker tracker = player.getWorldMapTracker();
                if (tracker != null) {
                  tracker.setPlayerMapFilter(null);
                }
                if (ref != null) {
                  clearHiddenPlayers(ref);
                }
              }
              Logger.debugWorldMap("[MapFilter] resetAll: cleared filters for %d players in %s",
                  players.size(), world.getName());
            } catch (Exception e) {
              ErrorHandler.report("[MapFilter] Error resetting filters in world " + world.getName(), e);
            }
          });
        } catch (Exception e) {
          // World thread not accepting tasks (e.g., dungeon instances shutting down) — safe to skip
          Logger.debugWorldMap("[MapFilter] Skipping world '%s' during reset: %s", world.getName(), e.getMessage());
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[MapFilter] Error resetting filters across all worlds", e);
    }

    // Clear all tracking data
    factionHiddenPairs.clear();
  }
}
