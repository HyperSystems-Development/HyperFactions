package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import com.hyperfactions.data.FactionRole;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Faction gameplay configuration (config/factions.json).
 *
 * <p>
 * Contains all faction-related settings: faction properties, power mechanics,
 * claim behavior, combat, spawn protection, relations, invites, and stuck command.
 *
 * <p>
 * Previously these settings lived in the monolithic config.json (CoreConfig).
 * Migration V5→V6 extracts them into this dedicated file.
 */
public class FactionsConfig extends ModuleConfig {

  // Role display names (presentation-only, does not affect data storage)
  private final EnumMap<FactionRole, String> roleDisplayNames = new EnumMap<>(Map.of(
      FactionRole.LEADER, "Leader",
      FactionRole.OFFICER, "Officer",
      FactionRole.MEMBER, "Member"
  ));

  private final EnumMap<FactionRole, String> roleShortNames = new EnumMap<>(Map.of(
      FactionRole.LEADER, "LD",
      FactionRole.OFFICER, "OF",
      FactionRole.MEMBER, "MB"
  ));

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

  private int decayClaimsPerCycle = 5;

  private List<String> worldWhitelist = new ArrayList<>();

  private List<String> worldBlacklist = new ArrayList<>();

  // Claim protection overrides (server-level, not exposed to factions)
  private boolean outsiderPickupAllowed = true;

  private boolean outsiderDropAllowed = true;

  private boolean factionlessExplosionsAllowed = false;

  private boolean enemyExplosionsAllowed = false;

  private boolean neutralExplosionsAllowed = false;

  private boolean fireSpreadAllowed = true;

  private boolean factionlessDamageAllowed = true;

  private boolean enemyDamageAllowed = true;

  private boolean neutralDamageAllowed = true;

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

  /** Creates a new FactionsConfig. */
  public FactionsConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "factions";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    // Field defaults are already set in declarations
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    // Role display names
    if (hasSection(root, "roles")) {
      JsonObject roles = root.getAsJsonObject("roles");
      for (FactionRole role : FactionRole.values()) {
        String key = role.name().toLowerCase();
        if (hasSection(roles, key)) {
          JsonObject roleObj = roles.getAsJsonObject(key);
          roleDisplayNames.put(role, getString(roleObj, "displayName", role.getDisplayName()));
          roleShortNames.put(role, getString(roleObj, "shortName", roleShortNames.get(role)));
        }
      }
    }

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
      decayClaimsPerCycle = getInt(claims, "decayClaimsPerCycle", decayClaimsPerCycle);
      worldWhitelist = getStringList(claims, "worldWhitelist");
      worldBlacklist = getStringList(claims, "worldBlacklist");

