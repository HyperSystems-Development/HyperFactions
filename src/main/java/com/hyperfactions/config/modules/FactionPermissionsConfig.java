package com.hyperfactions.config.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.data.FactionPermissions;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for faction territory permissions.
 *
 * <p>
 * Two-section design:
 * <ul>
 *   <li><b>defaults</b>: Default values for new factions AND the forced value when locked</li>
 *   <li><b>locks</b>: Whether each flag is locked (factions can't change it)</li>
 * </ul>
 * When a flag is locked, its effective value is always the defaults value.
 *
 * <p>
 * JSON format uses nested sections grouped by role level for readability:
 * <pre>
 * {
 *   "defaults": {
 *     "outsider": { "break": false, "place": false, ... },
 *     "ally": { ... },
 *     "member": { ... },
 *     "officer": { ... },
 *     "mobSpawning": { "enabled": true, "hostile": true, ... },
 *     "treasury": { "deposit": true, "withdraw": false, "transfer": false },
 *     "pvpEnabled": true,
 *     "officersCanEdit": false
 *   },
 *   "locks": { ... same structure ... }
 * }
 * </pre>
 */
public class FactionPermissionsConfig extends ModuleConfig {

  // JSON keys for per-level flag suffixes
  private static final String[][] LEVEL_SUFFIX_MAP = {
    {"break",         "Break"},
    {"place",         "Place"},
    {"interact",      "Interact"},
    {"doorUse",       "DoorUse"},
    {"containerUse",  "ContainerUse"},
    {"benchUse",      "BenchUse"},
    {"processingUse", "ProcessingUse"},
    {"seatUse",       "SeatUse"},
    {"transportUse",  "TransportUse"},
    {"crateUse",      "CrateUse"},
    {"npcTame",       "NpcTame"},
    {"pveDamage",     "PveDamage"}
  };

  // JSON keys for mob spawning sub-flags
  private static final String[][] MOB_SPAWNING_MAP = {
    {"enabled",  "mobSpawning"},
    {"hostile",  "hostileMobSpawning"},
    {"passive",  "passiveMobSpawning"},
    {"neutral",  "neutralMobSpawning"}
  };

  // JSON keys for treasury sub-flags
  private static final String[][] TREASURY_MAP = {
    {"deposit",  FactionPermissions.TREASURY_DEPOSIT},
    {"withdraw", FactionPermissions.TREASURY_WITHDRAW},
    {"transfer", FactionPermissions.TREASURY_TRANSFER}
  };

  private Map<String, Boolean> defaults = new HashMap<>();

  private Map<String, Boolean> locks = new HashMap<>();

  /**
   * Creates a new faction permissions config.
   *
   * @param filePath path to config/faction-permissions.json
   */
  public FactionPermissionsConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "faction-permissions";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    // Initialize defaults from FactionPermissions defaults
    FactionPermissions defaultPerms = FactionPermissions.defaults();
    defaults = new HashMap<>(defaultPerms.toMap());
    // All flags unlocked by default
    locks = new HashMap<>();
    for (String flag : FactionPermissions.ALL_FLAGS) {
      locks.put(flag, false);
    }
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    // Start with default values
    FactionPermissions defaultPerms = FactionPermissions.defaults();
    defaults = new HashMap<>(defaultPerms.toMap());
    locks = new HashMap<>();
    for (String flag : FactionPermissions.ALL_FLAGS) {
      locks.put(flag, false);
    }

    // Load defaults section
    if (hasSection(root, "defaults")) {
      JsonObject defaultsObj = root.getAsJsonObject("defaults");
      loadSection(defaultsObj, defaults);
    }

    // Load locks section
    if (hasSection(root, "locks")) {
      JsonObject locksObj = root.getAsJsonObject("locks");
      loadSection(locksObj, locks);
    }
  }

  /**
   * Loads a section (defaults or locks), auto-detecting flat vs nested format.
   */
  private void loadSection(@NotNull JsonObject section, @NotNull Map<String, Boolean> target) {
    // Detect format: if "outsider" is a JsonObject, it's nested; if "outsiderBreak" is present, it's flat
    boolean isNested = false;
    for (String level : FactionPermissions.ALL_LEVELS) {
      if (section.has(level) && section.get(level).isJsonObject()) {
        isNested = true;
        break;
      }
    }

    if (isNested) {
      loadNestedSection(section, target);
    } else {
      loadFlatSection(section, target);
    }
  }

  /**
   * Loads the new nested JSON format.
   */
  private void loadNestedSection(@NotNull JsonObject section, @NotNull Map<String, Boolean> target) {
    // Per-level flags
    for (String level : FactionPermissions.ALL_LEVELS) {
      if (section.has(level) && section.get(level).isJsonObject()) {
        JsonObject levelObj = section.getAsJsonObject(level);
        for (String[] mapping : LEVEL_SUFFIX_MAP) {
          String jsonKey = mapping[0];
          String flagKey = level + mapping[1];
          if (levelObj.has(jsonKey) && levelObj.get(jsonKey).isJsonPrimitive()) {
            target.put(flagKey, levelObj.get(jsonKey).getAsBoolean());
          }
        }
      }
    }

    // Mob spawning
    if (section.has("mobSpawning") && section.get("mobSpawning").isJsonObject()) {
      JsonObject mobObj = section.getAsJsonObject("mobSpawning");
      for (String[] mapping : MOB_SPAWNING_MAP) {
        String jsonKey = mapping[0];
        String flagKey = mapping[1];
        if (mobObj.has(jsonKey) && mobObj.get(jsonKey).isJsonPrimitive()) {
          target.put(flagKey, mobObj.get(jsonKey).getAsBoolean());
        }
      }
    }

    // Treasury flags
    if (section.has("treasury") && section.get("treasury").isJsonObject()) {
      JsonObject treasuryObj = section.getAsJsonObject("treasury");
      for (String[] mapping : TREASURY_MAP) {
        String jsonKey = mapping[0];
        String flagKey = mapping[1];
        if (treasuryObj.has(jsonKey) && treasuryObj.get(jsonKey).isJsonPrimitive()) {
          target.put(flagKey, treasuryObj.get(jsonKey).getAsBoolean());
        }
      }
    }

    // Global flags (flat at top level)
    if (section.has("pvpEnabled") && section.get("pvpEnabled").isJsonPrimitive()) {
      target.put(FactionPermissions.PVP_ENABLED, section.get("pvpEnabled").getAsBoolean());
    }
    if (section.has("officersCanEdit") && section.get("officersCanEdit").isJsonPrimitive()) {
      target.put(FactionPermissions.OFFICERS_CAN_EDIT, section.get("officersCanEdit").getAsBoolean());
    }
  }

  /**
   * Loads the old flat JSON format (backward compatibility).
   */
  private void loadFlatSection(@NotNull JsonObject section, @NotNull Map<String, Boolean> target) {
    for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
      if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
        if (FactionPermissions.isValidFlag(entry.getKey())) {
          target.put(entry.getKey(), entry.getValue().getAsBoolean());
        }
      }
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.add("defaults", writeNestedSection(defaults));
    root.add("locks", writeNestedSection(locks));
  }

  /**
   * Writes a section in the nested JSON format grouped by role level.
   */
  @NotNull
  private JsonObject writeNestedSection(@NotNull Map<String, Boolean> source) {
    JsonObject section = new JsonObject();

    // Per-level flags
    for (String level : FactionPermissions.ALL_LEVELS) {
      JsonObject levelObj = new JsonObject();
      for (String[] mapping : LEVEL_SUFFIX_MAP) {
        String jsonKey = mapping[0];
        String flagKey = level + mapping[1];
        levelObj.addProperty(jsonKey, source.getOrDefault(flagKey, false));
      }
      section.add(level, levelObj);
    }

    // Mob spawning
    JsonObject mobObj = new JsonObject();
    for (String[] mapping : MOB_SPAWNING_MAP) {
      String jsonKey = mapping[0];
      String flagKey = mapping[1];
      mobObj.addProperty(jsonKey, source.getOrDefault(flagKey, false));
    }
    section.add("mobSpawning", mobObj);

    // Treasury flags
    JsonObject treasuryObj = new JsonObject();
    for (String[] mapping : TREASURY_MAP) {
      String jsonKey = mapping[0];
      String flagKey = mapping[1];
      treasuryObj.addProperty(jsonKey, source.getOrDefault(flagKey, false));
    }
    section.add("treasury", treasuryObj);

    // Global flags
    section.addProperty("pvpEnabled", source.getOrDefault(FactionPermissions.PVP_ENABLED, true));
    section.addProperty("officersCanEdit", source.getOrDefault(FactionPermissions.OFFICERS_CAN_EDIT, false));

    return section;
  }

  // === FactionPermissions Integration ===

  /**
   * Gets the default permissions for new factions based on config.
   *
   * @return the default faction permissions from config
   */
  @NotNull
  public FactionPermissions getDefaultFactionPermissions() {
    return new FactionPermissions(new HashMap<>(defaults));
  }

  /**
   * Gets the effective permissions for a faction, applying server locks.
   * For each flag: if locked, use the default value; else use faction value.
   *
   * @param factionPerms the faction's permission settings
   * @return the effective permissions after applying server locks
   */
  @NotNull
  public FactionPermissions getEffectiveFactionPermissions(@NotNull FactionPermissions factionPerms) {
    Map<String, Boolean> effective = new HashMap<>(factionPerms.toMap());
    for (Map.Entry<String, Boolean> entry : locks.entrySet()) {
      if (entry.getValue()) {
        // Flag is locked — use default value
        effective.put(entry.getKey(), defaults.getOrDefault(entry.getKey(), false));
      }
    }
    return new FactionPermissions(effective);
  }

  /**
   * Checks if a specific permission is locked by the server.
   *
   * @param permissionName the permission name (e.g., "outsiderBreak", "pvpEnabled")
   * @return true if locked, false if factions can change it
   */
  public boolean isPermissionLocked(@NotNull String permissionName) {
    return locks.getOrDefault(permissionName, false);
  }

  /**
   * Gets the default value of a permission flag.
   *
   * @param flag the flag name
   * @return true if the flag is enabled by default
   */
  public boolean getDefault(@NotNull String flag) {
    return defaults.getOrDefault(flag, false);
  }

  /**
   * Sets the default value of a permission flag.
   *
   * @param flag  the flag name
   * @param value true to enable, false to disable
   */
  public void setDefault(@NotNull String flag, boolean value) {
    defaults.put(flag, value);
  }

  /**
   * Sets the lock state of a permission flag.
   *
   * @param flag   the flag name
   * @param locked true to lock, false to unlock
   */
  public void setLocked(@NotNull String flag, boolean locked) {
    locks.put(flag, locked);
  }
}
