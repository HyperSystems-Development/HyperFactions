package com.hyperfactions.migration;

import com.hyperfactions.backup.BackupMetadata;
import com.hyperfactions.backup.BackupType;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Executes migrations with backup and rollback support.
 *
 * <p>
 * The runner handles:
 * - Discovering applicable migrations via {@link MigrationRegistry}
 * - Creating timestamped backups before each migration
 * - Executing migrations in order (chaining v1 -> v2 -> v3)
 * - Rolling back on failure by restoring from backup
 * - Reporting progress via callbacks
 */
public class MigrationRunner {

  private final Path dataDir;

  private final MigrationOptions.ProgressCallback progressCallback;

  /**
   * Creates a new migration runner.
   *
   * @param dataDir the plugin data directory
   */
  public MigrationRunner(@NotNull Path dataDir) {
    this(dataDir, null);
  }

  /**
   * Creates a new migration runner with progress callback.
   *
   * @param dataDir          the plugin data directory
   * @param progressCallback callback for progress updates
   */
  public MigrationRunner(@NotNull Path dataDir, @Nullable MigrationOptions.ProgressCallback progressCallback) {
    this.dataDir = dataDir;
    this.progressCallback = progressCallback;
  }

  /**
   * Runs all pending migrations of a specific type.
   *
   * @param type the migration type to run
   * @return list of migration results
   */
  @NotNull
  public List<MigrationResult> runPendingMigrations(@NotNull MigrationType type) {
    List<Migration> migrations = MigrationRegistry.get().buildMigrationChain(type, dataDir);

    if (migrations.isEmpty()) {
      Logger.debug("[Migration] No pending %s migrations", type);
      return List.of();
    }

    Logger.info("[Migration] Found %d pending %s migration(s)", migrations.size(), type);

    List<MigrationResult> results = new ArrayList<>();
    for (Migration migration : migrations) {
      MigrationResult result = runMigration(migration);
      results.add(result);

      if (!result.success()) {
        ErrorHandler.report(String.format("[Migration] Migration '%s' failed: %s", migration.id(), result.errorMessage()), (Exception) null);
        if (result.rolledBack()) {
          Logger.info("[Migration] Successfully rolled back to backup");
        }

        // Stop running further migrations after failure
        break;
      }

      Logger.info("[Migration] Migration '%s' completed successfully in %dms",
          migration.id(), result.duration().toMillis());
    }

    return results;
  }

  /**
   * Runs a single migration with backup and rollback support.
   *
   * @param migration the migration to run
   * @return migration result
   */
  @NotNull
  public MigrationResult runMigration(@NotNull Migration migration) {
    Instant startTime = Instant.now();

    Logger.info("[Migration] Running migration '%s': %s", migration.id(), migration.description());
    Logger.info("[Migration] Upgrading from v%d to v%d", migration.fromVersion(), migration.toVersion());

    // Create backup
    Path backupPath = null;
    try {
      backupPath = createBackup(migration);
      Logger.info("[Migration] Created backup at: %s", backupPath);
    } catch (IOException e) {
      Duration duration = Duration.between(startTime, Instant.now());
      ErrorHandler.report("[Migration] Failed to create backup", e);
      return MigrationResult.failure(
        migration.id(),
        migration.fromVersion(),
        migration.toVersion(),
        null,
        "Failed to create backup: " + e.getMessage(),
        false,
        duration
      );
    }

    // Execute migration
    try {
      MigrationOptions options = progressCallback != null
          ? MigrationOptions.withProgress(backupPath, progressCallback)
          : MigrationOptions.defaults(backupPath);

      MigrationResult result = migration.execute(dataDir, options);

      if (!result.success() && backupPath != null) {
        // Rollback on failure
        try {
          rollback(migration, backupPath);
          Duration duration = Duration.between(startTime, Instant.now());
          return MigrationResult.failure(
            migration.id(),
            migration.fromVersion(),
            migration.toVersion(),
            backupPath,
            result.errorMessage() != null ? result.errorMessage() : "Migration failed",
            true,
            duration
          );
        } catch (IOException rollbackError) {
          ErrorHandler.report("[Migration] Rollback failed", rollbackError);
          Duration duration = Duration.between(startTime, Instant.now());
          return MigrationResult.failure(
            migration.id(),
            migration.fromVersion(),
            migration.toVersion(),
            backupPath,
            "Migration failed and rollback also failed: " + rollbackError.getMessage(),
            false,
            duration
          );
        }
      }

      return result;

    } catch (Exception e) {
      ErrorHandler.report("[Migration] Migration threw exception", e);

      // Attempt rollback
      boolean rolledBack = false;
      if (backupPath != null) {
        try {
          rollback(migration, backupPath);
          rolledBack = true;
        } catch (IOException rollbackError) {
          ErrorHandler.report("[Migration] Rollback failed", rollbackError);
        }
      }

      Duration duration = Duration.between(startTime, Instant.now());
      return MigrationResult.failure(
        migration.id(),
        migration.fromVersion(),
        migration.toVersion(),
        backupPath,
        e.getMessage(),
        rolledBack,
        duration
      );
    }
  }

