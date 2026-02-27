package com.hyperfactions.api.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Simple event bus for faction events.
 */
public final class EventBus {

  private static final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

  private EventBus() {}

  /**
   * Registers a listener for an event type.
   *
   * @param eventClass the event class
   * @param listener   the listener
   * @param {@code <T>}        the event type
   */
  public static <T> void register(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) {
    listeners.computeIfAbsent(eventClass, k -> Collections.synchronizedList(new ArrayList<>()))
      .add(listener);
  }

  /**
   * Unregisters a listener for an event type.
   *
   * @param eventClass the event class
   * @param listener   the listener
   * @param {@code <T>}        the event type
   */
  public static <T> void unregister(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) {
    List<Consumer<?>> list = listeners.get(eventClass);
    if (list != null) {
      list.remove(listener);
    }
  }

  /**
   * Publishes an event to all registered listeners.
   *
   * @param event the event
   * @param {@code <T>}   the event type
   */
  @SuppressWarnings("unchecked")
  public static <T> void publish(@NotNull T event) {
    List<Consumer<?>> list = listeners.get(event.getClass());
    if (list != null) {
      for (Consumer<?> listener : list) {
        try {
          ((Consumer<T>) listener).accept(event);
        } catch (Exception e) {
          // Log but don't propagate
          System.err.println("[HyperFactions] Error in event listener: " + e.getMessage());
        }
      }
    }
  }

  /**
   * Clears all listeners.
   */
  public static void clearAll() {
    listeners.clear();
  }
}
