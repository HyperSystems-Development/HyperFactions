package com.hyperfactions.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Legacy core configuration reader for config.json.
 *
 * <p>
 * <b>Deprecated:</b> As of v0.9.2 (config version 6), settings have been split into
 * {@code config/factions.json} ({@link com.hyperfactions.config.modules.FactionsConfig})
 * and {@code config/server.json} ({@link com.hyperfactions.config.modules.ServerConfig}).
 *
 * <p>
 * This class is retained only as a fallback reader: if migration V5→V6 fails or is
 * interrupted, ConfigManager can still load settings from the old monolithic config.json
 * via this class. It is <b>never used for new installs</b> and will not write files.
 *
 * @deprecated Use {@link com.hyperfactions.config.modules.FactionsConfig} and
 *             {@link com.hyperfactions.config.modules.ServerConfig} instead.
 */
@Deprecated(since = "0.4.0", forRemoval = true)
public class CoreConfig extends ConfigFile {

  // Config version (for migration)
  private int configVersion = 2;

  // Faction settings
  private int maxMembers = 50;

  private int maxMembershipHistory = 10;

  private int maxNameLength = 24;

  private int minNameLength = 3;

  private boolean allowColors = true;

  // Power settings
  private double maxPlayerPower = 20.0;

  private double startingPower = 10.0;

  private double powerPerClaim = 2.0;

  private double deathPenalty = 1.0;

  private double killReward = 0.0;

  private boolean killRewardRequiresFaction = true;

  private boolean powerLossOnMobDeath = true;

  private boolean powerLossOnEnvironmentalDeath = true;

  private double regenPerMinute = 0.1;

  private boolean regenWhenOffline = false;

  private boolean hardcoreMode = false;

  // Claim settings
  private int maxClaims = 100;

  private boolean onlyAdjacent = false;

  private boolean preventDisconnect = false;

  private boolean decayEnabled = true;

  private int decayDaysInactive = 30;

  private boolean allowExplosionsInClaims = false;

  private List<String> worldWhitelist = new ArrayList<>();

  private List<String> worldBlacklist = new ArrayList<>();

  // Combat settings
  private int tagDurationSeconds = 15;

  private boolean allyDamage = false;

  private boolean factionDamage = false;

  private boolean taggedLogoutPenalty = true;

  private double logoutPowerLoss = 1.0;

  private double neutralAttackPenalty = 0.0;

  // Spawn protection settings
  private boolean spawnProtectionEnabled = true;

  private int spawnProtectionDurationSeconds = 5;

  private boolean spawnProtectionBreakOnAttack = true;

  private boolean spawnProtectionBreakOnMove = true;

  // Relation settings
  private int maxAllies = 10;

  private int maxEnemies = -1;

  // Invite/Request settings
  private int inviteExpirationMinutes = 5;

  private int joinRequestExpirationHours = 24;

  // Stuck command settings
  private int stuckMinRadius = 3;

  private int stuckRadiusIncrease = 3;

  private int stuckMaxAttempts = 10;

  private int stuckWarmupSeconds = 30;

  private int stuckCooldownSeconds = 300;

  // Teleport settings
  private int warmupSeconds = 5;

  private int cooldownSeconds = 300;

  private boolean cancelOnMove = true;

  private boolean cancelOnDamage = true;

  // Update settings
  private boolean updateCheckEnabled = true;

  private static final String OLD_ZENITH_URL = "https://api.github.com/repos/ZenithDevHQ/HyperFactions/releases/latest";

  private String updateCheckUrl = "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";

  private String releaseChannel = "stable";

  // HyperProtect-Mixin management
  private boolean hyperProtectAutoDownload = false;

  private boolean hyperProtectAutoUpdate = true;

  private String hyperProtectUpdateUrl = "https://api.github.com/repos/HyperSystemsDev/HyperProtect-Mixin/releases/latest";

  // Auto-save settings
  private boolean autoSaveEnabled = true;

  private int autoSaveIntervalMinutes = 5;

  // Message settings (v3 format: structured prefix)
  private String prefixText = "HyperFactions";

  private String prefixColor = "#55FFFF";

  private String prefixBracketColor = "#AAAAAA";

  private String primaryColor = "#00FFFF";

  // GUI settings
  private String guiTitle = "HyperFactions";

