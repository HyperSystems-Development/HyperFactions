package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a relation between two factions.
 *
 * @param targetFactionId the ID of the other faction
 * @param type            the type of relation
 * @param since           when this relation was established (epoch millis)
 */
public record FactionRelation(
  @NotNull UUID targetFactionId,
  @NotNull RelationType type,
  long since
) {
  /**
   * Creates a new relation at the current time.
   *
   * @param targetFactionId the target faction's ID
   * @param type            the relation type
   * @return a new FactionRelation
   */
  public static FactionRelation create(@NotNull UUID targetFactionId, @NotNull RelationType type) {
    return new FactionRelation(targetFactionId, type, System.currentTimeMillis());
  }

  /**
   * Creates a copy with an updated type.
   *
   * @param newType the new relation type
   * @return a new FactionRelation with updated type and timestamp
   */
  public FactionRelation withType(@NotNull RelationType newType) {
    return new FactionRelation(targetFactionId, newType, System.currentTimeMillis());
  }

  /**
   * Checks if this is an ally relation.
   *
   * @return true if ally
   */
  public boolean isAlly() {
    return type == RelationType.ALLY;
  }

  /**
   * Checks if this is an enemy relation.
   *
   * @return true if enemy
   */
  public boolean isEnemy() {
    return type == RelationType.ENEMY;
  }

  /**
   * Checks if this is a neutral relation.
   *
   * @return true if neutral
   */
  public boolean isNeutral() {
    return type == RelationType.NEUTRAL;
  }
}
