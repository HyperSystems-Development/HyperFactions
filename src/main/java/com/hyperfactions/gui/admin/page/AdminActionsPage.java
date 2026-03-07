package com.hyperfactions.gui.admin.page;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminActionsData;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Set;
import java.util.UUID;

/**
 * Admin Actions page - provides server-wide quick actions.
 * Currently includes global K/D reset with two-step confirmation.
 */
public class AdminActionsPage extends InteractiveCustomUIPage<AdminActionsData> {

  private final PlayerRef playerRef;

  private final PlayerStorage playerStorage;

  private final GuiManager guiManager;

  /** Two-step confirmation state for global K/D reset. */
  private boolean confirmResetKD = false;

  /** Creates a new AdminActionsPage. */
  public AdminActionsPage(PlayerRef playerRef,
              PlayerStorage playerStorage,
              GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminActionsData.CODEC);
    this.playerRef = playerRef;
    this.playerStorage = playerStorage;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.ADMIN_ACTIONS);

    // Setup admin nav bar (highlight "actions" tab)
    AdminNavBarHelper.setupBar(playerRef, "actions", cmd, events);

    buildContent(cmd, events);
  }

  private void buildContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Reset button text depends on confirmation state
    if (confirmResetKD) {
      cmd.set("#ResetAllKDBtn.Text", "Confirm Reset?");
    }

    // Bind the reset button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetAllKDBtn",
        EventData.of("Button", "ResetAllKD"), false);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminActionsData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle admin nav bar navigation
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button == null) {
      return;
    }

    switch (data.button) {
      case "ResetAllKD" -> {
        if (!confirmResetKD) {
          // First click — enter confirmation state
          confirmResetKD = true;
          UICommandBuilder cmd = new UICommandBuilder();
          UIEventBuilder events = new UIEventBuilder();
          cmd.set("#ResetAllKDBtn.Text", "Confirm Reset?");
          events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetAllKDBtn",
              EventData.of("Button", "ResetAllKD"), false);
          sendUpdate(cmd, events, false);
        } else {
          // Second click — execute the reset
          confirmResetKD = false;
          try {
            Set<UUID> allUuids = playerStorage.getAllPlayerUuids().join();
            for (UUID uuid : allUuids) {
              playerStorage.updatePlayerData(uuid, pd -> {
                pd.setKills(0);
                pd.setDeaths(0);
              });
            }
            player.sendMessage(MessageUtil.adminSuccess(
                "Reset K/D for " + allUuids.size() + " players."));
            Logger.info("[Admin] %s reset K/D stats for all %d players",
                playerRef.getUsername(), allUuids.size());
          } catch (Exception e) {
            player.sendMessage(MessageUtil.adminError("Failed to reset K/D: " + e.getMessage()));
            ErrorHandler.report("[Admin] Global K/D reset failed", e);
          }

          // Reopen page to reset state
          guiManager.openAdminActions(player, ref, store, playerRef);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
