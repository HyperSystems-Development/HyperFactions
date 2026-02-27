package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an active combat tag on a player.
 *
 * @param playerUuid      the tagged player's UUID
 * @param taggedAt        when the tag was applied (epoch millis)
 * @param durationSeconds the duration of the tag in seconds
 */
public record CombatTag(
  @NotNull UUID playerUuid,
  long taggedAt,
  int durationSeconds
) {
  /**
   * Creates a new combat tag at the current time.
   *
   * @param playerUuid      the player's UUID
   * @param durationSeconds the tag duration
   * @return a new CombatTag
   */
  public static CombatTag create(@NotNull UUID playerUuid, int durationSeconds) {
    return new CombatTag(playerUuid, System.currentTimeMillis(), durationSeconds);
  }

  /**
   * Gets when this tag expires.
   *
   * @return the expiration time in epoch millis
   */
  public long getExpiresAt() {
    return taggedAt + (durationSeconds * 1000L);
  }

  /**
   * Checks if this tag has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return System.currentTimeMillis() >= getExpiresAt();
  }

  /**
   * Gets the remaining time in seconds.
   *
   * @return remaining seconds, 0 if expired
   */
  public int getRemainingSeconds() {
    long remaining = getExpiresAt() - System.currentTimeMillis();
    return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
  }

  /**
   * Creates a refreshed tag (resets the timer).
   *
   * @return a new CombatTag with reset timer
   */
  public CombatTag refresh() {
    return new CombatTag(playerUuid, System.currentTimeMillis(), durationSeconds);
  }

  /**
   * Creates a refreshed tag with a new duration.
   *
   * @param newDuration the new duration in seconds
   * @return a new CombatTag with reset timer and new duration
   */
  public CombatTag refresh(int newDuration) {
    return new CombatTag(playerUuid, System.currentTimeMillis(), newDuration);
  }
}
