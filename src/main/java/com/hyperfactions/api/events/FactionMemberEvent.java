package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player joins or leaves a faction.
 */
public record FactionMemberEvent(
  @NotNull Faction faction,
  @NotNull UUID playerUuid,
  @NotNull Type type
) {
  /** Type enum. */
  public enum Type {
    JOIN,
    LEAVE,
    KICK,
    PROMOTE,
    DEMOTE
  }
}
