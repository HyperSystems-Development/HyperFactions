package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a pending invitation to join a faction.
 *
 * @param factionId the faction extending the invite
 * @param playerUuid the player being invited
 * @param invitedBy  UUID of the player who sent the invite
 * @param createdAt  when the invite was created (epoch millis)
 * @param expiresAt  when the invite expires (epoch millis)
 */
public record PendingInvite(
  @NotNull UUID factionId,
  @NotNull UUID playerUuid,
  @NotNull UUID invitedBy,
  long createdAt,
  long expiresAt
) {
  /**
   * Default invite duration in milliseconds (5 minutes).
   */
  public static final long DEFAULT_DURATION_MS = 5 * 60 * 1000L;

  /**
   * Creates a new invite with default expiration.
   *
   * @param factionId  the faction ID
   * @param playerUuid the invited player's UUID
   * @param invitedBy  UUID of the inviter
   * @return a new PendingInvite
   */
  public static PendingInvite create(@NotNull UUID factionId, @NotNull UUID playerUuid, @NotNull UUID invitedBy) {
    long now = System.currentTimeMillis();
    return new PendingInvite(factionId, playerUuid, invitedBy, now, now + DEFAULT_DURATION_MS);
  }

  /**
   * Creates a new invite with custom expiration.
   *
   * @param factionId   the faction ID
   * @param playerUuid  the invited player's UUID
   * @param invitedBy   UUID of the inviter
   * @param durationMs  the duration in milliseconds
   * @return a new PendingInvite
   */
  public static PendingInvite create(@NotNull UUID factionId, @NotNull UUID playerUuid,
                   @NotNull UUID invitedBy, long durationMs) {
    long now = System.currentTimeMillis();
    return new PendingInvite(factionId, playerUuid, invitedBy, now, now + durationMs);
  }

  /**
   * Checks if this invite has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return System.currentTimeMillis() >= expiresAt;
  }

  /**
   * Gets the remaining time in seconds.
   *
   * @return remaining seconds, 0 if expired
   */
  public int getRemainingSeconds() {
    long remaining = expiresAt - System.currentTimeMillis();
    return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
  }
}
