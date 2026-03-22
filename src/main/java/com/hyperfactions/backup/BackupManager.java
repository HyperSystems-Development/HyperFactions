package com.hyperfactions.backup;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.storage.StorageUtils;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages backup creation, restoration, and GFS (Grandfather-Father-Son) rotation.
 *
 * <p>Backup schedule:
 * - Hourly (Son): Every hour, keep last N hours (default: 24)
 * - Daily (Father): At midnight, keep last N days (default: 7)
 * - Weekly (Grandfather): Sunday midnight, keep last N weeks (default: 4)
 * - Manual: User-created, never auto-deleted
 *
 * <p>Backup contents:
 * - data/factions/ directory
 * - data/players/ directory
 * - data/zones.json
 * - config.json (core configuration)
 * - config/ directory (module configurations: backup.json, chat.json, debug.json, economy.json, faction-permissions.json)
 */
public class BackupManager {

  /** Result of a backup operation. */
  public sealed interface BackupResult permits BackupResult.Success, BackupResult.Failure {
    record Success(@NotNull BackupMetadata metadata, @NotNull Path file) implements BackupResult {}
    record Failure(@NotNull String error) implements BackupResult {}
  }

  /** Result of a restore operation. */
  public sealed interface RestoreResult permits RestoreResult.Success, RestoreResult.Failure {
    record Success(@NotNull String backupName, int filesRestored) implements RestoreResult {}
    record Failure(@NotNull String error) implements RestoreResult {}
  }

  private final Path dataDir;

  private final Path backupsDir;

  private final HyperFactions hyperFactions;

  private volatile int scheduledTaskId = -1;

  private volatile boolean initialized = false;

  private volatile boolean backupInProgress = false;

  private final Object backupLock = new Object();

  /**
   * Creates a new BackupManager.
   *
   * @param dataDir       the plugin data directory
   * @param hyperFactions the main plugin instance
   */
  public BackupManager(@NotNull Path dataDir, @NotNull HyperFactions hyperFactions) {
    this.dataDir = dataDir;
    this.backupsDir = dataDir.resolve("backups");
    this.hyperFactions = hyperFactions;
  }

  /**
   * Initializes the backup manager (creates directories).
   * Call startScheduledBackups() separately after the task scheduler is available.
   */
  public void init() {
    try {
      Files.createDirectories(backupsDir);
      initialized = true;
      Logger.info("[Backup] Initialized, backup directory: %s", backupsDir);
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to create backups directory", e);
    }
  }

  /**
   * Starts scheduled backups. Must be called after the task scheduler is set up.
   * This is safe to call multiple times - subsequent calls are ignored.
   */
  public synchronized void startScheduledBackups() {
    if (scheduledTaskId > 0) {
      Logger.debug("[Backup] Scheduled backups already running (task ID: %d)", scheduledTaskId);
      return;
    }

    if (!ConfigManager.get().isBackupEnabled()) {
      Logger.info("[Backup] Backups are disabled in config");
      return;
    }

    // Run every hour (20 ticks/sec * 60 sec * 60 min = 72000 ticks)
    int periodTicks = 60 * 60 * 20;
    scheduledTaskId = hyperFactions.scheduleRepeatingTask(periodTicks, periodTicks, this::runScheduledBackup);

    if (scheduledTaskId > 0) {
      Logger.info("[Backup] Scheduled backups enabled (hourly, task ID: %d)", scheduledTaskId);
    } else {
      Logger.warn("[Backup] Failed to schedule backup task - task scheduler may not be available");
    }
  }

  /**
   * Restarts the scheduled backup task with current config settings.
   * Call this after config reload to pick up changes to backup settings.
   */
  public synchronized void restartScheduledBackups() {
    // Cancel existing task
    if (scheduledTaskId > 0) {
      hyperFactions.cancelTask(scheduledTaskId);
      scheduledTaskId = -1;
      Logger.debug("[Backup] Cancelled existing backup task for restart");
    }

    // Start with new config
    startScheduledBackups();
  }

