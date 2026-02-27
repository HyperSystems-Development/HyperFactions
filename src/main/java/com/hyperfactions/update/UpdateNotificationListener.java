package com.hyperfactions.update;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.awt.Color;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for player connect events and notifies operators about available updates.
 * Notifications are sent only to players with the required permission and who have
 * not disabled notifications via their preferences.
 */
public final class UpdateNotificationListener {

  private static final Color GOLD = new Color(255, 170, 0);

  private static final Color GRAY = Color.GRAY;

  private static final Color GREEN = new Color(85, 255, 85);

  private static final Color WHITE = Color.WHITE;

  /** Delay before sending notification to avoid join message spam. */
  private static final long NOTIFICATION_DELAY_MS = 1500;

  private final HyperFactions hyperFactions;

  private final ScheduledExecutorService scheduler;

  /** Tracks players who have already been notified this session. */
  private final Set<UUID> notifiedPlayers = ConcurrentHashMap.newKeySet();

  /**
   * Creates a new update notification listener.
   *
   * @param hyperFactions the HyperFactions instance
   */
  public UpdateNotificationListener(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
    this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "HyperFactions-UpdateNotifier");
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * Registers this listener with the event registry.
   *
   * @param eventRegistry the event registry to register with
   */
  public void register(@NotNull EventRegistry eventRegistry) {
    eventRegistry.register(PlayerConnectEvent.class, this::onPlayerConnect);
    eventRegistry.register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    Logger.debug("[UpdateNotify] Registered update notification listener");
  }

  /**
   * Unregisters this listener and shuts down the scheduler.
   */
  public void shutdown() {
    scheduler.shutdown();
    try {
      scheduler.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    notifiedPlayers.clear();
    Logger.debug("[UpdateNotify] Unregistered update notification listener");
  }

  /**
   * Handles player connect event.
   * Schedules a delayed notification check for eligible players.
   *
   * @param event the player connect event
   */
  private void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();

    // Schedule the notification check with a delay
    scheduler.schedule(() -> {
      try {
        checkAndNotify(playerRef);
      } catch (Exception e) {
        Logger.warn("[UpdateNotify] Failed to send notification to %s: %s",
          playerRef.getUsername(), e.getMessage());
      }
    }, NOTIFICATION_DELAY_MS, TimeUnit.MILLISECONDS);
  }

  /**
   * Handles player disconnect event.
   * Clears the notified flag so they can be notified again next session.
   *
   * @param event the player disconnect event
   */
  private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    notifiedPlayers.remove(event.getPlayerRef().getUuid());
  }

  /**
   * Checks if the player should receive a notification and sends it.
   *
   * @param playerRef the player to check
   */
  private void checkAndNotify(PlayerRef playerRef) {
    UUID uuid = playerRef.getUuid();

    // Already notified this session
    if (notifiedPlayers.contains(uuid)) {
      return;
    }

    // Check permissions - admin permission required for update notifications
    if (!hasNotifyPermission(uuid)) {
      Logger.debug("[UpdateNotify] %s does not have notify permission", playerRef.getUsername());
      return;
    }

    // Check user preferences
    UpdateNotificationPreferences prefs = hyperFactions.getNotificationPreferences();
    if (prefs != null && !prefs.isEnabled(uuid)) {
      Logger.debug("[UpdateNotify] Notifications disabled for %s", playerRef.getUsername());
      return;
    }

    // Check if update checker is available
    UpdateChecker checker = hyperFactions.getUpdateChecker();
    if (checker == null) {
      return;
    }

    // Mark as notified before sending to prevent duplicates
    notifiedPlayers.add(uuid);

    // Send appropriate notification
    if (checker.hasUpdateAvailable()) {
      sendUpdateAvailableMessage(playerRef, checker);
    } else {
      sendUpToDateMessage(playerRef, checker);
    }
  }

  /**
   * Checks if the player has permission to receive update notifications.
   *
   * @param uuid the player's UUID
   * @return true if they should receive notifications
   */
  private boolean hasNotifyPermission(UUID uuid) {
    // Check admin permission or specific notify permission
    return PermissionManager.get().hasPermission(uuid, Permissions.ADMIN)
       || PermissionManager.get().hasPermission(uuid, "hyperfactions.updates.notify");
  }

  /**
   * Sends the "update available" notification message.
   *
   * @param playerRef the player to notify
   * @param checker   the update checker
   */
  private void sendUpdateAvailableMessage(PlayerRef playerRef, UpdateChecker checker) {
    UpdateChecker.UpdateInfo info = checker.getCachedUpdate();
    if (info == null) {
      return;
    }

    String currentVersion = checker.getCurrentVersion();
    String newVersion = info.version();
    String versionLabel = info.isPreRelease() ? "v" + newVersion + " (pre-release)" : "v" + newVersion;

    // [HyperFactions] A new version is available!
    playerRef.sendMessage(
      Message.raw("[HyperFactions] ").color(GOLD)
        .insert(Message.raw("A new version is available!").color(GOLD).bold(true))
    );

    // Current: v1.0.0 -> Latest: v1.1.0 (pre-release)
    playerRef.sendMessage(
      Message.raw("Current: ").color(GRAY)
        .insert(Message.raw("v" + currentVersion).color(WHITE))
        .insert(Message.raw(" -> ").color(GRAY))
        .insert(Message.raw("Latest: ").color(GRAY))
        .insert(Message.raw(versionLabel).color(GREEN))
    );

    // Run /f admin update to update the plugin.
    playerRef.sendMessage(
      Message.raw("Run ").color(GRAY)
        .insert(Message.raw("/f admin update").color(GREEN))
        .insert(Message.raw(" to update the plugin.").color(GRAY))
    );

    Logger.debug("[UpdateNotify] Sent update notification to %s", playerRef.getUsername());
  }

  /**
   * Sends the "up-to-date" notification message.
   *
   * @param playerRef the player to notify
   * @param checker   the update checker
   */
  private void sendUpToDateMessage(PlayerRef playerRef, UpdateChecker checker) {
    String currentVersion = checker.getCurrentVersion();

    // [HyperFactions] Plugin is up-to-date (v1.0.0)
    playerRef.sendMessage(
      Message.raw("[HyperFactions] ").color(GRAY)
        .insert(Message.raw("Plugin is up-to-date ").color(GRAY))
        .insert(Message.raw("(v" + currentVersion + ")").color(GREEN))
    );

    Logger.debug("[UpdateNotify] Sent up-to-date notification to %s", playerRef.getUsername());
  }
}
