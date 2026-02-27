package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents chunk claim info from HyFactions Claims.json.
 * Note: HyFactions uses ChunkY for what HyperFactions calls chunkZ.
 *
 * @param UUID           the faction UUID that owns this claim
 * @param ChunkX         the chunk X coordinate
 * @param ChunkY         the chunk Z coordinate (HyFactions naming convention)
 * @param CreatedTracker when this chunk was claimed
 */
public record HyFactionChunkInfo(
  @Nullable String UUID,
  int ChunkX,
  int ChunkY,
  @Nullable HyFactionTracker CreatedTracker
) {
  /**
   * Gets the chunk Z coordinate (HyFactions uses ChunkY for Z).
   *
   * @return the chunk Z coordinate
   */
  public int getChunkZ() {
    return ChunkY;
  }
}
