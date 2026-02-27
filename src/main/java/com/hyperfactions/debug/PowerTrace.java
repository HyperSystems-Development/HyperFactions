package com.hyperfactions.debug;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Debug trace record for power-related operations.
 *
 * @param playerUuid the player UUID
 * @param before     power before operation
 * @param after      power after operation
 * @param max        maximum power
 * @param operation  the operation performed (e.g., "death_penalty", "regen", "manual_set")
 * @param timestamp  when this occurred (epoch millis)
 */
public record PowerTrace(
  @NotNull UUID playerUuid,
  double before,
  double after,
  double max,
  @NotNull String operation,
  long timestamp
) {
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
      .withZone(ZoneId.systemDefault());

  /**
   * Creates a power trace for a death penalty.
   *
   * @param playerUuid the player UUID
   * @param before     power before penalty
   * @param after      power after penalty
   * @param max        maximum power
   * @return a new PowerTrace
   */
  public static PowerTrace deathPenalty(@NotNull UUID playerUuid, double before, double after, double max) {
    return new PowerTrace(playerUuid, before, after, max, "death_penalty", System.currentTimeMillis());
  }

  /**
   * Creates a power trace for regeneration.
   *
   * @param playerUuid the player UUID
   * @param before     power before regen
   * @param after      power after regen
   * @param max        maximum power
   * @return a new PowerTrace
   */
  public static PowerTrace regen(@NotNull UUID playerUuid, double before, double after, double max) {
    return new PowerTrace(playerUuid, before, after, max, "regen", System.currentTimeMillis());
  }

  /**
   * Creates a power trace for a manual set operation.
   *
   * @param playerUuid the player UUID
   * @param before     power before set
   * @param after      power after set
   * @param max        maximum power
   * @return a new PowerTrace
   */
  public static PowerTrace manualSet(@NotNull UUID playerUuid, double before, double after, double max) {
    return new PowerTrace(playerUuid, before, after, max, "manual_set", System.currentTimeMillis());
  }

  /**
   * Gets the power change (positive for gain, negative for loss).
   *
   * @return the change amount
   */
  public double getChange() {
    return after - before;
  }

  /**
   * Gets the power percentage after the operation.
   *
   * @return percentage (0-100)
   */
  public int getPercentAfter() {
    if (max <= 0) {
      return 0;
    }
    return (int) Math.round((after / max) * 100);
  }

  /**
   * Returns a compact string representation.
   *
   * @return compact string
   */
  @Override
  public String toString() {
    String change = getChange() >= 0 ? "+" + String.format("%.2f", getChange()) : String.format("%.2f", getChange());
    return String.format("[%s] %s: %.2f -> %.2f (%s) [%d%%]",
        operation, playerUuid.toString().substring(0, 8),
        before, after, change, getPercentAfter());
  }

  /**
   * Returns a verbose string with all details.
   *
   * @return verbose string
   */
  public String toVerboseString() {
    String time = TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    String change = getChange() >= 0 ? "+" + String.format("%.2f", getChange()) : String.format("%.2f", getChange());
    return String.format("[%s] PowerTrace { player=%s, operation=%s, before=%.2f, after=%.2f, max=%.2f, change=%s, percent=%d%% }",
        time, playerUuid, operation, before, after, max, change, getPercentAfter());
  }
}