      // Claim protection overrides
      outsiderPickupAllowed = getBool(claims, "outsiderPickupAllowed", outsiderPickupAllowed);
      outsiderDropAllowed = getBool(claims, "outsiderDropAllowed", outsiderDropAllowed);
      factionlessExplosionsAllowed = getBool(claims, "factionlessExplosionsAllowed", factionlessExplosionsAllowed);
      enemyExplosionsAllowed = getBool(claims, "enemyExplosionsAllowed", enemyExplosionsAllowed);
      neutralExplosionsAllowed = getBool(claims, "neutralExplosionsAllowed", neutralExplosionsAllowed);
      fireSpreadAllowed = getBool(claims, "fireSpreadAllowed", fireSpreadAllowed);
      factionlessDamageAllowed = getBool(claims, "factionlessDamageAllowed", factionlessDamageAllowed);
      enemyDamageAllowed = getBool(claims, "enemyDamageAllowed", enemyDamageAllowed);
      neutralDamageAllowed = getBool(claims, "neutralDamageAllowed", neutralDamageAllowed);
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
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    // Role display names
    JsonObject roles = new JsonObject();
    for (FactionRole role : FactionRole.values()) {
      JsonObject roleObj = new JsonObject();
      roleObj.addProperty("displayName", roleDisplayNames.get(role));
      roleObj.addProperty("shortName", roleShortNames.get(role));
      roles.add(role.name().toLowerCase(), roleObj);
    }
    root.add("roles", roles);

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
    claims.addProperty("decayClaimsPerCycle", decayClaimsPerCycle);
    claims.add("worldWhitelist", toJsonArray(worldWhitelist));
    claims.add("worldBlacklist", toJsonArray(worldBlacklist));
    // Claim protection overrides
    claims.addProperty("outsiderPickupAllowed", outsiderPickupAllowed);
    claims.addProperty("outsiderDropAllowed", outsiderDropAllowed);
    claims.addProperty("factionlessExplosionsAllowed", factionlessExplosionsAllowed);
    claims.addProperty("enemyExplosionsAllowed", enemyExplosionsAllowed);
    claims.addProperty("neutralExplosionsAllowed", neutralExplosionsAllowed);
    claims.addProperty("fireSpreadAllowed", fireSpreadAllowed);
    claims.addProperty("factionlessDamageAllowed", factionlessDamageAllowed);
    claims.addProperty("enemyDamageAllowed", enemyDamageAllowed);
    claims.addProperty("neutralDamageAllowed", neutralDamageAllowed);
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
  }

  // === Getters ===

  // Roles
  /** Returns the role display name. */
  @NotNull public String getRoleDisplayName(@NotNull FactionRole role) {
    return roleDisplayNames.get(role);
  }

  /** Returns the role short name. */
  @NotNull public String getRoleShortName(@NotNull FactionRole role) {
    return roleShortNames.get(role);
  }

  // Faction
  /** Returns the max members. */
  public int getMaxMembers() {
    return maxMembers;
  }

  public int getMaxMembershipHistory() {
    return maxMembershipHistory;
  }

  /** Returns the max name length. */
  public int getMaxNameLength() {
    return maxNameLength;
  }

  /** Returns the min name length. */
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

  /** Returns the starting power. */
  public double getStartingPower() {
    return startingPower;
  }

  public double getPowerPerClaim() {
    return powerPerClaim;
  }

  /** Returns the death penalty. */
  public double getDeathPenalty() {
    return deathPenalty;
  }

  public double getKillReward() {
    return killReward;
  }

  /** Checks if kill reward requires faction. */
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

  /** Returns the number of claims removed per decay cycle. */
  public int getDecayClaimsPerCycle() {
    return decayClaimsPerCycle;
  }

  /** Returns the world whitelist. */
  @NotNull public List<String> getWorldWhitelist() {
    return worldWhitelist;
  }

  /** Returns the world blacklist. */
  @NotNull public List<String> getWorldBlacklist() {
    return worldBlacklist;
  }

  // Claim protection overrides
  /** Checks if outsider pickup allowed. */
  public boolean isOutsiderPickupAllowed() {
    return outsiderPickupAllowed;
  }

  /** Checks if outsider drop allowed. */
  public boolean isOutsiderDropAllowed() {
    return outsiderDropAllowed;
  }

  /** Checks if factionless explosions allowed. */
  public boolean isFactionlessExplosionsAllowed() {
    return factionlessExplosionsAllowed;
  }

  /** Checks if enemy explosions allowed. */
  public boolean isEnemyExplosionsAllowed() {
    return enemyExplosionsAllowed;
  }

  public boolean isNeutralExplosionsAllowed() {
    return neutralExplosionsAllowed;
  }

  /** Checks if fire spread allowed. */
  public boolean isFireSpreadAllowed() {
    return fireSpreadAllowed;
  }

  /** Checks if factionless damage allowed. */
  public boolean isFactionlessDamageAllowed() {
    return factionlessDamageAllowed;
  }

  public boolean isEnemyDamageAllowed() {
    return enemyDamageAllowed;
  }

  /** Checks if neutral damage allowed. */
  public boolean isNeutralDamageAllowed() {
    return neutralDamageAllowed;
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
  public boolean isSpawnProtectionEnabled() {
    return spawnProtectionEnabled;
  }

  /** Returns the spawn protection duration seconds. */
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

  public int getMaxEnemies() {
    return maxEnemies;
  }

  // Invites
  /** Returns the invite expiration minutes. */
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

  /** Returns the stuck radius increase. */
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

  // === Utility Methods ===

  /**
   * Checks if a world is allowed for claiming.
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
   */
  public int calculateMaxClaims(double totalPower) {
    if (powerPerClaim <= 0) {
      return maxClaims;
    }
    int powerClaims = (int) Math.floor(totalPower / powerPerClaim);
    return Math.min(powerClaims, maxClaims);
  }

  // === Validation ===

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // Role display names
    for (FactionRole role : FactionRole.values()) {
      String key = role.name().toLowerCase();
      String display = roleDisplayNames.get(role);
      if (display == null || display.isBlank()) {
        result.addWarning(getConfigName(), "roles." + key + ".displayName",
            "Must not be empty", display, role.getDisplayName());
        roleDisplayNames.put(role, role.getDisplayName());
        needsSave = true;
      }
      String shortName = roleShortNames.get(role);
      if (shortName == null || shortName.isBlank()) {
        String defaultShort = switch (role) {
          case LEADER -> "LD";
          case OFFICER -> "OF";
          case MEMBER -> "MB";
        };
        result.addWarning(getConfigName(), "roles." + key + ".shortName",
            "Must not be empty", shortName, defaultShort);
        roleShortNames.put(role, defaultShort);
        needsSave = true;
      } else if (shortName.length() > 4) {
        String truncated = shortName.substring(0, 4);
        result.addWarning(getConfigName(), "roles." + key + ".shortName",
            "Must be 4 characters or fewer", shortName, truncated);
        roleShortNames.put(role, truncated);
        needsSave = true;
      }
    }

    // Faction settings
    maxMembers = validateMin(result, "faction.maxMembers", maxMembers, 1, 50);
    maxMembershipHistory = validateRange(result, "faction.maxMembershipHistory", maxMembershipHistory, 1, 100, 10);
    maxNameLength = validateRange(result, "faction.maxNameLength", maxNameLength, 1, 64, 24);
    minNameLength = validateRange(result, "faction.minNameLength", minNameLength, 1, maxNameLength, 3);

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
    decayClaimsPerCycle = validateMin(result, "claims.decayClaimsPerCycle", decayClaimsPerCycle, 1, 5);

    // Combat settings
    tagDurationSeconds = validateMin(result, "combat.tagDurationSeconds", tagDurationSeconds, 0, 15);
    logoutPowerLoss = validateMin(result, "combat.logoutPowerLoss", logoutPowerLoss, 0.0, 1.0);
    neutralAttackPenalty = validateMin(result, "combat.neutralAttackPenalty", neutralAttackPenalty, 0.0, 0.0);
    spawnProtectionDurationSeconds = validateMin(result, "combat.spawnProtection.durationSeconds",
        spawnProtectionDurationSeconds, 0, 5);

    // Relation settings (-1 means unlimited)
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

    return result;
  }
}
