package com.hyperfactions.migration.migrations.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.migration.Migration;
import com.hyperfactions.migration.MigrationOptions;
import com.hyperfactions.migration.MigrationResult;
import com.hyperfactions.migration.MigrationType;
import com.hyperfactions.util.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Migrates configuration from v3 to v4.
 *
 * This migration:
 * - Converts faction-permissions.json from 3-section (defaults/locks/forced) to 2-section (defaults/locks)
 * - When a flag was locked, merges forced value into defaults (locked flags use default value)
 * - Adds new flags: officer permissions, interaction sub-types, mob spawning
 * - Restructures flat format to nested role-level sections for readability
 * - Updates configVersion to 4
 */
public class ConfigV3ToV4Migration implements Migration {

  private static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  // JSON keys for per-level flag suffixes (nested format)
  private static final String[][] LEVEL_SUFFIX_MAP = {
    {"break",         "Break"},
    {"place",         "Place"},
    {"interact",      "Interact"},
    {"doorUse",       "DoorUse"},
    {"containerUse",  "ContainerUse"},
    {"benchUse",      "BenchUse"},
    {"processingUse", "ProcessingUse"},
    {"seatUse",       "SeatUse"}
  };

  // JSON keys for mob spawning sub-flags (nested format)
  private static final String[][] MOB_SPAWNING_MAP = {
    {"enabled",  "mobSpawning"},
    {"hostile",  "hostileMobSpawning"},
    {"passive",  "passiveMobSpawning"},
    {"neutral",  "neutralMobSpawning"}
  };

  /** Id. */
  @Override
  @NotNull
  public String id() {
    return "config-v3-to-v4";
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
    return 3;
  }

  /** Converts to version. */
  @Override
  public int toVersion() {
    return 4;
  }

