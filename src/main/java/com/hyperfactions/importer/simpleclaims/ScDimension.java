package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a dimension entry within SimpleClaims {@code Claims.json}.
 */
public record ScDimension(
  @Nullable String Dimension,
  @Nullable List<ScChunkInfo> ChunkInfo
) {}
