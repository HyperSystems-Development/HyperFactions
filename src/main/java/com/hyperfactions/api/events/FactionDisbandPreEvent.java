package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction is disbanded. Can be cancelled to prevent disbanding.
 */
public final class FactionDisbandPreEvent implements Cancellable {

  private final Faction faction;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public FactionDisbandPreEvent(@NotNull Faction faction, @Nullable UUID actorUuid) {
    this.faction = faction;
    this.actorUuid = actorUuid;
  }

  @NotNull public Faction faction() { return faction; }
  @Nullable public UUID actorUuid() { return actorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
