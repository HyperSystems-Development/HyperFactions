package com.hyperfactions.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Holds the chat history for a single faction.
 *
 * <p>
 * Immutable — each mutation returns a new instance.
 *
 * @param factionId the faction this history belongs to
 * @param messages  messages ordered newest first
 */
public record FactionChatHistory(
  @NotNull UUID factionId,
  @NotNull List<ChatMessage> messages
) {
  /**
   * Canonical constructor — defensively copies the message list.
   */
  public FactionChatHistory {
    messages = List.copyOf(messages);
  }

  /**
   * Creates an empty history for a faction.
   *
   * @param factionId the faction ID
   * @return empty history
   */
  public static FactionChatHistory empty(@NotNull UUID factionId) {
    return new FactionChatHistory(factionId, List.of());
  }

  /**
   * Returns a new history with the given message prepended (newest first),
   * truncated to the max size.
   *
   * @param msg         the new message
   * @param maxMessages maximum messages to retain
   * @return new history with the message added
   */
  public FactionChatHistory withMessage(@NotNull ChatMessage msg, int maxMessages) {
    List<ChatMessage> updated = new ArrayList<>(Math.min(messages.size() + 1, maxMessages));
    updated.add(msg);
    int limit = Math.min(messages.size(), maxMessages - 1);
    for (int i = 0; i < limit; i++) {
      updated.add(messages.get(i));
    }
    return new FactionChatHistory(factionId, updated);
  }

  /**
   * Returns a new history with messages older than the cutoff removed.
   *
   * @param cutoffMillis epoch millis — messages older than this are pruned
   * @return new history with old messages removed
   */
  public FactionChatHistory pruneOlderThan(long cutoffMillis) {
    List<ChatMessage> pruned = messages.stream()
        .filter(m -> m.timestamp() >= cutoffMillis)
        .toList();
    if (pruned.size() == messages.size()) {
      return this; // nothing pruned
    }
    return new FactionChatHistory(factionId, pruned);
  }

  /**
   * Gets the number of messages in this history.
   *
   * @return message count
   */
  public int size() {
    return messages.size();
  }

  /**
   * Checks if this history is empty.
   *
   * @return true if no messages
   */
  public boolean isEmpty() {
    return messages.isEmpty();
  }
}
