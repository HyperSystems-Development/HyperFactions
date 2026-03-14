package com.hyperfactions.worldmap;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.IWorldMap;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapSettings;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;

/**
 * World map provider that returns the HyperFactions world map generator.
 * This provider is set on the WorldConfig to replace the default world map
 * with our custom implementation that renders claim overlays.
 */
public class HyperFactionsWorldMapProvider implements IWorldMapProvider {

  public static final String ID = "HyperFactions";

  public static final BuilderCodec<HyperFactionsWorldMapProvider> CODEC =
      BuilderCodec.builder(HyperFactionsWorldMapProvider.class, HyperFactionsWorldMapProvider::new).build();

  /** Returns a new per-world generator instance. */
  @Override
  public IWorldMap getGenerator(World world) throws WorldMapLoadException {
    // This path is used during world config deserialization (future loads).
    // Capture whatever settings the world currently has as "original".
    WorldMapSettings currentSettings = null;
    try {
      WorldMapManager wmManager = world.getWorldMapManager();
      if (wmManager != null) {
        currentSettings = wmManager.getWorldMapSettings();
      }
    } catch (Exception e) {
      // WorldMapManager may not be initialized yet during early world load
    }
    return new HyperFactionsWorldMap(currentSettings, BetterMapCompat.isActive());
  }
}
