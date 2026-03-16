package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction is created. Can be cancelled to prevent creation.
 */
public final class FactionCreatePreEvent implements Cancellable {

  private final String factionName;
  private final UUID creatorUuid;
  private boolean cancelled;
  private String cancelReason;

  public FactionCreatePreEvent(@NotNull String factionName, @NotNull UUID creatorUuid) {
    this.factionName = factionName;
    this.creatorUuid = creatorUuid;
  }

  @NotNull public String factionName() { return factionName; }
  @NotNull public UUID creatorUuid() { return creatorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
