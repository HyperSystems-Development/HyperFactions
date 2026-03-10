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
 * Page opener methods extracted from GuiManager for faction pages.
 */
class FactionPageOpener {

  private final GuiManager guiManager;

  FactionPageOpener(GuiManager guiManager) {
    this.guiManager = guiManager;
  }

  /**
   * Opens the Main Menu page - central navigation hub.
   * Displayed when player types /f alone.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openMainMenu(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening MainMenuPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      MainMenuPage page = new MainMenuPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] MainMenuPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open MainMenuPage", e);
    }
  }

  /**
   * Opens the Faction Main page (dashboard) for a player.
   * If the player has a faction, opens the enhanced FactionDashboardPage.
   * Otherwise, opens the standard FactionMainPage.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openFactionMain(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    UUID uuid = playerRef.getUuid();
    Faction faction = guiManager.getFactionManager().get().getPlayerFaction(uuid);

    if (faction != null) {
      // Player has a faction - open enhanced dashboard
      Logger.debug("[GUI] Opening FactionDashboardPage for %s (faction: %s)",
          playerRef.getUsername(), faction.name());
      try {
        PageManager pageManager = player.getPageManager();
        FactionDashboardPage page = new FactionDashboardPage(
          playerRef,
          guiManager.getFactionManager().get(),
          guiManager.getClaimManager().get(),
          guiManager.getPowerManager().get(),
          guiManager.getTeleportManager().get(),
          guiManager,
          guiManager.getPlugin().get(),
          faction
        );
        pageManager.openCustomPage(ref, store, page);
        Logger.debug("[GUI] FactionDashboardPage opened successfully");
      } catch (Exception e) {
        ErrorHandler.report("[GUI] Failed to open FactionDashboardPage", e);
      }
    } else {
      // Player has no faction - redirect to new player browse page
      Logger.debug("[GUI] Player %s has no faction, redirecting to NewPlayerBrowsePage",
          playerRef.getUsername());
      guiManager.openNewPlayerBrowse(player, ref, store, playerRef);
    }
  }

  /**
   * Opens the Player Settings page.
   */
  public void openPlayerSettings(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening PlayerSettingsPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      PlayerSettingsPage page = new PlayerSettingsPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPlugin().get().getPlayerStorage(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] PlayerSettingsPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open PlayerSettingsPage", e);
    }
  }

  /**
   * Opens the Faction Members page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show members for
   */
  public void openFactionMembers(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    Logger.debug("[GUI] Opening FactionMembersPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionMembersPage page = new FactionMembersPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionMembersPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionMembersPage", e);
    }
  }

  /**
   * Opens the Chunk Map page (interactive territory view).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openChunkMap(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening ChunkMapPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      ChunkMapPage page = new ChunkMapPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getClaimManager().get(),
        guiManager.getRelationManager().get(),
        guiManager.getZoneManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] ChunkMapPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open ChunkMapPage", e);
    }
  }

  /**
   * Opens the Faction Relations page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show relations for
   */
  public void openFactionRelations(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    openFactionRelations(player, ref, store, playerRef, faction, null);
  }

  /**
   * Opens the Faction Relations page with an initial tab.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show relations for
   * @param tab       Initial tab to show ("relations", "pending", or null for default)
   */
  public void openFactionRelations(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, String tab) {
    Logger.debug("[GUI] Opening FactionRelationsPage for %s (tab: %s)", playerRef.getUsername(), tab);
    try {
      PageManager pageManager = player.getPageManager();
      FactionRelationsPage page = new FactionRelationsPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getRelationManager().get(),
        guiManager,
        faction,
        tab
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionRelationsPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionRelationsPage", e);
    }
  }

  /**
   * Opens the Create Faction page (multi-step form).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openCreateFaction(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    // Redirect to the new wizard-style create faction flow
    guiManager.openCreateFactionWizard(player, ref, store, playerRef);
  }

  /**
   * Opens the Faction Browser page (list all factions).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openFactionBrowser(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening FactionBrowserPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionBrowserPage page = new FactionBrowserPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionBrowserPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionBrowserPage", e);
    }
  }

  /**
   * Opens the Faction Leaderboard page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openLeaderboard(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening FactionLeaderboardPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      EconomyManager econ = guiManager.getPlugin().get().isTreasuryEnabled() ? guiManager.getPlugin().get().getEconomyManager() : null;
      FactionLeaderboardPage page = new FactionLeaderboardPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        econ,
        guiManager.getPlugin().get().getFactionKDCache(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionLeaderboardPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionLeaderboardPage", e);
    }
  }

  /**
   * Opens the Faction Invites page.
   * Shows outgoing invites and incoming join requests.
   * Requires officer or leader role.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to manage invites for
   */
  public void openFactionInvites(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    Logger.debug("[GUI] Opening FactionInvitesPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionInvitesPage page = new FactionInvitesPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getInviteManager().get(),
        guiManager.getJoinRequestManager(),
        guiManager,
        guiManager.getPlugin().get(),
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionInvitesPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionInvitesPage", e);
    }
  }

  /**
   * Opens the Faction Settings page with the default tab (general).
   * Requires officer or leader role.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to edit settings for
   */
  public void openFactionSettings(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    Logger.debug("[GUI] Opening FactionSettingsPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionSettingsPage page = new FactionSettingsPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getClaimManager().get(),
        guiManager,
        guiManager.getPlugin().get(),
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionSettingsPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionSettingsPage", e);
    }
  }

  /**
   * Opens the Set Relation modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to set relations for
   */
  public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    openSetRelationModal(player, ref, store, playerRef, faction, "", 0);
  }

  /**
   * Opens the Set Relation modal with search state.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param faction     The faction to set relations for
   * @param searchQuery The current search query
   * @param page        The current page
   */
  public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, String searchQuery, int page) {
    Logger.debug("[GUI] Opening SetRelationModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      SetRelationModalPage modalPage = new SetRelationModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getRelationManager().get(),
        guiManager,
        faction,
        searchQuery,
        page
      );
      pageManager.openCustomPage(ref, store, modalPage);
      Logger.debug("[GUI] SetRelationModalPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open SetRelationModalPage", e);
    }
  }

  /**
   * Opens the Tag modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to edit tag for
   */
  public void openTagModal(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef,
              Faction faction) {
    Logger.debug("[GUI] Opening TagModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      TagModalPage page = new TagModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        guiManager.getPlugin().get().getWorldMapService()
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] TagModalPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TagModalPage", e);
    }
  }

  /**
   * Opens the Description modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to edit description for
   */
  public void openDescriptionModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    Logger.debug("[GUI] Opening DescriptionModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      DescriptionModalPage page = new DescriptionModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] DescriptionModalPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open DescriptionModalPage", e);
    }
  }

  /**
   * Opens the Rename Faction modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to rename
   */
  public void openRenameModal(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    Logger.debug("[GUI] Opening RenameModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      RenameModalPage page = new RenameModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        guiManager.getPlugin().get().getWorldMapService()
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] RenameModalPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open RenameModalPage", e);
    }
  }

  /**
   * Opens the Disband Confirmation modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to disband
   */
  public void openDisbandConfirm(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    Logger.debug("[GUI] Opening DisbandConfirmPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      DisbandConfirmPage page = new DisbandConfirmPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] DisbandConfirmPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open DisbandConfirmPage", e);
    }
  }

  /**
   * Opens the Leadership Transfer Confirmation modal.
   * Shows a warning and requires confirmation to transfer leadership.
   *
   * @param player     The Player entity
   * @param ref        The entity reference
   * @param store      The entity store
   * @param playerRef  The PlayerRef component
   * @param faction    The faction
   * @param targetUuid The UUID of the player receiving leadership
   * @param targetName The name of the player receiving leadership
   */
  public void openTransferConfirm(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, UUID targetUuid, String targetName) {
    Logger.debug("[GUI] Opening TransferConfirmPage for %s -> %s", playerRef.getUsername(), targetName);
    try {
      PageManager pageManager = player.getPageManager();
      TransferConfirmPage page = new TransferConfirmPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        targetUuid,
        targetName
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] TransferConfirmPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TransferConfirmPage", e);
    }
  }

  /**
   * Opens the Faction Dashboard page.
   * Shows stat cards, quick actions, and activity feed for faction members.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show dashboard for
   */
  public void openFactionDashboard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    Logger.debug("[GUI] Opening FactionDashboardPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionDashboardPage page = new FactionDashboardPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getClaimManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getTeleportManager().get(),
        guiManager,
        guiManager.getPlugin().get(),
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionDashboardPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionDashboardPage", e);
    }
  }

  /**
   * Opens the Leave Faction Confirmation modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to leave
   */
  public void openLeaveConfirm(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    Logger.debug("[GUI] Opening LeaveConfirmPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      LeaveConfirmPage page = new LeaveConfirmPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] LeaveConfirmPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open LeaveConfirmPage", e);
    }
  }

  /**
   * Opens the Leader Leave Confirmation modal.
   * Shows succession information when a leader wants to leave.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction the leader is leaving
   */
  public void openLeaderLeaveConfirm(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    Faction faction) {
    Logger.debug("[GUI] Opening LeaderLeaveConfirmPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      LeaderLeaveConfirmPage page = new LeaderLeaveConfirmPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] LeaderLeaveConfirmPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open LeaderLeaveConfirmPage", e);
    }
  }

  /**
   * Opens the Faction Chat page (chat history with send-from-GUI).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show chat for
   */
  public void openFactionChat(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    Logger.debug("[GUI] Opening FactionChatPage for %s", playerRef.getUsername());
    try {
      if (guiManager.getChatManagerSupplier() == null || guiManager.getChatHistoryManagerSupplier() == null) {
        Logger.warn("[GUI] Chat managers not initialized, cannot open chat page");
        return;
      }
      PageManager pageManager = player.getPageManager();
      FactionChatPage page = new FactionChatPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getChatManagerSupplier().get(),
        guiManager.getChatHistoryManagerSupplier().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionChatPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionChatPage", e);
    }
  }

  /**
   * Opens the Faction Modules page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show modules for
   */
  public void openFactionModules(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    Logger.debug("[GUI] Opening FactionModulesPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      FactionModulesPage page = new FactionModulesPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        guiManager.getPlugin().get(),
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionModulesPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionModulesPage", e);
    }
  }

  /**
   * Opens the Faction Treasury page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to show treasury for
   */
  public void openFactionTreasury(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    Logger.debug("[GUI] Opening TreasuryPage for %s", playerRef.getUsername());
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
        return;
      }
      PageManager pageManager = player.getPageManager();
      TreasuryPage page = new TreasuryPage(
        playerRef,
        guiManager.getFactionManager().get(),
        econ,
        guiManager,
        guiManager.getPlugin().get(),
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] TreasuryPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TreasuryPage", e);
    }
  }

  /**
   * Opens the Treasury Deposit/Withdraw modal.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction
   * @param mode      "deposit" or "withdraw"
   */
  public void openTreasuryDepositModal(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     Faction faction, String mode) {
    Logger.debug("[GUI] Opening TreasuryDepositModal (%s) for %s", mode, playerRef.getUsername());
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
        return;
      }
      var page = new TreasuryDepositModalPage(playerRef, guiManager.getFactionManager().get(), econ,
          guiManager, guiManager.getPlugin().get(), faction, mode);
      player.getPageManager().openCustomPage(ref, store, page);
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TreasuryDepositModal", e);
    }
  }

  /**
   * Opens the Treasury Transfer Search modal.
   */
  public void openTreasuryTransferSearch(Player player, Ref<EntityStore> ref,
                      Store<EntityStore> store, PlayerRef playerRef,
                      Faction faction) {
    Logger.debug("[GUI] Opening TreasuryTransferSearch for %s", playerRef.getUsername());
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
        return;
      }
      var page = new TreasuryTransferSearchPage(playerRef, guiManager.getFactionManager().get(), econ,
          guiManager, guiManager.getPlugin().get(), faction);
      player.getPageManager().openCustomPage(ref, store, page);
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TreasuryTransferSearch", e);
    }
  }

  /**
   * Opens the Treasury Transfer Confirmation page.
   */
  public void openTreasuryTransferConfirm(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       Faction faction, String targetId,
                       String targetName, String targetType) {
    Logger.debug("[GUI] Opening TreasuryTransferConfirm for %s -> %s", playerRef.getUsername(), targetName);
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
        return;
      }
      var page = new TreasuryTransferConfirmPage(playerRef, guiManager.getFactionManager().get(), econ,
          guiManager, guiManager.getPlugin().get(), faction, targetId, targetName, targetType);
      player.getPageManager().openCustomPage(ref, store, page);
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TreasuryTransferConfirm", e);
    }
  }

  /**
   * Opens the Treasury Settings sub-page.
   */
  public void openTreasurySettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   Faction faction) {
    Logger.debug("[GUI] Opening TreasurySettings for %s", playerRef.getUsername());
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
        return;
      }
      var page = new TreasurySettingsPage(playerRef, guiManager.getFactionManager().get(), econ, guiManager, faction);
      player.getPageManager().openCustomPage(ref, store, page);
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open TreasurySettings", e);
    }
  }

  /**
   * Opens the Logs Viewer page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param faction   The faction to view logs for
   */
  public void openLogsViewer(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               Faction faction) {
    Logger.debug("[GUI] Opening LogsViewerPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      LogsViewerPage page = new LogsViewerPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] LogsViewerPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open LogsViewerPage", e);
    }
  }

  /**
   * Opens the Faction Info page.
   * Displays detailed information about a specific faction.
   *
   * @param player        The Player entity
   * @param ref           The entity reference
   * @param store         The entity store
   * @param playerRef     The PlayerRef component
   * @param targetFaction The faction to view info for
   */
  public void openFactionInfo(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction targetFaction) {
    openFactionInfo(player, ref, store, playerRef, targetFaction, null);
  }

  /**
   * Opens the Faction Info page with source tracking.
   *
   * @param player        The player viewing the page
   * @param ref           The entity reference
   * @param store         The entity store
   * @param playerRef     The PlayerRef component
   * @param targetFaction The faction to view info for
   * @param sourcePage    The source page to return to ("browser", "newplayer_browser", "admin_factions", or null)
   */
  public void openFactionInfo(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction targetFaction, String sourcePage) {
    Logger.debug("[GUI] Opening FactionInfoPage for %s (viewing %s, source: %s)",
        playerRef.getUsername(), targetFaction.name(), sourcePage);
    try {
      PageManager pageManager = player.getPageManager();
      FactionInfoPage page = new FactionInfoPage(
        playerRef,
        targetFaction,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getRelationManager().get(),
        guiManager.getPlugin().get().getEconomyManager(),
        guiManager,
        sourcePage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionInfoPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionInfoPage", e);
    }
  }

  /**
   * Opens the Faction Info page from a PlayerInfoPage context.
   * When back is clicked, returns to the PlayerInfoPage for the given player.
   *
   * @param player              The Player entity
   * @param ref                 The entity reference
   * @param store               The entity store
   * @param playerRef           The PlayerRef component
   * @param targetFaction       The faction to view
   * @param sourcePlayerUuid    The UUID of the player being viewed in PlayerInfoPage
   * @param sourcePlayerName    The name of the player being viewed
   * @param playerInfoSourcePage The sourcePage that PlayerInfoPage was opened with
   */
  public void openFactionInfoFromPlayerInfo(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       Faction targetFaction,
                       UUID sourcePlayerUuid, String sourcePlayerName,
                       String playerInfoSourcePage) {
    Logger.debug("[GUI] Opening FactionInfoPage for %s (viewing %s, from player_info: %s)",
        playerRef.getUsername(), targetFaction.name(), sourcePlayerName);
    try {
      PageManager pageManager = player.getPageManager();
      FactionInfoPage page = new FactionInfoPage(
        playerRef,
        targetFaction,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getRelationManager().get(),
        guiManager.getPlugin().get().getEconomyManager(),
        guiManager,
        "player_info",
        sourcePlayerUuid,
        sourcePlayerName,
        playerInfoSourcePage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] FactionInfoPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open FactionInfoPage", e);
    }
  }

  /**
   * Opens the Player Info page.
   *
   * @param player          The Player entity
   * @param ref             The entity reference
   * @param store           The entity store
   * @param playerRef       The PlayerRef component
   * @param targetUuid      The UUID of the player to view
   * @param targetName      The name of the player to view
   */
  public void openPlayerInfo(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               UUID targetUuid, String targetName) {
    openPlayerInfo(player, ref, store, playerRef, targetUuid, targetName, null);
  }

  /**
   * Opens the Player Info page with source page tracking.
   *
   * @param player     The Player entity
   * @param ref        The entity reference
   * @param store      The entity store
   * @param playerRef  The PlayerRef component
   * @param targetUuid The UUID of the player to view
   * @param targetName The name of the player to view
   * @param sourcePage The page to return to ("members", "browser", or null)
   */
  public void openPlayerInfo(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               UUID targetUuid, String targetName, String sourcePage) {
    Logger.debug("[GUI] Opening PlayerInfoPage for %s (viewing %s, source: %s)",
        playerRef.getUsername(), targetName, sourcePage);
    try {
      PageManager pageManager = player.getPageManager();
      PlayerInfoPage page = new PlayerInfoPage(
        playerRef,
        targetUuid,
        targetName,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getPlugin().get().getPlayerStorage(),
        guiManager,
        sourcePage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] PlayerInfoPage opened successfully");
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open PlayerInfoPage", e);
    }
  }

  /**
   * Opens the button style test page.
   * Temporary — DELETE after testing is complete.
   */
  public void openButtonTestPage(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    Logger.info("[GUI] Opening ButtonTestPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      ButtonTestPage page = new ButtonTestPage(playerRef);
      pageManager.openCustomPage(ref, store, page);
    } catch (Exception e) {
      ErrorHandler.report("[GUI] Failed to open ButtonTestPage", e);
    }
  }

}
