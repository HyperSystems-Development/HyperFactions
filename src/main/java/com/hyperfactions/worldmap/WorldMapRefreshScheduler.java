package com.hyperfactions.worldmap;

import com.hyperfactions.config.modules.WorldMapConfig.RefreshMode;
import com.hyperfactions.config.modules.WorldMapConfig;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages world map refresh scheduling with multiple modes for performance optimization.
 *
 * <p>Performance characteristics:
 *
 * <p>PROXIMITY (default, most performant) - Selective server cache clear + notify only nearby players.
 * Best for busy servers. Server regenerates only changed chunks; far players unaffected.
 *
 * <p>INCREMENTAL - Selective server cache clear + notify all players.
 * Good balance. Server regenerates only changed chunks; all players re-fetch from cache.
 *
 * <p>DEBOUNCED - Full refresh after quiet period with no changes.
 * Best for servers with constant claim activity. Batches many changes into one refresh.
 *
 * <p>IMMEDIATE - Full refresh on every change (original behavior).
 * For backwards compatibility. Most expensive - clears all caches on every claim.
 *
 * <p>MANUAL - No automatic refresh. Use /f admin map refresh to update manually.
 *
 * <p>Server-side cache uses WorldMapManager.clearImagesInChunks() for selective invalidation
 * (PROXIMITY/INCREMENTAL) or WorldMapManager.clearImages() for full clear (DEBOUNCED/IMMEDIATE).
 */
public class WorldMapRefreshScheduler {

  private final WorldMapConfig config;

  private final WorldMapService worldMapService;

  // Pending chunk updates per world: world name -> set of chunk keys (packed long)
  private final Map<String, Set<Long>> pendingChunks = new ConcurrentHashMap<>();

  // Debounce state
  private volatile long lastChangeTime = 0;

  private volatile ScheduledFuture<?> debounceTask = null;

