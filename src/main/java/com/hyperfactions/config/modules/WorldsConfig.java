package com.hyperfactions.config.modules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.nio.file.Path;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Per-world settings configuration.
 *
 * <p>
 * Controls which behaviors are active in each world. Worlds can be configured
 * individually with exact names or using wildcard patterns (% matches any sequence).
 *
 * <p>
 * Config file: config/worlds.json
 * <pre>
 * {
 *   "enabled": true,
 *   "defaultPolicy": "allow",
 *   "worlds": {
 *     "events": { "claiming": false, "powerLoss": false, "friendlyFireFaction": true },
 *     "arena_%": { "claiming": false, "powerLoss": false, "friendlyFireFaction": true, "friendlyFireAlly": true }
 *   },
 *   "claimBlacklist": []
 * }
 * </pre>
 */
public class WorldsConfig extends ModuleConfig {

  /**
   * Per-world settings record.
   *
   * @param claiming           whether claiming is allowed (null = use default)
   * @param powerLoss          whether power loss on death applies (null = use default)
   * @param friendlyFireFaction whether faction-on-faction friendly fire is allowed (null = use default)
   * @param friendlyFireAlly   whether ally-on-ally friendly fire is allowed (null = use default)
   */
  public record WorldSettings(
      Boolean claiming,
      Boolean powerLoss,
      Boolean friendlyFireFaction,
      Boolean friendlyFireAlly
  ) {
    /** Default settings — all null means defer to global config. */
    public static final WorldSettings DEFAULTS = new WorldSettings(null, null, null, null);
  }

  private String defaultPolicy = "allow";

  private final Map<String, WorldSettings> worlds = new LinkedHashMap<>();

  private List<String> claimBlacklist = new ArrayList<>();

  /** Creates a new WorldsConfig. */
  public WorldsConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "worlds";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    defaultPolicy = "allow";
    worlds.clear();
    // Block claiming in temporary instance worlds (power loss defers to global config)
    worlds.put("instance-%", new WorldSettings(false, null, null, null));
    // Example entry showing all available options (non-matching name won't affect real worlds)
    worlds.put("example-world-abc", new WorldSettings(true, true, false, false));
    claimBlacklist = new ArrayList<>();
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultPolicy = getString(root, "defaultPolicy", defaultPolicy);
    claimBlacklist = getStringList(root, "claimBlacklist");

    worlds.clear();
    if (root.has("worlds") && root.get("worlds").isJsonObject()) {
      JsonObject worldsObj = root.getAsJsonObject("worlds");
      for (Map.Entry<String, JsonElement> entry : worldsObj.entrySet()) {
        if (entry.getValue().isJsonObject()) {
          JsonObject worldObj = entry.getValue().getAsJsonObject();
          WorldSettings settings = new WorldSettings(
              getNullableBool(worldObj, "claiming"),
              getNullableBool(worldObj, "powerLoss"),
              getNullableBool(worldObj, "friendlyFireFaction"),
              getNullableBool(worldObj, "friendlyFireAlly")
          );
          worlds.put(entry.getKey(), settings);
        }
      }
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultPolicy", defaultPolicy);

    JsonObject worldsObj = new JsonObject();
    for (Map.Entry<String, WorldSettings> entry : worlds.entrySet()) {
      JsonObject worldObj = new JsonObject();
      WorldSettings s = entry.getValue();
      if (s.claiming() != null) {
        worldObj.addProperty("claiming", s.claiming());
      }
      if (s.powerLoss() != null) {
        worldObj.addProperty("powerLoss", s.powerLoss());
      }
      if (s.friendlyFireFaction() != null) {
        worldObj.addProperty("friendlyFireFaction", s.friendlyFireFaction());
      }
      if (s.friendlyFireAlly() != null) {
        worldObj.addProperty("friendlyFireAlly", s.friendlyFireAlly());
      }
      worldsObj.add(entry.getKey(), worldObj);
    }
    root.add("worlds", worldsObj);

    root.add("claimBlacklist", toJsonArray(claimBlacklist));
  }

  // === Getters ===

  /** Returns the default policy. */
  @NotNull
  public String getDefaultPolicy() {
    return defaultPolicy;
  }

  /** Returns the worlds. */
  @NotNull
  public Map<String, WorldSettings> getWorlds() {
    return Collections.unmodifiableMap(worlds);
  }

  /** Returns the claim blacklist. */
  @NotNull
  public List<String> getClaimBlacklist() {
    return claimBlacklist;
  }

  /**
   * Gets the settings for a specific world key (exact key match, no wildcard resolution).
   *
   * @param worldKey the exact key in the config
   * @return the settings, or null if not configured
   */
  public WorldSettings getWorldSettings(@NotNull String worldKey) {
    return worlds.get(worldKey);
  }

  /**
   * Sets the settings for a world key and marks dirty.
   *
   * @param worldKey the world name or pattern
   * @param settings the settings to apply
   */
  public void setWorldSettings(@NotNull String worldKey, @NotNull WorldSettings settings) {
    worlds.put(worldKey, settings);
  }

  /**
   * Removes a world entry and marks dirty.
   *
   * @param worldKey the world name or pattern to remove
   * @return true if an entry was removed
   */
  public boolean removeWorldSettings(@NotNull String worldKey) {
    return worlds.remove(worldKey) != null;
  }

  // === Validation ===

  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    if (!"allow".equals(defaultPolicy) && !"deny".equals(defaultPolicy)) {
      result.addWarning("worlds", "defaultPolicy",
          "must be 'allow' or 'deny'", defaultPolicy, "allow");
      defaultPolicy = "allow";
    }

    return result;
  }

  // === Helpers ===

  /**
   * Gets a nullable Boolean from a JSON object.
   * Returns null if the key doesn't exist or is null.
   */
  private Boolean getNullableBool(@NotNull JsonObject obj, @NotNull String key) {
    if (obj.has(key) && !obj.get(key).isJsonNull()) {
      return obj.get(key).getAsBoolean();
    }
    return null;
  }
}
