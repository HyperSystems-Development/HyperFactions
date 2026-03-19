package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a faction loses a claimed chunk.
 * Uses {@code factionId} (not a Faction object) because the Faction may already
 * be removed from cache in some paths (e.g., disband, decay).
 *
 * @param factionId the faction that lost the claim
 * @param world     the world name
 * @param chunkX    the chunk X coordinate
 * @param chunkZ    the chunk Z coordinate
 * @param reason    how the claim was lost
 * @param actorUuid the player who triggered it (null for system/decay)
 */
public record FactionUnclaimEvent(
    @NotNull UUID factionId,
    @NotNull String world,
    int chunkX,
    int chunkZ,
    @NotNull Reason reason,
    @Nullable UUID actorUuid
) {
  public enum Reason {
    /** Player manually unclaimed */
    UNCLAIM,
    /** Faction disbanded — all claims released */
    DISBAND,
    /** Another faction overclaimed this chunk */
    OVERCLAIM,
    /** Claim removed due to inactivity decay */
    DECAY
  }
}
