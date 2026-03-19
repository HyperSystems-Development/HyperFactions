package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a faction's home location is set or cleared.
 *
 * @param factionId the faction
 * @param home      the new home location (null if cleared)
 * @param actorUuid the player who set/cleared the home
 */
public record FactionHomeEvent(
    @NotNull UUID factionId,
    @Nullable Faction.FactionHome home,
    @NotNull UUID actorUuid
) {
  /** Returns true if the home was cleared (set to null). */
  public boolean isCleared() {
    return home == null;
  }
}
