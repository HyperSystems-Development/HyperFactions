package com.hyperfactions.data;

import com.hyperfactions.util.LegacyColorParser;
import com.hyperfactions.util.GuiKeys;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a faction with all its data.
 *
 * @param id          unique faction identifier
 * @param name        the faction's display name
 * @param description optional faction description
 * @param tag         optional short tag (1-5 chars, alphanumeric)
 * @param color       the faction's color as hex string (e.g., "#55FFFF")
 * @param createdAt   when the faction was created (epoch millis)
 * @param home        the faction home location, if set
 * @param members     map of member UUID to FactionMember
 * @param claims      set of faction claims
 * @param relations   map of target faction UUID to FactionRelation
 * @param logs        list of faction activity logs (newest first)
 * @param open           whether the faction is open to join without invite
 * @param permissions    territory permissions configuration (nullable, uses defaults if null)
 * @param hardcorePower  hardcore mode faction power pool (nullable, only set when hardcore mode is active)
 */
public record Faction(
  @NotNull UUID id,
  @NotNull String name,
  @Nullable String description,
  @Nullable String tag,
  @NotNull String color,
  long createdAt,
  @Nullable FactionHome home,
  @NotNull Map<UUID, FactionMember> members,
  @NotNull Set<FactionClaim> claims,
  @NotNull Map<UUID, FactionRelation> relations,
  @NotNull List<FactionLog> logs,
  boolean open,
  @Nullable FactionPermissions permissions,
  @Nullable Double hardcorePower
) {
  /**
   * Maximum number of log entries to keep.
   */
  public static final int MAX_LOGS = 100;

  /**
   * Compact constructor for immutability.
   */
  public Faction {
    members = members != null ? Map.copyOf(members) : Map.of();
    claims = claims != null ? Set.copyOf(claims) : Set.of();
    relations = relations != null ? Map.copyOf(relations) : Map.of();
    logs = logs != null ? List.copyOf(logs) : List.of();
    if (color == null || color.isEmpty()) {
      color = "#FFFFFF"; // Default white
    } else if (!color.startsWith("#") && color.length() == 1) {
      // Auto-migrate legacy single-char color codes to hex
      color = LegacyColorParser.codeToHex(color.charAt(0));
    }
  }

  /**
   * Creates a new faction with a leader.
   *
   * @param name        the faction name
   * @param leaderUuid  the leader's UUID
   * @param leaderName  the leader's username
   * @return a new Faction
   */
  public static Faction create(@NotNull String name, @NotNull UUID leaderUuid, @NotNull String leaderName) {
    FactionMember leader = FactionMember.createLeader(leaderUuid, leaderName);
    Map<UUID, FactionMember> members = new HashMap<>();
    members.put(leaderUuid, leader);

    List<FactionLog> logs = new ArrayList<>();
    logs.add(FactionLog.create(FactionLog.LogType.MEMBER_JOIN, leaderName + " created the faction", leaderUuid,
        GuiKeys.LogsGui.MSG_FACTION_CREATED, leaderName));

    return new Faction(
      UUID.randomUUID(),
      name,
      null,
      null, // No tag by default
      "#55FFFF", // Default cyan
      System.currentTimeMillis(),
      null,
      members,
      Set.of(),
      Map.of(),
      logs,
      false,
      null, // Use default permissions
      null  // No hardcore power
    );
  }

  /**
   * Gets the effective permissions for this faction.
   * Returns the faction's permissions if set, otherwise defaults.
   *
   * @return the effective permissions
   */
  @NotNull
  public FactionPermissions getEffectivePermissions() {
    return permissions != null ? permissions : FactionPermissions.defaults();
  }

  /**
   * Creates a copy with updated permissions.
   *
   * @param newPermissions the new permissions
   * @return a new Faction with updated permissions
   */
  public Faction withPermissions(@Nullable FactionPermissions newPermissions) {
    return new Faction(id, name, description, tag, color, createdAt, home,
      members, claims, relations, logs, open, newPermissions, hardcorePower);
  }

  // === Member operations ===

  /**
   * Gets the faction leader.
   *
   * @return the leader, or null if none found (shouldn't happen)
   */
  @Nullable
  public FactionMember getLeader() {
    return members.values().stream()
      .filter(FactionMember::isLeader)
      .findFirst()
      .orElse(null);
  }

  /**
   * Gets the leader's UUID.
   *
   * @return the leader's UUID, or null if none
   */
  @Nullable
  public UUID getLeaderId() {
    FactionMember leader = getLeader();
    return leader != null ? leader.uuid() : null;
  }

  /**
   * Finds the best successor for leadership when the current leader leaves.
   * Priority: highest role (officer over member), then oldest joinedAt (most tenure).
   *
   * @return the best successor, or null if no other members exist
   */
  @Nullable
  public FactionMember findSuccessor() {
    return members.values().stream()
        .filter(m -> !m.isLeader())  // Exclude current leader
        .max(Comparator
            .comparingInt((FactionMember m) -> m.role().getLevel())  // Highest role first
            .thenComparingLong(m -> -m.joinedAt()))  // Then oldest joinedAt (negative for ascending)
        .orElse(null);
  }

  /**
   * Gets a member by UUID.
   *
   * @param uuid the member's UUID
   * @return the member, or null if not found
   */
  @Nullable
  public FactionMember getMember(@NotNull UUID uuid) {
    return members.get(uuid);
  }

  /**
   * Checks if a player is a member of this faction.
   *
   * @param uuid the player's UUID
   * @return true if member
   */
  public boolean hasMember(@NotNull UUID uuid) {
    return members.containsKey(uuid);
  }

  /**
   * Gets the member count.
   *
   * @return number of members
   */
  public int getMemberCount() {
    return members.size();
  }

  /**
   * Gets all members sorted by role (leader first).
   *
   * @return sorted list of members
   */
  @NotNull
  public List<FactionMember> getMembersSorted() {
    return members.values().stream()
      .sorted(Comparator.comparingInt((FactionMember m) -> -m.role().getLevel())
        .thenComparing(FactionMember::username))
      .toList();
  }

  /**
   * Creates a copy with a new member added.
   *
   * @param member the member to add
   * @return a new Faction with the member
   */
  public Faction withMember(@NotNull FactionMember member) {
    Map<UUID, FactionMember> newMembers = new HashMap<>(members);
    newMembers.put(member.uuid(), member);
    return new Faction(id, name, description, tag, color, createdAt, home, newMembers, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy without the specified member.
   *
   * @param uuid the member's UUID
   * @return a new Faction without the member
   */
  public Faction withoutMember(@NotNull UUID uuid) {
    Map<UUID, FactionMember> newMembers = new HashMap<>(members);
    newMembers.remove(uuid);
    return new Faction(id, name, description, tag, color, createdAt, home, newMembers, claims, relations, logs, open, permissions, hardcorePower);
  }

  // === Claim operations ===

  /**
   * Gets the claim count.
   *
   * @return number of claims
   */
  public int getClaimCount() {
    return claims.size();
  }

  /**
   * Checks if this faction has a claim at the given location.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return true if claimed
   */
  public boolean hasClaimAt(@NotNull String world, int chunkX, int chunkZ) {
    return claims.stream().anyMatch(c -> c.isAt(world, chunkX, chunkZ));
  }

  /**
   * Creates a copy with a new claim added.
   *
   * @param claim the claim to add
   * @return a new Faction with the claim
   */
  public Faction withClaim(@NotNull FactionClaim claim) {
    Set<FactionClaim> newClaims = new HashSet<>(claims);
    newClaims.add(claim);
    return new Faction(id, name, description, tag, color, createdAt, home, members, newClaims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy without the claim at the specified location.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @return a new Faction without that claim
   */
  public Faction withoutClaimAt(@NotNull String world, int chunkX, int chunkZ) {
    Set<FactionClaim> newClaims = new HashSet<>(claims);
    newClaims.removeIf(c -> c.isAt(world, chunkX, chunkZ));
    return new Faction(id, name, description, tag, color, createdAt, home, members, newClaims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with all claims removed.
   *
   * @return a new Faction with no claims
   */
  public Faction withoutAllClaims() {
    return new Faction(id, name, description, tag, color, createdAt, home, members, Set.of(), relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with updated hardcore power.
   *
   * @param hardcorePower the new hardcore power value (null to clear)
   * @return a new Faction with updated hardcore power
   */
  public Faction withHardcorePower(@Nullable Double hardcorePower) {
    return new Faction(id, name, description, tag, color, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  // === Relation operations ===

  /**
   * Gets the relation with another faction.
   *
   * @param factionId the other faction's ID
   * @return the relation, or null if neutral/none
   */
  @Nullable
  public FactionRelation getRelation(@NotNull UUID factionId) {
    return relations.get(factionId);
  }

  /**
   * Gets the relation type with another faction.
   *
   * @param factionId the other faction's ID
   * @return the relation type, defaults to NEUTRAL
   */
  @NotNull
  public RelationType getRelationType(@NotNull UUID factionId) {
    FactionRelation rel = relations.get(factionId);
    return rel != null ? rel.type() : RelationType.NEUTRAL;
  }

  /**
   * Checks if another faction is an ally.
   *
   * @param factionId the other faction's ID
   * @return true if ally
   */
  public boolean isAlly(@NotNull UUID factionId) {
    return getRelationType(factionId) == RelationType.ALLY;
  }

  /**
   * Checks if another faction is an enemy.
   *
   * @param factionId the other faction's ID
   * @return true if enemy
   */
  public boolean isEnemy(@NotNull UUID factionId) {
    return getRelationType(factionId) == RelationType.ENEMY;
  }

  /**
   * Creates a copy with an updated relation.
   *
   * @param relation the new relation
   * @return a new Faction with updated relation
   */
  public Faction withRelation(@NotNull FactionRelation relation) {
    Map<UUID, FactionRelation> newRelations = new HashMap<>(relations);
    if (relation.type() == RelationType.NEUTRAL) {
      newRelations.remove(relation.targetFactionId());
    } else {
      newRelations.put(relation.targetFactionId(), relation);
    }
    return new Faction(id, name, description, tag, color, createdAt, home, members, claims, newRelations, logs, open, permissions, hardcorePower);
  }

  // === Home operations ===

  /**
   * Creates a copy with a new home.
   *
   * @param newHome the new home
   * @return a new Faction with updated home
   */
  public Faction withHome(@Nullable FactionHome newHome) {
    return new Faction(id, name, description, tag, color, createdAt, newHome, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Checks if the faction has a home set.
   *
   * @return true if home is set
   */
  public boolean hasHome() {
    return home != null;
  }

  // === Log operations ===

  /**
   * Creates a copy with a new log entry.
   *
   * @param log the log to add
   * @return a new Faction with the log
   */
  public Faction withLog(@NotNull FactionLog log) {
    List<FactionLog> newLogs = new ArrayList<>();
    newLogs.add(log);
    newLogs.addAll(logs);
    // Trim to max size
    if (newLogs.size() > MAX_LOGS) {
      newLogs = newLogs.subList(0, MAX_LOGS);
    }
    return new Faction(id, name, description, tag, color, createdAt, home, members, claims, relations, newLogs, open, permissions, hardcorePower);
  }

  // === Property updates ===

  /**
   * Creates a copy with a new name.
   *
   * @param newName the new name
   * @return a new Faction with updated name
   */
  public Faction withName(@NotNull String newName) {
    return new Faction(id, newName, description, tag, color, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with a new description.
   *
   * @param newDescription the new description
   * @return a new Faction with updated description
   */
  public Faction withDescription(@Nullable String newDescription) {
    return new Faction(id, name, newDescription, tag, color, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with a new tag.
   *
   * @param newTag the new tag (1-5 alphanumeric chars, or null)
   * @return a new Faction with updated tag
   */
  public Faction withTag(@Nullable String newTag) {
    return new Faction(id, name, description, newTag, color, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with a new color.
   *
   * @param newColor the new color code
   * @return a new Faction with updated color
   */
  public Faction withColor(@NotNull String newColor) {
    return new Faction(id, name, description, tag, newColor, createdAt, home, members, claims, relations, logs, open, permissions, hardcorePower);
  }

  /**
   * Creates a copy with updated open status.
   *
   * @param isOpen whether the faction is open
   * @return a new Faction with updated open status
   */
  public Faction withOpen(boolean isOpen) {
    return new Faction(id, name, description, tag, color, createdAt, home, members, claims, relations, logs, isOpen, permissions, hardcorePower);
  }

  /**
   * Gets the faction's hex color string.
   * Convenience for using in UI contexts where the raw hex is needed.
   *
   * @return the hex color string (e.g., "#55FFFF")
   */
  @NotNull
  public String getColorHex() {
    return color;
  }

  /**
   * Gets the colored faction name as a string.
   * Note: Since color is now hex, this returns the name without legacy formatting.
   *
   * @return the name (color should be applied via Message API in rendering contexts)
   */
  @NotNull
  public String getColoredName() {
    return name;
  }

  /**
   * Represents the faction home location.
   */
  public record FactionHome(
    @NotNull String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    long setAt,
    @NotNull UUID setBy
  ) {
    /**
     * Creates a new faction home.
     */
    public static FactionHome create(@NotNull String world, double x, double y, double z,
                    float yaw, float pitch, @NotNull UUID setBy) {
      return new FactionHome(world, x, y, z, yaw, pitch, System.currentTimeMillis(), setBy);
    }
  }
}