  /**
   * Creates a backup before running a migration.
   * Uses the same ZIP format as the normal backup system for consistency.
   *
   * <p>Backup naming: backup_migration_v{fromVersion}-to-v{toVersion}_YYYY-MM-DD_HH-mm-ss.zip
   * Location: backups/ directory
   *
   * @param migration the migration about to run
   * @return path to the backup ZIP file
   * @throws IOException if backup fails
   */
  @NotNull
  private Path createBackup(@NotNull Migration migration) throws IOException {
    // Create backups directory if it doesn't exist
    Path backupsDir = dataDir.resolve("backups");
    Files.createDirectories(backupsDir);

    // Generate backup filename with version info
    Instant timestamp = Instant.now();
    String versionSuffix = "v" + migration.fromVersion() + "-to-v" + migration.toVersion();
    String baseName = BackupMetadata.generateName(BackupType.MIGRATION, timestamp);
    // Insert version info before the timestamp: backup_migration_v2-to-v3_2024-01-15_12-30-45
    String name = baseName.replace("backup_migration_", "backup_migration_" + versionSuffix + "_");
    Path backupFile = backupsDir.resolve(name + ".zip");

    // Create ZIP file with relevant content based on migration type
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile.toFile()))) {
      switch (migration.type()) {
        case CONFIG -> {
          // Backup config.json
          Path configFile = dataDir.resolve("config.json");
          if (Files.exists(configFile)) {
            addFileToZip(zos, configFile, "config.json");
          }

          // Backup config/ directory
          Path configDir = dataDir.resolve("config");
          if (Files.exists(configDir) && Files.isDirectory(configDir)) {
            addDirectoryToZip(zos, configDir, "config");
          }
        }
        case DATA -> {
          // Backup factions/
          Path factionsDir = dataDir.resolve("factions");
          if (Files.exists(factionsDir)) {
            addDirectoryToZip(zos, factionsDir, "factions");
          }

          // Backup players/
          Path playersDir = dataDir.resolve("players");
          if (Files.exists(playersDir)) {
            addDirectoryToZip(zos, playersDir, "players");
          }

          // Backup chat/
          Path chatDir = dataDir.resolve("chat");
          if (Files.exists(chatDir)) {
            addDirectoryToZip(zos, chatDir, "chat");
          }

          // Backup economy/
          Path economyDir = dataDir.resolve("economy");
          if (Files.exists(economyDir)) {
            addDirectoryToZip(zos, economyDir, "economy");
          }

          // Backup zones.json
          Path zonesFile = dataDir.resolve("zones.json");
          if (Files.exists(zonesFile)) {
            addFileToZip(zos, zonesFile, "zones.json");
          }

          // Backup invites.json
          Path invitesFile = dataDir.resolve("invites.json");
          if (Files.exists(invitesFile)) {
            addFileToZip(zos, invitesFile, "invites.json");
          }

          // Backup join_requests.json
          Path joinRequestsFile = dataDir.resolve("join_requests.json");
          if (Files.exists(joinRequestsFile)) {
            addFileToZip(zos, joinRequestsFile, "join_requests.json");
          }
        }
        case SCHEMA -> {
          // For schema migrations, include any database-related files
          Logger.warn("[Migration] Schema migration backup not fully implemented");
        }
        default -> throw new IllegalStateException("Unexpected value");
      }
    }

    Logger.info("[Migration] Created ZIP backup: %s", backupFile.getFileName());
    return backupFile;
  }

  /**
   * Adds a single file to a ZIP output stream.
   */
  private void addFileToZip(@NotNull ZipOutputStream zos, @NotNull Path file, @NotNull String entryName)
      throws IOException {
    zos.putNextEntry(new ZipEntry(entryName));
    Files.copy(file, zos);
    zos.closeEntry();
  }

  /**
   * Adds a directory recursively to a ZIP output stream.
   */
  private void addDirectoryToZip(@NotNull ZipOutputStream zos, @NotNull Path dir, @NotNull String zipPath)
      throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      /** Visit File. */
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String entryName = zipPath + "/" + dir.relativize(file).toString().replace("\\", "/");
        addFileToZip(zos, file, entryName);
        return FileVisitResult.CONTINUE;
      }

      /** Pre Visit Directory. */
      @Override
      public FileVisitResult preVisitDirectory(Path dir2, BasicFileAttributes attrs) throws IOException {
        String entryName = zipPath + "/" + dir.relativize(dir2).toString().replace("\\", "/") + "/";
        if (!entryName.equals(zipPath + "//")) {
          zos.putNextEntry(new ZipEntry(entryName));
          zos.closeEntry();
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Rolls back a failed migration by restoring from the ZIP backup.
   *
   * @param migration  the migration that failed
   * @param backupPath path to the ZIP backup
   * @throws IOException if rollback fails
   */
  private void rollback(@NotNull Migration migration, @NotNull Path backupPath) throws IOException {
    Logger.info("[Migration] Rolling back migration '%s' from backup: %s", migration.id(), backupPath.getFileName());

    if (!Files.exists(backupPath) || !backupPath.toString().endsWith(".zip")) {
      throw new IOException("Backup file not found or invalid: " + backupPath);
    }

    // Extract ZIP contents back to data directory
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupPath))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        Path targetPath = dataDir.resolve(entry.getName());

        if (entry.isDirectory()) {
          Files.createDirectories(targetPath);
        } else {
          // Ensure parent directory exists
          Files.createDirectories(targetPath.getParent());
          Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        zis.closeEntry();
      }
    }

    Logger.info("[Migration] Rollback complete - restored from backup");
  }

  /**
   * Convenience method to run pending config migrations.
   *
   * @param dataDir the plugin data directory
   * @return list of migration results
   */
  @NotNull
  public static List<MigrationResult> runPendingMigrations(@NotNull Path dataDir, @NotNull MigrationType type) {
    return new MigrationRunner(dataDir).runPendingMigrations(type);
  }
}
