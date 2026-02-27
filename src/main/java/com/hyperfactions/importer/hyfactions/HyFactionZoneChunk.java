package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single zone chunk from HyFactions SafeZones/WarZones.
 *
 * @param dimension the dimension/world name
 * @param chunkX    the chunk X coordinate
 * @param chunkZ    the chunk Z coordinate
 */
public record HyFactionZoneChunk(
  @Nullable String dimension,
  int chunkX,
  int chunkZ
) {
}
