package com.hyperfactions.api.events;

import com.hyperfactions.data.ZoneType;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a new zone is created.
 *
 * @param zoneId    the zone ID
 * @param name      the zone name
 * @param type      the zone type (SAFEZONE or WARZONE)
 * @param world     the world name
 * @param createdBy the player who created it (null for system)
 */
public record ZoneCreateEvent(
    @NotNull UUID zoneId,
    @NotNull String name,
    @NotNull ZoneType type,
    @NotNull String world,
    @Nullable UUID createdBy
) {}
