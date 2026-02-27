package com.hyperfactions.importer.elbaphfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for zone chunk entries in ElbaphFactions zones.json.
 *
 * @param dimension the world/dimension name
 * @param chunkX    the chunk X coordinate
 * @param chunkZ    the chunk Z coordinate
 */
public record ElbaphZoneChunk(
  @Nullable String dimension,
  int chunkX,
  int chunkZ
) {
}
