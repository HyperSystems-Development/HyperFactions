package com.hyperfactions.gui.admin.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.economy.UpkeepProcessor;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminActionsData;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.AdminGuiKeys;
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

  private final HyperFactions plugin;

  /** Two-step confirmation state for global K/D reset. */
  private boolean confirmResetKD = false;

  /** Two-step confirmation state for manual upkeep trigger. */
  private boolean confirmUpkeep = false;

  /** Creates a new AdminActionsPage. */
  public AdminActionsPage(PlayerRef playerRef,
              PlayerStorage playerStorage,
              GuiManager guiManager,
              HyperFactions plugin) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminActionsData.CODEC);
    this.playerRef = playerRef;
    this.playerStorage = playerStorage;
    this.guiManager = guiManager;
    this.plugin = plugin;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.ADMIN_ACTIONS);

    // Setup admin nav bar (highlight "actions" tab)
    AdminNavBarHelper.setupBar(playerRef, "actions", cmd, events);

    // Localize page title and labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_TITLE_ACTIONS));
    cmd.set("#CombatStatsLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_COMBAT_STATS));
    cmd.set("#CombatDescLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_COMBAT_DESC));
    cmd.set("#EconomyLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_ECONOMY));
    cmd.set("#EconomyDescLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_ECONOMY_DESC));
    cmd.set("#BulkAdjustBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_BULK_ADJUST));
    cmd.set("#UpkeepLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_UPKEEP_COLLECTION));
    cmd.set("#UpkeepDescLabel.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_UPKEEP_DESC));

    buildContent(cmd, events);
  }

  private void buildContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Reset button text depends on confirmation state
    if (confirmResetKD) {
      cmd.set("#ResetAllKDBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.ACT_CONFIRM_RESET));
    } else {
      cmd.set("#ResetAllKDBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_RESET_KD));
    }

    // Bind the reset button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetAllKDBtn",
        EventData.of("Button", "ResetAllKD"), false);

    // Economy section visibility
    boolean economyEnabled = plugin.isTreasuryEnabled();
    cmd.set("#EconomySection.Visible", economyEnabled);

    if (economyEnabled) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#BulkAdjustBtn",
          EventData.of("Button", "BulkAdjust"), false);

      // Upkeep section visibility
      boolean upkeepEnabled = ConfigManager.get().isUpkeepEnabled();
      cmd.set("#UpkeepSection.Visible", upkeepEnabled);

      if (upkeepEnabled) {
        if (confirmUpkeep) {
          cmd.set("#TriggerUpkeepBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.ACT_CONFIRM_TRIGGER));
        } else {
          cmd.set("#TriggerUpkeepBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ACT_TRIGGER_UPKEEP));
        }
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TriggerUpkeepBtn",
            EventData.of("Button", "TriggerUpkeep"), false);
      }
    }
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
          confirmResetKD = true;
          UICommandBuilder cmd = new UICommandBuilder();
          UIEventBuilder events = new UIEventBuilder();
          cmd.set("#ResetAllKDBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.ACT_CONFIRM_RESET));
          events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetAllKDBtn",
              EventData.of("Button", "ResetAllKD"), false);
          sendUpdate(cmd, events, false);
        } else {
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
            player.sendMessage(MessageUtil.adminError(playerRef, AdminGuiKeys.AdminGui.ACT_KD_RESET_FAILED, e.getMessage()));
            ErrorHandler.report("[Admin] Global K/D reset failed", e);
          }
          guiManager.openAdminActions(player, ref, store, playerRef);
        }
      }

      case "BulkAdjust" -> guiManager.openAdminBulkEconomy(player, ref, store, playerRef);

      case "TriggerUpkeep" -> {
        if (!confirmUpkeep) {
          confirmUpkeep = true;
          UICommandBuilder cmd = new UICommandBuilder();
          UIEventBuilder events = new UIEventBuilder();
          cmd.set("#TriggerUpkeepBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.ACT_CONFIRM_TRIGGER));
          events.addEventBinding(CustomUIEventBindingType.Activating, "#TriggerUpkeepBtn",
              EventData.of("Button", "TriggerUpkeep"), false);
          sendUpdate(cmd, events, false);
        } else {
          confirmUpkeep = false;
          UpkeepProcessor processor = plugin.getUpkeepProcessor();
          if (processor == null) {
            player.sendMessage(MessageUtil.adminError(playerRef, AdminGuiKeys.AdminGui.ACT_UPKEEP_UNAVAILABLE));
          } else {
            try {
              processor.processUpkeep();
              player.sendMessage(MessageUtil.adminSuccess(playerRef, AdminGuiKeys.AdminGui.ACT_UPKEEP_TRIGGERED));
              Logger.info("[Admin] %s manually triggered upkeep collection via GUI",
                  playerRef.getUsername());
            } catch (Exception e) {
              player.sendMessage(MessageUtil.adminError(playerRef, AdminGuiKeys.AdminGui.ACT_UPKEEP_FAILED, e.getMessage()));
              ErrorHandler.report("[Admin] Manual upkeep trigger failed", e);
            }
          }
          guiManager.openAdminActions(player, ref, store, playerRef);
        }
      }

      default -> { }
    }
  }
}
