package com.hyperfactions.worldmap;

import com.hyperfactions.api.HyperFactionsAPI;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.map.WorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Custom world map generator that renders terrain with faction claim overlays.
 * This implementation generates chunk images from scratch with claim colors
 * baked in during pixel generation, matching Hyfaction's proven approach.
 *
 * <p>Unlike the overlay post-process approach, this method directly renders
 * claim colors as part of the terrain generation, ensuring claim overlays
 * always appear correctly on the map.
 */
public class HyperFactionsWorldMap implements IWorldMap {

  /** Singleton instance returned by the provider. */
  public static final HyperFactionsWorldMap INSTANCE = new HyperFactionsWorldMap();

  private HyperFactionsWorldMap() {
    // Singleton - use INSTANCE
  }

  /**
   * Gets the managers from HyperFactionsAPI.
   * These are accessed at generation time rather than construction time
   * to ensure they are initialized.
   */
  private FactionManager getFactionManager() {
    return HyperFactionsAPI.getFactionManager();
  }

  private ClaimManager getClaimManager() {
    return HyperFactionsAPI.getClaimManager();
  }

  private ZoneManager getZoneManager() {
    return HyperFactionsAPI.getZoneManager();
  }

  /** Returns the world map settings. */
  @Override
  public WorldMapSettings getWorldMapSettings() {
    UpdateWorldMapSettings settingsPacket = new UpdateWorldMapSettings();
    settingsPacket.enabled = true;
    settingsPacket.defaultScale = 128.0f;
    settingsPacket.minScale = 64.0f;
    settingsPacket.maxScale = 128.0f;
    return new WorldMapSettings(null, 3.0f, 1.0f, 16, 32, settingsPacket);
  }

  /** Generate. */
  @Override
  public CompletableFuture<WorldMap> generate(World world, int imageWidth, int imageHeight, LongSet chunksToGenerate) {
    // Log that our generator is being called (helps diagnose mod conflicts)
    Logger.debugWorldMap("[HyperFactionsWorldMap] generate() called - world=%s, chunks=%d, imageSize=%dx%d",
        world.getName(), chunksToGenerate.size(), imageWidth, imageHeight);

    // Get managers at generation time
    FactionManager factionManager = getFactionManager();
    ClaimManager claimManager = getClaimManager();
    ZoneManager zoneManager = getZoneManager();

    @SuppressWarnings("unchecked")
    CompletableFuture<ClaimImageBuilder>[] futures = new CompletableFuture[chunksToGenerate.size()];

    int futureIndex = 0;
    LongIterator iterator = chunksToGenerate.iterator();
    while (iterator.hasNext()) {
      long chunkIndex = iterator.nextLong();
      futures[futureIndex++] = ClaimImageBuilder.build(
          chunkIndex, imageWidth, imageHeight, world,
          factionManager, claimManager, zoneManager
      );
    }

    return CompletableFuture.allOf(futures).thenApply(unused -> {
      WorldMap worldMap = new WorldMap(futures.length);
      int rendered = 0;
      for (CompletableFuture<ClaimImageBuilder> future : futures) {
        ClaimImageBuilder builder = future.getNow(null);
        if (builder != null) {
          worldMap.getChunks().put(builder.getIndex(), builder.getImage());
          rendered++;
        }
      }
      Logger.debugWorldMap("[HyperFactionsWorldMap] generate() complete - %d/%d chunks rendered",
          rendered, futures.length);
      return worldMap;
    });
  }

  /** Generate Points Of Interest. */
  @Override
  public CompletableFuture<Map<String, MapMarker>> generatePointsOfInterest(World world) {
    // No custom markers for now - could add faction home markers in the future
    return CompletableFuture.completedFuture(new HashMap<>());
  }

  /** Cleans up resources. */
  @Override
  public void shutdown() {
    // No resources to clean up
  }
}
