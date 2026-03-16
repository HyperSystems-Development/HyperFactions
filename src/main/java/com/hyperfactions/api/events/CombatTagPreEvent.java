package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a player is combat tagged. Can be cancelled to prevent tagging.
 */
public final class CombatTagPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final UUID taggerUuid;
  private final int durationSeconds;
  private boolean cancelled;
  private String cancelReason;

  public CombatTagPreEvent(@NotNull UUID playerUuid, @Nullable UUID taggerUuid, int durationSeconds) {
    this.playerUuid = playerUuid;
    this.taggerUuid = taggerUuid;
    this.durationSeconds = durationSeconds;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @Nullable public UUID taggerUuid() { return taggerUuid; }
  public int durationSeconds() { return durationSeconds; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
