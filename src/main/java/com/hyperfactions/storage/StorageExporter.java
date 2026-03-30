package com.hyperfactions.storage;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for exporting/importing storage data to/from JSON files.
 *
 * <p>Used by the backup system to create portable backups regardless of
 * the active storage backend. JSON backups are always in the same format,
 * making them portable between backends.</p>
 */
public interface StorageExporter {

  /**
   * Result of an export operation.
   */
  record ExportResult(int factions, int players, int zones, int economies,
            int chatHistories, int invites, int joinRequests) {
    public int total() {
      return factions + players + zones + economies + chatHistories + invites + joinRequests;
    }
  }

  /**
   * Result of an import operation.
   */
  record ImportResult(int factions, int players, int zones, int economies,
            int chatHistories, int invites, int joinRequests) {
    public int total() {
      return factions + players + zones + economies + chatHistories + invites + joinRequests;
    }
  }

  /**
   * Exports all data to JSON files in the given directory.
   * The output follows the same directory structure as JSON file storage:
   * factions/{uuid}.json, players/{uuid}.json, zones.json, etc.
   *
   * @param targetDir the directory to export to
   * @return a future containing the export result
   */
  CompletableFuture<ExportResult> exportToJson(@NotNull Path targetDir);

  /**
   * Imports all data from JSON files in the given directory,
   * replacing current data in the active storage backend.
   *
   * @param sourceDir the directory to import from
   * @return a future containing the import result
   */
  CompletableFuture<ImportResult> importFromJson(@NotNull Path sourceDir);

  /**
   * Returns true if this exporter supports data export (SQL backends do,
   * JSON backend delegates to the file system directly).
   */
  boolean requiresExport();
}