  // Scheduler for batch processing
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
    Thread t = new Thread(r, "HyperFactions-WorldMapRefresh");
    t.setDaemon(true);
    return t;
  });

  // Batch processing task
  private ScheduledFuture<?> batchTask = null;

  // Statistics
  private final AtomicInteger totalRefreshes = new AtomicInteger(0);

  private final AtomicInteger chunksProcessed = new AtomicInteger(0);

  private final AtomicInteger playersNotified = new AtomicInteger(0);

  private volatile Instant lastRefreshTime = null;

  /**
   * Creates a new refresh scheduler.
   *
   * @param config the world map configuration
   * @param worldMapService the world map service
   */
  public WorldMapRefreshScheduler(@NotNull WorldMapConfig config, @NotNull WorldMapService worldMapService) {
    this.config = config;
    this.worldMapService = worldMapService;
  }

  /**
   * Starts the batch processing scheduler based on current mode.
   */
  public void start() {
    RefreshMode mode = getEffectiveMode();
    Logger.info("[WorldMap] Starting refresh scheduler in %s mode", mode.getConfigName());

    if (mode == RefreshMode.PROXIMITY || mode == RefreshMode.INCREMENTAL) {
      int intervalTicks = mode == RefreshMode.PROXIMITY
          ? config.getProximityBatchIntervalTicks()
          : config.getIncrementalBatchIntervalTicks();

      // Convert ticks to milliseconds (Hytale uses 30 TPS, so 1 tick = ~33.33ms)
      long intervalMs = (intervalTicks * 1000L) / 30;

      batchTask = scheduler.scheduleAtFixedRate(
          ErrorHandler.wrapTask("World map batch processing", this::processBatch),
          intervalMs,
          intervalMs,
          TimeUnit.MILLISECONDS
      );

      Logger.debugWorldMap("Batch processing started: interval=%dms", intervalMs);
    }
  }

  /**
   * Stops the scheduler and cleans up resources.
   */
  public void shutdown() {
    if (batchTask != null) {
      batchTask.cancel(false);
      batchTask = null;
    }
    if (debounceTask != null) {
      debounceTask.cancel(false);
      debounceTask = null;
    }
    scheduler.shutdown();
    pendingChunks.clear();
    Logger.debugWorldMap("Refresh scheduler shut down");
  }

  /**
   * Queues a chunk for refresh. Called when a claim changes.
   *
   * @param worldName the world name
   * @param chunkX chunk X coordinate
   * @param chunkZ chunk Z coordinate
   */
  public void queueChunkRefresh(@NotNull String worldName, int chunkX, int chunkZ) {
    if (!config.isEnabled()) {
      return;
    }

    RefreshMode mode = getEffectiveMode();

    switch (mode) {
      case PROXIMITY, INCREMENTAL -> {
        // Queue for batch processing
        long chunkKey = ChunkUtil.packChunkKey(chunkX, chunkZ);
        pendingChunks.computeIfAbsent(worldName, k -> ConcurrentHashMap.newKeySet()).add(chunkKey);
        Logger.debugWorldMap("Queued chunk refresh: world=%s, chunk=(%d,%d), queue size=%d",
            worldName, chunkX, chunkZ, getPendingChunkCount());
      }
      case DEBOUNCED -> {
        // Update last change time and schedule/reschedule debounce
        lastChangeTime = System.currentTimeMillis();
        scheduleDebounceRefresh();
      }
      case IMMEDIATE -> {
        // Full refresh immediately (original behavior)
        World world = Universe.get().getWorld(worldName);
        if (world != null) {
          worldMapService.refreshWorldMap(world);
          totalRefreshes.incrementAndGet();
          lastRefreshTime = Instant.now();
        }
      }
      case MANUAL -> {
        // Do nothing - manual refresh only
        Logger.debugWorldMap("Manual mode: skipping automatic refresh for chunk (%d,%d)", chunkX, chunkZ);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /**
   * Queues a faction-wide refresh (for rename, tag, color, zone changes).
   * Respects the configured refresh mode and threshold settings.
   *
   * <p>If chunks is null or exceeds threshold, falls back to full refresh.
   * Otherwise, queues each chunk individually for optimized refresh.
   *
   * @param chunks the chunks to refresh, or null for full refresh
   */
  public void queueFactionWideRefresh(@Nullable Set<ChunkKey> chunks) {
    if (!config.isEnabled()) {
      return;
    }

    RefreshMode mode = getEffectiveMode();
    int threshold = config.getFactionWideRefreshThreshold();

    // Check if we should use full refresh (null chunks, exceeds threshold, or IMMEDIATE mode)
    boolean useFullRefresh = chunks == null
        || chunks.isEmpty()
        || chunks.size() > threshold
        || mode == RefreshMode.IMMEDIATE;

    if (useFullRefresh) {
      // Full refresh path
      switch (mode) {
        case PROXIMITY, INCREMENTAL, IMMEDIATE -> {
          Logger.debugWorldMap("Faction-wide change: full refresh (chunks=%s, threshold=%d, mode=%s)",
              chunks == null ? "null" : chunks.size(), threshold, mode.getConfigName());
          forceFullRefresh();
        }
        case DEBOUNCED -> {
          Logger.debugWorldMap("Faction-wide change: triggering debounce timer");
          lastChangeTime = System.currentTimeMillis();
          scheduleDebounceRefresh();
        }
        case MANUAL -> {
          Logger.debugWorldMap("Manual mode: skipping automatic refresh for faction-wide change");
        }
        default -> throw new IllegalStateException("Unexpected value");
      }
    } else {
      // Queue each chunk individually for optimized refresh
      switch (mode) {
        case PROXIMITY, INCREMENTAL -> {
          Logger.debugWorldMap("Faction-wide change: queuing %d chunks (threshold=%d, mode=%s)",
              chunks.size(), threshold, mode.getConfigName());
          for (ChunkKey chunk : chunks) {
            queueChunkRefresh(chunk.world(), chunk.chunkX(), chunk.chunkZ());
          }
        }
        case DEBOUNCED -> {
          Logger.debugWorldMap("Faction-wide change: triggering debounce timer");
          lastChangeTime = System.currentTimeMillis();
          scheduleDebounceRefresh();
        }
        case MANUAL -> {
          Logger.debugWorldMap("Manual mode: skipping automatic refresh for faction-wide change");
        }
        default -> throw new IllegalStateException("Unexpected value");
      }
    }
  }

  /**
   * Forces an immediate full refresh of all world maps.
   * Used by /f admin map refresh command.
   */
  public void forceFullRefresh() {
    Logger.info("[WorldMap] Forcing full refresh of all world maps");

    // Clear any pending chunks since we're doing a full refresh
    pendingChunks.clear();

    // Refresh all registered worlds
    worldMapService.refreshAllWorldMaps();

    totalRefreshes.incrementAndGet();
    lastRefreshTime = Instant.now();
  }

  /**
   * Processes pending chunks in a batch.
   */
  private void processBatch() {
    if (pendingChunks.isEmpty()) {
      return;
    }

    RefreshMode mode = getEffectiveMode();
    int maxChunks = mode == RefreshMode.PROXIMITY
        ? config.getProximityMaxChunksPerBatch()
        : config.getIncrementalMaxChunksPerBatch();

    int processedCount = 0;
    int notifiedCount = 0;

    // Process each world's pending chunks
    for (Map.Entry<String, Set<Long>> entry : pendingChunks.entrySet()) {
      String worldName = entry.getKey();
      Set<Long> chunks = entry.getValue();

      if (chunks.isEmpty()) {
        continue;
      }

      World world = Universe.get().getWorld(worldName);
      if (world == null) {
        chunks.clear();
        continue;
      }

      // Take up to maxChunks from the set
      List<Long> toProcess = new ArrayList<>();
      Iterator<Long> iter = chunks.iterator();
      while (iter.hasNext() && toProcess.size() < maxChunks) {
        toProcess.add(iter.next());
        iter.remove();
      }

      if (toProcess.isEmpty()) {
        continue;
      }

      // Clear these chunks from the server-side cache
      WorldMapManager worldMapManager = world.getWorldMapManager();
      int cleared = clearSpecificImages(worldMapManager, toProcess);

      // Notify players based on mode
      if (mode == RefreshMode.PROXIMITY) {
        notifiedCount += notifyProximityPlayers(world, toProcess);
      } else {
        notifiedCount += notifyAllPlayers(world, toProcess);
      }

      processedCount += toProcess.size();
      Logger.debugWorldMap("Processed batch: world=%s, chunks=%d, cleared=%d, notified=%d",
          worldName, toProcess.size(), cleared, notifiedCount);
    }

    if (processedCount > 0) {
      chunksProcessed.addAndGet(processedCount);
      playersNotified.addAndGet(notifiedCount);
      totalRefreshes.incrementAndGet();
      lastRefreshTime = Instant.now();
    }
  }

  /**
   * Clears specific images from the WorldMapManager cache.
   * Uses the public clearImagesInChunks API for selective invalidation.
   *
   * @param manager the world map manager
   * @param chunkKeys the chunk keys to clear
   * @return number of chunks cleared
   */
  private int clearSpecificImages(@NotNull WorldMapManager manager, @NotNull List<Long> chunkKeys) {
    // Use the public API for selective chunk clearing
    LongOpenHashSet indices = new LongOpenHashSet(chunkKeys);
    manager.clearImagesInChunks(indices);
    return chunkKeys.size();
  }

  /**
   * Notifies players within proximity of changed chunks.
   *
   * @param world the world
   * @param chunkKeys the changed chunk keys
   * @return number of players notified
   */
  private int notifyProximityPlayers(@NotNull World world, @NotNull List<Long> chunkKeys) {
    int radius = config.getProximityChunkRadius();
    int notified = 0;

    for (Player player : world.getPlayers()) {
      // Get player position via PlayerRef.getTransform()
      var playerRef = player.getPlayerRef();
      if (playerRef == null || playerRef.getTransform() == null) {
        continue;
      }

      Vector3d pos = playerRef.getTransform().getPosition();
      int playerChunkX = ChunkUtil.toChunkCoord(pos.getX());
      int playerChunkZ = ChunkUtil.toChunkCoord(pos.getZ());

      // Check if any changed chunk is within radius of this player
      boolean inRange = false;
      for (Long chunkKey : chunkKeys) {
        int chunkX = ChunkUtil.unpackChunkX(chunkKey);
        int chunkZ = ChunkUtil.unpackChunkZ(chunkKey);

        int dx = Math.abs(chunkX - playerChunkX);
        int dz = Math.abs(chunkZ - playerChunkZ);

        if (dx <= radius && dz <= radius) {
          inRange = true;
          break;
        }
      }

      if (inRange) {
        markChunksPendingReload(player, chunkKeys);
        notified++;
      }
    }

    return notified;
  }

  /**
   * Notifies all players in a world about changed chunks.
   *
   * @param world the world
   * @param chunkKeys the changed chunk keys
   * @return number of players notified
   */
  private int notifyAllPlayers(@NotNull World world, @NotNull List<Long> chunkKeys) {
    int notified = 0;

    for (Player player : world.getPlayers()) {
      markChunksPendingReload(player, chunkKeys);
      notified++;
    }

    return notified;
  }

  /**
   * Notifies a player that specific chunks need to be reloaded on their world map.
   * Uses clearChunks() for surgical invalidation — only the changed chunks are
   * removed from the loaded set and queued for re-fetch on the next tick.
   * This avoids the nuclear clear() which wipes ALL loaded tiles and forces
   * a full spiral reload from scratch (causing empty map after teleport).
   *
   * @param player the player
   * @param chunkKeys the chunk keys that changed
   */
  private void markChunksPendingReload(@NotNull Player player, @NotNull List<Long> chunkKeys) {
    try {
      LongOpenHashSet indices = new LongOpenHashSet(chunkKeys);
      player.getWorldMapTracker().clearChunks(indices);
    } catch (Exception e) {
      Logger.debugWorldMap("Failed to clear world map chunks for player: %s", e.getMessage());
    }
  }

  /**
   * Schedules a debounced full refresh.
   */
  private void scheduleDebounceRefresh() {
    // Cancel existing debounce task
    if (debounceTask != null) {
      debounceTask.cancel(false);
    }

    int delaySeconds = config.getDebouncedDelaySeconds();

    debounceTask = scheduler.schedule(ErrorHandler.wrapTask("World map debounce refresh", () -> {
      // Check if enough time has passed since last change
      long elapsed = System.currentTimeMillis() - lastChangeTime;
      if (elapsed >= delaySeconds * 1000L) {
        Logger.debugWorldMap("Debounce period elapsed, performing full refresh");
        forceFullRefresh();
      } else {
        // Reschedule
        scheduleDebounceRefresh();
      }
    }), delaySeconds, TimeUnit.SECONDS);
  }

  /**
   * Gets the current refresh mode from config.
   *
   * @return the refresh mode
   */
  @NotNull
  public RefreshMode getEffectiveMode() {
    return config.getRefreshMode();
  }

  /**
   * Checks if the scheduler is in fallback mode.
   * Note: Fallback mode was removed - this always returns false for API compatibility.
   *
   * @return always false
   */
  public boolean isInFallbackMode() {
    return false;
  }

  /**
   * Gets the number of chunks pending refresh.
   *
   * @return pending chunk count
   */
  public int getPendingChunkCount() {
    return pendingChunks.values().stream().mapToInt(Set::size).sum();
  }

  /**
   * Gets the total number of refresh operations.
   *
   * @return total refreshes
   */
  public int getTotalRefreshes() {
    return totalRefreshes.get();
  }

  /**
   * Gets the total number of chunks processed.
   *
   * @return chunks processed
   */
  public int getChunksProcessed() {
    return chunksProcessed.get();
  }

  /**
   * Gets the total number of players notified.
   *
   * @return players notified
   */
  public int getPlayersNotified() {
    return playersNotified.get();
  }

  /**
   * Gets the time of the last refresh.
   *
   * @return last refresh time, or null if never refreshed
   */
  @Nullable
  public Instant getLastRefreshTime() {
    return lastRefreshTime;
  }

  /**
   * Resets statistics counters.
   */
  public void resetStats() {
    totalRefreshes.set(0);
    chunksProcessed.set(0);
    playersNotified.set(0);
  }
}
