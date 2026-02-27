package com.hyperfactions.importer.hyfactions;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the NameCache.json file structure from HyFactions.
 *
 * @param Values list of UUID to username mappings
 */
public record HyFactionNameCache(
  @Nullable List<HyFactionNameEntry> Values
) {
}
