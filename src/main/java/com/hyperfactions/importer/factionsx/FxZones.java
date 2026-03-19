package com.hyperfactions.importer.factionsx;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for FactionsX Zones.json root object.
 *
 * <p>Zone chunks are stored as {@code "chunkX:chunkZ"} strings grouped by dimension name.
 * Keys are "Safezone" and "Warzone" (singular, matching FactionsX source).
 */
public record FxZones(
  @Nullable Map<String, List<String>> Safezone,
  @Nullable Map<String, List<String>> Warzone
) {}
