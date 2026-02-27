package com.hyperfactions.backup;

import org.jetbrains.annotations.NotNull;

/**
 * Types of backups in the GFS (Grandfather-Father-Son) rotation scheme.
 */
public enum BackupType {

  /** Hourly backups (Son) - most recent, pruned first. */
  HOURLY("hourly", "Hourly"),

  /** Daily backups (Father) - created at midnight. */
  DAILY("daily", "Daily"),

  /** Weekly backups (Grandfather) - created on Sunday midnight. */
  WEEKLY("weekly", "Weekly"),

  /** Manual backups - user-created, never auto-deleted. */
  MANUAL("manual", "Manual"),

  /** Migration backups - created before config/data migrations. */
  MIGRATION("migration", "Migration");

  private final String prefix;

  private final String displayName;

  BackupType(@NotNull String prefix, @NotNull String displayName) {
    this.prefix = prefix;
    this.displayName = displayName;
  }

  /**
   * Gets the prefix used in backup filenames.
   *
   * @return the file prefix
   */
  @NotNull
  public String getPrefix() {
    return prefix;
  }

  /**
   * Gets the human-readable display name.
   *
   * @return the display name
   */
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this backup type is subject to automatic rotation/pruning.
   *
   * @return true if backups of this type can be auto-deleted
   */
  public boolean isAutoRotated() {
    return this != MANUAL && this != MIGRATION;
  }

  /**
   * Parses a BackupType from a prefix string.
   *
   * @param prefix the prefix to parse
   * @return the matching BackupType, or null if not found
   */
  public static BackupType fromPrefix(@NotNull String prefix) {
    for (BackupType type : values()) {
      if (type.prefix.equalsIgnoreCase(prefix)) {
        return type;
      }
    }
    return null;
  }
}
