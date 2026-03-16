package com.hyperfactions.api.events;

import com.hyperfactions.data.RelationType;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a faction relation change. Can be cancelled to prevent the change.
 */
public final class FactionRelationPreEvent implements Cancellable {

  private final UUID factionId1;
  private final UUID factionId2;
  private final RelationType oldRelation;
  private final RelationType newRelation;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public FactionRelationPreEvent(@NotNull UUID factionId1, @NotNull UUID factionId2,
                                 @NotNull RelationType oldRelation, @NotNull RelationType newRelation,
                                 @Nullable UUID actorUuid) {
    this.factionId1 = factionId1;
    this.factionId2 = factionId2;
    this.oldRelation = oldRelation;
    this.newRelation = newRelation;
    this.actorUuid = actorUuid;
  }

  @NotNull public UUID factionId1() { return factionId1; }
  @NotNull public UUID factionId2() { return factionId2; }
  @NotNull public RelationType oldRelation() { return oldRelation; }
  @NotNull public RelationType newRelation() { return newRelation; }
  @Nullable public UUID actorUuid() { return actorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
