package com.hyperfactions.importer.simpleclaims;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a chunk claim entry in SimpleClaims {@code Claims.json}.
 *
 * <p>Note: SimpleClaims legacy JSON stores the Z coordinate under the key "ChunkY" —
 * this is a naming bug from the {@code @FieldName("ChunkY")} annotation on the
 * {@code chunkZ} field. Use {@link #getChunkZ()} for the actual Z coordinate.
 */
public record ScChunkInfo(
  @Nullable String UUID,
  int ChunkX,
  int ChunkY,
  @Nullable ScTracker CreatedTracker
) {

  /** Returns the actual chunk Z coordinate (stored as ChunkY in legacy JSON). */
  public int getChunkZ() {
    return ChunkY;
  }
}
