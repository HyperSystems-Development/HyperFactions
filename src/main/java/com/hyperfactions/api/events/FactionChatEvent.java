package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Published after a message is sent in faction or ally chat.
 *
 * @param senderUuid the player who sent the message
 * @param factionId  the faction the message was sent to
 * @param channel    the chat channel (FACTION or ALLY)
 * @param message    the message content
 */
public record FactionChatEvent(
    @NotNull UUID senderUuid,
    @NotNull UUID factionId,
    @NotNull Channel channel,
    @NotNull String message
) {
  public enum Channel { FACTION, ALLY }
}
