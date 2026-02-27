package com.hyperfactions.gui;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the different GUI experiences in HyperFactions.
 * Each type has its own navigation bar and set of accessible pages.
 */
public enum GuiType {

  /**
   * GUI for players without a faction.
   * Focused on discovery and faction creation.
   * Nav: BROWSE | CREATE | INVITES | HELP
   */
  NEW_PLAYER("new_player"),

  /**
   * GUI for players who are in a faction.
   * Full faction management experience.
   * Nav: DASHBOARD | MEMBERS | BROWSE | MAP | RELATIONS | SETTINGS
   */
  FACTION_PLAYER("faction"),

  /**
   * Admin GUI for server administrators.
   * Server-wide faction management.
   * Nav: OVERVIEW | FACTIONS | ZONES | PLAYERS | CONFIG | LOGS
   */
  ADMIN("admin");

  private final String id;

  GuiType(@NotNull String id) {
    this.id = id;
  }

  /**
   * Gets the unique identifier for this GUI type.
   *
   * @return The type ID
   */
  @NotNull
  public String getId() {
    return id;
  }

  /**
   * Gets a GuiType by its ID.
   *
   * @param id The type ID
   * @return The matching GuiType, or null if not found
   */
  public static GuiType fromId(@NotNull String id) {
    for (GuiType type : values()) {
      if (type.id.equals(id)) {
        return type;
      }
    }
    return null;
  }
}
