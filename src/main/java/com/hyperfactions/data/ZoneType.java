package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the type of special zone.
 */
public enum ZoneType {
  SAFE("SafeZone", "\u00A7a", true, false),
  WAR("WarZone", "\u00A7c", false, true);

  private final String displayName;

  private final String colorCode;

  private final boolean pvpDisabled;

  private final boolean alwaysPvp;

  ZoneType(String displayName, String colorCode, boolean pvpDisabled, boolean alwaysPvp) {
    this.displayName = displayName;
    this.colorCode = colorCode;
    this.pvpDisabled = pvpDisabled;
    this.alwaysPvp = alwaysPvp;
  }

  /**
   * Gets the display name of this zone type.
   *
   * @return the display name
   */
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the color code for this zone type.
   *
   * @return the Minecraft color code
   */
  @NotNull
  public String getColorCode() {
    return colorCode;
  }

  /**
   * Checks if PvP is disabled in this zone type.
   *
   * @return true if PvP is disabled
   */
  public boolean isPvpDisabled() {
    return pvpDisabled;
  }

  /**
   * Checks if PvP is always enabled in this zone type (even for allies).
   *
   * @return true if PvP is always enabled
   */
  public boolean isAlwaysPvp() {
    return alwaysPvp;
  }
}
