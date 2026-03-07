package com.hyperfactions.migration.migrations.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * Migrates configuration from v4 to v5.
 *
 * <p>This migration:
 * - Removes deprecated {@code warzonePowerLoss} config option (replaced by {@code power_loss} zone flag)
 * - Updates configVersion to 5
 */
public class ConfigV4ToV5Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v4-to-v5";
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
    return 4;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 5;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Remove deprecated warzonePowerLoss config option (replaced by power_loss zone flag)";
  }

  @Override
  public boolean isApplicable(@NotNull Path dataDir) {
    Path configFile = dataDir.resolve("config.json");
    if (!Files.exists(configFile)) {
      return false;
    }

    try {
      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      if (!root.has("configVersion")) {
        return false;
      }
      return root.get("configVersion").getAsInt() == 4;
    } catch (Exception e) {
      Logger.warn("[Migration] Failed to check config version: %s", e.getMessage());
      return false;
    }
  }

  /** Executes the command. */
  @Override
  @NotNull
  public MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options) {
    Instant startTime = Instant.now();
    List<String> filesCreated = new ArrayList<>();
    List<String> filesModified = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Path configFile = dataDir.resolve("config.json");

    try {
      options.reportProgress("Reading v4 config", 1, 3);

      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      options.reportProgress("Removing deprecated warzonePowerLoss", 2, 3);

      // Remove warzonePowerLoss from root level
      boolean removedRoot = root.has("warzonePowerLoss");
      if (removedRoot) {
        root.remove("warzonePowerLoss");
        Logger.info("[Migration] Removed deprecated 'warzonePowerLoss' from config root");
      }

      // Remove warzonePowerLoss from power section if present
      boolean removedPower = false;
      if (root.has("power") && root.get("power").isJsonObject()) {
        JsonObject power = root.getAsJsonObject("power");
        if (power.has("warzonePowerLoss")) {
          power.remove("warzonePowerLoss");
          removedPower = true;
          Logger.info("[Migration] Removed deprecated 'warzonePowerLoss' from power section");
        }
      }

      if (!removedRoot && !removedPower) {
        warnings.add("warzonePowerLoss not found in config (already clean)");
      }

      // Bump configVersion to 5
      root.addProperty("configVersion", 5);
      Files.writeString(configFile, GSON.toJson(root));
      filesModified.add("config.json");

      options.reportProgress("Migration complete", 3, 3);

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v4->v5 completed in %dms", duration.toMillis());

      return MigrationResult.success(
        id(),
        fromVersion(),
        toVersion(),
        options.backupPath(),
        filesCreated,
        filesModified,
        warnings,
        duration
      );

    } catch (Exception e) {
      Duration duration = Duration.between(startTime, Instant.now());
      ErrorHandler.report("[Migration] Config migration v4->v5 failed", e);
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
