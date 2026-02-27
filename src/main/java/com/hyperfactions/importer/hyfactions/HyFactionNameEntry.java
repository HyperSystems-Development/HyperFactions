package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a name cache entry from HyFactions NameCache.json.
 *
 * @param UUID the player's UUID
 * @param Name the player's username
 */
public record HyFactionNameEntry(
  @Nullable String UUID,
  @Nullable String Name
) {
}
