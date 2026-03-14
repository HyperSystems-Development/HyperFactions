package com.hyperfactions.worldmap;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.WorldMapConfig;
import com.hyperfactions.data.ChunkKey;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages world map generator registration.
 * Handles registering the HyperFactionsWorldMap with worlds as players join them.
 * This provides colored chunk overlays showing claimed territory on the world map.
 *
 * <p>The HyperFactionsWorldMap generates chunk images from scratch with claim
 * colors baked in during pixel generation, ensuring reliable overlay display.
 */
public class WorldMapService {

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  /** Tracks which worlds have had the generator registered. */
  private final Set<String> registeredWorlds = ConcurrentHashMap.newKeySet();

  /** Refresh scheduler for optimized map updates. */
  private WorldMapRefreshScheduler refreshScheduler;

  /** Original world settings captured before replacing the generator, keyed by world name. */
  private final ConcurrentHashMap<String, WorldMapSettings> originalWorldSettings = new ConcurrentHashMap<>();

  /** Per-world generator instances for re-registration on refresh. */
  private final ConcurrentHashMap<String, HyperFactionsWorldMap> generators = new ConcurrentHashMap<>();

  /** Creates a new WorldMapService. */
  public WorldMapService(
      @NotNull FactionManager factionManager,
      @NotNull ClaimManager claimManager,
      @NotNull ZoneManager zoneManager,
      @NotNull RelationManager relationManager) {
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.zoneManager = zoneManager;
    // relationManager parameter kept for API compatibility but not used
  }

  /**
   * Initializes the refresh scheduler. Must be called after ConfigManager is loaded.
   *
   * @param worldMapConfig the world map configuration
   */
  public void initializeScheduler(@NotNull WorldMapConfig worldMapConfig) {
    if (refreshScheduler != null) {
      refreshScheduler.shutdown();
    }
    refreshScheduler = new WorldMapRefreshScheduler(worldMapConfig, this);
    refreshScheduler.start();
    Logger.debug("[WorldMap] Refresh scheduler initialized with mode: %s",
        worldMapConfig.getRefreshMode().getConfigName());
  }

  /**
   * Gets the refresh scheduler for status/statistics.
   *
   * @return the refresh scheduler, or null if not initialized
   */
  @Nullable
  public WorldMapRefreshScheduler getRefreshScheduler() {
    return refreshScheduler;
  }

  /**
   * Registers the HyperFactions world map generator with a world if not already registered.
   * Should be called when a player enters a world.
   *
   * <p>IMPORTANT: We must call WorldMapManager.setGenerator() directly, not just
   * WorldConfig.setWorldMapProvider(). The WorldConfig provider is only used
   * during world initialization - if the world is already loaded, we need to
   * update the live WorldMapManager directly.
   *
   * @param world the world to register with
   */
  public void registerProviderIfNeeded(@NotNull World world) {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    String worldName = world.getName();

    // Check if already registered
    if (registeredWorlds.contains(worldName)) {
      return;
    }

    try {
      WorldMapManager worldMapManager = world.getWorldMapManager();

      // Capture original settings BEFORE replacing the generator
      WorldMapSettings currentSettings = worldMapManager.getWorldMapSettings();
      String currentGeneratorName = worldMapManager.getGenerator() != null
          ? worldMapManager.getGenerator().getClass().getSimpleName() : "null";
      Logger.debugWorldMap("World map generator BEFORE: world=%s, generator=%s", worldName, currentGeneratorName);

      // Respect disabled worlds (Issue #96)
      if (ConfigManager.get().worldMap().isRespectWorldConfig() && isWorldMapDisabled(currentSettings)) {
        Logger.debug("[WorldMap] World '%s' has map disabled in world config — skipping registration", worldName);
        return;
      }

      // Store original settings for reference
      if (currentSettings != null) {
        originalWorldSettings.put(worldName, currentSettings);
        Logger.debugWorldMap("Captured original settings for world: %s", worldName);
      }

      // Create per-world generator with original settings
      boolean betterMapActive = BetterMapCompat.isActive();
      HyperFactionsWorldMap generator = new HyperFactionsWorldMap(currentSettings, betterMapActive);

      // Set our generator directly on the WorldMapManager
      worldMapManager.setGenerator(generator);

      // Also set the provider on WorldConfig for consistency (future loads)
      world.getWorldConfig().setWorldMapProvider(new HyperFactionsWorldMapProvider());

      // Track for refresh/re-registration
      generators.put(worldName, generator);
      registeredWorlds.add(worldName);

      Logger.debug("Registered world map for world: %s (replaced %s, betterMap=%s)",
          worldName, currentGeneratorName, betterMapActive);

    } catch (Exception e) {
      Logger.warn("Failed to register world map for world %s: %s", worldName, e.getMessage());
    }
  }

