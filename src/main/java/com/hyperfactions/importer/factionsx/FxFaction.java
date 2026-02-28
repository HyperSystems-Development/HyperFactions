package com.hyperfactions.importer.factionsx;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for FactionsX per-faction JSON files ({@code config/factions/{UUID}.json}).
 *
 * <p>Key quirk: the {@code Members} map does NOT include the faction owner.
 * The owner is always LEADER and stored separately in the {@code Owner} field.
 */
public record FxFaction(
  @Nullable String Id,
  @Nullable String Owner,
  @Nullable String Name,
  int Color,
  @Nullable String Description,
  @Nullable FxTracker CreatedTracker,
  @Nullable FxTracker ModifiedTracker,
  @Nullable String HomeDimension,
  double HomeX,
  double HomeY,
  double HomeZ,
  float HomeYaw,
  float HomePitch,
  @Nullable Map<String, String> Members,
  @Nullable Map<String, String> Relations,
  @Nullable Map<String, Map<String, Boolean>> Permissions
) {

  /** Returns true if the faction has a home set. */
  public boolean hasHome() {
    return HomeDimension != null && !HomeDimension.isEmpty();
  }

  /**
   * Returns the total member count including the owner.
   * The owner is NOT in the Members map, so we add 1 if Owner is present.
   */
  public int getMemberCount() {
    int count = Members != null ? Members.size() : 0;
    if (Owner != null && !Owner.isEmpty()) {
      count++;
    }
    return count;
  }
}
