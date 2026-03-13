package com.hyperfactions.importer.factionsx;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a dimension entry within FactionsX Claims.json.
 */
public record FxDimension(
  @Nullable String Dimension,
  @Nullable List<FxChunkInfo> ChunkInfo
) {}