  private boolean terrainMapEnabled = true;

  // Territory notification settings
  private boolean territoryNotificationsEnabled = true;

  // Permission settings
  private boolean adminRequiresOp = true;

  private boolean allowWithoutPermissionMod = false;

  /**
   * Creates a new core config.
   *
   * @param filePath path to config.json
   */
  public CoreConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    configVersion = 4;
    // Defaults are already set in field declarations
  }

  /** Loads from json. */
  @Override
  protected void loadFromJson(@NotNull JsonObject root) {
    configVersion = getInt(root, "configVersion", configVersion);

    // Faction settings
    if (hasSection(root, "faction")) {
      JsonObject faction = root.getAsJsonObject("faction");
      maxMembers = getInt(faction, "maxMembers", maxMembers);
      maxMembershipHistory = getInt(faction, "maxMembershipHistory", maxMembershipHistory);
      maxNameLength = getInt(faction, "maxNameLength", maxNameLength);
      minNameLength = getInt(faction, "minNameLength", minNameLength);
      allowColors = getBool(faction, "allowColors", allowColors);
    }

    // Power settings
    if (hasSection(root, "power")) {
      JsonObject power = root.getAsJsonObject("power");
      maxPlayerPower = getDouble(power, "maxPlayerPower", maxPlayerPower);
      startingPower = getDouble(power, "startingPower", startingPower);
      powerPerClaim = getDouble(power, "powerPerClaim", powerPerClaim);
      deathPenalty = getDouble(power, "deathPenalty", deathPenalty);
      killReward = getDouble(power, "killReward", killReward);
      killRewardRequiresFaction = getBool(power, "killRewardRequiresFaction", killRewardRequiresFaction);
      powerLossOnMobDeath = getBool(power, "powerLossOnMobDeath", powerLossOnMobDeath);
      powerLossOnEnvironmentalDeath = getBool(power, "powerLossOnEnvironmentalDeath", powerLossOnEnvironmentalDeath);
      regenPerMinute = getDouble(power, "regenPerMinute", regenPerMinute);
      regenWhenOffline = getBool(power, "regenWhenOffline", regenWhenOffline);
      hardcoreMode = getBool(power, "hardcoreMode", hardcoreMode);
    }

    // Claim settings
    if (hasSection(root, "claims")) {
      JsonObject claims = root.getAsJsonObject("claims");
      maxClaims = getInt(claims, "maxClaims", maxClaims);
      onlyAdjacent = getBool(claims, "onlyAdjacent", onlyAdjacent);
      preventDisconnect = getBool(claims, "preventDisconnect", preventDisconnect);
      decayEnabled = getBool(claims, "decayEnabled", decayEnabled);
      decayDaysInactive = getInt(claims, "decayDaysInactive", decayDaysInactive);
      allowExplosionsInClaims = getBool(claims, "allowExplosionsInClaims", allowExplosionsInClaims);
      worldWhitelist = getStringList(claims, "worldWhitelist");
      worldBlacklist = getStringList(claims, "worldBlacklist");
    }

    // Combat settings
    if (hasSection(root, "combat")) {
      JsonObject combat = root.getAsJsonObject("combat");
      tagDurationSeconds = getInt(combat, "tagDurationSeconds", tagDurationSeconds);
      allyDamage = getBool(combat, "allyDamage", allyDamage);
      factionDamage = getBool(combat, "factionDamage", factionDamage);
      taggedLogoutPenalty = getBool(combat, "taggedLogoutPenalty", taggedLogoutPenalty);
      logoutPowerLoss = getDouble(combat, "logoutPowerLoss", logoutPowerLoss);
      neutralAttackPenalty = getDouble(combat, "neutralAttackPenalty", neutralAttackPenalty);

      // Spawn protection sub-section
      if (hasSection(combat, "spawnProtection")) {
        JsonObject spawnProt = combat.getAsJsonObject("spawnProtection");
        spawnProtectionEnabled = getBool(spawnProt, "enabled", spawnProtectionEnabled);
        spawnProtectionDurationSeconds = getInt(spawnProt, "durationSeconds", spawnProtectionDurationSeconds);
        spawnProtectionBreakOnAttack = getBool(spawnProt, "breakOnAttack", spawnProtectionBreakOnAttack);
        spawnProtectionBreakOnMove = getBool(spawnProt, "breakOnMove", spawnProtectionBreakOnMove);
      }
    }

    // Relation settings
    if (hasSection(root, "relations")) {
      JsonObject relations = root.getAsJsonObject("relations");
      maxAllies = getInt(relations, "maxAllies", maxAllies);
      maxEnemies = getInt(relations, "maxEnemies", maxEnemies);
    }

    // Invite/Request settings
    if (hasSection(root, "invites")) {
      JsonObject invites = root.getAsJsonObject("invites");
      inviteExpirationMinutes = getInt(invites, "inviteExpirationMinutes", inviteExpirationMinutes);
      joinRequestExpirationHours = getInt(invites, "joinRequestExpirationHours", joinRequestExpirationHours);
    }

    // Stuck settings
    if (hasSection(root, "stuck")) {
      JsonObject stuck = root.getAsJsonObject("stuck");
      stuckMinRadius = getInt(stuck, "minRadius", stuckMinRadius);
      stuckRadiusIncrease = getInt(stuck, "radiusIncrease", stuckRadiusIncrease);
      stuckMaxAttempts = getInt(stuck, "maxAttempts", stuckMaxAttempts);
      stuckWarmupSeconds = getInt(stuck, "warmupSeconds", stuckWarmupSeconds);
      stuckCooldownSeconds = getInt(stuck, "cooldownSeconds", stuckCooldownSeconds);
    }

    // Teleport settings
    if (hasSection(root, "teleport")) {
      JsonObject teleport = root.getAsJsonObject("teleport");
      warmupSeconds = getInt(teleport, "warmupSeconds", warmupSeconds);
      cooldownSeconds = getInt(teleport, "cooldownSeconds", cooldownSeconds);
      cancelOnMove = getBool(teleport, "cancelOnMove", cancelOnMove);
      cancelOnDamage = getBool(teleport, "cancelOnDamage", cancelOnDamage);
    }

    // Update settings
    if (hasSection(root, "updates")) {
      JsonObject updates = root.getAsJsonObject("updates");
      updateCheckEnabled = getBool(updates, "enabled", updateCheckEnabled);
      updateCheckUrl = getString(updates, "url", updateCheckUrl);
      if (OLD_ZENITH_URL.equals(updateCheckUrl)) {
        updateCheckUrl = "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";
        needsSave = true;
      }
      releaseChannel = getString(updates, "releaseChannel", releaseChannel);
      // Validate release channel
      if (!releaseChannel.equals("stable") && !releaseChannel.equals("prerelease")) {
        releaseChannel = "stable";
      }

      // HyperProtect-Mixin sub-section
      if (hasSection(updates, "hyperProtect")) {
        JsonObject hp = updates.getAsJsonObject("hyperProtect");
        hyperProtectAutoDownload = getBool(hp, "autoDownload", hyperProtectAutoDownload);
        hyperProtectAutoUpdate = getBool(hp, "autoUpdate", hyperProtectAutoUpdate);
        hyperProtectUpdateUrl = getString(hp, "url", hyperProtectUpdateUrl);
      }
    }

    // Auto-save settings
    if (hasSection(root, "autoSave")) {
      JsonObject autoSave = root.getAsJsonObject("autoSave");
      autoSaveEnabled = getBool(autoSave, "enabled", autoSaveEnabled);
      autoSaveIntervalMinutes = getInt(autoSave, "intervalMinutes", autoSaveIntervalMinutes);
    }

    // Message settings (supports both v2 string format and v3 structured format)
    if (hasSection(root, "messages")) {
      JsonObject messages = root.getAsJsonObject("messages");
      primaryColor = getString(messages, "primaryColor", primaryColor);

      // Handle prefix - can be string (v2) or object (v3)
      if (messages.has("prefix")) {
        var prefixElement = messages.get("prefix");
        if (prefixElement.isJsonObject()) {
          // v3 format: structured object
          JsonObject prefixObj = prefixElement.getAsJsonObject();
          prefixText = getString(prefixObj, "text", prefixText);
          prefixColor = getString(prefixObj, "color", prefixColor);
          prefixBracketColor = getString(prefixObj, "bracketColor", prefixBracketColor);
        } else {
          // v2 format: plain string - will be migrated on next save
          String oldPrefix = prefixElement.getAsString();
          // Strip color codes and extract text
          prefixText = oldPrefix
              .replaceAll("\u00A7[0-9a-fk-or]", "")
              .replaceAll("&[0-9a-fk-or]", "")
              .replaceAll("[\\[\\]]", "")
              .trim();
          if (prefixText.isEmpty()) {
            prefixText = "HyperFactions";
          }
        }
      }
    }

    // GUI settings
    if (hasSection(root, "gui")) {
      JsonObject gui = root.getAsJsonObject("gui");
      guiTitle = getString(gui, "title", guiTitle);
      terrainMapEnabled = getBool(gui, "terrainMapEnabled", terrainMapEnabled);
    }

    // Territory notification settings
    if (hasSection(root, "territoryNotifications")) {
      JsonObject notifications = root.getAsJsonObject("territoryNotifications");
      territoryNotificationsEnabled = getBool(notifications, "enabled", territoryNotificationsEnabled);
    }

    // Note: worldMap section removed in v3 - settings now in config/worldmap.json

    // Permission settings
    if (hasSection(root, "permissions")) {
      JsonObject permissions = root.getAsJsonObject("permissions");
      adminRequiresOp = getBool(permissions, "adminRequiresOp", adminRequiresOp);
      allowWithoutPermissionMod = getBool(permissions, "allowWithoutPermissionMod", allowWithoutPermissionMod);
    }
  }

  /** Converts to json. */
  @Override
  @NotNull
  protected JsonObject toJson() {
    JsonObject root = new JsonObject();

    root.addProperty("configVersion", configVersion);

    // Faction settings
    JsonObject faction = new JsonObject();
    faction.addProperty("maxMembers", maxMembers);
    faction.addProperty("maxMembershipHistory", maxMembershipHistory);
    faction.addProperty("maxNameLength", maxNameLength);
    faction.addProperty("minNameLength", minNameLength);
    faction.addProperty("allowColors", allowColors);
    root.add("faction", faction);

    // Power settings
    JsonObject power = new JsonObject();
    power.addProperty("maxPlayerPower", maxPlayerPower);
    power.addProperty("startingPower", startingPower);
    power.addProperty("powerPerClaim", powerPerClaim);
    power.addProperty("deathPenalty", deathPenalty);
    power.addProperty("killReward", killReward);
    power.addProperty("killRewardRequiresFaction", killRewardRequiresFaction);
    power.addProperty("powerLossOnMobDeath", powerLossOnMobDeath);
    power.addProperty("powerLossOnEnvironmentalDeath", powerLossOnEnvironmentalDeath);
    power.addProperty("regenPerMinute", regenPerMinute);
    power.addProperty("regenWhenOffline", regenWhenOffline);
    power.addProperty("hardcoreMode", hardcoreMode);
    root.add("power", power);

    // Claim settings
    JsonObject claims = new JsonObject();
    claims.addProperty("maxClaims", maxClaims);
    claims.addProperty("onlyAdjacent", onlyAdjacent);
    claims.addProperty("preventDisconnect", preventDisconnect);
    claims.addProperty("decayEnabled", decayEnabled);
    claims.addProperty("decayDaysInactive", decayDaysInactive);
    claims.addProperty("allowExplosionsInClaims", allowExplosionsInClaims);
    claims.add("worldWhitelist", toJsonArray(worldWhitelist));
    claims.add("worldBlacklist", toJsonArray(worldBlacklist));
    root.add("claims", claims);

    // Combat settings
    JsonObject combat = new JsonObject();
    combat.addProperty("tagDurationSeconds", tagDurationSeconds);
    combat.addProperty("allyDamage", allyDamage);
    combat.addProperty("factionDamage", factionDamage);
    combat.addProperty("taggedLogoutPenalty", taggedLogoutPenalty);
    combat.addProperty("logoutPowerLoss", logoutPowerLoss);
    combat.addProperty("neutralAttackPenalty", neutralAttackPenalty);

    JsonObject spawnProt = new JsonObject();
    spawnProt.addProperty("enabled", spawnProtectionEnabled);
    spawnProt.addProperty("durationSeconds", spawnProtectionDurationSeconds);
    spawnProt.addProperty("breakOnAttack", spawnProtectionBreakOnAttack);
    spawnProt.addProperty("breakOnMove", spawnProtectionBreakOnMove);
    combat.add("spawnProtection", spawnProt);
    root.add("combat", combat);

    // Relation settings
    JsonObject relations = new JsonObject();
    relations.addProperty("maxAllies", maxAllies);
    relations.addProperty("maxEnemies", maxEnemies);
    root.add("relations", relations);

    // Invite/Request settings
    JsonObject invites = new JsonObject();
    invites.addProperty("inviteExpirationMinutes", inviteExpirationMinutes);
    invites.addProperty("joinRequestExpirationHours", joinRequestExpirationHours);
    root.add("invites", invites);

    // Stuck settings
    JsonObject stuck = new JsonObject();
    stuck.addProperty("minRadius", stuckMinRadius);
    stuck.addProperty("radiusIncrease", stuckRadiusIncrease);
    stuck.addProperty("maxAttempts", stuckMaxAttempts);
    stuck.addProperty("warmupSeconds", stuckWarmupSeconds);
    stuck.addProperty("cooldownSeconds", stuckCooldownSeconds);
    root.add("stuck", stuck);

    // Teleport settings
    JsonObject teleport = new JsonObject();
    teleport.addProperty("warmupSeconds", warmupSeconds);
    teleport.addProperty("cooldownSeconds", cooldownSeconds);
    teleport.addProperty("cancelOnMove", cancelOnMove);
    teleport.addProperty("cancelOnDamage", cancelOnDamage);
    root.add("teleport", teleport);

    // Update settings
    JsonObject updates = new JsonObject();
    updates.addProperty("enabled", updateCheckEnabled);
    updates.addProperty("url", updateCheckUrl);
    updates.addProperty("releaseChannel", releaseChannel);
    JsonObject hyperProtect = new JsonObject();
    hyperProtect.addProperty("autoDownload", hyperProtectAutoDownload);
    hyperProtect.addProperty("autoUpdate", hyperProtectAutoUpdate);
    hyperProtect.addProperty("url", hyperProtectUpdateUrl);
    updates.add("hyperProtect", hyperProtect);
    root.add("updates", updates);

    // Auto-save settings
    JsonObject autoSave = new JsonObject();
    autoSave.addProperty("enabled", autoSaveEnabled);
    autoSave.addProperty("intervalMinutes", autoSaveIntervalMinutes);
    root.add("autoSave", autoSave);

    // Message settings (v3 format: structured prefix)
    JsonObject messages = new JsonObject();
    JsonObject prefixObj = new JsonObject();
    prefixObj.addProperty("text", prefixText);
    prefixObj.addProperty("color", prefixColor);
    prefixObj.addProperty("bracketColor", prefixBracketColor);
    messages.add("prefix", prefixObj);
    messages.addProperty("primaryColor", primaryColor);
    root.add("messages", messages);

    // GUI settings
    JsonObject gui = new JsonObject();
    gui.addProperty("title", guiTitle);
    gui.addProperty("terrainMapEnabled", terrainMapEnabled);
    root.add("gui", gui);

    // Territory notification settings
    JsonObject notifications = new JsonObject();
    notifications.addProperty("enabled", territoryNotificationsEnabled);
    root.add("territoryNotifications", notifications);

    // Note: worldMap section removed in v3 - settings now in config/worldmap.json

    // Permission settings
    JsonObject permissions = new JsonObject();
    permissions.addProperty("adminRequiresOp", adminRequiresOp);
    permissions.addProperty("allowWithoutPermissionMod", allowWithoutPermissionMod);
    root.add("permissions", permissions);

    return root;
  }

  // === Getters ===

  /** Returns the config version. */
  public int getConfigVersion() {
    return configVersion;
  }

  // Faction
  /** Returns the max members. */
  public int getMaxMembers() {
    return maxMembers;
  }

  /** Returns the max membership history. */
  public int getMaxMembershipHistory() {
    return maxMembershipHistory;
  }

  /** Returns the max name length. */
  public int getMaxNameLength() {
    return maxNameLength;
  }

  public int getMinNameLength() {
    return minNameLength;
  }

  /** Checks if allow colors. */
  public boolean isAllowColors() {
    return allowColors;
  }

  // Power
  /** Returns the max player power. */
  public double getMaxPlayerPower() {
    return maxPlayerPower;
  }

  public double getStartingPower() {
    return startingPower;
  }

  /** Returns the power per claim. */
  public double getPowerPerClaim() {
    return powerPerClaim;
  }

  public double getDeathPenalty() {
    return deathPenalty;
  }

  /** Returns the kill reward. */
  public double getKillReward() {
    return killReward;
  }

  public boolean isKillRewardRequiresFaction() {
    return killRewardRequiresFaction;
  }

  /** Checks if power loss on mob death. */
  public boolean isPowerLossOnMobDeath() {
    return powerLossOnMobDeath;
  }

  /** Checks if power loss on environmental death. */
  public boolean isPowerLossOnEnvironmentalDeath() {
    return powerLossOnEnvironmentalDeath;
  }

  /** Returns the regen per minute. */
  public double getRegenPerMinute() {
    return regenPerMinute;
  }

  /** Checks if regen when offline. */
  public boolean isRegenWhenOffline() {
    return regenWhenOffline;
  }

  public boolean isHardcoreMode() {
    return hardcoreMode;
  }

  // Claims
  /** Returns the max claims. */
  public int getMaxClaims() {
    return maxClaims;
  }

  public boolean isOnlyAdjacent() {
    return onlyAdjacent;
  }

  /** Checks if prevent disconnect. */
  public boolean isPreventDisconnect() {
    return preventDisconnect;
  }

  /** Checks if decay enabled. */
  public boolean isDecayEnabled() {
    return decayEnabled;
  }

  /** Returns the decay days inactive. */
  public int getDecayDaysInactive() {
    return decayDaysInactive;
  }

  /** Checks if allow explosions in claims. */
  public boolean isAllowExplosionsInClaims() {
    return allowExplosionsInClaims;
  }

  /** Returns the world whitelist. */
  @NotNull public List<String> getWorldWhitelist() {
    return worldWhitelist;
  }

  /** Returns the world blacklist. */
  @NotNull public List<String> getWorldBlacklist() {
    return worldBlacklist;
  }

  // Combat
  /** Returns the tag duration seconds. */
  public int getTagDurationSeconds() {
    return tagDurationSeconds;
  }

  /** Checks if ally damage. */
  public boolean isAllyDamage() {
    return allyDamage;
  }

  /** Checks if faction damage. */
  public boolean isFactionDamage() {
    return factionDamage;
  }

  public boolean isTaggedLogoutPenalty() {
    return taggedLogoutPenalty;
  }

  /** Returns the logout power loss. */
  public double getLogoutPowerLoss() {
    return logoutPowerLoss;
  }

  /** Returns the neutral attack penalty. */
  public double getNeutralAttackPenalty() {
    return neutralAttackPenalty;
  }

  // Spawn Protection
  /** Checks if spawn protection enabled. */
  public boolean isSpawnProtectionEnabled() {
    return spawnProtectionEnabled;
  }

  public int getSpawnProtectionDurationSeconds() {
    return spawnProtectionDurationSeconds;
  }

  /** Checks if spawn protection break on attack. */
  public boolean isSpawnProtectionBreakOnAttack() {
    return spawnProtectionBreakOnAttack;
  }

  /** Checks if spawn protection break on move. */
  public boolean isSpawnProtectionBreakOnMove() {
    return spawnProtectionBreakOnMove;
  }

  // Relations
  /** Returns the max allies. */
  public int getMaxAllies() {
    return maxAllies;
  }

  /** Returns the max enemies. */
  public int getMaxEnemies() {
    return maxEnemies;
  }

  // Invites
  public int getInviteExpirationMinutes() {
    return inviteExpirationMinutes;
  }

  /** Returns the join request expiration hours. */
  public int getJoinRequestExpirationHours() {
    return joinRequestExpirationHours;
  }

  public long getInviteExpirationMs() {
    return inviteExpirationMinutes * 60 * 1000L;
  }

  /** Returns the join request expiration ms. */
  public long getJoinRequestExpirationMs() {
    return joinRequestExpirationHours * 60 * 60 * 1000L;
  }

  // Stuck
  /** Returns the stuck min radius. */
  public int getStuckMinRadius() {
    return stuckMinRadius;
  }

  public int getStuckRadiusIncrease() {
    return stuckRadiusIncrease;
  }

  /** Returns the stuck max attempts. */
  public int getStuckMaxAttempts() {
    return stuckMaxAttempts;
  }

  public int getStuckWarmupSeconds() {
    return stuckWarmupSeconds;
  }

  /** Returns the stuck cooldown seconds. */
  public int getStuckCooldownSeconds() {
    return stuckCooldownSeconds;
  }

  // Teleport
  /** Returns the warmup seconds. */
  public int getWarmupSeconds() {
    return warmupSeconds;
  }

  /** Returns the cooldown seconds. */
  public int getCooldownSeconds() {
    return cooldownSeconds;
  }

  /** Checks if cancel on move. */
  public boolean isCancelOnMove() {
    return cancelOnMove;
  }

  /** Checks if cancel on damage. */
  public boolean isCancelOnDamage() {
    return cancelOnDamage;
  }

  // Updates
  /** Checks if update check enabled. */
  public boolean isUpdateCheckEnabled() {
    return updateCheckEnabled;
  }

  /** Returns the update check url. */
  @NotNull public String getUpdateCheckUrl() {
    return updateCheckUrl;
  }

  /** Returns the release channel. */
  @NotNull public String getReleaseChannel() {
    return releaseChannel;
  }

  /** Checks if pre release channel. */
  public boolean isPreReleaseChannel() {
    return "prerelease".equals(releaseChannel);
  }

  // HyperProtect-Mixin management
  /** Checks if hyper protect auto download. */
  public boolean isHyperProtectAutoDownload() {
    return hyperProtectAutoDownload;
  }

  /** Checks if hyper protect auto update. */
  public boolean isHyperProtectAutoUpdate() {
    return hyperProtectAutoUpdate;
  }

  /** Returns the hyper protect update url. */
  @NotNull public String getHyperProtectUpdateUrl() {
    return hyperProtectUpdateUrl;
  }

  /** Sets the hyper protect auto download. */
  public void setHyperProtectAutoDownload(boolean value) {
    this.hyperProtectAutoDownload = value;
  }

  // Auto-save
  public boolean isAutoSaveEnabled() {
    return autoSaveEnabled;
  }

  /** Returns the auto save interval minutes. */
  public int getAutoSaveIntervalMinutes() {
    return autoSaveIntervalMinutes;
  }

  // Messages (v3 structured prefix)
  @NotNull public String getPrefixText() {
    return prefixText;
  }

  /** Returns the prefix color. */
  @NotNull public String getPrefixColor() {
    return prefixColor;
  }

  @NotNull public String getPrefixBracketColor() {
    return prefixBracketColor;
  }

  /** Returns the primary color. */
  @NotNull public String getPrimaryColor() {
    return primaryColor;
  }

  // GUI
  @NotNull public String getGuiTitle() {
    return guiTitle;
  }

  /** Checks if terrain map enabled. */
  public boolean isTerrainMapEnabled() {
    return terrainMapEnabled;
  }

  // Territory Notifications
  /** Checks if territory notifications enabled. */
  public boolean isTerritoryNotificationsEnabled() {
    return territoryNotificationsEnabled;
  }

  // Permissions
  /** Checks if admin requires op. */
  public boolean isAdminRequiresOp() {
    return adminRequiresOp;
  }

  /** Checks if allow without permission mod. */
  public boolean isAllowWithoutPermissionMod() {
    return allowWithoutPermissionMod;
  }

  // === Utility Methods ===

  /**
   * Checks if a world is allowed for claiming.
   *
   * @param worldName the world name
   * @return true if allowed
   */
  public boolean isWorldAllowed(@NotNull String worldName) {
    if (!worldWhitelist.isEmpty()) {
      return worldWhitelist.contains(worldName);
    }
    if (!worldBlacklist.isEmpty()) {
      return !worldBlacklist.contains(worldName);
    }
    return true;
  }

  /**
   * Calculates the max claims for a faction based on their power.
   *
   * @param totalPower the faction's total power
   * @return the max claims allowed
   */
  public int calculateMaxClaims(double totalPower) {
    if (powerPerClaim <= 0) {
      return maxClaims;
    }
    int powerClaims = (int) Math.floor(totalPower / powerPerClaim);
    return Math.min(powerClaims, maxClaims);
  }

  // === Validation ===

  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // Faction settings
    maxMembers = validateMin(result, "faction.maxMembers", maxMembers, 1, 50);
    maxMembershipHistory = validateRange(result, "faction.maxMembershipHistory", maxMembershipHistory, 1, 100, 10);
    maxNameLength = validateRange(result, "faction.maxNameLength", maxNameLength, 1, 64, 24);
    minNameLength = validateRange(result, "faction.minNameLength", minNameLength, 1, maxNameLength, 3);

    // Validate minNameLength <= maxNameLength
    if (minNameLength > maxNameLength) {
      result.addWarning(getConfigName(), "faction.minNameLength",
          "Must be less than or equal to maxNameLength", minNameLength, 3);
      minNameLength = 3;
      needsSave = true;
    }

    // Power settings
    maxPlayerPower = validateMin(result, "power.maxPlayerPower", maxPlayerPower, 0.0, 20.0);
    startingPower = validateRange(result, "power.startingPower", startingPower, 0.0, maxPlayerPower, 10.0);
    powerPerClaim = validateMin(result, "power.powerPerClaim", powerPerClaim, 0.0, 2.0);
    deathPenalty = validateMin(result, "power.deathPenalty", deathPenalty, 0.0, 1.0);
    killReward = validateMin(result, "power.killReward", killReward, 0.0, 0.0);
    regenPerMinute = validateMin(result, "power.regenPerMinute", regenPerMinute, 0.0, 0.1);

    // Claim settings
    maxClaims = validateMin(result, "claims.maxClaims", maxClaims, 0, 100);
    decayDaysInactive = validateMin(result, "claims.decayDaysInactive", decayDaysInactive, 1, 30);

    // Combat settings
    tagDurationSeconds = validateMin(result, "combat.tagDurationSeconds", tagDurationSeconds, 0, 15);
    logoutPowerLoss = validateMin(result, "combat.logoutPowerLoss", logoutPowerLoss, 0.0, 1.0);
    neutralAttackPenalty = validateMin(result, "combat.neutralAttackPenalty", neutralAttackPenalty, 0.0, 0.0);
    spawnProtectionDurationSeconds = validateMin(result, "combat.spawnProtection.durationSeconds",
        spawnProtectionDurationSeconds, 0, 5);

    // Relation settings (-1 means unlimited, so >= -1)
    if (maxAllies < -1) {
      result.addWarning(getConfigName(), "relations.maxAllies",
          "Must be at least -1 (unlimited)", maxAllies, 10);
      maxAllies = 10;
      needsSave = true;
    }
    if (maxEnemies < -1) {
      result.addWarning(getConfigName(), "relations.maxEnemies",
          "Must be at least -1 (unlimited)", maxEnemies, -1);
      maxEnemies = -1;
      needsSave = true;
    }

    // Invite/Request settings
    inviteExpirationMinutes = validateMin(result, "invites.inviteExpirationMinutes",
        inviteExpirationMinutes, 1, 5);
    joinRequestExpirationHours = validateMin(result, "invites.joinRequestExpirationHours",
        joinRequestExpirationHours, 1, 24);

    // Stuck settings
    stuckMinRadius = validateMin(result, "stuck.minRadius", stuckMinRadius, 1, 3);
    stuckRadiusIncrease = validateMin(result, "stuck.radiusIncrease", stuckRadiusIncrease, 1, 3);
    stuckMaxAttempts = validateMin(result, "stuck.maxAttempts", stuckMaxAttempts, 1, 10);
    stuckWarmupSeconds = validateMin(result, "stuck.warmupSeconds", stuckWarmupSeconds, 0, 30);
    stuckCooldownSeconds = validateMin(result, "stuck.cooldownSeconds", stuckCooldownSeconds, 0, 300);

    // Teleport settings
    warmupSeconds = validateMin(result, "teleport.warmupSeconds", warmupSeconds, 0, 5);
    cooldownSeconds = validateMin(result, "teleport.cooldownSeconds", cooldownSeconds, 0, 300);

    // Update settings
    releaseChannel = validateEnum(result, "updates.releaseChannel", releaseChannel,
        new String[]{"stable", "prerelease"}, "stable");

    // Auto-save settings
    autoSaveIntervalMinutes = validateMin(result, "autoSave.intervalMinutes",
        autoSaveIntervalMinutes, 1, 5);

    // Permission settings - no validation needed for boolean

    // Message settings - validate hex color
    validateHexColor(result, "messages.primaryColor", primaryColor);

    return result;
  }
}
