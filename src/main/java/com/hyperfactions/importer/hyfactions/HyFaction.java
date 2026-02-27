package com.hyperfactions.importer.hyfactions;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a faction from HyFactions.
 * Field names match the JSON structure exactly for Gson deserialization.
 *
 * @param Id                the faction UUID
 * @param Owner             the owner's UUID
 * @param Name              the faction name
 * @param Members           list of member UUIDs
 * @param Color             ARGB color as integer (e.g., -16739585)
 * @param CreatedTracker    creation info
 * @param ModifiedTracker   last modification info
 * @param TotalPower        the faction's total power
 * @param HomeDimension     the home world/dimension name
 * @param HomeX             home X coordinate
 * @param HomeY             home Y coordinate
 * @param HomeZ             home Z coordinate
 * @param HomeYaw           home yaw rotation
 * @param HomePitch         home pitch rotation
 * @param MemberGrades      map of member UUID to grade (OFFICER, etc.)
 * @param MemberPermissions map of member UUID to permissions (usually empty)
 * @param Logs              list of activity logs
 * @param Relations         map of target faction UUID to relation type ("ally", "enemy")
 */
public record HyFaction(
  @Nullable String Id,
  @Nullable String Owner,
  @Nullable String Name,
  @Nullable List<String> Members,
  int Color,
  @Nullable HyFactionTracker CreatedTracker,
  @Nullable HyFactionTracker ModifiedTracker,
  int TotalPower,
  @Nullable String HomeDimension,
  double HomeX,
  double HomeY,
  double HomeZ,
  float HomeYaw,
  float HomePitch,
  @Nullable Map<String, String> MemberGrades,
  @Nullable Map<String, Object> MemberPermissions,
  @Nullable List<HyFactionLog> Logs,
  @Nullable Map<String, String> Relations
) {
  /**
   * Checks if this faction has a home set.
   *
   * @return true if home dimension is set
   */
  public boolean hasHome() {
    return HomeDimension != null && !HomeDimension.isEmpty();
  }

  /**
   * Gets the member count.
   *
   * @return number of members, or 0 if null
   */
  public int getMemberCount() {
    return Members != null ? Members.size() : 0;
  }
}
