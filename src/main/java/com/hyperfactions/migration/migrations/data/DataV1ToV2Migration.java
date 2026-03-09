package com.hyperfactions.migration.migrations.data;

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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Migrates hardcore power data from a standalone {@code hardcore_power.json} file
 * into per-faction data files ({@code data/factions/{uuid}.json}).
 *
 * <p>
 * Before this migration, hardcore mode's faction power pool was stored in a single
 * {@code hardcore_power.json} file as a map of faction UUID to power double.
 * After migration, each faction's JSON file contains a {@code hardcorePower} field.
 *
 * <p>
 * <strong>Safety:</strong> The {@code data/.version} marker is updated last. If a crash
 * occurs before it's written, the migration re-runs on next startup. Already-injected
 * faction files are harmlessly re-updated since the operation is idempotent.
 */
public class DataV1ToV2Migration implements Migration {

  private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  @Override
  @NotNull
  public String id() {
    return "data-v1-to-v2";
  }

  @Override
  @NotNull
  public MigrationType type() {
    return MigrationType.DATA;
  }

  @Override
  public int fromVersion() {
    return 1;
  }

  @Override
  public int toVersion() {
    return 2;
  }

  @Override
  @NotNull
  public String description() {
    return "Move hardcore power data from standalone file into per-faction data files";
  }

  @Override
  public boolean isApplicable(@NotNull Path dataDir) {
    Path versionFile = dataDir.resolve("data/.version");
    if (!Files.exists(versionFile)) {
      return false;
    }
    try {
      String version = Files.readString(versionFile).trim();
      return "1".equals(version);
    } catch (IOException e) {
      return false;
    }
  }

  @Override
  @NotNull
  public MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options) {
    Instant startTime = Instant.now();
    List<String> filesCreated = new ArrayList<>();
    List<String> filesModified = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    Path dataPath = dataDir.resolve("data");
    Path hardcorePowerFile = dataDir.resolve("hardcore_power.json");
    Path factionsDir = dataPath.resolve("factions");

    try {
      int injected = 0;

      if (Files.exists(hardcorePowerFile)) {
        options.reportProgress("Reading hardcore_power.json", 1, 3);

        String json = Files.readString(hardcorePowerFile);
        JsonObject powerMap = JsonParser.parseString(json).getAsJsonObject();

        options.reportProgress("Injecting power into faction files", 2, 3);

        for (var entry : powerMap.entrySet()) {
          try {
            UUID factionId = UUID.fromString(entry.getKey());
            double power = entry.getValue().getAsDouble();

            Path factionFile = factionsDir.resolve(factionId + ".json");
            if (!Files.exists(factionFile)) {
              warnings.add("Faction file not found for UUID " + factionId + " — skipping power injection");
              continue;
            }

            // Read, inject, and write back
            String factionJson = Files.readString(factionFile);
            JsonObject factionObj = JsonParser.parseString(factionJson).getAsJsonObject();
            factionObj.addProperty("hardcorePower", power);
            Files.writeString(factionFile, gson.toJson(factionObj));

            filesModified.add("data/factions/" + factionId + ".json");
            injected++;
          } catch (Exception e) {
            warnings.add("Failed to process hardcore power entry: " + entry.getKey() + " — " + e.getMessage());
            ErrorHandler.report(String.format("Failed to inject hardcore power for %s", entry.getKey()), e);
          }
        }

        // Delete the standalone file
        Files.delete(hardcorePowerFile);
        Logger.info("[Migration] Deleted hardcore_power.json after injecting %d entries", injected);
      } else {
        options.reportProgress("No hardcore_power.json found (skipping)", 2, 3);
      }

      // Write version marker last (idempotency)
      options.reportProgress("Updating version marker", 3, 3);
      Path versionFile = dataPath.resolve(".version");
      Files.writeString(versionFile, "2");
      filesModified.add("data/.version");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Data migration v1→v2 completed in %dms — injected hardcore power into %d faction files",
        duration.toMillis(), injected);

      return MigrationResult.success(
        id(), fromVersion(), toVersion(), options.backupPath(),
        filesCreated, filesModified, warnings, duration
      );

    } catch (Exception e) {
      Duration duration = Duration.between(startTime, Instant.now());
      ErrorHandler.report("[Migration] Data migration v1→v2 failed", e);
      return MigrationResult.failure(
        id(), fromVersion(), toVersion(), options.backupPath(),
        e.getMessage(), false, duration
      );
    }
  }
}
