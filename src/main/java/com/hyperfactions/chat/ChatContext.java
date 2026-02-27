package com.hyperfactions.chat;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local storage for passing the chat message sender to the formatter.
 *
 * <p>The PlayerChatEvent.Formatter interface only receives the target player (viewer)
 * in its format() method. We need the sender to determine the faction relation
 * from the viewer's perspective. This class uses ThreadLocal to pass the sender
 * from the event handler to the formatter.
 */
public final class ChatContext {

  private static final ThreadLocal<PlayerRef> SENDER = new ThreadLocal<>();

  private ChatContext() {
    // Static utility class
  }

  /**
   * Sets the current chat message sender.
   * Call this before the formatter is invoked.
   *
   * @param sender the message sender
   */
  public static void setSender(@Nullable PlayerRef sender) {
    if (sender != null) {
      SENDER.set(sender);
    }
  }

  /**
   * Gets the current chat message sender.
   *
   * @return the sender, or null if not set
   */
  @Nullable
  public static PlayerRef getSender() {
    return SENDER.get();
  }

  /**
   * Clears the sender from the current thread.
   * Call this after message formatting is complete to prevent memory leaks.
   */
  public static void clear() {
    SENDER.remove();
  }
}
