package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a faction claims a chunk.
 */
public record FactionClaimEvent(
  @NotNull Faction faction,
  @NotNull UUID claimedBy,
  @NotNull String world,
  int chunkX,
  int chunkZ
) {}
