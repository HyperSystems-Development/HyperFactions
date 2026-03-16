package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published when a player's power changes.
 *
 * @param playerUuid the player
 * @param oldPower   power before the change
 * @param newPower   power after the change
 * @param reason     what caused the change
 */
public record PlayerPowerChangeEvent(
    @NotNull UUID playerUuid,
    double oldPower,
    double newPower,
    @NotNull Reason reason
) {
  public enum Reason {
    /** Player died */
    DEATH,
    /** Player killed another player */
    KILL,
    /** Player killed a neutral player (penalty) */
    NEUTRAL_KILL,
    /** Periodic power regeneration */
    REGEN,
    /** Combat logout penalty */
    COMBAT_LOGOUT,
    /** Admin set/adjust */
    ADMIN
  }

  /** Returns the delta (positive = gained, negative = lost). */
  public double delta() {
    return newPower - oldPower;
  }
}
