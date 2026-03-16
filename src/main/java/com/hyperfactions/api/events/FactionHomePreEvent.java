package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction home is set or cleared. Can be cancelled.
 */
public final class FactionHomePreEvent implements Cancellable {

  private final UUID factionId;
  private final Faction.FactionHome home;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public FactionHomePreEvent(@NotNull UUID factionId, @Nullable Faction.FactionHome home,
                             @NotNull UUID actorUuid) {
    this.factionId = factionId;
    this.home = home;
    this.actorUuid = actorUuid;
  }

  @NotNull public UUID factionId() { return factionId; }
  @Nullable public Faction.FactionHome home() { return home; }
  @NotNull public UUID actorUuid() { return actorUuid; }
  public boolean isClearing() { return home == null; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
