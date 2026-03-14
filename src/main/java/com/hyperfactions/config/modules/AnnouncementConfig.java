package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the server-wide faction announcement system.
 * Controls which faction events trigger broadcasts to all online players.
 */
public class AnnouncementConfig extends ModuleConfig {

  // Per-event toggle settings
  private boolean factionCreated = true;

  private boolean factionDisbanded = true;

  private boolean leadershipTransfer = true;

  private boolean overclaim = true;

  private boolean warDeclared = true;

  private boolean allianceFormed = true;

  private boolean allianceBroken = true;

  // Territory notification settings (moved from CoreConfig in V5→V6)
  private boolean territoryNotificationsEnabled = true;

  // Wilderness notification settings — customizable per exit context
  private boolean wildernessOnLeaveZoneEnabled = true;
  private String wildernessOnLeaveZoneUpper = "";
  private String wildernessOnLeaveZoneLower = "Wilderness";
  private boolean wildernessOnLeaveClaimEnabled = true;
  private String wildernessOnLeaveClaimUpper = "";
  private String wildernessOnLeaveClaimLower = "Wilderness";

  /**
   * Creates a new announcement config.
   *
   * @param filePath path to config/announcements.json
   */
  public AnnouncementConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "announcements";
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
    factionCreated = true;
    factionDisbanded = true;
    leadershipTransfer = true;
    overclaim = true;
    warDeclared = true;
    allianceFormed = true;
    allianceBroken = true;
    territoryNotificationsEnabled = true;
    wildernessOnLeaveZoneEnabled = true;
    wildernessOnLeaveZoneUpper = "";
    wildernessOnLeaveZoneLower = "Wilderness";
    wildernessOnLeaveClaimEnabled = true;
    wildernessOnLeaveClaimUpper = "";
    wildernessOnLeaveClaimLower = "Wilderness";
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    if (hasSection(root, "events")) {
      JsonObject events = root.getAsJsonObject("events");
      factionCreated = getBool(events, "factionCreated", factionCreated);
      factionDisbanded = getBool(events, "factionDisbanded", factionDisbanded);
      leadershipTransfer = getBool(events, "leadershipTransfer", leadershipTransfer);
      overclaim = getBool(events, "overclaim", overclaim);
      warDeclared = getBool(events, "warDeclared", warDeclared);
      allianceFormed = getBool(events, "allianceFormed", allianceFormed);
      allianceBroken = getBool(events, "allianceBroken", allianceBroken);
    }

    // Territory notifications
    if (hasSection(root, "territoryNotifications")) {
      JsonObject notifications = root.getAsJsonObject("territoryNotifications");
      territoryNotificationsEnabled = getBool(notifications, "enabled", territoryNotificationsEnabled);

      // Wilderness notification customization
      if (hasSection(notifications, "wilderness")) {
        JsonObject wilderness = notifications.getAsJsonObject("wilderness");

        if (hasSection(wilderness, "onLeaveZone")) {
          JsonObject leaveZone = wilderness.getAsJsonObject("onLeaveZone");
          wildernessOnLeaveZoneEnabled = getBool(leaveZone, "enabled", wildernessOnLeaveZoneEnabled);
          wildernessOnLeaveZoneUpper = getString(leaveZone, "upper", wildernessOnLeaveZoneUpper);
          wildernessOnLeaveZoneLower = getString(leaveZone, "lower", wildernessOnLeaveZoneLower);
        }

        if (hasSection(wilderness, "onLeaveClaim")) {
          JsonObject leaveClaim = wilderness.getAsJsonObject("onLeaveClaim");
          wildernessOnLeaveClaimEnabled = getBool(leaveClaim, "enabled", wildernessOnLeaveClaimEnabled);
          wildernessOnLeaveClaimUpper = getString(leaveClaim, "upper", wildernessOnLeaveClaimUpper);
          wildernessOnLeaveClaimLower = getString(leaveClaim, "lower", wildernessOnLeaveClaimLower);
        }
      }
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    JsonObject events = new JsonObject();
    events.addProperty("factionCreated", factionCreated);
    events.addProperty("factionDisbanded", factionDisbanded);
    events.addProperty("leadershipTransfer", leadershipTransfer);
    events.addProperty("overclaim", overclaim);
    events.addProperty("warDeclared", warDeclared);
    events.addProperty("allianceFormed", allianceFormed);
    events.addProperty("allianceBroken", allianceBroken);
    root.add("events", events);

    // Territory notifications
    JsonObject notifications = new JsonObject();
    notifications.addProperty("enabled", territoryNotificationsEnabled);

    // Wilderness notification customization
    JsonObject wilderness = new JsonObject();

    JsonObject leaveZone = new JsonObject();
    leaveZone.addProperty("enabled", wildernessOnLeaveZoneEnabled);
    leaveZone.addProperty("upper", wildernessOnLeaveZoneUpper);
    leaveZone.addProperty("lower", wildernessOnLeaveZoneLower);
    wilderness.add("onLeaveZone", leaveZone);

    JsonObject leaveClaim = new JsonObject();
    leaveClaim.addProperty("enabled", wildernessOnLeaveClaimEnabled);
    leaveClaim.addProperty("upper", wildernessOnLeaveClaimUpper);
    leaveClaim.addProperty("lower", wildernessOnLeaveClaimLower);
    wilderness.add("onLeaveClaim", leaveClaim);

    notifications.add("wilderness", wilderness);
    root.add("territoryNotifications", notifications);
  }

