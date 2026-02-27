package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a member of a faction.
 *
 * @param uuid       the player's UUID
 * @param username   the player's username (cached for display)
 * @param role       the member's role in the faction
 * @param joinedAt   when the player joined the faction (epoch millis)
 * @param lastOnline when the player was last online (epoch millis)
 */
public record FactionMember(
  @NotNull UUID uuid,
  @NotNull String username,
  @NotNull FactionRole role,
  long joinedAt,
  long lastOnline
) {
  /**
   * Creates a new member with the default MEMBER role.
   *
   * @param uuid     the player's UUID
   * @param username the player's username
   * @return a new FactionMember
   */
  public static FactionMember create(@NotNull UUID uuid, @NotNull String username) {
    long now = System.currentTimeMillis();
    return new FactionMember(uuid, username, FactionRole.MEMBER, now, now);
  }

  /**
   * Creates a new member as the faction leader.
   *
   * @param uuid     the player's UUID
   * @param username the player's username
   * @return a new FactionMember with LEADER role
   */
  public static FactionMember createLeader(@NotNull UUID uuid, @NotNull String username) {
    long now = System.currentTimeMillis();
    return new FactionMember(uuid, username, FactionRole.LEADER, now, now);
  }

  /**
   * Creates a copy with an updated role.
   *
   * @param newRole the new role
   * @return a new FactionMember with the updated role
   */
  public FactionMember withRole(@NotNull FactionRole newRole) {
    return new FactionMember(uuid, username, newRole, joinedAt, lastOnline);
  }

  /**
   * Creates a copy with an updated last online time.
   *
   * @param timestamp the new last online timestamp
   * @return a new FactionMember with updated lastOnline
   */
  public FactionMember withLastOnline(long timestamp) {
    return new FactionMember(uuid, username, role, joinedAt, timestamp);
  }

  /**
   * Creates a copy with an updated username.
   *
   * @param newUsername the new username
   * @return a new FactionMember with updated username
   */
  public FactionMember withUsername(@NotNull String newUsername) {
    return new FactionMember(uuid, newUsername, role, joinedAt, lastOnline);
  }

  /**
   * Checks if this member is the faction leader.
   *
   * @return true if leader
   */
  public boolean isLeader() {
    return role == FactionRole.LEADER;
  }

  /**
   * Checks if this member is at least an officer.
   *
   * @return true if officer or leader
   */
  public boolean isOfficerOrHigher() {
    return role.isAtLeast(FactionRole.OFFICER);
  }
}
