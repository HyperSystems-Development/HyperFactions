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
 * Page opener methods extracted from GuiManager for admin pages.
 */
class AdminPageOpener {

  private final GuiManager guiManager;

  AdminPageOpener(GuiManager guiManager) {
    this.guiManager = guiManager;
  }

  /**
   * Opens the Tag modal in admin mode (bypasses permission checks).
   */
  public void openAdminTagModal(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    Logger.debug("[GUI] Opening admin TagModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      TagModalPage page = new TagModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        guiManager.getPlugin().get().getWorldMapService(),
        true  // adminMode
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] Admin TagModalPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open admin TagModalPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Description modal in admin mode (bypasses permission checks).
   */
  public void openAdminDescriptionModal(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     Faction faction) {
    Logger.debug("[GUI] Opening admin DescriptionModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      DescriptionModalPage page = new DescriptionModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        true  // adminMode
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] Admin DescriptionModalPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open admin DescriptionModalPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Rename Faction modal in admin mode (bypasses permission checks).
   */
  public void openAdminRenameModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    Logger.debug("[GUI] Opening admin RenameModalPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      RenameModalPage page = new RenameModalPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        faction,
        guiManager.getPlugin().get().getWorldMapService(),
        true  // adminMode
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] Admin RenameModalPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open admin RenameModalPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Dashboard page.
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminMain(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    openAdminDashboard(player, ref, store, playerRef);
  }

  /**
   * Opens the Admin Dashboard page (server-wide stats overview).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminDashboard(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminDashboardPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminDashboardPage page = new AdminDashboardPage(
        playerRef,
        guiManager.getPlugin().get(),
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getZoneManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminDashboardPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminDashboardPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Actions page (server-wide quick actions).
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminActions(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminActionsPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminActionsPage page = new AdminActionsPage(
        playerRef,
        guiManager.getPlugin().get().getPlayerStorage(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminActionsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminActionsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Factions page (faction management with expanding rows).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminFactions(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminFactionsPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminFactionsPage page = new AdminFactionsPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminFactionsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminFactionsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Players page - server-wide player list.
   * Browse all known players with search, sort, and pagination.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminPlayers(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminPlayersPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminPlayersPage page = new AdminPlayersPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getPlugin().get().getPlayerStorage(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminPlayersPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminPlayersPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Disband Confirmation modal.
   * Shows a warning and requires explicit confirmation to disband a faction.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param factionId   The UUID of the faction to disband
   * @param factionName The name of the faction to disband
   */
  public void openAdminDisbandConfirm(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId, String factionName) {
    Logger.debug("[GUI] Opening AdminDisbandConfirmPage for faction %s", factionName);
    try {
      PageManager pageManager = player.getPageManager();
      AdminDisbandConfirmPage page = new AdminDisbandConfirmPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager,
        factionId,
        factionName
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminDisbandConfirmPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminDisbandConfirmPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Faction Info page.
   * Displays detailed info about a faction with admin navigation context.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param factionId The UUID of the faction to view
   */
  public void openAdminFactionInfo(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID factionId) {
    Logger.debug("[GUI] Opening AdminFactionInfoPage for %s (faction: %s)", playerRef.getUsername(), factionId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminFactionInfoPage page = new AdminFactionInfoPage(
        playerRef,
        factionId,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager.getRelationManager().get(),
        guiManager.getPlugin().get().getEconomyManager(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminFactionInfoPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminFactionInfoPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Economy overview page.
   * Shows server economy stats and scrollable faction treasury list.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminEconomy(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminEconomyPage for %s", playerRef.getUsername());
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Economy system is not enabled."));
        return;
      }
      PageManager pageManager = player.getPageManager();
      AdminEconomyPage page = new AdminEconomyPage(
        playerRef,
        guiManager.getFactionManager().get(),
        econ,
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminEconomyPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminEconomyPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Economy Adjust modal for a specific faction.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param factionId The UUID of the faction to adjust
   */
  public void openAdminEconomyAdjust(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID factionId) {
    Logger.debug("[GUI] Opening AdminEconomyAdjustPage for %s (faction: %s)", playerRef.getUsername(), factionId);
    try {
      EconomyManager econ = guiManager.getPlugin().get().getEconomyManager();
      if (econ == null) {
        player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Economy system is not enabled."));
        return;
      }
      PageManager pageManager = player.getPageManager();
      AdminEconomyAdjustPage page = new AdminEconomyAdjustPage(
        playerRef,
        guiManager.getFactionManager().get(),
        econ,
        guiManager,
        factionId
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminEconomyAdjustPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminEconomyAdjustPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Faction Members page.
   * Read-only member list with admin navigation context.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param factionId The UUID of the faction to view members for
   */
  public void openAdminFactionMembers(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId) {
    Logger.debug("[GUI] Opening AdminFactionMembersPage for %s (faction: %s)", playerRef.getUsername(), factionId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminFactionMembersPage page = new AdminFactionMembersPage(
        playerRef,
        factionId,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminFactionMembersPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminFactionMembersPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Player Info page with power controls.
   *
   * @param player            The Player entity
   * @param ref               The entity reference
   * @param store             The entity store
   * @param playerRef         The PlayerRef component
   * @param targetPlayerUuid  The UUID of the player to view
   * @param targetPlayerName  The username of the player to view
   * @param factionId         The faction ID for context
   * @param origin            Where the user navigated from (for Back button)
   */
  public void openAdminPlayerInfo(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID targetPlayerUuid, String targetPlayerName,
                  UUID factionId,
                  AdminPlayerInfoPage.Origin origin) {
    Logger.debug("[GUI] Opening AdminPlayerInfoPage for %s (target: %s)", playerRef.getUsername(), targetPlayerName);
    try {
      PageManager pageManager = player.getPageManager();
      AdminPlayerInfoPage page = new AdminPlayerInfoPage(
        playerRef,
        targetPlayerUuid,
        targetPlayerName,
        factionId,
        origin,
        guiManager.getFactionManager().get(),
        guiManager.getPowerManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminPlayerInfoPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminPlayerInfoPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Faction Relations page.
   * View and force-set relations with admin navigation context.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param factionId The UUID of the faction to view relations for
   */
  public void openAdminFactionRelations(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     UUID factionId) {
    Logger.debug("[GUI] Opening AdminFactionRelationsPage for %s (faction: %s)", playerRef.getUsername(), factionId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminFactionRelationsPage page = new AdminFactionRelationsPage(
        playerRef,
        factionId,
        guiManager.getFactionManager().get(),
        guiManager.getRelationManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminFactionRelationsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminFactionRelationsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Faction Settings page.
   * Allows admins to edit any faction's territory permissions.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param factionId The UUID of the faction to edit settings for
   */
  public void openAdminFactionSettings(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId) {
    Logger.debug("[GUI] Opening AdminFactionSettingsPage for %s (faction: %s)", playerRef.getUsername(), factionId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminFactionSettingsPage page = new AdminFactionSettingsPage(
        playerRef,
        factionId,
        guiManager.getFactionManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminFactionSettingsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminFactionSettingsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Unclaim All Confirmation modal.
   * Shows warning and requires confirmation to remove all claims from a faction.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param factionId   The UUID of the faction to unclaim from
   * @param factionName The name of the faction
   * @param claimCount  The number of claims to remove
   */
  public void openAdminUnclaimAllConfirm(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     UUID factionId, String factionName, int claimCount) {
    Logger.debug("[GUI] Opening AdminUnclaimAllConfirmPage for faction %s (%d claims)",
        factionName, claimCount);
    try {
      PageManager pageManager = player.getPageManager();
      AdminUnclaimAllConfirmPage page = new AdminUnclaimAllConfirmPage(
        playerRef,
        guiManager.getClaimManager().get(),
        guiManager,
        factionId,
        factionName,
        claimCount
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminUnclaimAllConfirmPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminUnclaimAllConfirmPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone page.
   * Requires hyperfactions.admin.zones permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminZone(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    openAdminZone(player, ref, store, playerRef, "all", 0);
  }

  /** Opens the admin zone page. */
  public void openAdminZone(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               String tab, int page) {
    Logger.debug("[GUI] Opening AdminZonePage for %s (tab=%s, page=%d)", playerRef.getUsername(), tab, page);
    try {
      PageManager pageManager = player.getPageManager();
      AdminZonePage zonePage = new AdminZonePage(
        playerRef,
        guiManager.getZoneManager().get(),
        guiManager,
        tab,
        page
      );
      pageManager.openCustomPage(ref, store, zonePage);
      Logger.debug("[GUI] AdminZonePage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZonePage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Config page (placeholder).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminConfig(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminConfigPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminConfigPage page = new AdminConfigPage(playerRef, guiManager);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminConfigPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminConfigPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Backups page (placeholder).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminBackups(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminBackupsPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminBackupsPage page = new AdminBackupsPage(playerRef, guiManager);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminBackupsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminBackupsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Activity Log page (global log aggregation).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminActivityLog(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminActivityLogPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminActivityLogPage page = new AdminActivityLogPage(
        playerRef,
        guiManager.getFactionManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminActivityLogPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminActivityLogPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Updates page (placeholder).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminUpdates(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminUpdatesPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminUpdatesPage page = new AdminUpdatesPage(playerRef, guiManager);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminUpdatesPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminUpdatesPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Help page (placeholder).
   * Requires hyperfactions.admin permission.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminHelp(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminHelpPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminHelpPage page = new AdminHelpPage(playerRef, guiManager);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminHelpPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminHelpPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Version page — mod versions and integration status.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openAdminVersion(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    Logger.debug("[GUI] Opening AdminVersionPage for %s", playerRef.getUsername());
    try {
      PageManager pageManager = player.getPageManager();
      AdminVersionPage page = new AdminVersionPage(playerRef, guiManager.getPlugin().get(), guiManager);
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminVersionPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminVersionPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Create Zone Wizard page.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    openCreateZoneWizard(player, ref, store, playerRef, com.hyperfactions.data.ZoneType.SAFE);
  }

  /**
   * Opens the Create Zone Wizard page with a specific type selected.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param selectedType The initially selected zone type
   */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType) {
    openCreateZoneWizard(player, ref, store, playerRef, selectedType, "");
  }

  /**
   * Opens the Create Zone Wizard page with a specific type and preserved name.
   * Used when switching zone types to preserve the entered name.
   *
   * @param player        The Player entity
   * @param ref           The entity reference
   * @param store         The entity store
   * @param playerRef     The PlayerRef component
   * @param selectedType  The selected zone type
   * @param preservedName The preserved zone name from previous input
   */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType,
                  String preservedName) {
    openCreateZoneWizard(player, ref, store, playerRef, selectedType, preservedName,
        CreateZoneWizardPage.ClaimMethod.NO_CLAIMS, 5, false);
  }

  /**
   * Opens the Create Zone Wizard page with full state.
   * Used when changing wizard options to preserve all entered state.
   *
   * @param player         The Player entity
   * @param ref            The entity reference
   * @param store          The entity store
   * @param playerRef      The PlayerRef component
   * @param selectedType   The selected zone type
   * @param preservedName  The preserved zone name from previous input
   * @param claimMethod    The selected claiming method
   * @param selectedRadius The selected radius (for radius methods)
   * @param customizeFlags Whether to open flag settings after creation
   */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType,
                  String preservedName,
                  CreateZoneWizardPage.ClaimMethod claimMethod,
                  int selectedRadius,
                  boolean customizeFlags) {
    Logger.debug("[GUI] Opening CreateZoneWizardPage for %s (type: %s, name: '%s', method: %s, radius: %d)",
        playerRef.getUsername(), selectedType.name(), preservedName,
        claimMethod != null ? claimMethod.name() : "NO_CLAIMS", selectedRadius);
    try {
      PageManager pageManager = player.getPageManager();
      CreateZoneWizardPage page = new CreateZoneWizardPage(
        playerRef,
        guiManager.getZoneManager().get(),
        guiManager,
        selectedType,
        preservedName,
        claimMethod,
        selectedRadius,
        customizeFlags
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] CreateZoneWizardPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open CreateZoneWizardPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone Map page for editing a specific zone.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param zone      The zone to edit
   */
  public void openAdminZoneMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Zone zone) {
    openAdminZoneMap(player, ref, store, playerRef, zone, false);
  }

  /**
   * Opens the Admin Zone Map page for editing zone chunks.
   *
   * @param player          The Player entity
   * @param ref             The entity reference
   * @param store           The entity store
   * @param playerRef       The PlayerRef component
   * @param zone            The zone to edit
   * @param openFlagsAfter  Whether to open flags settings when done with map
   */
  public void openAdminZoneMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Zone zone, boolean openFlagsAfter) {
    Logger.debug("[GUI] Opening AdminZoneMapPage for %s (zone: %s, openFlagsAfter: %s)",
        playerRef.getUsername(), zone.name(), openFlagsAfter);
    try {
      PageManager pageManager = player.getPageManager();
      AdminZoneMapPage page = new AdminZoneMapPage(
        playerRef,
        zone,
        guiManager.getZoneManager().get(),
        guiManager.getClaimManager().get(),
        guiManager,
        openFlagsAfter
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminZoneMapPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZoneMapPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone Properties page for configuring zone name, type, and notifications.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param zoneId      The UUID of the zone to configure
   * @param currentTab  The current tab filter in AdminZonePage
   * @param currentPage The current page number in AdminZonePage
   */
  public void openAdminZoneProperties(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, String currentTab, int currentPage) {
    Logger.debug("[GUI] Opening AdminZonePropertiesPage for %s (zone: %s)", playerRef.getUsername(), zoneId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminZonePropertiesPage page = new AdminZonePropertiesPage(
        playerRef,
        zoneId,
        guiManager.getZoneManager().get(),
        guiManager,
        currentTab,
        currentPage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminZonePropertiesPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZonePropertiesPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone Settings page for configuring zone flags.
   * Back button returns to the zone list.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param zoneId    The UUID of the zone to configure
   */
  public void openAdminZoneSettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID zoneId) {
    Logger.debug("[GUI] Opening AdminZoneSettingsPage for %s (zone: %s)", playerRef.getUsername(), zoneId);
    try {
      PageManager pageManager = player.getPageManager();
      AdminZoneSettingsPage page = new AdminZoneSettingsPage(
        playerRef,
        zoneId,
        guiManager.getZoneManager().get(),
        guiManager
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminZoneSettingsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZoneSettingsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone Settings page for configuring zone flags,
   * with a back-target to control where the Back button navigates.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param zoneId      The UUID of the zone to configure
   * @param backTarget  Where to go on Back: "settings" for properties page, "list" for zone list
   * @param currentTab  The current tab filter in AdminZonePage
   * @param currentPage The current page number in AdminZonePage
   */
  public void openAdminZoneSettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID zoneId, String backTarget,
                   String currentTab, int currentPage) {
    Logger.debug("[GUI] Opening AdminZoneSettingsPage for %s (zone: %s, backTarget: %s)",
        playerRef.getUsername(), zoneId, backTarget);
    try {
      PageManager pageManager = player.getPageManager();
      AdminZoneSettingsPage page = new AdminZoneSettingsPage(
        playerRef,
        zoneId,
        guiManager.getZoneManager().get(),
        guiManager,
        backTarget,
        currentTab,
        currentPage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminZoneSettingsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZoneSettingsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Admin Zone Integration Flags page for configuring integration-specific zone flags.
   *
   * @param player    The Player entity
   * @param ref       The entity reference
   * @param store     The entity store
   * @param playerRef The PlayerRef component
   * @param zoneId    The UUID of the zone to configure
   */
  public void openAdminZoneIntegrationFlags(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       UUID zoneId) {
    Logger.debug("[GUI] Opening AdminZoneIntegrationFlagsPage for %s (zone: %s)", playerRef.getUsername(), zoneId);
    try {
      PageManager pageManager = player.getPageManager();
      var gs = guiManager.getPlugin().get().getProtectionChecker().getGravestoneIntegration();
      AdminZoneIntegrationFlagsPage page = new AdminZoneIntegrationFlagsPage(
        playerRef,
        zoneId,
        guiManager.getZoneManager().get(),
        guiManager,
        gs
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] AdminZoneIntegrationFlagsPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open AdminZoneIntegrationFlagsPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Zone Rename modal.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param zoneId      The UUID of the zone to rename
   * @param currentTab  The current tab filter in AdminZonePage
   * @param currentPage The current page number in AdminZonePage
   */
  public void openZoneRenameModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID zoneId, String currentTab, int currentPage) {
    Logger.debug("[GUI] Opening ZoneRenameModalPage for %s (zone: %s)", playerRef.getUsername(), zoneId);
    try {
      PageManager pageManager = player.getPageManager();
      ZoneRenameModalPage page = new ZoneRenameModalPage(
        playerRef,
        guiManager.getZoneManager().get(),
        guiManager,
        zoneId,
        currentTab,
        currentPage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] ZoneRenameModalPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open ZoneRenameModalPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Opens the Zone Change Type modal.
   *
   * @param player      The Player entity
   * @param ref         The entity reference
   * @param store       The entity store
   * @param playerRef   The PlayerRef component
   * @param zoneId      The UUID of the zone to change type
   * @param currentTab  The current tab filter in AdminZonePage
   * @param currentPage The current page number in AdminZonePage
   */
  public void openZoneChangeTypeModal(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, String currentTab, int currentPage) {
    openZoneChangeTypeModal(player, ref, store, playerRef, zoneId, false, currentTab, currentPage);
  }

  /**
   * Opens the Zone Change Type modal with optional return-to-settings behavior.
   *
   * @param player           The Player entity
   * @param ref              The entity reference
   * @param store            The entity store
   * @param playerRef        The PlayerRef component
   * @param zoneId           The UUID of the zone to change type
   * @param returnToSettings If true, returns to properties page after change; otherwise returns to zone list
   * @param currentTab       The current tab filter in AdminZonePage
   * @param currentPage      The current page number in AdminZonePage
   */
  public void openZoneChangeTypeModal(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, boolean returnToSettings,
                    String currentTab, int currentPage) {
    Logger.debug("[GUI] Opening ZoneChangeTypeModalPage for %s (zone: %s, returnToSettings: %s)",
        playerRef.getUsername(), zoneId, returnToSettings);
    try {
      PageManager pageManager = player.getPageManager();
      ZoneChangeTypeModalPage page = new ZoneChangeTypeModalPage(
        playerRef,
        guiManager.getZoneManager().get(),
        guiManager,
        zoneId,
        returnToSettings,
        currentTab,
        currentPage
      );
      pageManager.openCustomPage(ref, store, page);
      Logger.debug("[GUI] ZoneChangeTypeModalPage opened successfully");
    } catch (Exception e) {
      Logger.severe("[GUI] Failed to open ZoneChangeTypeModalPage: %s", e.getMessage());
      e.printStackTrace();
    }
  }

}
