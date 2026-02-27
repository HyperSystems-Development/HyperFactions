package com.hyperfactions.gui;

import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.MembershipRecord;
import org.jetbrains.annotations.NotNull;

/**
 * Centralized GUI color constants and resolution methods.
 *
 * <p>Provides consistent colors for roles, statuses, relations, and
 * other semantic values across all GUI pages.</p>
 */
public final class GuiColors {

  private GuiColors() {}

  // --- Role colors ---
  public static final String ROLE_LEADER = "#FFD700";
  public static final String ROLE_OFFICER = "#00AAFF";
  public static final String ROLE_MEMBER = "#888888";

  // --- Online status ---
  public static final String STATUS_ONLINE = "#55FF55";
  public static final String STATUS_OFFLINE = "#888888";
  public static final String STATUS_OFFLINE_BAR = "#555555";

  // --- Leave reason colors ---
  public static final String LEAVE_ACTIVE = "#55FF55";
  public static final String LEAVE_LEFT = "#FFAA00";
  public static final String LEAVE_KICKED = "#FF5555";
  public static final String LEAVE_DISBANDED = "#AA00AA";

  // --- Relation colors ---
  public static final String RELATION_ALLY = "#00AAFF";
  public static final String RELATION_ENEMY = "#FF5555";
  public static final String RELATION_NEUTRAL = "#888888";
  public static final String RELATION_TRUCE = "#FFFF55";
  public static final String RELATION_OWN = "#55FF55";

  // --- Power level colors ---
  public static final String POWER_HIGH = "#55FF55";
  public static final String POWER_MEDIUM = "#FFAA00";
  public static final String POWER_LOW = "#FF5555";

  // --- Rank colors (leaderboard) ---
  public static final String RANK_FIRST = "#FFD700";
  public static final String RANK_SECOND = "#C0C0C0";
  public static final String RANK_THIRD = "#CD7F32";
  public static final String RANK_DEFAULT = "#888888";

  /**
   * Returns the color for a faction role.
   */
  @NotNull
  public static String forRole(@NotNull FactionRole role) {
    return switch (role) {
      case LEADER -> ROLE_LEADER;
      case OFFICER -> ROLE_OFFICER;
      case MEMBER -> ROLE_MEMBER;
    };
  }

  /**
   * Returns the color for a membership leave reason.
   */
  @NotNull
  public static String forLeaveReason(@NotNull MembershipRecord.LeaveReason reason) {
    return switch (reason) {
      case ACTIVE -> LEAVE_ACTIVE;
      case LEFT -> LEAVE_LEFT;
      case KICKED -> LEAVE_KICKED;
      case DISBANDED -> LEAVE_DISBANDED;
    };
  }

  /**
   * Returns the color for a faction log type.
   */
  @NotNull
  public static String forLogType(@NotNull FactionLog.LogType type) {
    return switch (type) {
      case MEMBER_JOIN -> "#55FF55";
      case MEMBER_LEAVE, MEMBER_KICK -> "#FF5555";
      case MEMBER_PROMOTE -> "#FFD700";
      case MEMBER_DEMOTE -> "#FFAA00";
      case CLAIM, OVERCLAIM -> "#5555FF";
      case UNCLAIM -> "#AAAAAA";
      case HOME_SET -> "#55FFFF";
      case RELATION_ALLY -> "#55FF55";
      case RELATION_ENEMY -> "#FF5555";
      case RELATION_NEUTRAL -> "#FFFF55";
      case LEADER_TRANSFER -> "#FF55FF";
      case SETTINGS_CHANGE -> "#AA00FF";
      case POWER_CHANGE -> "#FFAA00";
      case ECONOMY -> "#FFD700";
      case ADMIN_POWER -> "#FF55FF";
    };
  }

  /**
   * Returns a color based on power percentage (0-100).
   */
  @NotNull
  public static String forPowerLevel(int percent) {
    if (percent >= 80) return POWER_HIGH;
    if (percent >= 40) return POWER_MEDIUM;
    return POWER_LOW;
  }

  /**
   * Returns a color based on power ratio (0.0-1.0).
   */
  @NotNull
  public static String forPowerRatio(double ratio) {
    if (ratio >= 0.8) return POWER_HIGH;
    if (ratio >= 0.4) return POWER_MEDIUM;
    return POWER_LOW;
  }

  /**
   * Returns the color for online/offline status.
   */
  @NotNull
  public static String forOnlineStatus(boolean online) {
    return online ? STATUS_ONLINE : STATUS_OFFLINE;
  }

  /**
   * Returns the color for a leaderboard rank position.
   */
  @NotNull
  public static String forRank(int rank) {
    return switch (rank) {
      case 1 -> RANK_FIRST;
      case 2 -> RANK_SECOND;
      case 3 -> RANK_THIRD;
      default -> RANK_DEFAULT;
    };
  }
}
