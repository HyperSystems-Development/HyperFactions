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
 * Migrates configuration from v5 to v6.
 *
 * <p>
 * This migration splits the monolithic config.json into:
 * <ul>
 *   <li>{@code config/factions.json} — faction gameplay settings (faction, power, claims, combat, relations, invites, stuck)</li>
 *   <li>{@code config/server.json} — server behavior settings (teleport, autoSave, messages, gui, permissions, updates)</li>
 * </ul>
 *
 * <p>
 * Also moves {@code territoryNotifications.enabled} into {@code config/announcements.json}
 * and transforms {@code allowExplosionsInClaims} into 3 granular explosion flags.
 *
 * <p>
 * After extraction, config.json is deleted — configVersion now lives in config/server.json.
 */
public class ConfigV5ToV6Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v5-to-v6";
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
    return 5;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 6;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Split config.json into config/factions.json + config/server.json; "
       + "transform allowExplosionsInClaims into granular explosion flags";
  }

  /** Checks if applicable. */
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
      return root.get("configVersion").getAsInt() == 5;
    } catch (Exception e) {
      ErrorHandler.report("[Migration] Failed to check config version for V5->V6", e);
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
    Path configDir = dataDir.resolve("config");
    Path factionsFile = configDir.resolve("factions.json");
    Path serverFile = configDir.resolve("server.json");
    Path announcementsFile = configDir.resolve("announcements.json");

    try {
      options.reportProgress("Reading v5 config", 1, 5);

      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      // Ensure config/ directory exists
      Files.createDirectories(configDir);

      // === Step 2: Create config/factions.json ===
      options.reportProgress("Creating factions.json", 2, 5);

      if (Files.exists(factionsFile)) {
        warnings.add("config/factions.json already exists — skipping creation");
      } else {
        JsonObject factions = buildFactionsConfig(root, warnings);
        Files.writeString(factionsFile, GSON.toJson(factions));
        filesCreated.add("config/factions.json");
        Logger.info("[Migration] Created config/factions.json");
      }

      // === Step 3: Create config/server.json ===
      options.reportProgress("Creating server.json", 3, 5);

      if (Files.exists(serverFile)) {
        warnings.add("config/server.json already exists — skipping creation");
      } else {
        JsonObject server = buildServerConfig(root);
        Files.writeString(serverFile, GSON.toJson(server));
        filesCreated.add("config/server.json");
        Logger.info("[Migration] Created config/server.json");
      }

      // === Step 4: Move territoryNotifications to announcements.json ===
      options.reportProgress("Migrating territory notifications", 4, 5);

      if (root.has("territoryNotifications") && root.get("territoryNotifications").isJsonObject()) {
        JsonObject notificationsSection = root.getAsJsonObject("territoryNotifications");
        boolean enabled = notificationsSection.has("enabled")
            ? notificationsSection.get("enabled").getAsBoolean()
            : true;

        if (Files.exists(announcementsFile)) {
          // Merge into existing announcements.json
          String annJson = Files.readString(announcementsFile);
          JsonObject annRoot = JsonParser.parseString(annJson).getAsJsonObject();

          if (!annRoot.has("territoryNotifications")) {
            JsonObject terrNotif = new JsonObject();
            terrNotif.addProperty("enabled", enabled);
            annRoot.add("territoryNotifications", terrNotif);
            Files.writeString(announcementsFile, GSON.toJson(annRoot));
            filesModified.add("config/announcements.json");
            Logger.info("[Migration] Added territoryNotifications to announcements.json");
          } else {
            warnings.add("announcements.json already has territoryNotifications — skipped");
          }
        } else {
          // Create minimal announcements.json with territory notifications
          JsonObject annRoot = new JsonObject();
          annRoot.addProperty("enabled", true);
          JsonObject terrNotif = new JsonObject();
          terrNotif.addProperty("enabled", enabled);
          annRoot.add("territoryNotifications", terrNotif);
          Files.writeString(announcementsFile, GSON.toJson(annRoot));
          filesCreated.add("config/announcements.json");
          Logger.info("[Migration] Created announcements.json with territoryNotifications");
        }
      }

      // === Step 5: Delete config.json ===
      options.reportProgress("Removing legacy config.json", 5, 5);

      Files.deleteIfExists(configFile);
      filesModified.add("config.json (deleted)");
      Logger.info("[Migration] Deleted legacy config.json — settings now in config/factions.json + config/server.json");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v5->v6 completed in %dms", duration.toMillis());

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
      ErrorHandler.report("[Migration] Config migration v5->v6 failed", e);
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
   * Builds the factions.json content from the legacy config.json root.
   * Extracts faction, power, claims, combat, relations, invites, stuck sections.
   * Transforms allowExplosionsInClaims into 3 granular flags.
   */
  @NotNull
  private JsonObject buildFactionsConfig(@NotNull JsonObject root, @NotNull List<String> warnings) {
    JsonObject factions = new JsonObject();
    factions.addProperty("enabled", true);

    // Copy sections as-is
    copySection(root, factions, "faction");
    copySection(root, factions, "power");
    copySection(root, factions, "combat");
    copySection(root, factions, "relations");
    copySection(root, factions, "invites");
    copySection(root, factions, "stuck");

    // Claims section needs transformation
    if (root.has("claims") && root.get("claims").isJsonObject()) {
      JsonObject sourceClaims = root.getAsJsonObject("claims").deepCopy();

      // Transform allowExplosionsInClaims -> 3 granular flags
      boolean allowExplosions = false;
      if (sourceClaims.has("allowExplosionsInClaims")) {
        allowExplosions = sourceClaims.get("allowExplosionsInClaims").getAsBoolean();
        sourceClaims.remove("allowExplosionsInClaims");
        Logger.info("[Migration] Transformed allowExplosionsInClaims=%s into 3 granular explosion flags",
            allowExplosions);
      }

      sourceClaims.addProperty("factionlessExplosionsAllowed", allowExplosions);
      sourceClaims.addProperty("enemyExplosionsAllowed", allowExplosions);
      sourceClaims.addProperty("neutralExplosionsAllowed", allowExplosions);

      // Add new protection settings with behavior-preserving defaults
      sourceClaims.addProperty("outsiderPickupAllowed", false);   // was hardcoded deny
      sourceClaims.addProperty("outsiderDropAllowed", true);      // drops were always allowed
      sourceClaims.addProperty("fireSpreadAllowed", false);       // was hardcoded block
      sourceClaims.addProperty("factionlessDamageAllowed", true); // damage wasn't blocked
      sourceClaims.addProperty("enemyDamageAllowed", true);
      sourceClaims.addProperty("neutralDamageAllowed", true);

      factions.add("claims", sourceClaims);
    } else {
      // No claims section — create one with defaults
      JsonObject claims = new JsonObject();
      claims.addProperty("factionlessExplosionsAllowed", false);
      claims.addProperty("enemyExplosionsAllowed", false);
      claims.addProperty("neutralExplosionsAllowed", false);
      claims.addProperty("outsiderPickupAllowed", false);
      claims.addProperty("outsiderDropAllowed", true);
      claims.addProperty("fireSpreadAllowed", false);
      claims.addProperty("factionlessDamageAllowed", true);
      claims.addProperty("enemyDamageAllowed", true);
      claims.addProperty("neutralDamageAllowed", true);
      factions.add("claims", claims);
      warnings.add("No claims section found in config.json — used defaults");
    }

    return factions;
  }

  /**
   * Builds the server.json content from the legacy config.json root.
   * Extracts teleport, autoSave, messages, gui, permissions, updates sections.
   */
  @NotNull
  private JsonObject buildServerConfig(@NotNull JsonObject root) {
    JsonObject server = new JsonObject();
    server.addProperty("enabled", true);
    server.addProperty("configVersion", 6);

    copySection(root, server, "teleport");
    copySection(root, server, "autoSave");
    copySection(root, server, "messages");
    copySection(root, server, "gui");
    copySection(root, server, "permissions");
    copySection(root, server, "updates");

    return server;
  }

  /**
   * Copies a JSON section from source to target if it exists.
   */
  private void copySection(@NotNull JsonObject source, @NotNull JsonObject target, @NotNull String key) {
    if (source.has(key)) {
      target.add(key, source.get(key).deepCopy());
    }
  }
}
