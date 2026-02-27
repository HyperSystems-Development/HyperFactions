package com.hyperfactions.chat;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to public chat events and applies faction tag formatting.
 *
 * <p>Registers at LATE priority by default (configurable) to run after
 * other plugins like LuckPerms that may set formatters at NORMAL priority.
 * This allows us to wrap their formatter and add faction tags.
 */
public class PublicChatListener {

  private final HyperFactions hyperFactions;

  private final FactionChatFormatter formatter;

  /**
   * Creates a new PublicChatListener.
   *
   * @param hyperFactions the HyperFactions instance
   */
  public PublicChatListener(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
    this.formatter = new FactionChatFormatter(
      hyperFactions.getFactionManager(),
      hyperFactions.getRelationManager()
    );
  }

  /**
   * Gets the configured event priority for chat formatting.
   *
   * @return the event priority
   */
  @NotNull
  public EventPriority getEventPriority() {
    String priority = ConfigManager.get().getChatEventPriority();
    try {
      return EventPriority.valueOf(priority.toUpperCase());
    } catch (IllegalArgumentException e) {
      Logger.warn("Invalid chat event priority '%s', using LATE", priority);
      return EventPriority.LATE;
    }
  }

  /**
   * Handles the PlayerChatEvent asynchronously.
   * Sets up ChatContext with sender and applies the faction formatter.
   *
   * @param futureEvent the async event future
   * @return the modified event future
   */
  @NotNull
  public CompletableFuture<PlayerChatEvent> onPlayerChatAsync(
      @NotNull CompletableFuture<PlayerChatEvent> futureEvent) {
    return futureEvent.thenApply(event -> {
      try {
        return handleChatEvent(event);
      } catch (Exception e) {
        Logger.severe("Error handling chat event", e);
        return event;
      }
    });
  }

  /**
   * Processes the chat event.
   */
  @NotNull
  private PlayerChatEvent handleChatEvent(@NotNull PlayerChatEvent event) {
    // Skip if cancelled
    if (event.isCancelled()) {
      return event;
    }

    // Skip if chat formatting is disabled
    if (!ConfigManager.get().isChatFormattingEnabled()) {
      return event;
    }

    // Skip if HyperPerms chat is enabled - let HyperPerms handle formatting
    // HyperPerms includes faction info via FactionIntegration
    if (isHyperPermsChatEnabled()) {
      Logger.debug("[PublicChat] HyperPerms chat enabled, skipping HyperFactions formatter");
      return event;
    }

    PlayerRef sender = event.getSender();

    // Store sender in ThreadLocal for the formatter
    // The formatter is called once per target player
    ChatContext.setSender(sender);

    try {
      // Replace the formatter with our faction-aware formatter
      // This completely replaces any existing formatter
      event.setFormatter(formatter);

      Logger.debug("[PublicChat] Set faction formatter for message from %s", sender.getUsername());
    } finally {
      // Note: We don't clear ChatContext here because the formatter
      // will be called later when messages are actually sent.
      // The context should be cleared after all targets receive the message.
      // Since Hytale's chat is async, we rely on the thread being reused
      // or the ThreadLocal being cleared on the next chat message.
    }

    return event;
  }

  /**
   * Checks if HyperPerms chat formatting is enabled.
   * If so, we should let HyperPerms handle chat to avoid conflicts.
   */
  private boolean isHyperPermsChatEnabled() {
    try {
      Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
      java.lang.reflect.Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
      Object hyperPerms = getInstanceMethod.invoke(null);
      if (hyperPerms == null) {
        return false;
      }

      java.lang.reflect.Method getChatManagerMethod = hyperPerms.getClass().getMethod("getChatManager");
      Object chatManager = getChatManagerMethod.invoke(hyperPerms);
      if (chatManager == null) {
        return false;
      }

      java.lang.reflect.Method isEnabledMethod = chatManager.getClass().getMethod("isEnabled");
      Object result = isEnabledMethod.invoke(chatManager);
      return result instanceof Boolean && (Boolean) result;
    } catch (ClassNotFoundException e) {
      // HyperPerms not installed
      return false;
    } catch (Exception e) {
      Logger.debug("[PublicChat] Error checking HyperPerms chat status: %s", e.getMessage());
      return false;
    }
  }
}
