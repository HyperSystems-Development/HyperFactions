package com.hyperfactions.backup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Metadata about a backup file.
 *
 * @param name      the backup name (filename without extension)
 * @param type      the backup type (hourly, daily, weekly, manual)
 * @param timestamp when the backup was created
 * @param size      the backup file size in bytes
 * @param createdBy the UUID of the player who created it (null for auto-backups)
 */
public record BackupMetadata(
  @NotNull String name,
  @NotNull BackupType type,
  @NotNull Instant timestamp,
  long size,
  @Nullable UUID createdBy
) {
  /** Formatter for backup filenames. */
  private static final DateTimeFormatter FILENAME_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm").withZone(ZoneId.systemDefault());

  /** Formatter for display. */
  private static final DateTimeFormatter DISPLAY_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  /**
   * Gets the formatted timestamp for display.
   *
   * @return the formatted timestamp string
   */
  @NotNull
  public String getFormattedTimestamp() {
    return DISPLAY_FORMATTER.format(timestamp);
  }

  /**
   * Gets the human-readable file size.
   *
   * @return formatted size string (e.g., "1.5 MB")
   */
  @NotNull
  public String getFormattedSize() {
    if (size < 1024) {
      return size + " B";
    } else if (size < 1024 * 1024) {
      return String.format("%.1f KB", size / 1024.0);
    } else if (size < 1024 * 1024 * 1024) {
      return String.format("%.1f MB", size / (1024.0 * 1024));
    } else {
      return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
  }

  /**
   * Gets the expected filename for this backup.
   *
   * @return the filename with .zip extension
   */
  @NotNull
  public String getFilename() {
    return name + ".zip";
  }

  /**
   * Checks if this backup was created by a player (manual).
   *
   * @return true if created by a player
   */
  public boolean isManual() {
    return type == BackupType.MANUAL;
  }

  /**
   * Checks if this backup is subject to automatic rotation.
   *
   * @return true if this backup can be auto-deleted during rotation
   */
  public boolean isAutoRotated() {
    return type.isAutoRotated();
  }

  /**
   * Generates a backup name from type and timestamp.
   *
   * @param type      the backup type
   * @param timestamp the creation time
   * @return the generated backup name
   */
  @NotNull
  public static String generateName(@NotNull BackupType type, @NotNull Instant timestamp) {
    return "backup_" + type.getPrefix() + "_" + FILENAME_FORMATTER.format(timestamp);
  }

  /**
   * Generates a manual backup name with a custom suffix and timestamp.
   *
   * @param customName the custom name provided by the user
   * @return the generated backup name
   */
  @NotNull
  public static String generateManualName(@NotNull String customName) {
    // Sanitize the custom name
    String sanitized = customName.replaceAll("[^a-zA-Z0-9_-]", "_");
    if (sanitized.length() > 32) {
      sanitized = sanitized.substring(0, 32);
    }

    // Include timestamp to prevent overwrites
    String timestamp = FILENAME_FORMATTER.format(Instant.now());
    return "backup_manual_" + sanitized + "_" + timestamp;
  }

  /**
   * Parses backup metadata from a filename.
   *
   * @param filename the filename to parse (e.g., "backup_hourly_2024-01-15_12-30.zip")
   * @param size     the file size in bytes
   * @return the parsed metadata, or null if the filename is invalid
   */
  @Nullable
  public static BackupMetadata fromFilename(@NotNull String filename, long size) {
    // Remove .zip extension if present
    String name = filename;
    if (name.endsWith(".zip")) {
      name = name.substring(0, name.length() - 4);
    }

    // Expected format: backup_<type>_<timestamp> or backup_manual_<name>
    if (!name.startsWith("backup_")) {
      return null;
    }

    String remainder = name.substring(7); // Remove "backup_"
    int firstUnderscore = remainder.indexOf('_');
    if (firstUnderscore < 0) {
      return null;
    }

    String typeStr = remainder.substring(0, firstUnderscore);
    BackupType type = BackupType.fromPrefix(typeStr);
    if (type == null) {
      return null;
    }

    String dateOrName = remainder.substring(firstUnderscore + 1);

    // For manual backups, we can't parse a specific timestamp
    Instant timestamp;
    if (type == BackupType.MANUAL) {
      // Use file's last modified time or current time as fallback
      timestamp = Instant.now();
    } else {
      // Parse the timestamp from the filename
      try {
        timestamp = FILENAME_FORMATTER.parse(dateOrName, Instant::from);
      } catch (Exception e) {
        // Invalid timestamp format
        timestamp = Instant.now();
      }
    }

    return new BackupMetadata(name, type, timestamp, size, null);
  }
}
