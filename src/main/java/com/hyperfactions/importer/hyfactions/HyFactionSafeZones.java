package com.hyperfactions.importer.hyfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the SafeZones.json file structure from HyFactions.
 *
 * @param SafeZones list of safe zone chunks
 */
public record HyFactionSafeZones(
  @Nullable List<HyFactionZoneChunk> SafeZones
) {
}
