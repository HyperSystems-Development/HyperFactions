package com.hyperfactions.migration;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Options for migration execution.
 *
 * @param backupPath         path where backup was created (may be null if backup disabled)
 * @param dryRun             if true, don't actually modify files
 * @param verbose            if true, log detailed progress
 * @param progressCallback   callback for progress updates (may be null)
 */
public record MigrationOptions(
  Path backupPath,
  boolean dryRun,
  boolean verbose,
  ProgressCallback progressCallback
) {
  /**
   * Callback interface for migration progress updates.
   */
  @FunctionalInterface
  public interface ProgressCallback {
    /**
     * Called when migration progress updates.
     *
     * @param step     current step description
     * @param current  current step number (1-based)
     * @param total    total number of steps
     */
    void onProgress(@NotNull String step, int current, int total);
  }

  /**
   * Creates default options with no callback.
   *
   * @param backupPath path to backup
   * @return default options
   */
  public static MigrationOptions defaults(Path backupPath) {
    return new MigrationOptions(backupPath, false, false, null);
  }

  /**
   * Creates options with progress callback.
   *
   * @param backupPath path to backup
   * @param callback   progress callback
   * @return options with callback
   */
  public static MigrationOptions withProgress(Path backupPath, ProgressCallback callback) {
    return new MigrationOptions(backupPath, false, false, callback);
  }

  /**
   * Reports progress if a callback is registered.
   *
   * @param step    current step description
   * @param current current step number
   * @param total   total steps
   */
  public void reportProgress(@NotNull String step, int current, int total) {
    if (progressCallback != null) {
      progressCallback.onProgress(step, current, total);
    }
  }
}
