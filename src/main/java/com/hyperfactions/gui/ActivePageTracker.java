package com.hyperfactions.gui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks which GUI page each player currently has open.
 * Used by {@link GuiUpdateService} to dispatch real-time updates
 * to players viewing affected pages.
 *
 * <p>Thread-safe: uses ConcurrentHashMap for concurrent access from
 * world threads and manager callbacks.
 */
public class ActivePageTracker {

  /**
   * Information about a player's currently open page.
   *
   * @param pageId    The page identifier (e.g., "members", "invites", "map")
   * @param factionId The faction context (null for non-faction pages)
   * @param page      The page instance
   */
  public record ActivePageInfo(
      @NotNull String pageId,
      @Nullable UUID factionId,
      @NotNull InteractiveCustomUIPage<?> page
  ) {}

  private final ConcurrentHashMap<UUID, ActivePageInfo> activePlayers = new ConcurrentHashMap<>();

  /**
   * Registers a player's currently open page.
   * Overwrites any previous entry (player can only view one page at a time).
   *
   * @param playerUuid The player's UUID
   * @param pageId     The page identifier
   * @param factionId  The faction context (null for non-faction pages)
   * @param page       The page instance
   */
  public void register(@NotNull UUID playerUuid, @NotNull String pageId,
            @Nullable UUID factionId, @NotNull InteractiveCustomUIPage<?> page) {
    activePlayers.put(playerUuid, new ActivePageInfo(pageId, factionId, page));
  }

  /**
   * Unregisters a player (e.g., on disconnect or page dismiss).
   *
   * @param playerUuid The player's UUID
   */
  public void unregister(@NotNull UUID playerUuid) {
    activePlayers.remove(playerUuid);
  }

  /**
   * Gets the active page info for a player.
   *
   * @param playerUuid The player's UUID
   * @return The active page info, or null if no page is tracked
   */
  @Nullable
  public ActivePageInfo get(@NotNull UUID playerUuid) {
    return activePlayers.get(playerUuid);
  }

  /**
   * Gets all players currently viewing a specific page for a specific faction.
   *
   * @param pageId    The page identifier to match
   * @param factionId The faction ID to match (null matches pages with no faction context)
   * @return List of player UUIDs viewing the matching page
   */
  @NotNull
  public List<UUID> getPlayersOnPage(@NotNull String pageId, @Nullable UUID factionId) {
    List<UUID> result = new ArrayList<>();
    for (Map.Entry<UUID, ActivePageInfo> entry : activePlayers.entrySet()) {
      ActivePageInfo info = entry.getValue();
      if (info.pageId().equals(pageId) && Objects.equals(info.factionId(), factionId)) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Gets all players currently viewing a specific page (any faction context).
   *
   * @param pageId The page identifier to match
   * @return List of player UUIDs viewing the matching page
   */
  @NotNull
  public List<UUID> getPlayersOnPage(@NotNull String pageId) {
    List<UUID> result = new ArrayList<>();
    for (Map.Entry<UUID, ActivePageInfo> entry : activePlayers.entrySet()) {
      if (entry.getValue().pageId().equals(pageId)) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  /**
   * Clears all tracked pages. Used during shutdown.
   */
  public void clear() {
    activePlayers.clear();
  }
}