  /** Description. */
  @Override
  @NotNull
  public String description() {
    return "Expand faction permissions: officer level, interaction sub-types, mob spawning; simplify config to defaults/locks; restructure to nested format";
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
      return root.get("configVersion").getAsInt() == 3;
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
    Path permissionsFile = dataDir.resolve("config").resolve("faction-permissions.json");

    try {
      options.reportProgress("Reading v3 config", 1, 4);

      // Step 1: Migrate faction-permissions.json
      if (Files.exists(permissionsFile)) {
        options.reportProgress("Migrating faction-permissions.json", 2, 4);
        migratePermissionsConfig(permissionsFile, warnings);
        filesModified.add("config/faction-permissions.json");
      } else {
        warnings.add("faction-permissions.json not found, will be created on load with new format");
      }

      options.reportProgress("Updating config version", 3, 4);

      // Step 2: Update configVersion in config.json
      String json = Files.readString(configFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      root.addProperty("configVersion", 4);
      Files.writeString(configFile, GSON.toJson(root));
      filesModified.add("config.json");

      options.reportProgress("Migration complete", 4, 4);

      Duration duration = Duration.between(startTime, Instant.now());
      Logger.info("[Migration] Config migration v3->v4 completed in %dms", duration.toMillis());

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
      Logger.severe("[Migration] Config migration v3->v4 failed: %s", e.getMessage());
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

  private void migratePermissionsConfig(Path permissionsFile, List<String> warnings) throws Exception {
    String json = Files.readString(permissionsFile);
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();

    // Read old sections
    JsonObject oldDefaults = root.has("defaults") ? root.getAsJsonObject("defaults") : new JsonObject();
    JsonObject oldLocks = root.has("locks") ? root.getAsJsonObject("locks") : new JsonObject();
    JsonObject oldForced = root.has("forced") ? root.getAsJsonObject("forced") : new JsonObject();

    // Build new flat defaults: for locked flags, use forced value; else use old default
    JsonObject newDefaults = new JsonObject();
    JsonObject newLocks = new JsonObject();

    // Old flags from v3
    String[] oldFlags = {
      "outsiderBreak", "outsiderPlace", "outsiderInteract",
      "allyBreak", "allyPlace", "allyInteract",
      "memberBreak", "memberPlace", "memberInteract",
      "pvpEnabled", "officersCanEdit"
    };

    for (String flag : oldFlags) {
      boolean isLocked = getBool(oldLocks, flag, false);
      boolean defaultVal = getBool(oldDefaults, flag, getV3Default(flag));
      boolean forcedVal = getBool(oldForced, flag, defaultVal);

      // If locked, merge forced value into defaults
      newDefaults.addProperty(flag, isLocked ? forcedVal : defaultVal);
      newLocks.addProperty(flag, isLocked);
    }

    // Add new officer flags (derived from member defaults)
    boolean memberBreakDefault = getBool(newDefaults, "memberBreak", true);
    boolean memberPlaceDefault = getBool(newDefaults, "memberPlace", true);
    boolean memberInteractDefault = getBool(newDefaults, "memberInteract", true);

    newDefaults.addProperty("officerBreak", memberBreakDefault);
    newDefaults.addProperty("officerPlace", memberPlaceDefault);
    newDefaults.addProperty("officerInteract", memberInteractDefault);
    newDefaults.addProperty("officerDoorUse", memberInteractDefault);
    newDefaults.addProperty("officerContainerUse", memberInteractDefault);
    newDefaults.addProperty("officerBenchUse", memberInteractDefault);
    newDefaults.addProperty("officerProcessingUse", memberInteractDefault);
    newDefaults.addProperty("officerSeatUse", memberInteractDefault);

    // Add interaction sub-type flags for existing levels (derived from interact flag)
    for (String level : new String[]{"outsider", "ally", "member"}) {
      boolean interactDefault = getBool(newDefaults, level + "Interact", false);
      newDefaults.addProperty(level + "DoorUse", interactDefault);
      newDefaults.addProperty(level + "ContainerUse", interactDefault);
      newDefaults.addProperty(level + "BenchUse", interactDefault);
      newDefaults.addProperty(level + "ProcessingUse", interactDefault);
      newDefaults.addProperty(level + "SeatUse", interactDefault);
    }

    // Add mob spawning flags (enabled by default = no spawn blocking)
    newDefaults.addProperty("mobSpawning", true);
    newDefaults.addProperty("hostileMobSpawning", true);
    newDefaults.addProperty("passiveMobSpawning", true);
    newDefaults.addProperty("neutralMobSpawning", true);

    // Add locks for all new flags (unlocked by default)
    for (String flag : FactionPermissions.ALL_FLAGS) {
      if (!newLocks.has(flag)) {
        newLocks.addProperty(flag, false);
      }
    }

    // Convert flat maps to nested role-level format
    JsonObject nestedDefaults = convertToNested(newDefaults);
    JsonObject nestedLocks = convertToNested(newLocks);

    // Build new root with nested format
    JsonObject newRoot = new JsonObject();
    // Preserve enabled flag if present
    if (root.has("enabled")) {
      newRoot.addProperty("enabled", root.get("enabled").getAsBoolean());
    }
    newRoot.add("defaults", nestedDefaults);
    newRoot.add("locks", nestedLocks);

    Files.writeString(permissionsFile, GSON.toJson(newRoot));

    Logger.info("[Migration] Migrated faction-permissions.json: removed 'forced' section, added %d new flags, restructured to nested format",
      FactionPermissions.ALL_FLAGS.size() - oldFlags.length);
  }

  /**
   * Converts a flat flag map to nested role-level JSON structure.
   */
  private JsonObject convertToNested(JsonObject flat) {
    JsonObject nested = new JsonObject();

    // Per-level flags
    for (String level : FactionPermissions.ALL_LEVELS) {
      JsonObject levelObj = new JsonObject();
      for (String[] mapping : LEVEL_SUFFIX_MAP) {
        String jsonKey = mapping[0];
        String flatKey = level + mapping[1];
        levelObj.addProperty(jsonKey, getBool(flat, flatKey, false));
      }
      nested.add(level, levelObj);
    }

    // Mob spawning
    JsonObject mobObj = new JsonObject();
    for (String[] mapping : MOB_SPAWNING_MAP) {
      String jsonKey = mapping[0];
      String flatKey = mapping[1];
      mobObj.addProperty(jsonKey, getBool(flat, flatKey, false));
    }
    nested.add("mobSpawning", mobObj);

    // Global flags
    nested.addProperty("pvpEnabled", getBool(flat, "pvpEnabled", true));
    nested.addProperty("officersCanEdit", getBool(flat, "officersCanEdit", false));

    return nested;
  }

  private boolean getBool(JsonObject obj, String key, boolean defaultValue) {
    if (obj != null && obj.has(key)) {
      JsonElement el = obj.get(key);
      if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean()) {
        return el.getAsBoolean();
      }
    }
    return defaultValue;
  }

  private boolean getV3Default(String flag) {
    return switch (flag) {
      case "outsiderBreak", "outsiderPlace", "outsiderInteract" -> false;
      case "allyBreak", "allyPlace" -> false;
      case "allyInteract" -> true;
      case "memberBreak", "memberPlace", "memberInteract" -> true;
      case "pvpEnabled" -> true;
      case "officersCanEdit" -> false;
      default -> false;
    };
  }
}
