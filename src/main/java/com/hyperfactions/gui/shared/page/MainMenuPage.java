package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.shared.data.MainMenuData;
import com.hyperfactions.integration.PermissionManager;
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
import java.util.UUID;

/**
 * Main Menu page - central navigation hub for HyperFactions.
 * Displayed when player types /f alone.
 */
public class MainMenuPage extends InteractiveCustomUIPage<MainMenuData> {

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  /** Creates a new MainMenuPage. */
  public MainMenuPage(PlayerRef playerRef,
            FactionManager factionManager,
            GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, MainMenuData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    UUID uuid = playerRef.getUuid();
    Faction faction = factionManager.getPlayerFaction(uuid);
    boolean hasAdmin = PermissionManager.get().hasPermission(uuid, "hyperfactions.admin");

    // Load the main menu template
    cmd.append(UIPaths.MAIN_MENU);

    // Set title
    cmd.set("#MenuTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.TITLE));

    // Section: My Faction
    if (faction != null) {
      cmd.append("#MyFactionSection", UIPaths.MENU_SECTION);
      cmd.set("#MyFactionSection #SectionTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.SECTION_MY_FACTION));
      cmd.append("#MyFactionSection #SectionContent", UIPaths.MAIN_MENU_FACTION);
      cmd.set("#MyFactionSection #FactionNameLabel.Text", faction.name());

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#MyFactionSection #ViewFactionBtn",
          EventData.of("Button", "MyFaction"),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#MyFactionSection #MembersBtn",
          EventData.of("Button", "Members"),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#MyFactionSection #RelationsBtn",
          EventData.of("Button", "Relations"),
          false
      );
    } else {
      cmd.append("#MyFactionSection", UIPaths.MENU_SECTION);
      cmd.set("#MyFactionSection #SectionTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.SECTION_GET_STARTED));
      cmd.append("#MyFactionSection #SectionContent", UIPaths.MAIN_MENU_NO_FACTION);

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#MyFactionSection #CreateFactionBtn",
          EventData.of("Button", "CreateFaction"),
          false
      );
    }

    // Section: Territory
    cmd.append("#TerritorySection", UIPaths.MENU_SECTION);
    cmd.set("#TerritorySection #SectionTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.SECTION_TERRITORY));
    cmd.append("#TerritorySection #SectionContent", UIPaths.MAIN_MENU_TERRITORY);

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TerritorySection #MapBtn",
        EventData.of("Button", "Map"),
        false
    );

    if (faction != null) {
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#TerritorySection #ClaimBtn",
          EventData.of("Button", "Claim"),
          false
      );
    }

    // Section: Browse
    cmd.append("#BrowseSection", UIPaths.MENU_SECTION);
    cmd.set("#BrowseSection #SectionTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.SECTION_BROWSE));
    cmd.append("#BrowseSection #SectionContent", UIPaths.MAIN_MENU_BROWSE);

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BrowseSection #BrowseFactionsBtn",
        EventData.of("Button", "BrowseFactions"),
        false
    );

    // Section: Admin (if permission)
    if (hasAdmin) {
      cmd.append("#AdminSection", UIPaths.MENU_SECTION);
      cmd.set("#AdminSection #SectionTitle.Text", HFMessages.get(playerRef, MessageKeys.MainMenu.SECTION_ADMIN));
      cmd.append("#AdminSection #SectionContent", UIPaths.MAIN_MENU_ADMIN);

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#AdminSection #AdminPanelBtn",
          EventData.of("Button", "AdminPanel"),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#AdminSection #ZonesBtn",
          EventData.of("Button", "AdminZones"),
          false
      );
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                MainMenuData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();
    Faction faction = factionManager.getPlayerFaction(uuid);

    switch (data.button) {
      case "MyFaction" -> {
        if (faction != null) {
          guiManager.openFactionMain(player, ref, store, playerRef);
        }
      }

      case "Members" -> {
        if (faction != null) {
          guiManager.openFactionMembers(player, ref, store, playerRef, faction);
        }
      }

      case "Relations" -> {
        if (faction != null) {
          guiManager.openFactionRelations(player, ref, store, playerRef, faction);
        }
      }

      case "CreateFaction" -> guiManager.openCreateFaction(player, ref, store, playerRef);

      case "Map" -> guiManager.openChunkMap(player, ref, store, playerRef);

      case "Claim" -> {
        if (faction != null) {
          guiManager.closePage(player, ref, store);
          player.sendMessage(
            com.hypixel.hytale.server.core.Message.raw(
                HFMessages.get(playerRef, MessageKeys.MainMenu.CLAIM_HINT)).color("#AAAAAA")
          );
        }
      }

      case "BrowseFactions" -> guiManager.openFactionBrowser(player, ref, store, playerRef);

      case "AdminPanel" -> {
        if (PermissionManager.get().hasPermission(uuid, "hyperfactions.admin")) {
          guiManager.openAdminMain(player, ref, store, playerRef);
        }
      }

      case "AdminZones" -> {
        if (PermissionManager.get().hasPermission(uuid, "hyperfactions.admin.zones")) {
          guiManager.openAdminZone(player, ref, store, playerRef);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
