package com.hyperfactions.gui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.admin.page.*;
import com.hyperfactions.gui.faction.page.*;
import com.hyperfactions.gui.help.HelpCategory;
import com.hyperfactions.gui.help.page.HelpMainPage;
import com.hyperfactions.gui.newplayer.page.*;
import com.hyperfactions.gui.shared.page.*;
import com.hyperfactions.gui.test.ButtonTestPage;
import com.hyperfactions.manager.*;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Page opener methods extracted from GuiManager for newplayer pages.
 */
class NewPlayerPageOpener {

  private final GuiManager guiManager;

  NewPlayerPageOpener(GuiManager guiManager) {
    this.guiManager = guiManager;
  }

  /**
   * Opens the New Player Browse page (default landing page for players without a faction).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    openNewPlayerBrowse(player, ref, store, playerRef, 0, "power");
  }

  /**
   * Opens the New Player Browse page with custom page and sort state.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param page      The page number (0-indexed)
   * @param sortBy    The sort mode (power, members, name)
   */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  int page, String sortBy) {
    openNewPlayerBrowse(player, ref, store, playerRef, page, sortBy, "");
  }

  /**
   * Opens the New Player Browse page with custom page, sort, and search state.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param page        The page number (0-indexed)
   * @param sortBy      The sort mode (power, members, name)
   * @param searchQuery The search query to filter factions
   */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  int page, String sortBy, String searchQuery) {
    Logger.debug("[GUI] Opening NewPlayerBrowsePage for %s (page=%d, sort=%s, search=%s)",
        playerRef.getUsername(), page, sortBy, searchQuery);
    try {
      PageManager pageManager = player.getPageManager();
      NewPlayerBrowsePage browsePage = new NewPlayerBrowsePage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getInviteManager().get(),
        guiManager,
        page,
        sortBy,
        searchQuery
      );
      pageManager.openCustomPage(ref, store, browsePage);
      Logger.debug("[GUI] NewPlayerBrowsePage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open NewPlayerBrowsePage", e);
    }
  }

  /**
   * Opens the Create Faction wizard (Step 1).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openCreateFactionWizard(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening CreateFactionPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      CreateFactionPage page = new CreateFactionPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] CreateFactionPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open CreateFactionPage", e);
    }
  }

  /**
   * Opens the Invites page for new players.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openInvitesPage(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening InvitesPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      InvitesPage page = new InvitesPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getInviteManager().get(),
        guiManager.getJoinRequestManager(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] InvitesPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open InvitesPage", e);
    }
  }

  /**
   * Opens the read-only Map page for new players.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openNewPlayerMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening NewPlayerMapPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      NewPlayerMapPage page = new NewPlayerMapPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getClaimManager().get(),
        guiManager.getRelationManager().get(),
        guiManager.getZoneManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] NewPlayerMapPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open NewPlayerMapPage", e);
    }
  }

  /**
   * Opens the Help page with the default category (WELCOME).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openHelpPage(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    openHelp(player, ref, store, playerRef, HelpCategory.WELCOME);
  }

  /**
   * Opens the Help page with a specific category selected.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param category  The initial category to display
   */
  public void openHelp(Player player, Ref<EntityStore> ref,
            Store<EntityStore> store, PlayerRef playerRef,
            HelpCategory category) {
    Logger.debug("[GUI] Opening HelpMainPage for %s (category: %s)",
        playerRef.getUsername(), category.displayName());
    try {
      PageManager pageManager = player.getPageManager();
      HelpMainPage page = new HelpMainPage(playerRef, guiManager, guiManager.getFactionManager().get(), category);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] HelpMainPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open HelpMainPage", e);
    }
  }

}
