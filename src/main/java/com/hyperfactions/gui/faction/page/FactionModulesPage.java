package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionModulesData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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
import java.util.List;

/**
 * Faction Modules page - displays available faction modules with status.
 * Currently shows placeholder cards for upcoming features.
 */
public class FactionModulesPage extends InteractiveCustomUIPage<FactionModulesData> {

  private static final String PAGE_ID = "modules";

  // Module definitions for the 2x2 grid (treasury state is dynamic)
  private static final List<ModuleInfo> MODULES = List.of(
      new ModuleInfo("treasury", MessageKeys.ModulesGui.TREASURY_NAME, MessageKeys.ModulesGui.TREASURY_DESC, "#fbbf24"),
      new ModuleInfo("raids", MessageKeys.ModulesGui.RAIDS_NAME, MessageKeys.ModulesGui.RAIDS_DESC, "#ef4444"),
      new ModuleInfo("levels", MessageKeys.ModulesGui.LEVELS_NAME, MessageKeys.ModulesGui.LEVELS_DESC, "#22c55e"),
      new ModuleInfo("war", MessageKeys.ModulesGui.WAR_NAME, MessageKeys.ModulesGui.WAR_DESC, "#a855f7")
  );

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final HyperFactions hyperFactions;

  private final Faction faction;

  /** Creates a new FactionModulesPage. */
  public FactionModulesPage(PlayerRef playerRef,
               FactionManager factionManager,
               GuiManager guiManager,
               HyperFactions hyperFactions,
               Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionModulesData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.hyperFactions = hyperFactions;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the modules template
    cmd.append(UIPaths.FACTION_MODULES);

    // Localize static labels
    cmd.set("#ModulesTitle.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.TITLE));
    cmd.set("#ModulesDescription.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.DESCRIPTION));
    cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.BACK_BTN));

    // Setup navigation bar
    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

    // Populate module cards
    for (int i = 0; i < MODULES.size(); i++) {
      ModuleInfo module = MODULES.get(i);
      String cardSelector = "#ModuleCard" + i;

      // Set module info
      cmd.set(cardSelector + " #ModuleName.Text", HFMessages.get(playerRef, module.nameKey));
      cmd.set(cardSelector + " #ModuleDesc.Text", HFMessages.get(playerRef, module.descKey));

      // Set color indicator
      cmd.set(cardSelector + " #ColorBar.Background.Color", module.color);

      // Treasury has dynamic state
      if ("treasury".equals(module.id)) {
        buildTreasuryCard(cmd, events, cardSelector);
      } else {
        // Other modules: coming soon
        cmd.set(cardSelector + " #StatusBadge.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.COMING_SOON));
        cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#888888");
      }
    }

    // Back button - return to settings
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                FactionModulesData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if ("Nav".equals(data.button) && data.navBar != null) {
      FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
      if (entry != null) {
        Faction currentFaction = factionManager.getFaction(faction.id());
        var page = entry.guiSupplier().create(player, ref, store, playerRef, currentFaction, guiManager);
        if (page != null) {
          player.getPageManager().openCustomPage(ref, store, page);
          return;
        }
      }
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Back" -> {
        guiManager.openFactionSettings(player, ref, store, playerRef, faction);
      }

      case "OpenModule" -> {
        if ("treasury".equals(data.moduleId) && hyperFactions.isTreasuryEnabled()) {
          Faction currentFaction = factionManager.getFaction(faction.id());
          if (currentFaction != null) {
            guiManager.openFactionTreasury(player, ref, store, playerRef, currentFaction);
            return;
          }
        }
        sendUpdate();
      }

      default -> sendUpdate();
    }
  }

  /**
   * Builds the treasury card with three possible states:
   * 1. Active (treasury enabled) - green badge, "View Treasury" button
   * 2. Disabled by config - gray badge, disabled message
   * 3. No economy plugin - amber badge, unavailable message
   */
  private void buildTreasuryCard(UICommandBuilder cmd, UIEventBuilder events, String cardSelector) {
    if (hyperFactions.isTreasuryEnabled()) {
      // State 1: Active
      cmd.set(cardSelector + " #StatusBadge.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.ACTIVE));
      cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#22c55e");
      cmd.set(cardSelector + " #ModuleBtn.Visible", true);
      cmd.set(cardSelector + " #ModuleBtn.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.VIEW_TREASURY));
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          cardSelector + " #ModuleBtn",
          EventData.of("Button", "OpenModule").append("ModuleId", "treasury"),
          false
      );
    } else {
      String reason = hyperFactions.getTreasuryDisabledReason();
      if (reason != null && reason.contains("economy plugin")) {
        // State 3: Config enabled but no economy plugin
        cmd.set(cardSelector + " #StatusBadge.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.UNAVAILABLE));
        cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#fbbf24");
        cmd.set(cardSelector + " #ModuleDesc.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.NO_ECONOMY));
      } else {
        // State 2: Disabled by server config
        cmd.set(cardSelector + " #StatusBadge.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.DISABLED));
        cmd.set(cardSelector + " #StatusBadge.Style.TextColor", "#888888");
        cmd.set(cardSelector + " #ModuleDesc.Text", HFMessages.get(playerRef, MessageKeys.ModulesGui.ECONOMY_NOT_AVAILABLE));
      }
    }
  }

  private record ModuleInfo(String id, String nameKey, String descKey, String color) {}
}
