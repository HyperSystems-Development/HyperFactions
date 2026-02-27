package com.hyperfactions.importer.hyfactions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a log entry from HyFactions.
 *
 * @param Action       the action type (CREATE, CLAIM, JOIN, INVITE, SETHOME, etc.)
 * @param UserUUID     the UUID of the user who performed the action
 * @param UserName     the username at the time of the action
 * @param Date         the ISO 8601 timestamp
 * @param TargetPlayer optional target player name (for INVITE actions)
 */
public record HyFactionLog(
  @Nullable String Action,
  @Nullable String UserUUID,
  @Nullable String UserName,
  @Nullable String Date,
  @Nullable String TargetPlayer
) {
  /**
   * Converts the ISO 8601 date string to epoch milliseconds.
   *
   * @return epoch millis, or current time if parsing fails
   */
  public long toEpochMillis() {
    if (Date == null || Date.isEmpty()) {
      return System.currentTimeMillis();
    }

    // Reuse the tracker's parsing logic
    HyFactionTracker tracker = new HyFactionTracker(UserUUID, UserName, Date);
    return tracker.toEpochMillis();
  }
}
