package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a role within a faction with hierarchical permissions.
 */
public enum FactionRole {
  LEADER(3, "Leader"),
  OFFICER(2, "Officer"),
  MEMBER(1, "Member");

  private final int level;

  private final String displayName;

  FactionRole(int level, String displayName) {
    this.level = level;
    this.displayName = displayName;
  }

  /**
   * Gets the permission level of this role.
   * Higher level = more permissions.
   *
   * @return the permission level
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the display name of this role.
   *
   * @return the display name
   */
  @NotNull
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this role has at least the given level.
   *
   * @param requiredLevel the minimum level required
   * @return true if this role's level is >= requiredLevel
   */
  public boolean hasLevel(int requiredLevel) {
    return level >= requiredLevel;
  }

  /**
   * Checks if this role is at or above the given role.
   *
   * @param other the role to compare against
   * @return true if this role's level is >= other's level
   */
  public boolean isAtLeast(@NotNull FactionRole other) {
    return level >= other.level;
  }

  /**
   * Checks if this role can manage the given role.
   * A role can manage roles strictly below it.
   *
   * @param other the role to check
   * @return true if this role can manage the other
   */
  public boolean canManage(@NotNull FactionRole other) {
    return level > other.level;
  }
}
