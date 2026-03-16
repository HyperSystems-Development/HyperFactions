package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a player's combat tag state changes.
 *
 * @param playerUuid the affected player
 * @param type       what happened
 * @param taggerUuid the player who caused the tag (null for EXPIRED/CLEARED)
 * @param durationSeconds tag duration in seconds (0 for EXPIRED/CLEARED)
 */
public record CombatTagEvent(
    @NotNull UUID playerUuid,
    @NotNull Type type,
    @Nullable UUID taggerUuid,
    int durationSeconds
) {
  public enum Type {
    /** Player was tagged in combat */
    TAGGED,
    /** Combat tag expired naturally */
    EXPIRED,
    /** Combat tag was manually cleared */
    CLEARED
  }
}
