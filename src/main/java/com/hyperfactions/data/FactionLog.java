package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a log entry for faction activity.
 *
 * @param type      the type of log entry
 * @param message   the log message
 * @param timestamp when this occurred (epoch millis)
 * @param actorUuid UUID of the player who performed the action (null for system)
 */
public record FactionLog(
  @NotNull LogType type,
  @NotNull String message,
  long timestamp,
  @Nullable UUID actorUuid
) {
  /**
   * Types of faction log entries.
   */
  public enum LogType {
    MEMBER_JOIN("Join"),
    MEMBER_LEAVE("Leave"),
    MEMBER_KICK("Kick"),
    MEMBER_PROMOTE("Promote"),
    MEMBER_DEMOTE("Demote"),
    CLAIM("Claim"),
    UNCLAIM("Unclaim"),
    OVERCLAIM("Overclaim"),
    HOME_SET("Home Set"),
    RELATION_ALLY("Ally"),
    RELATION_ENEMY("Enemy"),
    RELATION_NEUTRAL("Neutral"),
    LEADER_TRANSFER("Transfer"),
    SETTINGS_CHANGE("Settings"),
    POWER_CHANGE("Power"),
    ECONOMY("Economy"),
    ADMIN_POWER("Admin Power");

    private final String displayName;

    LogType(String displayName) {
      this.displayName = displayName;
    }

    /** Returns the display name. */
    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * Creates a new log entry at the current time.
   *
   * @param type      the log type
   * @param message   the message
   * @param actorUuid the actor's UUID
   * @return a new FactionLog
   */
  public static FactionLog create(@NotNull LogType type, @NotNull String message, @Nullable UUID actorUuid) {
    return new FactionLog(type, message, System.currentTimeMillis(), actorUuid);
  }

  /**
   * Creates a system log entry (no actor).
   *
   * @param type    the log type
   * @param message the message
   * @return a new FactionLog with null actor
   */
  public static FactionLog system(@NotNull LogType type, @NotNull String message) {
    return new FactionLog(type, message, System.currentTimeMillis(), null);
  }

  /**
   * Checks if this is a system log (no actor).
   *
   * @return true if no actor
   */
  public boolean isSystemLog() {
    return actorUuid == null;
  }
}
