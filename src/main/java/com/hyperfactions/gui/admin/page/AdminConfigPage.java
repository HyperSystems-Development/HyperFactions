package com.hyperfactions.gui.admin.page;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminConfigData;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Admin Config page - placeholder for configuration management.
 */
public class AdminConfigPage extends InteractiveCustomUIPage<AdminConfigData> {

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  /** Creates a new AdminConfigPage. */
  public AdminConfigPage(PlayerRef playerRef, GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminConfigData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    // Load the placeholder template first (nav bar elements must exist before setupBar)
    cmd.append(UIPaths.ADMIN_CONFIG);

    // Setup admin nav bar (must be after template load)
    AdminNavBarHelper.setupBar(playerRef, "config", cmd, events);

    // Localize page title and labels
    cmd.set("#Title.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_CONFIG));
    cmd.set("#ComingSoon.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CONFIG_HEADING));
    cmd.set("#ComingSoonSub.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_COMING_SOON));
    cmd.set("#Description.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CONFIG_DESC1));
    cmd.set("#Description2.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CONFIG_DESC2));
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminConfigData data) {
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

    // Handle other button events (placeholder for future implementation)
    if (data.button != null) {
      switch (data.button) {
        case "Back" -> guiManager.closePage(player, ref, store);
        default -> throw new IllegalStateException("Unexpected value");
      }
    }
  }
}