  /**
   * Shuts down the backup manager.
   * Creates a shutdown backup if configured.
   */
  public void shutdown() {
    // Cancel scheduled task first
    if (scheduledTaskId > 0) {
      hyperFactions.cancelTask(scheduledTaskId);
      scheduledTaskId = -1;
    }

    // Wait for any in-progress backup to complete
    synchronized (backupLock) {
      if (backupInProgress) {
        Logger.info("[Backup] Waiting for in-progress backup to complete...");
        // Simple spin-wait with sleep (max ~10 seconds)
        for (int i = 0; i < 20 && backupInProgress; i++) {
          try {
            backupLock.wait(500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    // Create shutdown backup if enabled
    if (ConfigManager.get().isBackupOnShutdown()) {
      Logger.info("[Backup] Creating shutdown backup...");
      createBackup(BackupType.MANUAL, "shutdown", null).join();
      rotateShutdownBackups();
    }
  }

  /**
   * Runs a scheduled backup with automatic type determination.
   * Thread-safe - skips if a backup is already in progress.
   */
  private void runScheduledBackup() {
    synchronized (backupLock) {
      if (backupInProgress) {
        Logger.warn("[Backup] Skipping scheduled backup - another backup is already in progress");
        return;
      }
      backupInProgress = true;
    }

    try {
      BackupType type = determineBackupType();
      Logger.info("[Backup] Running scheduled %s backup", type.getDisplayName().toLowerCase());

      createBackup(type, null, null).thenAccept(result -> {
        try {
          if (result instanceof BackupResult.Success success) {
            Logger.info("[Backup] %s backup created: %s (%s)",
              type.getDisplayName(), success.metadata().name(), success.metadata().getFormattedSize());
            performRotation();
          } else if (result instanceof BackupResult.Failure failure) {
            String msg = String.format("[Backup] %s backup failed: %s", type.getDisplayName(), failure.error());
            ErrorHandler.report(msg, (Exception) null);
          }
        } finally {
          synchronized (backupLock) {
            backupInProgress = false;
            backupLock.notifyAll();
          }
        }
      }).exceptionally(ex -> {
        ErrorHandler.report("[Backup] Backup task failed with exception", ex);
        synchronized (backupLock) {
          backupInProgress = false;
          backupLock.notifyAll();
        }
        return null;
      });
    } catch (Exception e) {
      synchronized (backupLock) {
        backupInProgress = false;
        backupLock.notifyAll();
      }
      ErrorHandler.report("[Backup] Failed to start backup", e);
    }
  }

  /**
   * Determines what type of backup to create based on current time.
   */
  @NotNull
  private BackupType determineBackupType() {
    LocalDateTime now = LocalDateTime.now();

    // Weekly: Sunday at midnight
    if (now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() == 0) {
      return BackupType.WEEKLY;
    }

    // Daily: Midnight (non-Sunday)
    if (now.getHour() == 0) {
      return BackupType.DAILY;
    }

    // All other times: Hourly
    return BackupType.HOURLY;
  }

  /**
   * Creates a backup asynchronously.
   *
   * @param type      the backup type
   * @param customName optional custom name for manual backups
   * @param createdBy  the UUID of the player creating the backup (null for auto)
   * @return a future containing the backup result
   */
  @NotNull
  public CompletableFuture<BackupResult> createBackup(
      @NotNull BackupType type,
      @Nullable String customName,
      @Nullable UUID createdBy) {

    return CompletableFuture.supplyAsync(() -> {
      // Note: pre-backup cleanup of .tmp/.bak files removed — it raced with
      // concurrent writeAtomic() calls, deleting in-flight temp files and
      // causing NoSuchFileException (HYPERFACTIONS-4). The ZIP only includes
      // .json files anyway, and startup init() handles orphan cleanup safely.

      // Generate backup name
      Path dataPath = dataDir.resolve("data");
      Instant timestamp = Instant.now();
      String name;
      if (type == BackupType.MANUAL && customName != null && !customName.isEmpty()) {
        name = BackupMetadata.generateManualName(customName);
      } else {
        name = BackupMetadata.generateName(type, timestamp);
      }

      Path backupFile = backupsDir.resolve(name + ".zip");

      try {
        // Ensure backups directory exists (may have been deleted after init())
        Files.createDirectories(backupsDir);

        // Create ZIP file — data files are under data/ subdirectory
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile.toFile()))) {
          // Add data/factions/ directory
          Path factionsDir = dataPath.resolve("factions");
          if (Files.exists(factionsDir)) {
            addDirectoryToZip(zos, factionsDir, "data/factions");
          }

          // Add data/players/ directory
          Path playersDir = dataPath.resolve("players");
          if (Files.exists(playersDir)) {
            addDirectoryToZip(zos, playersDir, "data/players");
          }

          // Add data/chat/ directory
          Path chatDir = dataPath.resolve("chat");
          if (Files.exists(chatDir)) {
            addDirectoryToZip(zos, chatDir, "data/chat");
          }

          // Add data/economy/ directory
          Path economyDir = dataPath.resolve("economy");
          if (Files.exists(economyDir)) {
            addDirectoryToZip(zos, economyDir, "data/economy");
          }

          // Add data/zones.json
          Path zonesFile = dataPath.resolve("zones.json");
          if (Files.exists(zonesFile)) {
            addFileToZip(zos, zonesFile, "data/zones.json");
          }

          // Add data/invites.json
          Path invitesFile = dataPath.resolve("invites.json");
          if (Files.exists(invitesFile)) {
            addFileToZip(zos, invitesFile, "data/invites.json");
          }

          // Add data/join_requests.json
          Path joinRequestsFile = dataPath.resolve("join_requests.json");
          if (Files.exists(joinRequestsFile)) {
            addFileToZip(zos, joinRequestsFile, "data/join_requests.json");
          }

          // Add config.json (if legacy monolithic config still exists)
          Path configFile = dataDir.resolve("config.json");
          if (Files.exists(configFile)) {
            addFileToZip(zos, configFile, "config.json");
          }

          // Add config/ directory (module configs)
          Path configDir = dataDir.resolve("config");
          if (Files.exists(configDir)) {
            addDirectoryToZip(zos, configDir, "config");
          }
        }

        // Create metadata
        long size = Files.size(backupFile);
        BackupMetadata metadata = new BackupMetadata(name, type, timestamp, size, createdBy);

        return new BackupResult.Success(metadata, backupFile);

      } catch (Exception e) {
        // Delete incomplete backup ZIP
        try {
          Files.deleteIfExists(backupFile);
        } catch (IOException ignored) {}
        ErrorHandler.report("[Backup] Failed to create backup", e);
        return new BackupResult.Failure("Failed to create backup: " + e.getMessage());
      }
    });
  }

  /**
   * Restores from a backup asynchronously.
   *
   * @param backupName the name of the backup to restore
   * @return a future containing the restore result
   */
  @NotNull
  public CompletableFuture<RestoreResult> restoreBackup(@NotNull String backupName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Find the backup file
        Path backupFile = findBackupFile(backupName);
        if (backupFile == null) {
          return new RestoreResult.Failure("Backup not found: " + backupName);
        }

        int filesRestored = 0;

        // Extract ZIP contents
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
          ZipEntry entry;
          while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
              Files.createDirectories(dataDir.resolve(entry.getName()));
            } else {
              Path targetFile = dataDir.resolve(entry.getName());
              Files.createDirectories(targetFile.getParent());
              Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
              filesRestored++;
            }
            zis.closeEntry();
          }
        }

