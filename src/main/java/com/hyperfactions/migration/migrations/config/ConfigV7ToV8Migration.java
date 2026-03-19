package com.hyperfactions.migration.migrations.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.migration.Migration;
import com.hyperfactions.migration.MigrationOptions;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationType;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Migrates configuration from v7 to v8.
 *
 * <p>
 * This migration:
 * <ul>
 *   <li>Converts {@code claimBlacklist} entries from worlds.json into per-world
 *       settings with {@code claiming: false}, then removes the claimBlacklist field.</li>
 * </ul>
 */
public class ConfigV7ToV8Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v7-to-v8";
  }

  /** Type. */
  @Override
  @NotNull
  public MigrationType type() {
    return MigrationType.CONFIG;
  }

  /** Creates from version. */
  @Override
  public int fromVersion() {
    return 7;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 8;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Convert claimBlacklist entries to per-world settings with claiming disabled";
  }

  /** Checks if applicable. */
  @Override
  public boolean isApplicable(@NotNull Path dataDir) {
    Path serverFile = dataDir.resolve("config/server.json");
    if (!Files.exists(serverFile)) {
      return false;
    }

    try {
      String json = Files.readString(serverFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      if (!root.has("configVersion")) {
        return false;
      }
      return root.get("configVersion").getAsInt() == 7;
    } catch (Exception e) {
      ErrorHandler.report("[Migration] Failed to check config version for V7->V8", e);
      return false;
    }
  }

  /** Executes the migration. */
  @Override
  @NotNull
  public MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options) {
    Instant startTime = Instant.now();
    List<String> filesModified = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    try {
      // === Step 1: Migrate claimBlacklist in worlds.json ===
      options.reportProgress("Migrating claimBlacklist in worlds.json", 1, 3);

      Path worldsFile = dataDir.resolve("config/worlds.json");
      if (Files.exists(worldsFile)) {
        String worldsJson = Files.readString(worldsFile);
        JsonObject worldsRoot = JsonParser.parseString(worldsJson).getAsJsonObject();

        if (worldsRoot.has("claimBlacklist") && worldsRoot.get("claimBlacklist").isJsonArray()) {
          JsonArray blacklist = worldsRoot.getAsJsonArray("claimBlacklist");

          if (!blacklist.isEmpty()) {
            // Ensure "worlds" object exists
            JsonObject worldsObj;
            if (worldsRoot.has("worlds") && worldsRoot.get("worlds").isJsonObject()) {
              worldsObj = worldsRoot.getAsJsonObject("worlds");
            } else {
              worldsObj = new JsonObject();
              worldsRoot.add("worlds", worldsObj);
            }

            int migrated = 0;
            for (JsonElement entry : blacklist) {
              String worldName = entry.getAsString();
              if (!worldsObj.has(worldName)) {
                // Create new per-world entry with claiming disabled
                JsonObject worldSettings = new JsonObject();
                worldSettings.addProperty("claiming", false);
                worldsObj.add(worldName, worldSettings);
                migrated++;
                Logger.info("[Migration] Converted blacklist entry '%s' to per-world claiming=false", worldName);
              } else {
                // World already has settings — ensure claiming is false
                JsonObject existing = worldsObj.getAsJsonObject(worldName);
                if (!existing.has("claiming") || existing.get("claiming").getAsBoolean()) {
                  existing.addProperty("claiming", false);
                  migrated++;
                  Logger.info("[Migration] Updated existing world '%s' to claiming=false (was blacklisted)", worldName);
                }
              }
            }

            Logger.info("[Migration] Migrated %d claimBlacklist entries to per-world settings", migrated);
          }

          // Remove the claimBlacklist field
          worldsRoot.remove("claimBlacklist");
          Logger.info("[Migration] Removed claimBlacklist from worlds.json");

          Files.writeString(worldsFile, GSON.toJson(worldsRoot));
          filesModified.add("config/worlds.json");
        }
      }

      // === Step 2: Bump configVersion in server.json ===
      options.reportProgress("Bumping configVersion to 8", 2, 3);

      Path serverFile = dataDir.resolve("config/server.json");
      String serverJson = Files.readString(serverFile);
      JsonObject serverRoot = JsonParser.parseString(serverJson).getAsJsonObject();
      serverRoot.addProperty("configVersion", 8);
      Files.writeString(serverFile, GSON.toJson(serverRoot));
      filesModified.add("config/server.json");

      // === Step 3: Done ===
      options.reportProgress("Migration complete", 3, 3);

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v7->v8 completed in %dms", duration.toMillis());

      return MigrationResult.success(
          id(),
          fromVersion(),
          toVersion(),
          options.backupPath(),
          List.of(),
          filesModified,
          warnings,
          duration
      );

    } catch (Exception e) {
      Duration duration = Duration.between(startTime, Instant.now());
      ErrorHandler.report("[Migration] Config migration v7->v8 failed", e);
      return MigrationResult.failure(
          id(),
          fromVersion(),
          toVersion(),
          options.backupPath(),
          e.getMessage(),
          false,
          duration
      );
    }
  }
}
