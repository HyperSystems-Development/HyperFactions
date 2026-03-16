package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a join request is created, accepted, or declined.
 *
 * @param factionId  the faction
 * @param playerUuid the requesting player
 * @param type       what happened
 * @param message    the request message (only for CREATED, null otherwise)
 */
public record FactionJoinRequestEvent(
    @NotNull UUID factionId,
    @NotNull UUID playerUuid,
    @NotNull Type type,
    @Nullable String message
) {
  public enum Type {
    /** Request was submitted */
    CREATED,
    /** Request was accepted (player joining handled by FactionMemberEvent) */
    ACCEPTED,
    /** Request was declined */
    DECLINED,
    /** Request expired */
    EXPIRED
  }
}
