package com.hyperfactions.importer.hyfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the WarZones.json file structure from HyFactions.
 *
 * @param WarZones list of war zone chunks
 */
public record HyFactionWarZones(
  @Nullable List<HyFactionZoneChunk> WarZones
) {
}
