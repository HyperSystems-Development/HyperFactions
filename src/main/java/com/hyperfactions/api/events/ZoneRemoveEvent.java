package com.hyperfactions.api.events;

import com.hyperfactions.data.ZoneType;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published when a zone is removed.
 *
 * @param zoneId the zone ID
 * @param name   the zone name (captured before deletion)
 * @param type   the zone type
 * @param world  the world
 */
public record ZoneRemoveEvent(
    @NotNull UUID zoneId,
    @NotNull String name,
    @NotNull ZoneType type,
    @NotNull String world
) {}
