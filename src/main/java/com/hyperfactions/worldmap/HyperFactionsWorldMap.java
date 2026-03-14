package com.hyperfactions.worldmap;

import com.hyperfactions.api.HyperFactionsAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.WorldMapConfig;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom world map generator that renders terrain with faction claim overlays.
 * This implementation generates chunk images from scratch with claim colors
 * baked in during pixel generation, matching Hyfaction's proven approach.
 *
 * <p>Unlike the overlay post-process approach, this method directly renders
 * claim colors as part of the terrain generation, ensuring claim overlays
 * always appear correctly on the map.
 *
 * <p>Each instance is per-world: it captures the world's original
 * {@link WorldMapSettings} and merges them with HyperFactions config overrides,
 * inheriting values the server admin has not explicitly overridden.
 */
public class HyperFactionsWorldMap implements IWorldMap {

  /** The world's original map settings captured before we replaced the generator, or null for legacy mode. */
  private final @Nullable WorldMapSettings originalSettings;

  /** Whether BetterMap compatibility mode was active at construction time. */
  private final boolean betterMapActive;

  /**
   * Creates a per-world HyperFactions world map generator.
   *
   * @param originalSettings the world's original map settings (null for legacy/fallback)
   * @param betterMapActive  whether BetterMap compatibility is active
   */
  public HyperFactionsWorldMap(@Nullable WorldMapSettings originalSettings, boolean betterMapActive) {
    this.originalSettings = originalSettings;
    this.betterMapActive = betterMapActive;
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

  /**
   * Returns the world map settings.
   *
   * <p>When {@code respectWorldConfig} is enabled and original settings are available,
   * merges the world's original settings with HyperFactions config overrides.
   * Otherwise falls back to legacy hardcoded values.
   */
  @Override
  @NotNull
  public WorldMapSettings getWorldMapSettings() {
    WorldMapConfig config = ConfigManager.get().worldMap();

    if (config.isRespectWorldConfig() && originalSettings != null
        && originalSettings.getSettingsPacket() != null) {
      return mergeWithOriginal(config);
    }

    // Legacy fallback: hardcoded settings (pre-0.12 behavior)
    UpdateWorldMapSettings settingsPacket = new UpdateWorldMapSettings();
    settingsPacket.enabled = true;
    settingsPacket.defaultScale = 128.0f;
    settingsPacket.minScale = 64.0f;
    settingsPacket.maxScale = 128.0f;
    return new WorldMapSettings(null, 3.0f, 1.0f, 16, 32, settingsPacket);
  }

  /**
   * Merges the world's original settings with HyperFactions config overrides.
   * Values not overridden in config are inherited from the original settings.
   *
   * @param config the world map configuration with optional overrides
   * @return merged settings
   */
  @NotNull
  private WorldMapSettings mergeWithOriginal(@NotNull WorldMapConfig config) {
    assert originalSettings != null; // Caller guarantees non-null

    UpdateWorldMapSettings original = originalSettings.getSettingsPacket();
    UpdateWorldMapSettings merged = new UpdateWorldMapSettings();

    // Always enable — we need the map for claim overlays
    merged.enabled = true;

    // Inherit biome data from the world's original settings
    merged.biomeDataMap = original.biomeDataMap;

    // Scale values: use config override if set, otherwise inherit from original
    merged.defaultScale = config.getOverrideDefaultScale() != null
        ? config.getOverrideDefaultScale()
        : original.defaultScale;
    merged.minScale = config.getOverrideMinScale() != null
        ? config.getOverrideMinScale()
        : original.minScale;
    merged.maxScale = config.getOverrideMaxScale() != null
        ? config.getOverrideMaxScale()
        : original.maxScale;

    // Configurable allow* flags: use config override if set, otherwise inherit
    merged.allowTeleportToCoordinates = config.getOverrideAllowTeleportToCoordinates() != null
        ? config.getOverrideAllowTeleportToCoordinates()
        : original.allowTeleportToCoordinates;
    merged.allowTeleportToMarkers = config.getOverrideAllowTeleportToMarkers() != null
        ? config.getOverrideAllowTeleportToMarkers()
        : original.allowTeleportToMarkers;
    merged.allowCreatingMapMarkers = config.getOverrideAllowCreatingMapMarkers() != null
        ? config.getOverrideAllowCreatingMapMarkers()
        : original.allowCreatingMapMarkers;

    // Always-inherited allow* flags (no config override — respect whatever the world set)
    merged.allowShowOnMapToggle = original.allowShowOnMapToggle;
    merged.allowCompassTrackingToggle = original.allowCompassTrackingToggle;
    merged.allowRemovingOtherPlayersMarkers = original.allowRemovingOtherPlayersMarkers;

    // ImageScale: when BetterMap is active, always use the world's original value
    // so BetterMap's quality system can control it. Otherwise use config override if set.
    float imageScale;
    if (betterMapActive) {
      imageScale = originalSettings.getImageScale();
    } else if (config.getOverrideImageScale() != null) {
      imageScale = config.getOverrideImageScale();
    } else {
      imageScale = originalSettings.getImageScale();
    }

    // Remaining WorldMapSettings fields: always inherit from original
    // viewRadiusMultiplier, viewRadiusMin, viewRadiusMax are private with no public getters,
    // so we use reflection to read them from the original settings
    float viewRadiusMultiplier = getPrivateFloat(originalSettings, "viewRadiusMultiplier", 1.0f);
    int viewRadiusMin = getPrivateInt(originalSettings, "viewRadiusMin", 1);
    int viewRadiusMax = getPrivateInt(originalSettings, "viewRadiusMax", 512);

    return new WorldMapSettings(
        originalSettings.getWorldMapArea(),
        imageScale,
        viewRadiusMultiplier,
        viewRadiusMin,
        viewRadiusMax,
        merged
    );
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

  // === Reflection helpers for private WorldMapSettings fields ===

  /**
   * Reads a private float field from an object via reflection.
   * Falls back to the default value if the field is not accessible.
   */
  private static float getPrivateFloat(@NotNull Object obj, @NotNull String fieldName, float defaultValue) {
    try {
      java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.getFloat(obj);
    } catch (Exception e) {
      Logger.debug("[WorldMap] Could not read field '%s' via reflection, using default %.1f", fieldName, defaultValue);
      return defaultValue;
    }
  }

  /**
   * Reads a private int field from an object via reflection.
   * Falls back to the default value if the field is not accessible.
   */
  private static int getPrivateInt(@NotNull Object obj, @NotNull String fieldName, int defaultValue) {
    try {
      java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.getInt(obj);
    } catch (Exception e) {
      Logger.debug("[WorldMap] Could not read field '%s' via reflection, using default %d", fieldName, defaultValue);
      return defaultValue;
    }
  }
}
