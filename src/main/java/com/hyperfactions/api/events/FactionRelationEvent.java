package com.hyperfactions.api.events;

import com.hyperfactions.data.RelationType;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when the diplomatic relation between two factions changes.
 * The {@code OWN} relation type will never appear in this event — it represents
 * a player's own faction, not an inter-faction relation.
 *
 * @param factionId1  the first faction
 * @param factionId2  the second faction
 * @param oldRelation the previous relation type (ALLY, ENEMY, or NEUTRAL)
 * @param newRelation the new relation type (ALLY, ENEMY, or NEUTRAL)
 * @param actorUuid   the player who triggered the change (null for system)
 */
public record FactionRelationEvent(
    @NotNull UUID factionId1,
    @NotNull UUID factionId2,
    @NotNull RelationType oldRelation,
    @NotNull RelationType newRelation,
    @Nullable UUID actorUuid
) {
  /** Validates that OWN is not used as an inter-faction relation. */
  public FactionRelationEvent {
    if (oldRelation == RelationType.OWN || newRelation == RelationType.OWN) {
      throw new IllegalArgumentException("OWN is not a valid inter-faction relation");
    }
  }
}
