package com.hyperfactions.migration.migrations.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.migration.Migration;
import com.hyperfactions.migration.MigrationOptions;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationType;
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
 * Migrates configuration from v1 (monolithic) to v2 (hybrid file structure).
 *
 * <p>
 * This migration:
 * <ul>
 *   <li>Splits the monolithic config.json into core config + module configs</li>
 *   <li>Creates config/ subdirectory with backup.json, chat.json, debug.json, economy.json, faction-permissions.json</li>
 *   <li>Adds configVersion: 2 to the core config</li>
 *   <li>Adds "enabled": true to each module config</li>
 * </ul>
 */
public class ConfigV1ToV2Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v1-to-v2";
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
    return 1;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 2;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Split monolithic config.json into hybrid file structure with module configs";
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

      // No version = v1, or explicit version < 2
      if (!root.has("configVersion")) {
        return true;
      }
      return root.get("configVersion").getAsInt() < 2;
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
    Path configDir = dataDir.resolve("config");

    try {
      options.reportProgress("Reading v1 config", 1, 7);

      // Step 1: Read v1 config
      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      options.reportProgress("Creating config directory", 2, 7);

      // Step 2: Create config/ directory
      Files.createDirectories(configDir);
      filesCreated.add("config/");

      options.reportProgress("Extracting backup module", 3, 7);

      // Step 3: Extract backup module
      extractBackupModule(root, configDir, filesCreated);

      options.reportProgress("Extracting chat module", 4, 7);

      // Step 4: Extract chat module
      extractChatModule(root, configDir, filesCreated);

      options.reportProgress("Extracting debug module", 5, 7);

      // Step 5: Extract debug module
      extractDebugModule(root, configDir, filesCreated);

      options.reportProgress("Extracting economy and faction-permissions modules", 6, 7);

      // Step 6: Extract economy module
      extractEconomyModule(root, configDir, filesCreated);

      // Step 7: Extract faction-permissions module
      extractFactionPermissionsModule(root, configDir, filesCreated);

      options.reportProgress("Writing updated core config", 7, 7);

      // Step 8: Add version and save core config
      root.addProperty("configVersion", 2);
      Files.writeString(configFile, GSON.toJson(root));
      filesModified.add("config.json");

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v1->v2 completed in %dms", duration.toMillis());

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
      Logger.severe("[Migration] Config migration failed: %s", e.getMessage());
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
   * Extracts backup settings to config/backup.json.
   */
  private void extractBackupModule(JsonObject root, Path configDir, List<String> filesCreated) throws IOException {
    JsonObject backup = new JsonObject();
    backup.addProperty("enabled", true);

    if (root.has("backup")) {
      JsonObject source = root.getAsJsonObject("backup");
      copyProperty(source, backup, "hourlyRetention", 24);
      copyProperty(source, backup, "dailyRetention", 7);
      copyProperty(source, backup, "weeklyRetention", 4);
      copyProperty(source, backup, "manualRetention", 10);
      copyProperty(source, backup, "onShutdown", true);
      root.remove("backup");
    } else {
      backup.addProperty("hourlyRetention", 24);
      backup.addProperty("dailyRetention", 7);
      backup.addProperty("weeklyRetention", 4);
      backup.addProperty("manualRetention", 10);
      backup.addProperty("onShutdown", true);
    }

    Path backupFile = configDir.resolve("backup.json");
    Files.writeString(backupFile, GSON.toJson(backup));
    filesCreated.add("config/backup.json");
  }

  /**
   * Extracts chat settings to config/chat.json.
   */
  private void extractChatModule(JsonObject root, Path configDir, List<String> filesCreated) throws IOException {
    JsonObject chat = new JsonObject();
    chat.addProperty("enabled", true);

    if (root.has("chat")) {
      JsonObject source = root.getAsJsonObject("chat");
      copyProperty(source, chat, "format", "{faction_tag}{prefix}{player}{suffix}: {message}");
      copyProperty(source, chat, "tagDisplay", "tag");
      copyProperty(source, chat, "tagFormat", "[{tag}] ");
      copyProperty(source, chat, "noFactionTag", "");
      copyProperty(source, chat, "priority", "LATE");

      // Copy relationColors nested object
      if (source.has("relationColors")) {
        chat.add("relationColors", source.get("relationColors").deepCopy());
      } else {
        JsonObject colors = new JsonObject();
        colors.addProperty("own", "#00FF00");
        colors.addProperty("ally", "#FF69B4");
        colors.addProperty("neutral", "#AAAAAA");
        colors.addProperty("enemy", "#FF0000");
        chat.add("relationColors", colors);
      }

      root.remove("chat");
    } else {
      chat.addProperty("format", "{faction_tag}{prefix}{player}{suffix}: {message}");
      chat.addProperty("tagDisplay", "tag");
      chat.addProperty("tagFormat", "[{tag}] ");
      chat.addProperty("noFactionTag", "");
      chat.addProperty("priority", "LATE");

      JsonObject colors = new JsonObject();
      colors.addProperty("own", "#00FF00");
      colors.addProperty("ally", "#FF69B4");
      colors.addProperty("neutral", "#AAAAAA");
      colors.addProperty("enemy", "#FF0000");
      chat.add("relationColors", colors);
    }

    Path chatFile = configDir.resolve("chat.json");
    Files.writeString(chatFile, GSON.toJson(chat));
    filesCreated.add("config/chat.json");
  }

  /**
   * Extracts debug settings to config/debug.json.
   */
  private void extractDebugModule(JsonObject root, Path configDir, List<String> filesCreated) throws IOException {
    JsonObject debug = new JsonObject();
    debug.addProperty("enabled", false); // Debug disabled by default

    if (root.has("debug")) {
      JsonObject source = root.getAsJsonObject("debug");
      copyProperty(source, debug, "enabledByDefault", false);
      copyProperty(source, debug, "logToConsole", true);

      // Copy categories nested object
      if (source.has("categories")) {
        debug.add("categories", source.get("categories").deepCopy());
      } else {
        JsonObject categories = new JsonObject();
        categories.addProperty("power", false);
        categories.addProperty("claim", false);
        categories.addProperty("combat", false);
        categories.addProperty("protection", false);
        categories.addProperty("relation", false);
        categories.addProperty("territory", false);
        debug.add("categories", categories);
      }

      root.remove("debug");
    } else {
      debug.addProperty("enabledByDefault", false);
      debug.addProperty("logToConsole", true);

      JsonObject categories = new JsonObject();
      categories.addProperty("power", false);
      categories.addProperty("claim", false);
      categories.addProperty("combat", false);
      categories.addProperty("protection", false);
      categories.addProperty("relation", false);
      categories.addProperty("territory", false);
      debug.add("categories", categories);
    }

    Path debugFile = configDir.resolve("debug.json");
    Files.writeString(debugFile, GSON.toJson(debug));
    filesCreated.add("config/debug.json");
  }

  /**
   * Extracts economy settings to config/economy.json.
   */
  private void extractEconomyModule(JsonObject root, Path configDir, List<String> filesCreated) throws IOException {
    JsonObject economy = new JsonObject();
    economy.addProperty("enabled", true);

    if (root.has("economy")) {
      JsonObject source = root.getAsJsonObject("economy");
      copyProperty(source, economy, "currencyName", "dollar");
      copyProperty(source, economy, "currencyNamePlural", "dollars");
      copyProperty(source, economy, "currencySymbol", "$");
      copyProperty(source, economy, "startingBalance", 0.0);
      root.remove("economy");
    } else {
      economy.addProperty("currencyName", "dollar");
      economy.addProperty("currencyNamePlural", "dollars");
      economy.addProperty("currencySymbol", "$");
      economy.addProperty("startingBalance", 0.0);
    }

    Path economyFile = configDir.resolve("economy.json");
    Files.writeString(economyFile, GSON.toJson(economy));
    filesCreated.add("config/economy.json");
  }

  /**
   * Extracts faction permissions to config/faction-permissions.json.
   */
  private void extractFactionPermissionsModule(JsonObject root, Path configDir, List<String> filesCreated) throws IOException {
    JsonObject factionPerms = new JsonObject();
    factionPerms.addProperty("enabled", true);

    if (root.has("factionPermissions")) {
      JsonObject source = root.getAsJsonObject("factionPermissions");

      // Copy defaults section
      if (source.has("defaults")) {
        factionPerms.add("defaults", source.get("defaults").deepCopy());
      } else {
        factionPerms.add("defaults", createDefaultPermissions());
      }

      // Copy locks section
      if (source.has("locks")) {
        factionPerms.add("locks", source.get("locks").deepCopy());
      } else {
        factionPerms.add("locks", createDefaultLocks());
      }

      // Copy forced section
      if (source.has("forced")) {
        factionPerms.add("forced", source.get("forced").deepCopy());
      } else {
        factionPerms.add("forced", createDefaultForced());
      }

      root.remove("factionPermissions");
    } else {
      factionPerms.add("defaults", createDefaultPermissions());
      factionPerms.add("locks", createDefaultLocks());
      factionPerms.add("forced", createDefaultForced());
    }

    Path factionPermsFile = configDir.resolve("faction-permissions.json");
    Files.writeString(factionPermsFile, GSON.toJson(factionPerms));
    filesCreated.add("config/faction-permissions.json");
  }

  private JsonObject createDefaultPermissions() {
    JsonObject defaults = new JsonObject();
    defaults.addProperty("outsiderBreak", false);
    defaults.addProperty("outsiderPlace", false);
    defaults.addProperty("outsiderInteract", false);
    defaults.addProperty("allyBreak", false);
    defaults.addProperty("allyPlace", false);
    defaults.addProperty("allyInteract", true);
    defaults.addProperty("memberBreak", true);
    defaults.addProperty("memberPlace", true);
    defaults.addProperty("memberInteract", true);
    defaults.addProperty("pvpEnabled", true);
    defaults.addProperty("officersCanEdit", false);
    return defaults;
  }

  private JsonObject createDefaultLocks() {
    JsonObject locks = new JsonObject();
    locks.addProperty("outsiderBreak", false);
    locks.addProperty("outsiderPlace", false);
    locks.addProperty("outsiderInteract", false);
    locks.addProperty("allyBreak", false);
    locks.addProperty("allyPlace", false);
    locks.addProperty("allyInteract", false);
    locks.addProperty("memberBreak", false);
    locks.addProperty("memberPlace", false);
    locks.addProperty("memberInteract", false);
    locks.addProperty("pvpEnabled", false);
    locks.addProperty("officersCanEdit", false);
    return locks;
  }

  private JsonObject createDefaultForced() {
    JsonObject forced = new JsonObject();
    forced.addProperty("outsiderBreak", false);
    forced.addProperty("outsiderPlace", false);
    forced.addProperty("outsiderInteract", false);
    forced.addProperty("allyBreak", false);
    forced.addProperty("allyPlace", false);
    forced.addProperty("allyInteract", true);
    forced.addProperty("memberBreak", true);
    forced.addProperty("memberPlace", true);
    forced.addProperty("memberInteract", true);
    forced.addProperty("pvpEnabled", true);
    forced.addProperty("officersCanEdit", false);
    return forced;
  }

  /**
   * Copies a property from source to target, using default if not present.
   */
  private void copyProperty(JsonObject source, JsonObject target, String key, Object defaultValue) {
    if (source.has(key)) {
      target.add(key, source.get(key));
    } else if (defaultValue instanceof Boolean b) {
      target.addProperty(key, b);
    } else if (defaultValue instanceof Number n) {
      target.addProperty(key, n);
    } else if (defaultValue instanceof String s) {
      target.addProperty(key, s);
    }
  }
}
