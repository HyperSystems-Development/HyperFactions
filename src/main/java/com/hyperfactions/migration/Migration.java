package com.hyperfactions.migration;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Core interface for all migrations.
 *
 * <p>
 * Migrations handle versioned changes to configuration files, data structures,
 * or database schemas. Each migration specifies its source and target versions
 * and can be chained together for multi-step upgrades.
 */
public interface Migration {

  /**
   * Gets the unique identifier for this migration.
   *
   * <p>
   * Format: "{type}-v{from}-to-v{to}" (e.g., "config-v1-to-v2")
   *
   * @return unique migration identifier
   */
  @NotNull
  String id();

  /**
   * Gets the type of this migration.
   *
   * @return migration type
   */
  @NotNull
  MigrationType type();

  /**
   * Gets the source version this migration upgrades from.
   *
   * @return source version number
   */
  int fromVersion();

  /**
   * Gets the target version this migration upgrades to.
   *
   * @return target version number
   */
  int toVersion();

  /**
   * Gets a human-readable description of what this migration does.
   *
   * @return migration description
   */
  @NotNull
  String description();

  /**
   * Checks if this migration is applicable to the current data directory.
   *
   * <p>
   * This should check if the current version matches {@link #fromVersion()}
   * without modifying any files.
   *
   * @param dataDir the plugin data directory
   * @return true if migration should be executed
   */
  boolean isApplicable(@NotNull Path dataDir);

  /**
   * Executes the migration.
   *
   * <p>
   * The migration runner will handle backup creation before calling this method.
   * If this method throws an exception or returns a failed result, the runner
   * will attempt to roll back using the backup.
   *
   * @param dataDir the plugin data directory
   * @param options migration options
   * @return migration result
   */
  @NotNull
  MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options);
}
