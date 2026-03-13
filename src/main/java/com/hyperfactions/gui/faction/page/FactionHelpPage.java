package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.GuiKeys;
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

    // Localize all static content
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.GETTING_STARTED_TITLE));
    cmd.set("#WhatTitle.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_TITLE));
    cmd.set("#WhatDesc1.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_1));
    cmd.set("#WhatDesc2.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_2));
    cmd.set("#WhatBullet1.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_BULLET_1));
    cmd.set("#WhatBullet2.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_BULLET_2));
    cmd.set("#WhatBullet3.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.WHAT_ARE_FACTIONS_BULLET_3));
    cmd.set("#JoinTitle.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.JOINING_TITLE));
    cmd.set("#JoinDesc.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.JOINING_DESC));
    cmd.set("#JoinBullet1.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.JOINING_BULLET_1));
    cmd.set("#JoinBullet2.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.JOINING_BULLET_2));
    cmd.set("#JoinBullet3.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.JOINING_BULLET_3));
    cmd.set("#CreateTitle.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CREATING_TITLE));
    cmd.set("#CreateDesc.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CREATING_DESC));
    cmd.set("#CreateBullet1.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CREATING_BULLET_1));
    cmd.set("#CreateBullet2.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CREATING_BULLET_2));
    cmd.set("#CmdTitle.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.COMMANDS_TITLE));
    cmd.set("#CmdF.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CMD_F));
    cmd.set("#CmdFList.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CMD_F_LIST));
    cmd.set("#CmdFJoin.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CMD_F_JOIN));
    cmd.set("#CmdFCreate.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CMD_F_CREATE));
    cmd.set("#CmdFHelp.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.CMD_F_HELP));
    cmd.set("#TipText.Text", HFMessages.get(playerRef, GuiKeys.HelpGui.TIP));
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
