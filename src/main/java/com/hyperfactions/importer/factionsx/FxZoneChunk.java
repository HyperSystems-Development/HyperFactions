package com.hyperfactions.importer.factionsx;

import org.jetbrains.annotations.NotNull;

/**
 * Parsed zone chunk record (not a direct JSON mapping).
 * Created by splitting the {@code "chunkX:chunkZ"} strings from FactionsX Zones.json.
 */
public record FxZoneChunk(
  @NotNull String dimension,
  int chunkX,
  int chunkZ
) {}
