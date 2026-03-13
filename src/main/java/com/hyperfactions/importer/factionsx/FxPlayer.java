package com.hyperfactions.importer.factionsx;

import org.jetbrains.annotations.Nullable;

/**
 * Gson-mapped record for FactionsX per-player JSON files ({@code config/players/{UUID}.json}).
 *
 * <p>Power is per-player (not per-faction). FactionId/FactionRole may be null if the player
 * is not in a faction.
 */
public record FxPlayer(
  @Nullable String Uuid,
  @Nullable String LastKnownName,
  @Nullable String FactionId,
  @Nullable String FactionRole,
  int Power,
  int MaxPower
) {}
