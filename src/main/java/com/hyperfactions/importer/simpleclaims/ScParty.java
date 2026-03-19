package com.hyperfactions.importer.simpleclaims;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for a SimpleClaims party from {@code Parties.json}.
 *
 * <p>Key quirk: the {@code Owner} is NOT in the {@code Members} list.
 * Members only contains non-owner members.
 */
public record ScParty(
  @Nullable String Id,
  @Nullable String Owner,
  @Nullable String Name,
  @Nullable String Description,
  @Nullable List<String> Members,
  int Color,
  @Nullable List<ScOverride> Overrides,
  @Nullable ScTracker CreatedTracker,
  @Nullable ScTracker ModifiedTracker,
  @Nullable List<String> PartyAllies,
  @Nullable List<String> PlayerAllies
) {

  /** Returns the total member count including the owner. */
  public int getMemberCount() {
    int count = Members != null ? Members.size() : 0;
    if (Owner != null && !Owner.isEmpty()) {
      count++;
    }
    return count;
  }
}
