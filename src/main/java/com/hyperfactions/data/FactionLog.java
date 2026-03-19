package com.hyperfactions.data;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a log entry for faction activity.
 *
 * <p>Supports i18n via optional {@code messageKey} and {@code messageArgs} fields.
 * When present, display code resolves the key per-locale using HFMessages.
 * The {@code message} field always contains the English fallback text.
 *
 * @param type        the type of log entry
 * @param message     the log message (English fallback, always populated)
 * @param timestamp   when this occurred (epoch millis)
 * @param actorUuid   UUID of the player who performed the action (null for system)
 * @param messageKey  i18n message key for localized display (null for legacy logs)
 * @param messageArgs arguments for the message key placeholders (null if no args)
 */
public record FactionLog(
  @NotNull LogType type,
  @NotNull String message,
  long timestamp,
  @Nullable UUID actorUuid,
  @Nullable String messageKey,
  @Nullable List<String> messageArgs
) {

  /** Backward-compatible constructor for legacy logs (no i18n key). */
  public FactionLog(@NotNull LogType type, @NotNull String message,
                    long timestamp, @Nullable UUID actorUuid) {
    this(type, message, timestamp, actorUuid, null, null);
  }

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
   * @param message   the English fallback message
   * @param actorUuid the actor's UUID
   * @return a new FactionLog
   */
  public static FactionLog create(@NotNull LogType type, @NotNull String message, @Nullable UUID actorUuid) {
    return new FactionLog(type, message, System.currentTimeMillis(), actorUuid, null, null);
  }

  /**
   * Creates a new log entry with i18n support.
   *
   * @param type      the log type
   * @param message   the English fallback message
   * @param actorUuid the actor's UUID
   * @param key       the i18n message key
   * @param args      arguments for the message key placeholders
   * @return a new FactionLog with i18n data
   */
  public static FactionLog create(@NotNull LogType type, @NotNull String message,
                                  @Nullable UUID actorUuid, @NotNull String key, String... args) {
    return new FactionLog(type, message, System.currentTimeMillis(), actorUuid,
        key, args.length > 0 ? List.of(args) : null);
  }

  /**
   * Creates a system log entry (no actor).
   *
   * @param type    the log type
   * @param message the English fallback message
   * @return a new FactionLog with null actor
   */
  public static FactionLog system(@NotNull LogType type, @NotNull String message) {
    return new FactionLog(type, message, System.currentTimeMillis(), null, null, null);
  }

  /**
   * Creates a system log entry with i18n support (no actor).
   *
   * @param type    the log type
   * @param message the English fallback message
   * @param key     the i18n message key
   * @param args    arguments for the message key placeholders
   * @return a new FactionLog with i18n data and null actor
   */
  public static FactionLog system(@NotNull LogType type, @NotNull String message,
                                  @NotNull String key, String... args) {
    return new FactionLog(type, message, System.currentTimeMillis(), null,
        key, args.length > 0 ? List.of(args) : null);
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
