package com.hyperfactions.api.events;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Published before a message is sent in faction or ally chat. Can be cancelled to block the message.
 */
public final class FactionChatPreEvent implements Cancellable {

  private final UUID senderUuid;
  private final UUID factionId;
  private final FactionChatEvent.Channel channel;
  private final String message;
  private boolean cancelled;
  private String cancelReason;

  public FactionChatPreEvent(@NotNull UUID senderUuid, @NotNull UUID factionId,
                             @NotNull FactionChatEvent.Channel channel, @NotNull String message) {
    this.senderUuid = senderUuid;
    this.factionId = factionId;
    this.channel = channel;
    this.message = message;
  }

  @NotNull public UUID senderUuid() { return senderUuid; }
  @NotNull public UUID factionId() { return factionId; }
  @NotNull public FactionChatEvent.Channel channel() { return channel; }
  @NotNull public String message() { return message; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
