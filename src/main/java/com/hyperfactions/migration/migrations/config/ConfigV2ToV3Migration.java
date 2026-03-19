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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Migrates configuration from v2 to v3.
 *
 * <p>This migration:
 * - Moves worldMap.enabled from config.json to config/worldmap.json
 * - Updates messages.prefix to use hex color format instead of Minecraft color codes
 * - Adds configVersion: 3 to the core config
 */
public class ConfigV2ToV3Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v2-to-v3";
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
    return 2;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 3;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Move worldMap.enabled to worldmap.json, update message prefix format";
  }

  @Override
  public boolean isApplicable(@NotNull Path dataDir) {
    Path configFile = dataDir.resolve("config.json");

    // If config doesn't exist, not applicable (fresh install)
    if (!Files.exists(configFile)) {
      return false;
    }

    try {
      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      // Check version is exactly 2
      if (!root.has("configVersion")) {
        return false; // No version means v1, needs v1->v2 first
      }
      return root.get("configVersion").getAsInt() == 2;
    } catch (Exception e) {
      ErrorHandler.report("[Migration] Failed to check config version for V2->V3", e);
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
    Path worldMapFile = dataDir.resolve("config").resolve("worldmap.json");

    try {
      options.reportProgress("Reading v2 config", 1, 4);

      // Step 1: Read v2 config
      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      options.reportProgress("Migrating worldMap.enabled", 2, 4);

      // Step 2: Migrate worldMap.enabled to worldmap.json
      boolean worldMapEnabled = true; // Default
      if (root.has("worldMap")) {
        JsonObject worldMapSection = root.getAsJsonObject("worldMap");
        if (worldMapSection.has("enabled")) {
          worldMapEnabled = worldMapSection.get("enabled").getAsBoolean();
        }
        root.remove("worldMap");
      }

      // Update worldmap.json with the enabled value from main config
      if (Files.exists(worldMapFile)) {
        String worldMapJson = Files.readString(worldMapFile);
        JsonObject worldMapRoot = JsonParser.parseString(worldMapJson).getAsJsonObject();
        worldMapRoot.addProperty("enabled", worldMapEnabled);
        Files.writeString(worldMapFile, GSON.toJson(worldMapRoot));
        filesModified.add("config/worldmap.json");
      } else {
        // This shouldn't happen if v1->v2 ran, but handle gracefully
        warnings.add("worldmap.json not found, will be created on load with enabled=" + worldMapEnabled);
      }

      options.reportProgress("Migrating message prefix format", 3, 4);

      // Step 3: Migrate messages.prefix format
      if (root.has("messages")) {
        JsonObject messages = root.getAsJsonObject("messages");
        if (messages.has("prefix")) {
          String oldPrefix = messages.get("prefix").getAsString();

          // Check if it's using old Minecraft color codes
          if (oldPrefix.contains("\u00A7") || oldPrefix.contains("&")) {
            // Convert to new format - extract text and use configured primary color
            String text = oldPrefix
                .replaceAll("\u00A7[0-9a-fk-or]", "") // Remove section codes
                .replaceAll("&[0-9a-fk-or]", "")      // Remove ampersand codes
                .trim();

            // If text is empty after stripping codes, use default
            if (text.isEmpty()) {
              text = "[HyperFactions] ";
            }

            // Create new nested format
            JsonObject prefixObj = new JsonObject();
            prefixObj.addProperty("text", text);
            prefixObj.addProperty("color", "#55FFFF"); // Cyan/aqua color
            prefixObj.addProperty("bracketColor", "#AAAAAA"); // Gray brackets

            messages.remove("prefix");
            messages.add("prefix", prefixObj);

            Logger.info("[Migration] Converted prefix from '%s' to structured format",
                oldPrefix.replace("\u00A7", "§"));
          }
        }
      } else {
        // Create messages section with new format
        JsonObject messages = new JsonObject();
        JsonObject prefixObj = new JsonObject();
        prefixObj.addProperty("text", "HyperFactions");
        prefixObj.addProperty("color", "#55FFFF");
        prefixObj.addProperty("bracketColor", "#AAAAAA");
        messages.add("prefix", prefixObj);
        messages.addProperty("primaryColor", "#00FFFF");
        root.add("messages", messages);
      }

      options.reportProgress("Writing updated config", 4, 4);

      // Step 4: Update version and save
      root.addProperty("configVersion", 3);
      Files.writeString(configFile, GSON.toJson(root));
      filesModified.add("config.json");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v2->v3 completed in %dms", duration.toMillis());

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
      ErrorHandler.report("[Migration] Config migration failed", e);
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
