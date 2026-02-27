package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a single faction or ally chat message.
 *
 * @param senderId        UUID of the player who sent the message
 * @param senderName      display name of the sender at send time
 * @param senderFactionTag faction tag of the sender at send time
 * @param channel         which chat channel (FACTION or ALLY)
 * @param message         the message text
 * @param timestamp       when the message was sent (epoch millis)
 */
public record ChatMessage(
  @NotNull UUID senderId,
  @NotNull String senderName,
  @NotNull String senderFactionTag,
  @NotNull Channel channel,
  @NotNull String message,
  long timestamp
) {
  /**
   * Chat channel types.
   */
  public enum Channel {
    FACTION,
    ALLY
  }

  /**
   * Creates a new chat message at the current time.
   *
   * @param senderId        the sender's UUID
   * @param senderName      the sender's display name
   * @param senderFactionTag the sender's faction tag
   * @param channel         the chat channel
   * @param message         the message text
   * @return a new ChatMessage
   */
  public static ChatMessage create(@NotNull UUID senderId, @NotNull String senderName,
                  @NotNull String senderFactionTag, @NotNull Channel channel,
                  @NotNull String message) {
    return new ChatMessage(senderId, senderName, senderFactionTag, channel, message, System.currentTimeMillis());
  }
}
