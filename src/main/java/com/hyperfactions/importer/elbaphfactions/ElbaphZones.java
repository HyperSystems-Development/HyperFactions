package com.hyperfactions.importer.elbaphfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for ElbaphFactions zones.json.
 *
 * @param safeZones   list of safe zone chunks
 * @param warZones    list of war zone chunks
 * @param adminBypass list of admin bypass UUIDs
 */
public record ElbaphZones(
  @Nullable List<ElbaphZoneChunk> safeZones,
  @Nullable List<ElbaphZoneChunk> warZones,
  @Nullable List<String> adminBypass
) {
}
