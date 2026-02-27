package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Records a player's membership in a faction, including their highest role
 * achieved and the reason they left. Used for membership history tracking.
 *
 * @param factionId    the faction's UUID (for lookup if still exists)
 * @param factionName  cached faction name (survives disband)
 * @param factionTag   cached faction tag (survives disband)
 * @param highestRole  the highest role achieved during membership
 * @param joinedAt     when the player joined (epoch millis)
 * @param leftAt       when the player left (epoch millis, 0 if currently active)
 * @param reason       why they left (ACTIVE if still a member)
 */
public record MembershipRecord(
  @NotNull UUID factionId,
  @NotNull String factionName,
  @Nullable String factionTag,
  @NotNull FactionRole highestRole,
  long joinedAt,
  long leftAt,
  @NotNull LeaveReason reason
) {

  /**
   * Reasons a player may leave a faction.
   */
  public enum LeaveReason {
    ACTIVE,
    LEFT,
    KICKED,
    DISBANDED
  }

  /**
   * Whether this record represents an active (current) membership.
   */
  public boolean isActive() {
    return reason == LeaveReason.ACTIVE;
  }

  /**
   * Creates a new active membership record.
   */
  public static MembershipRecord createActive(@NotNull UUID factionId,
                         @NotNull String factionName,
                         @Nullable String factionTag,
                         @NotNull FactionRole role) {
    return new MembershipRecord(factionId, factionName, factionTag, role,
        System.currentTimeMillis(), 0, LeaveReason.ACTIVE);
  }

  /**
   * Creates a copy with the membership closed (left/kicked/disbanded).
   */
  public MembershipRecord withClosed(@NotNull LeaveReason leaveReason) {
    return new MembershipRecord(factionId, factionName, factionTag, highestRole,
        joinedAt, System.currentTimeMillis(), leaveReason);
  }

  /**
   * Creates a copy with the highest role updated (if the new role is higher).
   */
  public MembershipRecord withHighestRole(@NotNull FactionRole role) {
    if (role.getLevel() > highestRole.getLevel()) {
      return new MembershipRecord(factionId, factionName, factionTag, role,
          joinedAt, leftAt, reason);
    }
    return this;
  }
}
