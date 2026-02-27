package com.hyperfactions.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for time formatting and calculations.
 */
public final class TimeUtil {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

  private TimeUtil() {}

  /**
   * Formats a duration in a human-readable way.
   * Examples: "5s", "2m 30s", "1h 15m", "3d 2h"
   *
   * @param millis the duration in milliseconds
   * @return the formatted duration
   */
  @NotNull
  public static String formatDuration(long millis) {
    if (millis < 1000) {
      return "0s";
    }

    long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
    long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
    long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
    long days = TimeUnit.MILLISECONDS.toDays(millis);

    StringBuilder sb = new StringBuilder();
    if (days > 0) {
      sb.append(days).append("d ");
    }
    if (hours > 0) {
      sb.append(hours).append("h ");
    }
    if (minutes > 0 && days == 0) {
      sb.append(minutes).append("m ");
    }
    if (seconds > 0 && hours == 0 && days == 0) {
      sb.append(seconds).append("s");
    }

    return sb.toString().trim();
  }

  /**
   * Formats a duration in seconds.
   *
   * @param seconds the duration in seconds
   * @return the formatted duration
   */
  @NotNull
  public static String formatDurationSeconds(int seconds) {
    return formatDuration(seconds * 1000L);
  }

  /**
   * Formats a timestamp as a relative time.
   * Examples: "just now", "5 minutes ago", "2 hours ago", "3 days ago"
   *
   * @param timestamp the timestamp in epoch milliseconds
   * @return the formatted relative time
   */
  @NotNull
  public static String formatRelative(long timestamp) {
    long diff = System.currentTimeMillis() - timestamp;

    if (diff < 60_000) {
      return "just now";
    } else if (diff < 3600_000) {
      long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
      return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
    } else if (diff < 86400_000) {
      long hours = TimeUnit.MILLISECONDS.toHours(diff);
      return hours + (hours == 1 ? " hour ago" : " hours ago");
    } else if (diff < 604800_000) {
      long days = TimeUnit.MILLISECONDS.toDays(diff);
      return days + (days == 1 ? " day ago" : " days ago");
    } else if (diff < 2592000_000L) {
      long weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7;
      return weeks + (weeks == 1 ? " week ago" : " weeks ago");
    } else {
      return formatDate(timestamp);
    }
  }

  /**
   * Formats a timestamp as a date.
   *
   * @param timestamp the timestamp in epoch milliseconds
   * @return the formatted date
   */
  @NotNull
  public static String formatDate(long timestamp) {
    LocalDateTime dateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(timestamp),
      ZoneId.systemDefault()
    );
    return dateTime.format(DATE_FORMAT);
  }

  /**
   * Formats a timestamp as date and time.
   *
   * @param timestamp the timestamp in epoch milliseconds
   * @return the formatted datetime
   */
  @NotNull
  public static String formatDateTime(long timestamp) {
    LocalDateTime dateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(timestamp),
      ZoneId.systemDefault()
    );
    return dateTime.format(DATETIME_FORMAT);
  }

  /**
   * Gets the number of days since a timestamp.
   *
   * @param timestamp the timestamp in epoch milliseconds
   * @return the number of days
   */
  public static long daysSince(long timestamp) {
    return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp);
  }

  /**
   * Gets the number of minutes since a timestamp.
   *
   * @param timestamp the timestamp in epoch milliseconds
   * @return the number of minutes
   */
  public static long minutesSince(long timestamp) {
    return TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - timestamp);
  }

  /**
   * Checks if a timestamp is within the given number of minutes.
   *
   * @param timestamp the timestamp to check
   * @param minutes   the time window in minutes
   * @return true if within the time window
   */
  public static boolean isWithinMinutes(long timestamp, int minutes) {
    return minutesSince(timestamp) < minutes;
  }

  /**
   * Converts ticks to milliseconds.
   *
   * @param ticks the number of ticks
   * @return milliseconds (assuming 20 ticks per second)
   */
  public static long ticksToMillis(int ticks) {
    return ticks * 50L;
  }

  /**
   * Converts milliseconds to ticks.
   *
   * @param millis the milliseconds
   * @return ticks (assuming 20 ticks per second)
   */
  public static int millisToTicks(long millis) {
    return (int) (millis / 50);
  }

  /**
   * Converts seconds to ticks.
   *
   * @param seconds the seconds
   * @return ticks
   */
  public static int secondsToTicks(int seconds) {
    return seconds * 20;
  }
}
