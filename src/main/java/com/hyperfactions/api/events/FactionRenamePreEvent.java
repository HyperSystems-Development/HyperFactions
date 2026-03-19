package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction name/tag/description/color change. Can be cancelled.
 */
public final class FactionRenamePreEvent implements Cancellable {

  private final UUID factionId;
  private final FactionRenameEvent.Field field;
  private final String oldValue;
  private final String newValue;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public FactionRenamePreEvent(@NotNull UUID factionId, @NotNull FactionRenameEvent.Field field,
                               @Nullable String oldValue, @Nullable String newValue,
                               @NotNull UUID actorUuid) {
    this.factionId = factionId;
    this.field = field;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.actorUuid = actorUuid;
  }

  @NotNull public UUID factionId() { return factionId; }
  @NotNull public FactionRenameEvent.Field field() { return field; }
  @Nullable public String oldValue() { return oldValue; }
  @Nullable public String newValue() { return newValue; }
  @NotNull public UUID actorUuid() { return actorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
