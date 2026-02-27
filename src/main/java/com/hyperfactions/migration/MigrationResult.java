package com.hyperfactions.migration;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a migration execution.
 *
 * @param success        true if migration completed successfully
 * @param migrationId    unique identifier of the migration
 * @param fromVersion    source version before migration
 * @param toVersion      target version after migration
 * @param backupPath     path to the backup created before migration
 * @param filesCreated   list of files created during migration
 * @param filesModified  list of files modified during migration
 * @param warnings       list of non-fatal warnings encountered
 * @param errorMessage   error message if migration failed
 * @param rolledBack     true if migration was rolled back after failure
 * @param duration       time taken to execute the migration
 */
public record MigrationResult(
  boolean success,
  @NotNull String migrationId,
  int fromVersion,
  int toVersion,
  @Nullable Path backupPath,
  @NotNull List<String> filesCreated,
  @NotNull List<String> filesModified,
  @NotNull List<String> warnings,
  @Nullable String errorMessage,
  boolean rolledBack,
  @NotNull Duration duration
) {
  /**
   * Creates a successful migration result.
   *
   * @param migrationId   unique identifier of the migration
   * @param fromVersion   source version
   * @param toVersion     target version
   * @param backupPath    path to backup
   * @param filesCreated  files created
   * @param filesModified files modified
   * @param warnings      any warnings
   * @param duration      time taken
   * @return successful result
   */
  public static MigrationResult success(
      @NotNull String migrationId,
      int fromVersion,
      int toVersion,
      @Nullable Path backupPath,
      @NotNull List<String> filesCreated,
      @NotNull List<String> filesModified,
      @NotNull List<String> warnings,
      @NotNull Duration duration) {
    return new MigrationResult(
      true, migrationId, fromVersion, toVersion, backupPath,
      filesCreated, filesModified, warnings, null, false, duration
    );
  }

  /**
   * Creates a failed migration result.
   *
   * @param migrationId  unique identifier of the migration
   * @param fromVersion  source version
   * @param toVersion    target version
   * @param backupPath   path to backup
   * @param errorMessage error message
   * @param rolledBack   true if rollback was performed
   * @param duration     time taken
   * @return failed result
   */
  public static MigrationResult failure(
      @NotNull String migrationId,
      int fromVersion,
      int toVersion,
      @Nullable Path backupPath,
      @NotNull String errorMessage,
      boolean rolledBack,
      @NotNull Duration duration) {
    return new MigrationResult(
      false, migrationId, fromVersion, toVersion, backupPath,
      List.of(), List.of(), List.of(), errorMessage, rolledBack, duration
    );
  }

  /**
   * Creates a skipped migration result (not applicable).
   *
   * @param migrationId unique identifier of the migration
   * @param fromVersion source version
   * @param toVersion   target version
   * @return skipped result
   */
  public static MigrationResult skipped(
      @NotNull String migrationId,
      int fromVersion,
      int toVersion) {
    return new MigrationResult(
      true, migrationId, fromVersion, toVersion, null,
      List.of(), List.of(), List.of("Migration was skipped - not applicable"),
      null, false, Duration.ZERO
    );
  }
}
