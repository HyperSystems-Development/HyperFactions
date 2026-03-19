package com.hyperfactions.importer.factionsx;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a chunk claim entry in FactionsX Claims.json.
 *
 * <p>Note: FactionsX stores the Z coordinate under the key "ChunkY" — this is a naming
 * bug inherited from HyFactions. Use {@link #getChunkZ()} for the actual Z coordinate.
 */
public record FxChunkInfo(
  @Nullable String UUID,
  int ChunkX,
  int ChunkY,
  @Nullable FxTracker CreatedTracker
) {

  /** Returns the actual chunk Z coordinate (stored as ChunkY in FactionsX data). */
  public int getChunkZ() {
    return ChunkY;
  }
}
