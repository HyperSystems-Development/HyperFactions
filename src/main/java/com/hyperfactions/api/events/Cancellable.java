package com.hyperfactions.api.events;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for events that can be cancelled by listeners.
 * When a pre-event is cancelled, the corresponding action is aborted.
 *
 * <p>Listeners can optionally provide a cancellation reason via
 * {@link #setCancelReason(String)}, which will be sent to the player
 * instead of the default denial message.
 */
public interface Cancellable {

  boolean isCancelled();

  void setCancelled(boolean cancelled);

  @Nullable
  String getCancelReason();

  void setCancelReason(@Nullable String reason);
}