  /**
   * Checks if the world's map settings indicate the map is disabled.
   *
   * @param settings the world's current settings
   * @return true if the world map is disabled
   */
  private boolean isWorldMapDisabled(@Nullable WorldMapSettings settings) {
    if (settings == null) {
      return false;
    }
    // Check for the DISABLED sentinel instance.
    // We only check identity equality with the static DISABLED singleton,
    // NOT the packet's enabled field — UpdateWorldMapSettings defaults
    // enabled=false (Java boolean default), so even normal WorldGen worlds
    // would incorrectly appear disabled if we checked that field.
    return settings == WorldMapSettings.DISABLED;
  }

  /**
   * Gets the original world settings captured before registration.
   *
   * @param worldName the world name
   * @return the original settings, or null if not captured
   */
  @Nullable
  public WorldMapSettings getOriginalSettings(@NotNull String worldName) {
    return originalWorldSettings.get(worldName);
  }

  /**
   * Forces a refresh of the world map for all players.
   * Call this when claims change to update the overlays.
   *
   * @param world the world to refresh
   */
  public void refreshWorldMap(@NotNull World world) {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    try {
      WorldMapManager worldMapManager = world.getWorldMapManager();

      // Check if our generator is still active (another mod may have overwritten it)
      IWorldMap currentGenerator = worldMapManager.getGenerator();
      boolean isOurGenerator = currentGenerator instanceof HyperFactionsWorldMap;
      if (!isOurGenerator) {
        String generatorName = currentGenerator != null ? currentGenerator.getClass().getName() : "null";
        Logger.warn("[WorldMap] Generator overwritten! Expected HyperFactionsWorldMap but found: %s", generatorName);

        // Re-register using stored generator (preserves original settings)
        HyperFactionsWorldMap storedGenerator = generators.get(world.getName());
        if (storedGenerator != null) {
          worldMapManager.setGenerator(storedGenerator);
          Logger.warn("[WorldMap] Re-registered stored generator for world: %s", world.getName());
        } else {
          // Fallback: create new instance
          WorldMapSettings origSettings = originalWorldSettings.get(world.getName());
          HyperFactionsWorldMap newGenerator = new HyperFactionsWorldMap(origSettings, BetterMapCompat.isActive());
          worldMapManager.setGenerator(newGenerator);
          generators.put(world.getName(), newGenerator);
          Logger.warn("[WorldMap] Created new generator for world: %s", world.getName());
        }
      }

      // Clear cached images on server to force regeneration with new claim data
      worldMapManager.clearImages();

      // Clear each player's world map tracker to force them to re-request tiles
      // This sends ClearWorldMap packet to clients
      for (com.hypixel.hytale.server.core.entity.entities.Player player : world.getPlayers()) {
        try {
          player.getWorldMapTracker().clear();
        } catch (Exception e) {
          Logger.warn("Failed to clear world map tracker for player: %s", e.getMessage());
        }
      }

      Logger.debugWorldMap("Cleared world map images for world: %s (%d players)",
          world.getName(), world.getPlayers().size());
    } catch (Exception e) {
      Logger.warn("Failed to refresh world map for world %s: %s", world.getName(), e.getMessage());
    }
  }

