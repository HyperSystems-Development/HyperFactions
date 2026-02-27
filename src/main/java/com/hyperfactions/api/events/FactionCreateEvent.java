package com.hyperfactions.api.events;

import com.hyperfactions.data.Faction;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a faction is created.
 */
public record FactionCreateEvent(
  @NotNull Faction faction,
  @NotNull UUID creatorUuid
) {}
