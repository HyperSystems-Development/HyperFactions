package com.hyperfactions.update;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks for plugin updates from the GitHub Releases API.
 * Uses the GitHub API endpoint for the latest release, parsing
 * tag_name for the version, body for the changelog,
 * and the first JAR asset's browser_download_url for the download.
 */
public final class UpdateChecker {

  private static final int TIMEOUT_MS = 10000;

  private static final Gson GSON = new Gson();

  private final Path dataDirectory;

  private final String currentVersion;

  private final String checkUrl;

  private final boolean includePreReleases;

  private final String artifactName;

  private final Path targetDirectory;

  private final AtomicReference<UpdateInfo> cachedUpdate = new AtomicReference<>();

  private volatile long lastCheckTime = 0;

  private static final long CACHE_DURATION_MS = 300000; // 5 minutes

  /** Creates a new UpdateChecker. */
  public UpdateChecker(@NotNull Path dataDirectory, @NotNull String currentVersion, @NotNull String checkUrl) {
    this(dataDirectory, currentVersion, checkUrl, false, "HyperFactions", null);
  }

  /** Creates a new UpdateChecker. */
  public UpdateChecker(@NotNull Path dataDirectory, @NotNull String currentVersion, @NotNull String checkUrl, boolean includePreReleases) {
    this(dataDirectory, currentVersion, checkUrl, includePreReleases, "HyperFactions", null);
  }

  /**
   * Full constructor supporting multiple artifacts.
   *
   * @param dataDirectory    the plugin data directory
   * @param currentVersion   the currently installed version
   * @param checkUrl         the GitHub API URL for release checks
   * @param includePreReleases whether to include pre-release versions
   * @param artifactName     the JAR artifact name (e.g. "HyperFactions" or "HyperProtect-Mixin")
   * @param targetDirectory  the directory to download/manage JARs in (defaults to dataDirectory.getParent())
   */
  public UpdateChecker(@NotNull Path dataDirectory, @NotNull String currentVersion,
            @NotNull String checkUrl, boolean includePreReleases,
            @NotNull String artifactName, @Nullable Path targetDirectory) {
    this.dataDirectory = dataDirectory;
    this.currentVersion = currentVersion;
    // Adjust URL based on release channel
    if (includePreReleases && checkUrl.endsWith("/releases/latest")) {
      this.checkUrl = checkUrl.replace("/releases/latest", "/releases");
    } else {
      this.checkUrl = checkUrl;
    }
    this.includePreReleases = includePreReleases;
    this.artifactName = artifactName;
    this.targetDirectory = targetDirectory != null ? targetDirectory : dataDirectory.getParent();
  }

  /**
   * Gets the artifact name this checker manages.
   */
  @NotNull
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Gets the current plugin version.
   */
  @NotNull
  public String getCurrentVersion() {
    return currentVersion;
  }

  /**
   * Checks for updates asynchronously.
   *
   * @return a future containing update info, or null if up-to-date or error
   */
  public CompletableFuture<UpdateInfo> checkForUpdates() {
    return checkForUpdates(false);
  }

