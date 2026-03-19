package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player teleports to their faction home.
 * Fired for both instant (warmup=0) and warmup-completed teleports.
 *
 * <p>This event fires AFTER the teleport component has been added,
 * meaning the teleport is committed. It cannot be cancelled.
 *
 * <p>Source coordinates represent the player's position before teleporting.
 * Destination coordinates are the faction home location.
 */
public record FactionHomeTeleportEvent(
  @NotNull UUID playerUuid,
  @NotNull UUID factionId,
  @NotNull String sourceWorld,
  double sourceX,
  double sourceY,
  double sourceZ,
  @NotNull String destWorld,
  double destX,
  double destY,
  double destZ,
  float destYaw,
  float destPitch
) {}
