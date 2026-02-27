package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the type of relation between factions.
 */
public enum RelationType {
  OWN("Your", "\u00A7a", true),
  ALLY("Ally", "\u00A7a", true),
  NEUTRAL("Neutral", "\u00A77", false),
  ENEMY("Enemy", "\u00A7c", false);

  private final String displayName;

  private final String colorCode;

  private final boolean friendly;

  RelationType(String displayName, String colorCode, boolean friendly) {
    this.displayName = displayName;
    this.colorCode = colorCode;
    this.friendly = friendly;
  }

  /**
   * Gets the display name of this relation type.
   *
   * @return the display name
   */
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the color code for this relation type.
   *
   * @return the Minecraft color code
   */
  @NotNull
  public String getColorCode() {
    return colorCode;
  }

  /**
   * Checks if this relation type is considered friendly.
   *
   * @return true if friendly (ally)
   */
  public boolean isFriendly() {
    return friendly;
  }

  /**
   * Checks if this relation type is hostile.
   *
   * @return true if enemy
   */
  public boolean isHostile() {
    return this == ENEMY;
  }
}
