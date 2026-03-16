package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published when a faction invite is created, accepted, or declined.
 *
 * @param factionId  the faction
 * @param playerUuid the invited player
 * @param invitedBy  the player who sent the invite
 * @param type       what happened
 */
public record FactionInviteEvent(
    @NotNull UUID factionId,
    @NotNull UUID playerUuid,
    @NotNull UUID invitedBy,
    @NotNull Type type
) {
  public enum Type {
    /** Invite was sent */
    CREATED,
    /** Invite was accepted (player joining handled by FactionMemberEvent) */
    ACCEPTED,
    /** Invite was declined */
    DECLINED,
    /** Invite expired */
    EXPIRED
  }
}
