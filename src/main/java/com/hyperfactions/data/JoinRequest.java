package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a player's request to join a closed faction.
 *
 * @param factionId  the faction the player wants to join
 * @param playerUuid the player requesting to join
 * @param playerName cached player name for display
 * @param message    optional intro message from the player
 * @param createdAt  when the request was created (epoch millis)
 * @param expiresAt  when the request expires (epoch millis)
 */
public record JoinRequest(
  @NotNull UUID factionId,
  @NotNull UUID playerUuid,
  @NotNull String playerName,
  @Nullable String message,
  long createdAt,
  long expiresAt
) {
  /**
   * Default request duration in milliseconds (24 hours).
   */
  public static final long DEFAULT_DURATION_MS = 24 * 60 * 60 * 1000L;

  /**
   * Creates a new join request with default expiration (24 hours).
   *
   * @param factionId  the faction ID
   * @param playerUuid the player's UUID
   * @param playerName the player's name
   * @param message    optional intro message
   * @return a new JoinRequest
   */
  public static JoinRequest create(@NotNull UUID factionId, @NotNull UUID playerUuid,
                   @NotNull String playerName, @Nullable String message) {
    long now = System.currentTimeMillis();
    return new JoinRequest(factionId, playerUuid, playerName, message, now, now + DEFAULT_DURATION_MS);
  }

  /**
   * Creates a new join request with custom expiration.
   *
   * @param factionId   the faction ID
   * @param playerUuid  the player's UUID
   * @param playerName  the player's name
   * @param message     optional intro message
   * @param durationMs  the duration in milliseconds
   * @return a new JoinRequest
   */
  public static JoinRequest create(@NotNull UUID factionId, @NotNull UUID playerUuid,
                   @NotNull String playerName, @Nullable String message,
                   long durationMs) {
    long now = System.currentTimeMillis();
    return new JoinRequest(factionId, playerUuid, playerName, message, now, now + durationMs);
  }

  /**
   * Checks if this request has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return System.currentTimeMillis() >= expiresAt;
  }

  /**
   * Gets the remaining time in hours.
   *
   * @return remaining hours, 0 if expired
   */
  public int getRemainingHours() {
    long remaining = expiresAt - System.currentTimeMillis();
    return remaining > 0 ? (int) Math.ceil(remaining / (60.0 * 60.0 * 1000.0)) : 0;
  }

  /**
   * Gets the remaining time in minutes.
   *
   * @return remaining minutes, 0 if expired
   */
  public int getRemainingMinutes() {
    long remaining = expiresAt - System.currentTimeMillis();
    return remaining > 0 ? (int) Math.ceil(remaining / (60.0 * 1000.0)) : 0;
  }
}
