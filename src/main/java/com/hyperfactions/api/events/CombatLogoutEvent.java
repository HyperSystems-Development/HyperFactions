package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published when a combat-tagged player disconnects.
 *
 * @param playerUuid       the player who logged out
 * @param remainingSeconds seconds remaining on their combat tag
 */
public record CombatLogoutEvent(
    @NotNull UUID playerUuid,
    int remainingSeconds
) {}
