package com.hyperfactions.storage.json;

import com.hyperfactions.storage.StorageExporter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * JSON storage exporter — no-op because data is already in JSON files.
 * The backup system zips the data/ directory directly.
 */
public class JsonStorageExporter implements StorageExporter {

  @Override
  public CompletableFuture<ExportResult> exportToJson(@NotNull Path targetDir) {
    // No-op: JSON data is already in files, backup system zips them directly
    return CompletableFuture.completedFuture(new ExportResult(0, 0, 0, 0, 0, 0, 0));
  }

  @Override
  public CompletableFuture<ImportResult> importFromJson(@NotNull Path sourceDir) {
    // No-op: JSON restore extracts ZIP directly to data/ directory
    return CompletableFuture.completedFuture(new ImportResult(0, 0, 0, 0, 0, 0, 0));
  }

  @Override
  public boolean requiresExport() {
    return false;
  }
}
