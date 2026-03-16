package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a chunk is claimed. Can be cancelled to prevent the claim.
 */
public final class FactionClaimPreEvent implements Cancellable {

  private final UUID factionId;
  private final UUID playerUuid;
  private final String world;
  private final int chunkX;
  private final int chunkZ;
  private boolean cancelled;
  private String cancelReason;

  public FactionClaimPreEvent(@NotNull UUID factionId, @NotNull UUID playerUuid,
                              @NotNull String world, int chunkX, int chunkZ) {
    this.factionId = factionId;
    this.playerUuid = playerUuid;
    this.world = world;
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
  }

  @NotNull public UUID factionId() { return factionId; }
  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String world() { return world; }
  public int chunkX() { return chunkX; }
  public int chunkZ() { return chunkZ; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
