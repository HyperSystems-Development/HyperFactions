package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for GravestonePlugin integration.
 * Controls faction-aware gravestone access rules per zone type.
 */
public class GravestoneConfig extends ModuleConfig {

  private boolean protectInOwnTerritory = true;

  private boolean factionMembersCanAccess = true;

  private boolean alliesCanAccess = false;

  private boolean protectInSafeZone = true;

  private boolean protectInWarZone = false;

  private boolean protectInWilderness = false;

  private boolean announceDeathLocation = true;

  // Territory context settings
  private boolean protectInEnemyTerritory = false;

  private boolean protectInNeutralTerritory = true;

  private boolean enemiesCanLootInOwnTerritory = false;

  // Raid/War placeholders (not enforced until raid/war systems are implemented)
  private boolean allowLootDuringRaid = true;

  private boolean allowLootDuringWar = true;

  /**
   * Creates a new gravestone config.
   *
   * @param filePath path to config/gravestones.json
   */
  public GravestoneConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "gravestones";
  }

  /** Returns the default enabled. */
  @Override
  protected boolean getDefaultEnabled() {
    return true;
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    protectInOwnTerritory = true;
    factionMembersCanAccess = true;
    alliesCanAccess = false;
    protectInSafeZone = true;
    protectInWarZone = false;
    protectInWilderness = false;
    announceDeathLocation = true;
    protectInEnemyTerritory = false;
    protectInNeutralTerritory = true;
    enemiesCanLootInOwnTerritory = false;
    allowLootDuringRaid = true;
    allowLootDuringWar = true;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    protectInOwnTerritory = getBool(root, "protectInOwnTerritory", protectInOwnTerritory);
    factionMembersCanAccess = getBool(root, "factionMembersCanAccess", factionMembersCanAccess);
    alliesCanAccess = getBool(root, "alliesCanAccess", alliesCanAccess);
    protectInSafeZone = getBool(root, "protectInSafeZone", protectInSafeZone);
    protectInWarZone = getBool(root, "protectInWarZone", protectInWarZone);
    protectInWilderness = getBool(root, "protectInWilderness", protectInWilderness);
    announceDeathLocation = getBool(root, "announceDeathLocation", announceDeathLocation);
    protectInEnemyTerritory = getBool(root, "protectInEnemyTerritory", protectInEnemyTerritory);
    protectInNeutralTerritory = getBool(root, "protectInNeutralTerritory", protectInNeutralTerritory);
    enemiesCanLootInOwnTerritory = getBool(root, "enemiesCanLootInOwnTerritory", enemiesCanLootInOwnTerritory);
    allowLootDuringRaid = getBool(root, "allowLootDuringRaid", allowLootDuringRaid);
    allowLootDuringWar = getBool(root, "allowLootDuringWar", allowLootDuringWar);
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("protectInOwnTerritory", protectInOwnTerritory);
    root.addProperty("factionMembersCanAccess", factionMembersCanAccess);
    root.addProperty("alliesCanAccess", alliesCanAccess);
    root.addProperty("protectInSafeZone", protectInSafeZone);
    root.addProperty("protectInWarZone", protectInWarZone);
    root.addProperty("protectInWilderness", protectInWilderness);
    root.addProperty("announceDeathLocation", announceDeathLocation);
    root.addProperty("protectInEnemyTerritory", protectInEnemyTerritory);
    root.addProperty("protectInNeutralTerritory", protectInNeutralTerritory);
    root.addProperty("enemiesCanLootInOwnTerritory", enemiesCanLootInOwnTerritory);
    root.addProperty("allowLootDuringRaid", allowLootDuringRaid);
    root.addProperty("allowLootDuringWar", allowLootDuringWar);
  }

  // === Getters ===

  /** Checks if protect in own territory. */
  public boolean isProtectInOwnTerritory() {
    return protectInOwnTerritory;
  }

  /** Checks if faction members can access. */
  public boolean isFactionMembersCanAccess() {
    return factionMembersCanAccess;
  }

  /** Checks if allies can access. */
  public boolean isAlliesCanAccess() {
    return alliesCanAccess;
  }

  /** Checks if protect in safe zone. */
  public boolean isProtectInSafeZone() {
    return protectInSafeZone;
  }

  public boolean isProtectInWarZone() {
    return protectInWarZone;
  }

  /** Checks if protect in wilderness. */
  public boolean isProtectInWilderness() {
    return protectInWilderness;
  }

  /** Checks if announce death location. */
  public boolean isAnnounceDeathLocation() {
    return announceDeathLocation;
  }

  /** Checks if protect in enemy territory. */
  public boolean isProtectInEnemyTerritory() {
    return protectInEnemyTerritory;
  }

  /** Checks if protect in neutral territory. */
  public boolean isProtectInNeutralTerritory() {
    return protectInNeutralTerritory;
  }

  /** Checks if enemies can loot in own territory. */
  public boolean isEnemiesCanLootInOwnTerritory() {
    return enemiesCanLootInOwnTerritory;
  }

  public boolean isAllowLootDuringRaid() {
    return allowLootDuringRaid;
  }

  /** Checks if allow loot during war. */
  public boolean isAllowLootDuringWar() {
    return allowLootDuringWar;
  }
}
