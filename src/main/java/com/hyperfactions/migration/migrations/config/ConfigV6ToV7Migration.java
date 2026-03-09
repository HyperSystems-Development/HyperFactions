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
 * Migrates configuration from v6 to v7.
 *
 * <p>
 * This migration:
 * <ul>
 *   <li>Updates updater URLs from the old {@code HyperSystemsDev} GitHub
 *       organization to the new {@code HyperSystems-Development} organization.</li>
 *   <li>Removes the {@code worldMap} section from server.json — these settings
 *       have been consolidated into worldmap.json's {@code playerVisibility} section.</li>
 *   <li>Restructures economy.json from flat keys to grouped sections
 *       (currency, treasury, fees, upkeep).</li>
 * </ul>
 *
 * <p>
 * Only exact matches are replaced — custom URLs are left untouched.
 */
public class ConfigV6ToV7Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  private static final String OLD_HF_URL =
      "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";
  private static final String NEW_HF_URL =
      "https://api.github.com/repos/HyperSystems-Development/HyperFactions/releases/latest";

  private static final String OLD_HP_URL =
      "https://api.github.com/repos/HyperSystemsDev/HyperProtect-Mixin/releases/latest";
  private static final String NEW_HP_URL =
      "https://api.github.com/repos/HyperSystems-Development/HyperProtect-Mixin/releases/latest";

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v6-to-v7";
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
    return 6;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 7;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Migrate updater URLs, remove worldMap section, restructure economy.json";
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
      return root.get("configVersion").getAsInt() == 6;
    } catch (Exception e) {
      Logger.warn("[Migration] Failed to check config version: %s", e.getMessage());
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

    Path serverFile = dataDir.resolve("config/server.json");

    try {
      options.reportProgress("Reading config/server.json", 1, 3);

      String json = Files.readString(serverFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      boolean changed = false;

      // === Step 2: Remove worldMap section (moved to worldmap.json playerVisibility) ===
      options.reportProgress("Removing worldMap section", 2, 4);

      if (root.has("worldMap")) {
        root.remove("worldMap");
        changed = true;
        Logger.info("[Migration] Removed worldMap section from server.json"
            + " (settings consolidated into worldmap.json playerVisibility)");
      }

      // === Step 3: Migrate URLs ===
      options.reportProgress("Migrating updater URLs", 3, 4);

      if (root.has("updates") && root.get("updates").isJsonObject()) {
        JsonObject updates = root.getAsJsonObject("updates");

        // Migrate HyperFactions update URL
        if (updates.has("url") && OLD_HF_URL.equals(updates.get("url").getAsString())) {
          updates.addProperty("url", NEW_HF_URL);
          changed = true;
          Logger.info("[Migration] Updated HyperFactions updater URL to HyperSystems-Development");
        }

        // Migrate HyperProtect-Mixin update URL
        if (updates.has("hyperProtect") && updates.get("hyperProtect").isJsonObject()) {
          JsonObject hp = updates.getAsJsonObject("hyperProtect");
          if (hp.has("url") && OLD_HP_URL.equals(hp.get("url").getAsString())) {
            hp.addProperty("url", NEW_HP_URL);
            changed = true;
            Logger.info("[Migration] Updated HyperProtect-Mixin updater URL to HyperSystems-Development");
          }
        }
      }

      // === Step 4: Restructure economy.json (flat → grouped sections) ===
      options.reportProgress("Restructuring economy.json", 4, 5);

      Path economyFile = dataDir.resolve("config/economy.json");
      if (Files.exists(economyFile)) {
        try {
          String econJson = Files.readString(economyFile);
          JsonObject econ = JsonParser.parseString(econJson).getAsJsonObject();

          // Only migrate if still in flat format (has flat keys, no nested sections)
          if (econ.has("currencyName") && !econ.has("currency")) {
            JsonObject migrated = new JsonObject();

            // Preserve top-level enabled
            if (econ.has("enabled")) {
              migrated.addProperty("enabled", econ.get("enabled").getAsBoolean());
            }

            // Currency section
            JsonObject currency = new JsonObject();
            moveProperty(econ, "currencyName", currency, "name");
            moveProperty(econ, "currencyNamePlural", currency, "namePlural");
            moveProperty(econ, "currencySymbol", currency, "symbol");
            migrated.add("currency", currency);

            // Treasury section
            JsonObject treasury = new JsonObject();
            moveProperty(econ, "startingBalance", treasury, "startingBalance");
            moveProperty(econ, "disbandRefundToLeader", treasury, "disbandRefundToLeader");

            JsonObject limits = new JsonObject();
            moveProperty(econ, "defaultMaxWithdrawAmount", limits, "maxWithdrawAmount");
            moveProperty(econ, "defaultMaxWithdrawPerPeriod", limits, "maxWithdrawPerPeriod");
            moveProperty(econ, "defaultMaxTransferAmount", limits, "maxTransferAmount");
            moveProperty(econ, "defaultMaxTransferPerPeriod", limits, "maxTransferPerPeriod");
            moveProperty(econ, "defaultLimitPeriodHours", limits, "periodHours");
            treasury.add("limits", limits);
            migrated.add("treasury", treasury);

            // Fees section
            JsonObject fees = new JsonObject();
            moveProperty(econ, "depositFeePercent", fees, "depositPercent");
            moveProperty(econ, "withdrawFeePercent", fees, "withdrawPercent");
            moveProperty(econ, "transferFeePercent", fees, "transferPercent");
            migrated.add("fees", fees);

            // Upkeep section
            JsonObject upkeep = new JsonObject();
            moveProperty(econ, "upkeepEnabled", upkeep, "enabled");
            moveProperty(econ, "upkeepCostPerChunk", upkeep, "costPerChunk");
            moveProperty(econ, "upkeepIntervalHours", upkeep, "intervalHours");
            moveProperty(econ, "upkeepGracePeriodHours", upkeep, "gracePeriodHours");
            moveProperty(econ, "upkeepAutoPayDefault", upkeep, "autoPayDefault");
            moveProperty(econ, "upkeepFreeChunks", upkeep, "freeChunks");
            moveProperty(econ, "upkeepClaimLossPerCycle", upkeep, "claimLossPerCycle");
            moveProperty(econ, "upkeepWarningHours", upkeep, "warningHours");
            moveProperty(econ, "upkeepMaxCostCap", upkeep, "maxCostCap");
            moveProperty(econ, "upkeepScalingMode", upkeep, "scalingMode");
            if (econ.has("upkeepScalingTiers")) {
              upkeep.add("scalingTiers", econ.get("upkeepScalingTiers"));
            }
            migrated.add("upkeep", upkeep);

            Files.writeString(economyFile, GSON.toJson(migrated));
            filesModified.add("config/economy.json");
            Logger.info("[Migration] Restructured economy.json into grouped sections");
          }
        } catch (Exception e) {
          warnings.add("Failed to restructure economy.json: " + e.getMessage()
              + " (will be auto-fixed on next save)");
          Logger.warn("[Migration] Failed to restructure economy.json: %s", e.getMessage());
        }
      }

      // === Step 5: Bump configVersion ===
      options.reportProgress("Bumping configVersion to 7", 5, 5);

      root.addProperty("configVersion", 7);
      changed = true;

      Files.writeString(serverFile, GSON.toJson(root));
      filesModified.add("config/server.json");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v6->v7 completed in %dms", duration.toMillis());

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
      ErrorHandler.report("[Migration] Config migration v6->v7 failed", e);
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

  /**
   * Moves a property from one JSON object to another with a new key name.
   * No-op if the source key doesn't exist.
   */
  private static void moveProperty(@NotNull JsonObject from, @NotNull String fromKey,
                    @NotNull JsonObject to, @NotNull String toKey) {
    if (from.has(fromKey)) {
      to.add(toKey, from.get(fromKey));
    }
  }
}
