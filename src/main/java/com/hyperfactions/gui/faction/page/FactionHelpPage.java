package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

/**
 * Help Page for faction members.
 * Uses the faction nav bar for navigation.
 */
public class FactionHelpPage extends InteractiveCustomUIPage<FactionPageData> {

  private static final String PAGE_ID = "help";

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  private final Faction faction;

  /** Creates a new FactionHelpPage. */
  public FactionHelpPage(PlayerRef playerRef, GuiManager guiManager, @Nullable Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the same help template (content is the same)
    cmd.append(UIPaths.NEWPLAYER_HELP);

    // Setup faction navigation bar
    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                FactionPageData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle faction navigation
    if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
      return;
    }

    // Default - just refresh
    sendUpdate();
  }
}
