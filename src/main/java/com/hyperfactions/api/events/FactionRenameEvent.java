package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published when a faction's name, tag, description, or color changes.
 *
 * @param factionId  the faction
 * @param field      which field changed
 * @param oldValue   the previous value (null if previously unset)
 * @param newValue   the new value (null if cleared)
 * @param actorUuid  the player who made the change
 */
public record FactionRenameEvent(
    @NotNull UUID factionId,
    @NotNull Field field,
    @Nullable String oldValue,
    @Nullable String newValue,
    @NotNull UUID actorUuid
) {
  public enum Field { NAME, TAG, DESCRIPTION, COLOR }
}