  // === Getters ===

  /** Checks if faction created. */
  public boolean isFactionCreated() {
    return factionCreated;
  }

  /** Checks if faction disbanded. */
  public boolean isFactionDisbanded() {
    return factionDisbanded;
  }

  /** Checks if leadership transfer. */
  public boolean isLeadershipTransfer() {
    return leadershipTransfer;
  }

  /** Checks if overclaim. */
  public boolean isOverclaim() {
    return overclaim;
  }

  public boolean isWarDeclared() {
    return warDeclared;
  }

  /** Checks if alliance formed. */
  public boolean isAllianceFormed() {
    return allianceFormed;
  }

  /** Checks if alliance broken. */
  public boolean isAllianceBroken() {
    return allianceBroken;
  }

  /** Checks if territory notifications enabled. */
  public boolean isTerritoryNotificationsEnabled() {
    return territoryNotificationsEnabled;
  }

  // === Setters (for admin config editor) ===

  /** Sets territory notifications enabled. */
  public void setTerritoryNotificationsEnabled(boolean value) { this.territoryNotificationsEnabled = value; }

  /** Sets wilderness on leave zone enabled. */
  public void setWildernessOnLeaveZoneEnabled(boolean value) { this.wildernessOnLeaveZoneEnabled = value; }

  /** Sets wilderness on leave claim enabled. */
  public void setWildernessOnLeaveClaimEnabled(boolean value) { this.wildernessOnLeaveClaimEnabled = value; }

  /** Sets faction created. */
  public void setFactionCreated(boolean value) { this.factionCreated = value; }

  /** Sets faction disbanded. */
  public void setFactionDisbanded(boolean value) { this.factionDisbanded = value; }

  /** Sets leadership transfer. */
  public void setLeadershipTransfer(boolean value) { this.leadershipTransfer = value; }

  /** Sets overclaim. */
  public void setOverclaim(boolean value) { this.overclaim = value; }

  /** Sets war declared. */
  public void setWarDeclared(boolean value) { this.warDeclared = value; }

  /** Sets alliance formed. */
  public void setAllianceFormed(boolean value) { this.allianceFormed = value; }

  /** Sets alliance broken. */
  public void setAllianceBroken(boolean value) { this.allianceBroken = value; }

  // === Wilderness notification getters ===

  public boolean isWildernessOnLeaveZoneEnabled() {
    return wildernessOnLeaveZoneEnabled;
  }

  @NotNull
  public String getWildernessOnLeaveZoneUpper() {
    return wildernessOnLeaveZoneUpper;
  }

  @NotNull
  public String getWildernessOnLeaveZoneLower() {
    return wildernessOnLeaveZoneLower;
  }

  public boolean isWildernessOnLeaveClaimEnabled() {
    return wildernessOnLeaveClaimEnabled;
  }

  @NotNull
  public String getWildernessOnLeaveClaimUpper() {
    return wildernessOnLeaveClaimUpper;
  }

  @NotNull
  public String getWildernessOnLeaveClaimLower() {
    return wildernessOnLeaveClaimLower;
  }

  /** Sets wilderness on leave zone upper text. */
  public void setWildernessOnLeaveZoneUpper(@NotNull String value) { this.wildernessOnLeaveZoneUpper = value; }

  /** Sets wilderness on leave zone lower text. */
  public void setWildernessOnLeaveZoneLower(@NotNull String value) { this.wildernessOnLeaveZoneLower = value; }

  /** Sets wilderness on leave claim upper text. */
  public void setWildernessOnLeaveClaimUpper(@NotNull String value) { this.wildernessOnLeaveClaimUpper = value; }

  /** Sets wilderness on leave claim lower text. */
  public void setWildernessOnLeaveClaimLower(@NotNull String value) { this.wildernessOnLeaveClaimLower = value; }
}