  /**
   * Checks for updates asynchronously.
   *
   * @param forceRefresh if true, ignores cache
   * @return a future containing update info, or null if up-to-date or error
   */
  public CompletableFuture<UpdateInfo> checkForUpdates(boolean forceRefresh) {
    // Check cache first
    if (!forceRefresh && System.currentTimeMillis() - lastCheckTime < CACHE_DURATION_MS) {
      return CompletableFuture.completedFuture(cachedUpdate.get());
    }

    return CompletableFuture.supplyAsync(() -> {
      try {
        Logger.info("[Update:%s] Checking for updates from %s", artifactName, checkUrl);

        URL url = URI.create(checkUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", artifactName + "-UpdateChecker");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);

        int responseCode = conn.getResponseCode();
        if (responseCode == 404) {
          Logger.info("[Update:%s] No releases found yet (404)", artifactName);
          lastCheckTime = System.currentTimeMillis();
          return null;
        }
        if (responseCode != 200) {
          Logger.warn("[Update:%s] Failed to check for updates: HTTP %d", artifactName, responseCode);
          return null;
        }

        // Read response
        String json;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
          StringBuilder sb = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
            sb.append(line);
          }
          json = sb.toString();
        }

        // Parse GitHub release JSON
        // When using /releases endpoint, response is an array; /releases/latest is a single object
        JsonObject releaseObj = null;

        if (json.trim().startsWith("[")) {
          // Array response from /releases endpoint
          JsonArray releases = GSON.fromJson(json, JsonArray.class);
          if (releases.isEmpty()) {
            Logger.info("[Update:%s] No releases found", artifactName);
            return null;
          }

          // First release in the array is the latest (including pre-releases)
          releaseObj = releases.get(0).getAsJsonObject();
        } else {
          // Single object response from /releases/latest
          releaseObj = GSON.fromJson(json, JsonObject.class);
        }

        String tagName = releaseObj.get("tag_name").getAsString();
        String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        String changelog = releaseObj.has("body") && !releaseObj.get("body").isJsonNull()
            ? releaseObj.get("body").getAsString() : null;
        boolean isPreRelease = releaseObj.has("prerelease") && releaseObj.get("prerelease").getAsBoolean();

        // Find the first .jar asset download URL
        String downloadUrl = null;
        if (releaseObj.has("assets") && releaseObj.get("assets").isJsonArray()) {
          JsonArray assets = releaseObj.getAsJsonArray("assets");
          for (int i = 0; i < assets.size(); i++) {
            JsonObject asset = assets.get(i).getAsJsonObject();
            String assetName = asset.get("name").getAsString();
            if (assetName.endsWith(".jar")) {
              downloadUrl = asset.get("browser_download_url").getAsString();
              break;
            }
          }
        }

        lastCheckTime = System.currentTimeMillis();

        // Compare versions
        if (isNewerVersion(latestVersion, currentVersion)) {
          UpdateInfo info = new UpdateInfo(latestVersion, downloadUrl, changelog, isPreRelease);
          cachedUpdate.set(info);
          String channelNote = isPreRelease ? " (pre-release)" : "";
          Logger.info("[Update:%s] New version available: %s%s (current: %s)", artifactName, latestVersion, channelNote, currentVersion);
          return info;
        } else {
          cachedUpdate.set(null);
          Logger.info("[Update:%s] Plugin is up-to-date (v%s)", artifactName, currentVersion);
          return null;
        }

      } catch (java.net.UnknownHostException e) {
        // DNS failure — expected in environments without internet access
        Logger.info("[Update:%s] Could not resolve host (no internet?): %s", artifactName, e.getMessage());
        return null;
      } catch (java.net.ConnectException e) {
        Logger.info("[Update:%s] Connection refused: %s", artifactName, e.getMessage());
        return null;
      } catch (java.net.SocketTimeoutException e) {
        Logger.info("[Update:%s] Connection timed out: %s", artifactName, e.getMessage());
        return null;
      } catch (Exception e) {
        ErrorHandler.report(String.format("[Update:%s] Failed to check for updates", artifactName), e);
        return null;
      }
    });
  }

  /**
   * Downloads the update to the mods folder.
   *
   * @param info the update info
   * @return a future containing the downloaded file path, or null on failure
   */
  public CompletableFuture<Path> downloadUpdate(@NotNull UpdateInfo info) {
    if (info.downloadUrl() == null || info.downloadUrl().isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.supplyAsync(() -> {
      try {
        Logger.info("[Update:%s] Downloading update v%s from %s", artifactName, info.version(), info.downloadUrl());

        URL url = URI.create(info.downloadUrl()).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", artifactName + "-UpdateChecker");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(60000); // 60 second read timeout for downloads

        // Follow redirects
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
          Logger.warn("[Update:%s] Failed to download update: HTTP %d", artifactName, responseCode);
          return null;
        }

        // Determine output path
        // targetDirectory is used instead of dataDirectory.getParent()
        Path updateFile = targetDirectory.resolve(artifactName + "-" + info.version() + ".jar");
        Path currentJar = targetDirectory.resolve(artifactName + "-" + currentVersion + ".jar");

        // Download to temp file first
        Path tempFile = targetDirectory.resolve(artifactName + "-" + info.version() + ".jar.tmp");

        try (InputStream in = conn.getInputStream()) {
          Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Verify download (basic check - file size > 0)
        if (Files.size(tempFile) < 1000) {
          Logger.warn("[Update:%s] Downloaded file seems too small, aborting", artifactName);
          Files.deleteIfExists(tempFile);
          return null;
        }

        // Rename temp to final
        Files.move(tempFile, updateFile, StandardCopyOption.REPLACE_EXISTING);

        // Backup current JAR if it exists
        // On Windows, the JAR may be locked by the JVM - handle gracefully
        if (Files.exists(currentJar)) {
          Path backupFile = targetDirectory.resolve(artifactName + "-" + currentVersion + ".jar.backup");
          try {
            Files.move(currentJar, backupFile, StandardCopyOption.REPLACE_EXISTING);
            Logger.info("[Update:%s] Backed up current JAR to %s", artifactName, backupFile.getFileName());
          } catch (java.nio.file.FileSystemException e) {
            // Windows locks loaded JARs - can't backup while running
            ErrorHandler.report(String.format("[Update:%s] Could not backup old JAR (file in use): %s", artifactName, currentJar.getFileName()), e);
          }
        }

        Logger.info("[Update:%s] Successfully downloaded update to %s", artifactName, updateFile.getFileName());
        return updateFile;

      } catch (Exception e) {
        ErrorHandler.report(String.format("[Update:%s] Failed to download update", artifactName), e);
        return null;
      }
    });
  }

  /**
   * Gets the cached update info, if any.
   */
  @Nullable
  public UpdateInfo getCachedUpdate() {
    return cachedUpdate.get();
  }

  /**
   * Checks if there's a cached update available.
   */
  public boolean hasUpdateAvailable() {
    return cachedUpdate.get() != null;
  }

  /**
   * Cleans up old JAR backup files, keeping only the most recent backup version.
   * This should be called after a successful update download.
   *
   * @param keepVersion the version to keep (usually the version we just upgraded from)
   * @return the number of backup files deleted
   */
  public int cleanupOldBackups(@Nullable String keepVersion) {
    // targetDirectory is used instead of dataDirectory.getParent()
    List<Path> backupFiles = new ArrayList<>();

    // Find all HyperFactions backup files
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDirectory, artifactName + "-*.jar.backup")) {
      for (Path file : stream) {
        backupFiles.add(file);
      }
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Failed to list backup files", artifactName), e);
      return 0;
    }

    if (backupFiles.isEmpty()) {
      Logger.info("[Update:%s] No old backup files to clean up", artifactName);
      return 0;
    }

    // Sort by version (newest first)
    backupFiles.sort((a, b) -> {
      String versionA = extractVersionFromBackup(a.getFileName().toString());
      String versionB = extractVersionFromBackup(b.getFileName().toString());
      return compareVersions(versionB, versionA); // Descending order
    });

    int deleted = 0;
    boolean keepVersionFound = false;

    for (Path backupFile : backupFiles) {
      String fileVersion = extractVersionFromBackup(backupFile.getFileName().toString());

      // Keep the specified version (the one we just upgraded from)
      if (keepVersion != null && fileVersion.equals(keepVersion)) {
        keepVersionFound = true;
        Logger.info("[Update:%s] Keeping backup: %s (rollback version)", artifactName, backupFile.getFileName());
        continue;
      }

      // If no keepVersion specified, keep the most recent (first in sorted list)
      if (keepVersion == null && !keepVersionFound) {
        keepVersionFound = true;
        Logger.info("[Update:%s] Keeping backup: %s (most recent)", artifactName, backupFile.getFileName());
        continue;
      }

      // Delete older backups
      try {
        Files.delete(backupFile);
        deleted++;
        Logger.info("[Update:%s] Cleanup: Removed old backup %s", artifactName, backupFile.getFileName());
      } catch (IOException e) {
        ErrorHandler.report(String.format("[Update:%s] Failed to delete backup %s", artifactName, backupFile.getFileName()), e);
      }
    }

    if (deleted > 0) {
      Logger.info("[Update:%s] Cleanup complete: Removed %d old backup(s)", artifactName, deleted);
    }

    return deleted;
  }

  /**
   * Extracts the version string from a backup filename.
   * Example: "ArtifactName-0.5.2.jar.backup" -> "0.5.2"
   */
  @NotNull
  private String extractVersionFromBackup(@NotNull String filename) {
    // Remove prefix "ArtifactName-" and suffix ".jar.backup"
    String version = filename;
    String prefix = artifactName + "-";
    if (version.startsWith(prefix)) {
      version = version.substring(prefix.length());
    }
    if (version.endsWith(".jar.backup")) {
      version = version.substring(0, version.length() - ".jar.backup".length());
    }
    return version;
  }

  /**
   * Compares two version strings.
   *
   * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
   */
  private int compareVersions(@NotNull String v1, @NotNull String v2) {
    // Remove 'v' prefix if present
    String ver1 = v1.toLowerCase().startsWith("v") ? v1.substring(1) : v1;
    String ver2 = v2.toLowerCase().startsWith("v") ? v2.substring(1) : v2;

    String[] parts1 = ver1.split("\\.");
    String[] parts2 = ver2.split("\\.");

    int maxLen = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < maxLen; i++) {
      int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
      int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

      if (num1 != num2) {
        return num1 - num2;
      }
    }
    return 0;
  }

  /**
   * Compares two version strings.
   *
   * @return true if newVersion is newer than oldVersion
   */
  private boolean isNewerVersion(@NotNull String newVersion, @NotNull String oldVersion) {
    try {
      // Remove 'v' prefix if present
      String v1 = newVersion.toLowerCase().startsWith("v") ? newVersion.substring(1) : newVersion;
      String v2 = oldVersion.toLowerCase().startsWith("v") ? oldVersion.substring(1) : oldVersion;

      String[] parts1 = v1.split("\\.");
      String[] parts2 = v2.split("\\.");

      int maxLen = Math.max(parts1.length, parts2.length);
      for (int i = 0; i < maxLen; i++) {
        int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
        int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;

        if (num1 > num2) {
          return true;
        }
        if (num1 < num2) {
          return false;
        }
      }

      return false; // Equal versions
    } catch (Exception e) {
      // Fallback to string comparison
      return newVersion.compareTo(oldVersion) > 0;
    }
  }

  private int parseVersionPart(@NotNull String part) {
    // Handle versions like "1.0.0-beta1"
    String numPart = part.split("-")[0];
    return Integer.parseInt(numPart);
  }

  // ==================== Rollback Support ====================

  private static final String ROLLBACK_MARKER_FILE = ".rollback-safe";

  /**
   * Creates the rollback-safe marker file.
   * This should be called after a successful update download, before server restart.
   * The marker indicates that rollback is safe (no migrations have run yet).
   *
   * @param fromVersion the version we upgraded from
   * @param toVersion the version we upgraded to
   */
  public void createRollbackMarker(@NotNull String fromVersion, @NotNull String toVersion) {
    Path markerFile = dataDirectory.resolve(ROLLBACK_MARKER_FILE);
    try {
      String content = "from=" + fromVersion + "\nto=" + toVersion + "\ntimestamp=" + System.currentTimeMillis();
      Files.writeString(markerFile, content);
      Logger.debug("[Update:%s] Created rollback marker: %s -> %s", artifactName, fromVersion, toVersion);
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Failed to create rollback marker", artifactName), e);
    }
  }

  /**
   * Removes the rollback-safe marker file.
   * This should be called on server startup to indicate that migrations may have run.
   */
  public void clearRollbackMarker() {
    Path markerFile = dataDirectory.resolve(ROLLBACK_MARKER_FILE);
    try {
      if (Files.deleteIfExists(markerFile)) {
        Logger.debug("[Update:%s] Cleared rollback marker (server restarted with new version)", artifactName);
      }
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Failed to clear rollback marker", artifactName), e);
    }
  }

  /**
   * Checks if rollback is safe (marker file exists, meaning server hasn't restarted since update).
   *
   * @return true if rollback is safe
   */
  public boolean isRollbackSafe() {
    Path markerFile = dataDirectory.resolve(ROLLBACK_MARKER_FILE);
    return Files.exists(markerFile);
  }

  /**
   * Gets info about the pending rollback from the marker file.
   *
   * @return rollback info, or null if no marker exists
   */
  @Nullable
  public RollbackInfo getRollbackInfo() {
    Path markerFile = dataDirectory.resolve(ROLLBACK_MARKER_FILE);
    if (!Files.exists(markerFile)) {
      return null;
    }

    try {
      String content = Files.readString(markerFile);
      String fromVersion = null;
      String toVersion = null;

      for (String line : content.split("\n")) {
        if (line.startsWith("from=")) {
          fromVersion = line.substring(5);
        } else if (line.startsWith("to=")) {
          toVersion = line.substring(3);
        }
      }

      if (fromVersion != null && toVersion != null) {
        return new RollbackInfo(fromVersion, toVersion, true);
      }
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Failed to read rollback marker", artifactName), e);
    }

    return null;
  }

  /**
   * Finds the most recent JAR backup file.
   *
   * @return path to the backup, or null if none exists
   */
  @Nullable
  public Path findLatestBackup() {
    // targetDirectory is used instead of dataDirectory.getParent()
    List<Path> backupFiles = new ArrayList<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetDirectory, artifactName + "-*.jar.backup")) {
      for (Path file : stream) {
        backupFiles.add(file);
      }
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Failed to list backup files for findLatestBackup", artifactName), e);
      return null;
    }

    if (backupFiles.isEmpty()) {
      return null;
    }

    // Sort by version (newest first) and return the first
    backupFiles.sort((a, b) -> {
      String versionA = extractVersionFromBackup(a.getFileName().toString());
      String versionB = extractVersionFromBackup(b.getFileName().toString());
      return compareVersions(versionB, versionA); // Descending order
    });

    return backupFiles.get(0);
  }

  /**
   * Performs a JAR rollback by swapping the current JAR with the backup.
   * This should only be called if isRollbackSafe() returns true.
   *
   * @return rollback result
   */
  @NotNull
  public RollbackResult performRollback() {
    // targetDirectory is used instead of dataDirectory.getParent()
    Path latestBackup = findLatestBackup();

    if (latestBackup == null) {
      return new RollbackResult(false, null, null, "No backup JAR found to rollback to");
    }

    String backupVersion = extractVersionFromBackup(latestBackup.getFileName().toString());
    Path currentJar = targetDirectory.resolve(artifactName + "-" + currentVersion + ".jar");
    Path newVersionJar = null;

    // Find the new version JAR (the one we're rolling back from)
    RollbackInfo info = getRollbackInfo();
    if (info != null) {
      newVersionJar = targetDirectory.resolve(artifactName + "-" + info.toVersion() + ".jar");
    }

    try {
      // Step 1: If the new version JAR exists, delete it (or rename it)
      if (newVersionJar != null && Files.exists(newVersionJar)) {
        Path rolledBackJar = targetDirectory.resolve(artifactName + "-" + info.toVersion() + ".jar.rolledback");
        Files.move(newVersionJar, rolledBackJar, StandardCopyOption.REPLACE_EXISTING);
        Logger.info("[Update:%s] Moved new JAR to: %s", artifactName, rolledBackJar.getFileName());
      }

      // Step 2: Restore the backup to its original name
      Path restoredJar = targetDirectory.resolve(artifactName + "-" + backupVersion + ".jar");
      Files.move(latestBackup, restoredJar, StandardCopyOption.REPLACE_EXISTING);
      Logger.info("[Update:%s] Restored backup: %s -> %s", artifactName, latestBackup.getFileName(), restoredJar.getFileName());

      // Step 3: Remove the rollback marker
      clearRollbackMarker();

      return new RollbackResult(true, backupVersion, info != null ? info.toVersion() : null, null);

    } catch (IOException e) {
      ErrorHandler.report(String.format("[Update:%s] Rollback failed", artifactName), e);
      return new RollbackResult(false, backupVersion, null, "Failed to rollback: " + e.getMessage());
    }
  }

  /**
   * Record containing rollback information from the marker file.
   */
  public record RollbackInfo(
      @NotNull String fromVersion,
      @NotNull String toVersion,
      boolean safe
  ) {}

  /**
   * Record containing the result of a rollback operation.
   */
  public record RollbackResult(
      boolean success,
      @Nullable String restoredVersion,
      @Nullable String removedVersion,
      @Nullable String errorMessage
  ) {}

  /**
   * Record containing update information.
   */
  public record UpdateInfo(
      @NotNull String version,
      @Nullable String downloadUrl,
      @Nullable String changelog,
      boolean isPreRelease
  ) {
    /** Backwards-compatible constructor without pre-release flag. */
    public UpdateInfo(@NotNull String version, @Nullable String downloadUrl, @Nullable String changelog) {
      this(version, downloadUrl, changelog, false);
    }
  }
}
