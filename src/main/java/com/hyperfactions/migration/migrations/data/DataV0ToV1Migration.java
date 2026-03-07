package com.hyperfactions.migration.migrations.data;

import com.hyperfactions.migration.Migration;
import com.hyperfactions.migration.MigrationOptions;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationType;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Migrates data files from the plugin root directory into a {@code data/} subdirectory.
 *
 * <p>
 * Before this migration, all data files lived directly under the plugin data directory
 * alongside config and backup files. After migration, all data files are organized
 * under {@code data/}.
 *
 * <p>
 * <strong>Moves:</strong>
 * <pre>
 * factions/           → data/factions/
 * players/            → data/players/
 * chat/               → data/chat/
 * economy/            → data/economy/
 * zones.json          → data/zones.json
 * invites.json        → data/invites.json
 * join_requests.json  → data/join_requests.json
 * </pre>
 *
 * <p>
 * <strong>Safety:</strong> The {@code data/.version} marker is written last. If a crash
 * occurs before it's written, the migration re-runs on next startup. The MigrationRunner
 * creates a ZIP backup before execution, so rollback is possible on failure.
 */
public class DataV0ToV1Migration implements Migration {

  /** Items to migrate: directories. */
  private static final String[] DIRS = {"factions", "players", "chat", "economy"};

  /** Items to migrate: individual files. */
  private static final String[] FILES = {"zones.json", "invites.json", "join_requests.json"};

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "data-v0-to-v1";
  }

  /** Type. */
  @Override
  @NotNull
  public MigrationType type() {
    return MigrationType.DATA;
  }

  /** Creates from version. */
  @Override
  public int fromVersion() {
    return 0;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 1;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Move data files into data/ subdirectory for cleaner filesystem layout";
  }

  @Override
  public boolean isApplicable(@NotNull Path dataDir) {
    // Already migrated if data/.version exists
    Path versionFile = dataDir.resolve("data/.version");
    if (Files.exists(versionFile)) {
      return false;
    }

    // Check if any old-path data exists (at least one of the primary items)
    return Files.exists(dataDir.resolve("factions"))
      || Files.exists(dataDir.resolve("players"))
      || Files.exists(dataDir.resolve("zones.json"));
  }

  /** Executes the command. */
  @Override
  @NotNull
  public MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options) {
    Instant startTime = Instant.now();
    List<String> filesCreated = new ArrayList<>();
    List<String> filesModified = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Path dataPath = dataDir.resolve("data");
    int totalSteps = DIRS.length + FILES.length + 2; // +1 for create dir, +1 for version marker
    int step = 0;

    try {
      // Step 1: Create data/ directory
      options.reportProgress("Creating data/ directory", ++step, totalSteps);
      Files.createDirectories(dataPath);
      filesCreated.add("data/");

      // Step 2-5: Move directories
      for (String dirName : DIRS) {
        options.reportProgress("Moving " + dirName + "/", ++step, totalSteps);
        Path source = dataDir.resolve(dirName);
        Path target = dataPath.resolve(dirName);

        if (Files.exists(source) && Files.isDirectory(source)) {
          moveItem(source, target);
          filesModified.add(dirName + "/ → data/" + dirName + "/");
          Logger.info("[Migration] Moved %s/ → data/%s/", dirName, dirName);

          // Also move any .bak files alongside data files in the directory
          moveBakFiles(dataDir, dataPath, dirName);
        }
      }

      // Step 6-8: Move individual files
      for (String fileName : FILES) {
        options.reportProgress("Moving " + fileName, ++step, totalSteps);
        Path source = dataDir.resolve(fileName);
        Path target = dataPath.resolve(fileName);

        if (Files.exists(source)) {
          moveItem(source, target);
          filesModified.add(fileName + " → data/" + fileName);
          Logger.info("[Migration] Moved %s → data/%s", fileName, fileName);

          // Move .bak file if it exists
          Path bakSource = dataDir.resolve(fileName + ".bak");
          if (Files.exists(bakSource)) {
            moveItem(bakSource, dataPath.resolve(fileName + ".bak"));
          }
        }
      }

      // Step 9: Write version marker (LAST — ensures incomplete migration re-runs)
      options.reportProgress("Writing version marker", ++step, totalSteps);
      Path versionFile = dataPath.resolve(".version");
      Files.writeString(versionFile, "1");
      filesCreated.add("data/.version");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Data migration v0→v1 completed in %dms — moved %d items into data/",
          duration.toMillis(), filesModified.size());

      return MigrationResult.success(
        id(), fromVersion(), toVersion(), options.backupPath(),
        filesCreated, filesModified, warnings, duration
      );

    } catch (Exception e) {
      Duration duration = Duration.between(startTime, Instant.now());
      ErrorHandler.report("[Migration] Data migration v0→v1 failed", e);
      return MigrationResult.failure(
        id(), fromVersion(), toVersion(), options.backupPath(),
        e.getMessage(), false, duration
      );
    }
  }

  /**
   * Moves a file or directory, trying atomic move first then falling back to copy+delete.
   */
  private void moveItem(@NotNull Path source, @NotNull Path target) throws IOException {
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      // Fallback: copy then delete
      if (Files.isDirectory(source)) {
        copyDirectory(source, target);
        deleteDirectory(source);
      } else {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(source);
      }
    }
  }

  /**
   * Moves .bak files that correspond to files within a data directory.
   * These live alongside their .json files in directories like factions/ and players/.
   */
  private void moveBakFiles(@NotNull Path dataDir, @NotNull Path dataPath, @NotNull String dirName)
      throws IOException {
    Path sourceDir = dataDir.resolve(dirName);
    Path targetDir = dataPath.resolve(dirName);

    // The directory was already moved, but check for stray .bak files in the parent
    // (some storage implementations put .bak files next to the directory rather than inside)
    // This is a no-op if the directory was moved atomically since .bak files inside move with it
    if (!Files.exists(sourceDir)) {
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "*.bak")) {
      for (Path bakFile : stream) {
        Path target = targetDir.resolve(bakFile.getFileName());
        moveItem(bakFile, target);
      }
    }
  }

  /**
   * Recursively copies a directory.
   */
  private void copyDirectory(@NotNull Path source, @NotNull Path target) throws IOException {
    Files.walkFileTree(source, new SimpleFileVisitor<>() {
      /** Pre Visit Directory. */
      @Override
      public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs)
          throws IOException {
        Files.createDirectories(target.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
          throws IOException {
        Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Recursively deletes a directory.
   */
  private void deleteDirectory(@NotNull Path dir) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      /** Visit File. */
      @Override
      public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
          throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      /** Post Visit Directory. */
      @Override
      public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
        Files.delete(d);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
