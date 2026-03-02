package com.hyperfactions.manager;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Caches aggregated faction K/D statistics for the leaderboard.
 * Refreshes periodically in the background to avoid per-request computation.
 */
public class FactionKDCache {

  private final FactionManager factionManager;

  private final PlayerStorage playerStorage;

  private final Map<UUID, FactionKDStats> cache = new ConcurrentHashMap<>();

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "HyperFactions-KDCache");
    t.setDaemon(true);
    return t;
  });

  /**
   * Aggregated K/D statistics for a single faction.
   */
  public record FactionKDStats(int totalKills, int totalDeaths, double kdr) {}

  /** Creates a new FactionKDCache. */
  public FactionKDCache(@NotNull FactionManager factionManager, @NotNull PlayerStorage playerStorage) {
    this.factionManager = factionManager;
    this.playerStorage = playerStorage;
  }

  /**
   * Starts periodic cache refresh.
   *
   * @param intervalSeconds seconds between refreshes
   */
  public void start(int intervalSeconds) {
    scheduler.scheduleAtFixedRate(this::refresh, 0, intervalSeconds, TimeUnit.SECONDS);
    Logger.debug("[KDCache] Started with %ds refresh interval", intervalSeconds);
  }

  /**
   * Shuts down the background scheduler.
   */
  public void shutdown() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Gets the cached K/D stats for a faction.
   *
   * @param factionId the faction UUID
   * @return cached stats, or zeros if not yet cached
   */
  @NotNull
  public FactionKDStats getFactionKD(@NotNull UUID factionId) {
    return cache.getOrDefault(factionId, new FactionKDStats(0, 0, 0.0));
  }

  /**
   * Refreshes all faction K/D stats by summing member kills/deaths.
   */
  private void refresh() {
    try {
      Map<UUID, FactionKDStats> newCache = new ConcurrentHashMap<>();

      for (Faction faction : factionManager.getAllFactions()) {
        int totalKills = 0;
        int totalDeaths = 0;

        for (UUID memberUuid : faction.members().keySet()) {
          try {
            Optional<PlayerData> dataOpt = playerStorage.loadPlayerData(memberUuid).join();
            if (dataOpt.isPresent()) {
              totalKills += dataOpt.get().getKills();
              totalDeaths += dataOpt.get().getDeaths();
            }
          } catch (Exception e) {
            // Skip individual player failures
          }
        }

        double kdr = totalDeaths > 0 ? (double) totalKills / totalDeaths : totalKills;
        newCache.put(faction.id(), new FactionKDStats(totalKills, totalDeaths, kdr));
      }

      // Atomically replace cache contents
      cache.clear();
      cache.putAll(newCache);

      Logger.debug("[KDCache] Refreshed K/D stats for %d factions", newCache.size());
    } catch (Exception e) {
      ErrorHandler.report("[KDCache] Failed to refresh", e);
    }
  }
}
