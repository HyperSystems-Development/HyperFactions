package com.hyperfactions.importer.hyfactions;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the Created/Modified tracker from HyFactions.
 * Used for tracking creation and modification times with user attribution.
 *
 * @param UserUUID the UUID of the user who performed the action
 * @param UserName the username at the time of the action
 * @param Date     the ISO 8601 timestamp (e.g., "2026-01-22T22:17:02.563322051")
 */
public record HyFactionTracker(
  @Nullable String UserUUID,
  @Nullable String UserName,
  @Nullable String Date
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
    try {
      // Handle nanosecond precision timestamps
      Instant instant = Instant.parse(Date.replace(" ", "T") + (Date.contains("Z") ? "" : "Z"));
      return instant.toEpochMilli();
    } catch (DateTimeParseException e) {
      try {
        // Try parsing without Z suffix (local time format)
        String normalized = Date;
        // Truncate nanoseconds to milliseconds if needed
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0 && normalized.length() > dotIndex + 4) {
          normalized = normalized.substring(0, dotIndex + 4);
        }
        Instant instant = DateTimeFormatter.ISO_LOCAL_DATE_TIME
          .parse(normalized, Instant::from);
        return instant.toEpochMilli();
      } catch (Exception e2) {
        // Fallback: current time
        return System.currentTimeMillis();
      }
    }
  }
}
