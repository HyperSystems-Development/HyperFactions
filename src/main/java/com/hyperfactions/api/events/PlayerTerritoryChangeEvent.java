package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a player moves between faction territories.
 * Fires when the owning faction of the player's current chunk changes.
 *
 * @param playerUuid   the player who moved
 * @param world        the world name
 * @param chunkX       the new chunk X
 * @param chunkZ       the new chunk Z
 * @param oldFactionId the previous territory owner (null for wilderness)
 * @param newFactionId the new territory owner (null for wilderness)
 */
public record PlayerTerritoryChangeEvent(
    @NotNull UUID playerUuid,
    @NotNull String world,
    int chunkX,
    int chunkZ,
    @Nullable UUID oldFactionId,
    @Nullable UUID newFactionId
) {
  /** Returns true if the player entered wilderness. */
  public boolean enteredWilderness() {
    return newFactionId == null;
  }

  /** Returns true if the player left wilderness into claimed territory. */
  public boolean leftWilderness() {
    return oldFactionId == null && newFactionId != null;
  }
}
