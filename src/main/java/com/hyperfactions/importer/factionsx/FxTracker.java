package com.hyperfactions.importer.factionsx;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for FactionsX tracker objects (CreatedTracker / ModifiedTracker).
 * Date is a LocalDateTime ISO-8601 string (e.g. "2026-01-22T22:17:02.563322051").
 */
public record FxTracker(
  @Nullable String UserUUID,
  @Nullable String UserName,
  @Nullable String Date
) {

  /**
   * Parses the ISO-8601 date string to epoch milliseconds.
   * Falls back to current time if parsing fails.
   */
  public long toEpochMillis() {
    if (Date == null || Date.isEmpty()) {
      return System.currentTimeMillis();
    }

    try {
      // FactionsX uses LocalDateTime.now().toString() which produces ISO-8601 without zone
      LocalDateTime ldt = LocalDateTime.parse(Date, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    } catch (DateTimeParseException e) {
      // Try ISO instant format as fallback
      try {
        return Instant.parse(Date).toEpochMilli();
      } catch (DateTimeParseException e2) {
        return System.currentTimeMillis();
      }
    }
  }
}
