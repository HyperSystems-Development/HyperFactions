package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published when a pending faction home teleport warmup is cancelled.
 *
 * @param playerUuid the player whose teleport was cancelled
 * @param reason     why the teleport was cancelled
 */
public record TeleportCancelledEvent(
    @NotNull UUID playerUuid,
    @NotNull Reason reason
) {
  public enum Reason {
    /** Player moved during warmup */
    MOVED,
    /** Player took damage during warmup */
    DAMAGE,
    /** Player became combat tagged during warmup */
    COMBAT_TAGGED,
    /** Teleport was manually cancelled */
    MANUAL
  }
}
