package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction home teleport executes. Can be cancelled to prevent the teleport.
 * Fires for both instant and warmup-completed teleports.
 */
public final class FactionHomeTeleportPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final UUID factionId;
  private final String sourceWorld;
  private final double sourceX;
  private final double sourceY;
  private final double sourceZ;
  private final String destWorld;
  private final double destX;
  private final double destY;
  private final double destZ;
  private boolean cancelled;
  private String cancelReason;

  public FactionHomeTeleportPreEvent(@NotNull UUID playerUuid, @NotNull UUID factionId,
                                     @NotNull String sourceWorld, double sourceX, double sourceY, double sourceZ,
                                     @NotNull String destWorld, double destX, double destY, double destZ) {
    this.playerUuid = playerUuid;
    this.factionId = factionId;
    this.sourceWorld = sourceWorld;
    this.sourceX = sourceX;
    this.sourceY = sourceY;
    this.sourceZ = sourceZ;
    this.destWorld = destWorld;
    this.destX = destX;
    this.destY = destY;
    this.destZ = destZ;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public UUID factionId() { return factionId; }
  @NotNull public String sourceWorld() { return sourceWorld; }
  public double sourceX() { return sourceX; }
  public double sourceY() { return sourceY; }
  public double sourceZ() { return sourceZ; }
  @NotNull public String destWorld() { return destWorld; }
  public double destX() { return destX; }
  public double destY() { return destY; }
  public double destZ() { return destZ; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
