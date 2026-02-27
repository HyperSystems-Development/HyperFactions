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
}
