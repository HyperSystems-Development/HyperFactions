package com.hyperfactions.importer.elbaphfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for ElbaphFactions claims.json entries.
 * Claims are stored as objects keyed by "dimension:x:z".
 *
 * @param dimension the world/dimension name
 * @param x         the chunk X coordinate
 * @param z         the chunk Z coordinate
 * @param factionId the owning faction's UUID
 * @param claimedAt when the chunk was claimed (epoch millis)
 * @param claimedBy the UUID of the entity that claimed (faction UUID in ElbaphFactions)
 */
public record ElbaphClaim(
  @Nullable String dimension,
  int x,
  int z,
  @Nullable String factionId,
  long claimedAt,
  @Nullable String claimedBy
) {
}
