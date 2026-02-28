package com.hyperfactions.protection;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Debounces protection denial messages to prevent chat spam when players
 * repeatedly attempt blocked actions (e.g., spam-clicking protected blocks).
 *
 * <p>
 * Each player+action combination has an independent cooldown. Messages within
 * the cooldown window are silently dropped.
 */
public final class ProtectionMessageDebounce {

  private static final long DEFAULT_COOLDOWN_MS = 2000;

  private static final ConcurrentHashMap<String, Long> lastMessageTimes = new ConcurrentHashMap<>();

  private ProtectionMessageDebounce() {}

  /**
   * Sends a protection denial message if the player is not on cooldown for this action.
   *
   * @param player    the player to send the message to
   * @param actionKey a short key identifying the action type (e.g., "block_break", "pve_damage")
   * @param message   the message to send
   */
  public static void sendIfNotOnCooldown(@NotNull PlayerRef player, @NotNull String actionKey,
                       @NotNull Message message) {
    String key = player.getUuid() + ":" + actionKey;
    long now = System.currentTimeMillis();
    Long last = lastMessageTimes.get(key);
    if (last != null && (now - last) < DEFAULT_COOLDOWN_MS) {
      return;
    }
    lastMessageTimes.put(key, now);
    player.sendMessage(message);
  }

  /**
   * Sends a protection denial message if the player is not on cooldown for this action.
   * Overload for Player entity (which is not a PlayerRef).
   *
   * @param player    the player entity
   * @param actionKey a short key identifying the action type
   * @param message   the message to send
   */
  public static void sendIfNotOnCooldown(@NotNull Player player, @NotNull String actionKey,
                       @NotNull Message message) {
    UUID uuid = player.getUuid();
    if (uuid == null) {
      player.sendMessage(message);
      return;
    }
    String key = uuid + ":" + actionKey;
    long now = System.currentTimeMillis();
    Long last = lastMessageTimes.get(key);
    if (last != null && (now - last) < DEFAULT_COOLDOWN_MS) {
      return;
    }
    lastMessageTimes.put(key, now);
    player.sendMessage(message);
  }

  /**
   * Removes stale entries older than 2x the cooldown to prevent unbounded growth.
   * Should be called periodically (e.g., every 30 seconds).
   */
  public static void cleanup() {
    long cutoff = System.currentTimeMillis() - (DEFAULT_COOLDOWN_MS * 2);
    lastMessageTimes.entrySet().removeIf(e -> e.getValue() < cutoff);
  }
}
