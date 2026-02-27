package com.hyperfactions.importer.elbaphfactions;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for ElbaphFactions factions.json entries.
 * Field names match the JSON structure exactly for deserialization.
 *
 * @param id                  the faction UUID
 * @param ownerUuid           the owner's UUID
 * @param name                the faction name
 * @param tag                 the faction tag (1-5 chars)
 * @param description         the faction description
 * @param memberUuids         list of member UUIDs
 * @param memberRanks         map of member UUID to rank + lastLogin entries
 * @param allies              list of allied faction UUIDs
 * @param enemies             list of enemy faction UUIDs
 * @param pendingAllyRequests list of pending ally request UUIDs
 * @param createdAt           creation timestamp (epoch millis)
 * @param currentPower        the faction's current power
 * @param maxPower            the faction's max power
 * @param lastPowerRegen      last power regen timestamp
 * @param claimCount          number of claims (metadata only)
 * @param factionPoints       faction points (no HyperFactions equivalent)
 * @param factionPointsEarned total earned
 * @param factionPointsSpent  total spent
 * @param totalKills          total kills
 * @param totalDeaths         total deaths
 * @param warsWon             wars won
 * @param lastDeathTime       last death timestamp
 * @param color               ARGB color as signed integer
 * @param outsiderCanBreak    territory permission
 * @param outsiderCanPlace    territory permission
 * @param outsiderCanInteract territory permission
 * @param allyCanBreak        territory permission
 * @param allyCanPlace        territory permission
 * @param allyCanInteract     territory permission
 * @param memberCanBreak      territory permission
 * @param memberCanPlace      territory permission
 * @param memberCanInteract   territory permission
 * @param pvpEnabled          territory PvP toggle
 * @param workersEnabled      workers toggle (no equivalent)
 * @param guardsEnabled       guards toggle (no equivalent)
 * @param farmersEnabled      farmers toggle (no equivalent)
 * @param lumberjacksEnabled  lumberjacks toggle (no equivalent)
 * @param workerMessagesEnabled worker messages toggle (no equivalent)
 * @param teleporterSlots     teleporter slots (no equivalent)
 * @param teleportersPlaced   teleporters placed (no equivalent)
 * @param homeX               home X coordinate
 * @param homeY               home Y coordinate
 * @param homeZ               home Z coordinate
 * @param homeSet             whether home is set
 * @param farmPlots           farm plots (no equivalent)
 * @param workers             workers (no equivalent)
 * @param activityLog         activity log entries
 */
public record ElbaphFaction(
  @Nullable String id,
  @Nullable String ownerUuid,
  @Nullable String name,
  @Nullable String tag,
  @Nullable String description,
  @Nullable List<String> memberUuids,
  @Nullable Map<String, Object> memberRanks,
  @Nullable List<String> allies,
  @Nullable List<String> enemies,
  @Nullable List<String> pendingAllyRequests,
  long createdAt,
  double currentPower,
  double maxPower,
  long lastPowerRegen,
  int claimCount,
  double factionPoints,
  double factionPointsEarned,
  double factionPointsSpent,
  int totalKills,
  int totalDeaths,
  int warsWon,
  long lastDeathTime,
  int color,
  boolean outsiderCanBreak,
  boolean outsiderCanPlace,
  boolean outsiderCanInteract,
  boolean allyCanBreak,
  boolean allyCanPlace,
  boolean allyCanInteract,
  boolean memberCanBreak,
  boolean memberCanPlace,
  boolean memberCanInteract,
  boolean pvpEnabled,
  boolean workersEnabled,
  boolean guardsEnabled,
  boolean farmersEnabled,
  boolean lumberjacksEnabled,
  boolean workerMessagesEnabled,
  int teleporterSlots,
  int teleportersPlaced,
  double homeX,
  double homeY,
  double homeZ,
  boolean homeSet,
  @Nullable List<Object> farmPlots,
  @Nullable List<Object> workers,
  @Nullable List<Object> activityLog
) {
  /**
   * Checks if this faction has a home set.
   */
  public boolean hasHome() {
    return homeSet;
  }

  /**
   * Gets the member count.
   */
  public int getMemberCount() {
    return memberUuids != null ? memberUuids.size() : 0;
  }

  /**
   * Gets a member's role from the memberRanks map.
   * The map contains both role entries (uuid -> "LEADER") and
   * lastLogin entries (uuid_lastLogin -> timestamp).
   *
   * @param uuid the member UUID string
   * @return the role string, or null if not found
   */
  @Nullable
  public String getMemberRole(@Nullable String uuid) {
    if (memberRanks == null || uuid == null) {
      return null;
    }
    Object value = memberRanks.get(uuid);
    return value instanceof String ? (String) value : null;
  }

  /**
   * Gets a member's last login timestamp from the memberRanks map.
   * Stored as uuid_lastLogin -> timestamp (as Double from Gson).
   *
   * @param uuid the member UUID string
   * @return the last login epoch millis, or 0 if not found
   */
  public long getMemberLastLogin(@Nullable String uuid) {
    if (memberRanks == null || uuid == null) {
      return 0;
    }
    Object value = memberRanks.get(uuid + "_lastLogin");
    if (value instanceof Number number) {
      return number.longValue();
    }
    return 0;
  }
}
