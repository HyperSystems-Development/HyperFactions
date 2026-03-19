package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a member joins, leaves, or changes roles. Can be cancelled.
 */
public final class FactionMemberPreEvent implements Cancellable {

  private final Faction faction;
  private final UUID playerUuid;
  private final FactionMemberEvent.Type type;
  private boolean cancelled;
  private String cancelReason;

  public FactionMemberPreEvent(@NotNull Faction faction, @NotNull UUID playerUuid,
                               @NotNull FactionMemberEvent.Type type) {
    this.faction = faction;
    this.playerUuid = playerUuid;
    this.type = type;
  }

  @NotNull public Faction faction() { return faction; }
  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public FactionMemberEvent.Type type() { return type; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
