package com.hyperfactions.importer.hyfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a dimension entry from HyFactions Claims.json.
 *
 * @param Dimension the dimension/world name
 * @param ChunkInfo list of chunk claims in this dimension
 */
public record HyFactionDimension(
  @Nullable String Dimension,
  @Nullable List<HyFactionChunkInfo> ChunkInfo
) {
}