  /**
   * Forces a refresh of the world map for all registered worlds.
   * Call this when faction data changes (color, claims, etc.).
   *
   * <p>
   * Note: This performs an immediate full refresh, bypassing the scheduler.
   * For normal claim changes, use {@link #queueChunkRefresh} instead.
   */
  public void refreshAllWorldMaps() {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    try {
      int refreshed = 0;
      for (World world : com.hypixel.hytale.server.core.universe.Universe.get().getWorlds().values()) {
        if (registeredWorlds.contains(world.getName())) {
          refreshWorldMap(world);
          refreshed++;
        }
      }
      Logger.debugWorldMap("Refreshed world maps for %d/%d worlds", refreshed, registeredWorlds.size());
    } catch (Exception e) {
      Logger.warn("Failed to refresh all world maps: %s", e.getMessage());
    }
  }

  /**
   * Queues a chunk for refresh through the scheduler.
   * This is the preferred method for claim changes as it respects the configured refresh mode.
   *
   * @param worldName the world name
   * @param chunkX chunk X coordinate
   * @param chunkZ chunk Z coordinate
   */
  public void queueChunkRefresh(@NotNull String worldName, int chunkX, int chunkZ) {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    if (refreshScheduler != null) {
      refreshScheduler.queueChunkRefresh(worldName, chunkX, chunkZ);
    } else {
      // Fallback to immediate refresh if scheduler not initialized
      Logger.debugWorldMap("Scheduler not initialized, using immediate refresh");
      World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldName);
      if (world != null && registeredWorlds.contains(worldName)) {
        refreshWorldMap(world);
      }
    }
  }

  /**
   * Forces an immediate full refresh, bypassing the scheduler.
   * Use for admin commands or critical updates.
   */
  public void forceFullRefresh() {
    if (refreshScheduler != null) {
      refreshScheduler.forceFullRefresh();
    } else {
      refreshAllWorldMaps();
    }
  }

  /**
   * Triggers a refresh for a faction's claimed chunks, respecting the configured refresh mode.
   * Use for faction-wide changes (rename, tag, color) that affect all claimed chunks.
   *
   * <p>If the faction has more claims than the configured threshold, falls back to full refresh.
   *
   * @param factionId the faction whose claims need refreshing
   */
  public void triggerFactionWideRefresh(@NotNull UUID factionId) {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    Set<ChunkKey> claims = claimManager.getFactionClaims(factionId);
    triggerFactionWideRefresh(claims);
  }

  /**
   * Triggers a refresh for specific chunks, respecting the configured refresh mode.
   * Use for zone changes or other multi-chunk updates.
   *
   * <p>If chunks is null or exceeds the configured threshold, falls back to full refresh.
   *
   * @param chunks the chunks to refresh, or null for full refresh
   */
  public void triggerFactionWideRefresh(@Nullable Set<ChunkKey> chunks) {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    if (refreshScheduler != null) {
      refreshScheduler.queueFactionWideRefresh(chunks);
    } else {
      refreshAllWorldMaps();
    }
  }

  /**
   * Triggers a full refresh that respects the configured refresh mode.
   * Use when chunk set is unknown or for legacy callers.
   *
   * <p>Behavior by mode:
   * - PROXIMITY/INCREMENTAL/IMMEDIATE: Full refresh
   * - DEBOUNCED: Triggers debounce timer
   * - MANUAL: No automatic refresh
   */
  public void triggerFactionWideRefresh() {
    if (!ConfigManager.get().isWorldMapMarkersEnabled()) {
      return;
    }

    if (refreshScheduler != null) {
      refreshScheduler.queueFactionWideRefresh(null);
    } else {
      refreshAllWorldMaps();
    }
  }

  /**
   * Unregisters the overlay from a world.
   * Note: This restores the original generator if one was wrapped.
   *
   * @param worldName the world name
   */
  public void unregisterProvider(@NotNull String worldName) {
    registeredWorlds.remove(worldName);
    originalWorldSettings.remove(worldName);
    generators.remove(worldName);
  }

  /**
   * Checks if the overlay is registered for a world.
   *
   * @param worldName the world name
   * @return true if registered
   */
  public boolean isRegistered(@NotNull String worldName) {
    return registeredWorlds.contains(worldName);
  }

  /**
   * Clears all registration state.
   * Called on plugin shutdown.
   */
  public void shutdown() {
    if (refreshScheduler != null) {
      refreshScheduler.shutdown();
      refreshScheduler = null;
    }
    registeredWorlds.clear();
    originalWorldSettings.clear();
    generators.clear();
  }
}