        Logger.info("[Backup] Restored %d files from backup '%s'", filesRestored, backupName);
        return new RestoreResult.Success(backupName, filesRestored);

      } catch (Exception e) {
        ErrorHandler.report("[Backup] Failed to restore backup", e);
        return new RestoreResult.Failure("Failed to restore backup: " + e.getMessage());
      }
    });
  }

  /**
   * Deletes a backup.
   *
   * @param backupName the name of the backup to delete
   * @return true if deleted successfully
   */
  @NotNull
  public CompletableFuture<Boolean> deleteBackup(@NotNull String backupName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Path backupFile = findBackupFile(backupName);
        if (backupFile == null) {
          return false;
        }
        Files.delete(backupFile);
        Logger.info("[Backup] Deleted backup: %s", backupName);
        return true;
      } catch (Exception e) {
        ErrorHandler.report(String.format("[Backup] Failed to delete backup '%s'", backupName), e);
        return false;
      }
    });
  }

  /**
   * Lists all available backups.
   *
   * @return list of backup metadata, sorted by timestamp (newest first)
   */
  @NotNull
  public List<BackupMetadata> listBackups() {
    List<BackupMetadata> backups = new ArrayList<>();

    try {
      if (!Files.exists(backupsDir)) {
        return backups;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "backup_*.zip")) {
        for (Path file : stream) {
          try {
            long size = Files.size(file);
            BackupMetadata metadata = BackupMetadata.fromFilename(file.getFileName().toString(), size);
            if (metadata != null) {
              // Update timestamp from file's actual modification time
              Instant fileTime = Files.getLastModifiedTime(file).toInstant();
              backups.add(new BackupMetadata(
                metadata.name(), metadata.type(), fileTime, size, metadata.createdBy()
              ));
            }
          } catch (Exception e) {
            ErrorHandler.report(String.format("[Backup] Could not read backup metadata for %s", file.getFileName()), e);
          }
        }
      }
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to list backups", e);
    }

    // Sort by timestamp, newest first
    backups.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
    return backups;
  }

  /**
   * Gets backups grouped by type.
   *
   * @return map of backup type to list of backups
   */
  @NotNull
  public Map<BackupType, List<BackupMetadata>> getBackupsGroupedByType() {
    return listBackups().stream()
      .collect(Collectors.groupingBy(BackupMetadata::type));
  }

  /**
   * Performs GFS rotation, pruning old backups according to retention settings.
   */
  public void performRotation() {
    ConfigManager config = ConfigManager.get();
    Map<BackupType, List<BackupMetadata>> grouped = getBackupsGroupedByType();

    // Rotate hourly backups
    rotateBackups(grouped.getOrDefault(BackupType.HOURLY, List.of()),
      config.getBackupHourlyRetention());

    // Rotate daily backups
    rotateBackups(grouped.getOrDefault(BackupType.DAILY, List.of()),
      config.getBackupDailyRetention());

    // Rotate weekly backups
    rotateBackups(grouped.getOrDefault(BackupType.WEEKLY, List.of()),
      config.getBackupWeeklyRetention());

    // Rotate manual backups if retention is configured (0 = keep all)
    int manualRetention = config.getBackupManualRetention();
    if (manualRetention > 0) {
      rotateBackups(grouped.getOrDefault(BackupType.MANUAL, List.of()), manualRetention);
    }
  }

  /**
   * Rotates backups of a specific type, keeping only the most recent N.
   */
  private void rotateBackups(@NotNull List<BackupMetadata> backups, int retentionCount) {
    if (retentionCount <= 0 || backups.size() <= retentionCount) {
      return;
    }

    // Sort by timestamp, newest first
    List<BackupMetadata> sorted = new ArrayList<>(backups);
    sorted.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

    // Delete backups beyond the retention count
    for (int i = retentionCount; i < sorted.size(); i++) {
      BackupMetadata toDelete = sorted.get(i);
      deleteBackup(toDelete.name()).thenAccept(success -> {
        if (success) {
          Logger.debug("[Backup] Rotated out old backup: %s", toDelete.name());
        }
      });
    }
  }

  /**
   * Rotates shutdown backups, keeping only the most recent N.
   * Shutdown backups are manual backups with "shutdown" in the name.
   */
  private void rotateShutdownBackups() {
    int retention = ConfigManager.get().getBackupShutdownRetention();
    if (retention <= 0) {
      return; // 0 = keep all
    }

    try {
      // Find all shutdown backup files
      List<Path> shutdownBackups = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "backup_manual_shutdown_*.zip")) {
        for (Path file : stream) {
          shutdownBackups.add(file);
        }
      }

      if (shutdownBackups.size() <= retention) {
        return;
      }

      // Sort by last modified time, newest first
      shutdownBackups.sort((a, b) -> {
        try {
          return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
        } catch (IOException e) {
          return 0;
        }
      });

      // Delete backups beyond the retention count
      for (int i = retention; i < shutdownBackups.size(); i++) {
        Path toDelete = shutdownBackups.get(i);
        try {
          Files.delete(toDelete);
          Logger.debug("[Backup] Rotated out old shutdown backup: %s", toDelete.getFileName());
        } catch (IOException e) {
          ErrorHandler.report(String.format("[Backup] Failed to delete old shutdown backup %s", toDelete.getFileName()), e);
        }
      }

      int deleted = shutdownBackups.size() - retention;
      Logger.info("[Backup] Cleaned up %d old shutdown backup(s), keeping %d", deleted, retention);
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to rotate shutdown backups", e);
    }
  }

  /**
   * Finds a backup file by name.
   */
  @Nullable
  private Path findBackupFile(@NotNull String name) {
    // Try exact match first
    Path exact = backupsDir.resolve(name + ".zip");
    if (Files.exists(exact)) {
      return exact;
    }

    // Try with backup_ prefix
    Path withPrefix = backupsDir.resolve("backup_" + name + ".zip");
    if (Files.exists(withPrefix)) {
      return withPrefix;
    }

    // Search for partial match
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "*" + name + "*.zip")) {
      for (Path file : stream) {
        return file; // Return first match
      }
    } catch (IOException ignored) {}

    return null;
  }

  /**
   * Adds a directory to a ZIP output stream.
   */
  private void addDirectoryToZip(@NotNull ZipOutputStream zos, @NotNull Path dir, @NotNull String zipPath)
      throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      /** Visit File. */
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".tmp") || fileName.endsWith(".bak")) {
          return FileVisitResult.CONTINUE;
        }
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

      /** Skip files that vanish between directory listing and attribute read (concurrent writeAtomic). */
      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        Logger.debug("[Backup] Skipping vanished file during backup: %s", file.getFileName());
        return FileVisitResult.CONTINUE;
      }
    });
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
   * Gets the backups directory path.
   *
   * @return the backups directory
   */
  @NotNull
  public Path getBackupsDir() {
    return backupsDir;
  }
}
