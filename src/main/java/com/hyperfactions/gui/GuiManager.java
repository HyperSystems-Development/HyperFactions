package com.hyperfactions.gui;

import static com.hyperfactions.gui.faction.FactionPageRegistry.Entry;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.admin.AdminPageRegistry;
import com.hyperfactions.gui.admin.page.*;
import com.hyperfactions.gui.faction.*;
import com.hyperfactions.gui.faction.page.*;
import com.hyperfactions.gui.help.HelpCategory;
import com.hyperfactions.gui.help.page.HelpMainPage;
import com.hyperfactions.gui.newplayer.NewPlayerPageRegistry;
import com.hyperfactions.gui.newplayer.page.*;
import com.hyperfactions.gui.shared.page.*;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Central manager for HyperFactions GUI pages.
 * Provides methods to open various UI screens.
 */
public class GuiManager {

  private final Supplier<HyperFactions> plugin;

  private final Supplier<FactionManager> factionManager;

  private final Supplier<ClaimManager> claimManager;

  private final Supplier<PowerManager> powerManager;

  private final Supplier<RelationManager> relationManager;

  private final Supplier<ZoneManager> zoneManager;

  private final Supplier<TeleportManager> teleportManager;

  private final Supplier<InviteManager> inviteManager;

  private final Supplier<JoinRequestManager> joinRequestManager;

  private final Supplier<Path> dataDir;

  private ActivePageTracker activePageTracker;

  private Supplier<ChatManager> chatManagerSupplier;

  private Supplier<ChatHistoryManager> chatHistoryManagerSupplier;

  private final FactionPageOpener factionPageOpener;

  private final AdminPageOpener adminPageOpener;

  private final NewPlayerPageOpener newPlayerPageOpener;

  /** Creates a new GuiManager. */
  public GuiManager(Supplier<HyperFactions> plugin,
           Supplier<FactionManager> factionManager,
           Supplier<ClaimManager> claimManager,
           Supplier<PowerManager> powerManager,
           Supplier<RelationManager> relationManager,
           Supplier<ZoneManager> zoneManager,
           Supplier<TeleportManager> teleportManager,
           Supplier<InviteManager> inviteManager,
           Supplier<JoinRequestManager> joinRequestManager,
           Supplier<Path> dataDir) {
    this.plugin = plugin;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.powerManager = powerManager;
    this.relationManager = relationManager;
    this.zoneManager = zoneManager;
    this.teleportManager = teleportManager;
    this.inviteManager = inviteManager;
    this.joinRequestManager = joinRequestManager;
    this.dataDir = dataDir;

    // Register all pages with the central registry
    registerPages();
    registerNewPlayerPages();
    registerAdminPages();

    // Initialize page opener delegates
    this.factionPageOpener = new FactionPageOpener(this);
    this.adminPageOpener = new AdminPageOpener(this);
    this.newPlayerPageOpener = new NewPlayerPageOpener(this);
  }

  /**
   * Registers all GUI pages with the central FactionPageRegistry.
   * This enables navigation between pages via the NavBarHelper.
   */
  private void registerPages() {
    FactionPageRegistry registry = FactionPageRegistry.getInstance();

    // Dashboard (main faction page)
    // If player has faction, show enhanced dashboard; otherwise show main page
    registry.registerEntry(new Entry(
        "dashboard",
        MessageKeys.Nav.DASHBOARD,
        null, // No permission required
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction != null) {
            return new FactionDashboardPage(playerRef, factionManager.get(), claimManager.get(),
                powerManager.get(), teleportManager.get(), guiManager, plugin.get(), faction);
          }
          return new FactionMainPage(playerRef, factionManager.get(), claimManager.get(),
              powerManager.get(), teleportManager.get(), inviteManager.get(), guiManager);
        },
        true, // Show in nav bar
        false, // Doesn't require faction
        0 // Order
    ));

    // Chat page (faction/ally chat history with send-from-GUI)
    registry.registerEntry(new Entry(
        "chat",
        MessageKeys.Nav.CHAT,
        Permissions.CHAT_FACTION,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          if (chatManagerSupplier == null || chatHistoryManagerSupplier == null) {
            return null;
          }
          return new FactionChatPage(playerRef, factionManager.get(),
              chatManagerSupplier.get(), chatHistoryManagerSupplier.get(),
              guiManager, faction);
        },
        true, // Show in nav bar
        true, // Requires faction
        1
    ));

    // Members page
    registry.registerEntry(new Entry(
        "members",
        MessageKeys.Nav.MEMBERS,
        Permissions.MEMBERS,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          return new FactionMembersPage(playerRef, factionManager.get(), powerManager.get(), guiManager, faction);
        },
        true,
        true, // Requires faction
        2
    ));

    // Invites page (officers+ only) - shows outgoing invites and incoming join requests
    registry.registerEntry(new Entry(
        "invites",
        MessageKeys.Nav.INVITES,
        Permissions.INVITE,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          return new FactionInvitesPage(playerRef, factionManager.get(), inviteManager.get(),
              joinRequestManager.get(), guiManager, plugin.get(), faction);
        },
        true, // Show in nav bar
        true, // Requires faction
        FactionRole.OFFICER, // Minimum role required
        3
    ));

    // Browser page
    registry.registerEntry(new Entry(
        "browser",
        MessageKeys.Nav.BROWSER,
        null,
        (player, ref, store, playerRef, faction, guiManager) ->
            new FactionBrowserPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
        true,
        false,
        4
    ));

    // Map page
    registry.registerEntry(new Entry(
        "map",
        MessageKeys.Nav.MAP,
        Permissions.MAP,
        (player, ref, store, playerRef, faction, guiManager) ->
            new ChunkMapPage(playerRef, factionManager.get(), claimManager.get(),
                relationManager.get(), zoneManager.get(), guiManager),
        true,
        false,
        5
    ));

    // Leaderboard page
    registry.registerEntry(new Entry(
        "leaderboard",
        MessageKeys.Nav.LEADERBOARD,
        null,
        (player, ref, store, playerRef, faction, guiManager) -> {
          EconomyManager econ = plugin.get().isTreasuryEnabled() ? plugin.get().getEconomyManager() : null;
          return new FactionLeaderboardPage(playerRef, factionManager.get(), powerManager.get(), econ, plugin.get().getFactionKDCache(), guiManager);
        },
        true,
        false,
        6
    ));

    // Relations page
    registry.registerEntry(new Entry(
        "relations",
        MessageKeys.Nav.RELATIONS,
        Permissions.RELATIONS,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          return new FactionRelationsPage(playerRef, factionManager.get(),
              relationManager.get(), guiManager, faction, null);
        },
        true,
        true,
        7
    ));

    // Treasury page (conditional - only if economy is enabled)
    if (plugin.get().isTreasuryEnabled()) {
      registry.registerEntry(new Entry(
          "treasury",
          MessageKeys.Nav.TREASURY,
          Permissions.ECONOMY_BALANCE,
          (player, ref, store, playerRef, faction, guiManager) -> {
            if (faction == null) {
              return null;
            }
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
              return null;
            }
            return new TreasuryPage(playerRef, factionManager.get(), econ, guiManager, plugin.get(), faction);
          },
          true, // Show in nav bar
          true, // Requires faction
          8
      ));
    }

    // Settings page (officers+) - unified two-column layout
    registry.registerEntry(new Entry(
        "settings",
        MessageKeys.Nav.SETTINGS,
        null,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          return new FactionSettingsPage(playerRef, factionManager.get(), claimManager.get(), guiManager, plugin.get(), faction);
        },
        true, // Show in nav bar
        true, // Requires faction
        9
    ));

    // Logs page (faction activity log)
    registry.registerEntry(new Entry(
        "logs",
        MessageKeys.Nav.LOGS,
        Permissions.LOGS,
        (player, ref, store, playerRef, faction, guiManager) -> {
          if (faction == null) {
            return null;
          }
          return new LogsViewerPage(playerRef, factionManager.get(), guiManager, faction);
        },
        true, // Show in nav bar
        true, // Requires faction
        10
    ));

    // Help page (available to all players in faction nav bar)
    registry.registerEntry(new Entry(
        "help",
        MessageKeys.Nav.HELP,
        null,
        (player, ref, store, playerRef, faction, guiManager) ->
            new HelpMainPage(playerRef, guiManager, factionManager.get()),
        true, // Show in nav bar
        false, // Doesn't require faction
        11
    ));

    // Player Settings page (registered but NOT in nav bar — rendered separately on far right)
    registry.registerEntry(new Entry(
        "player_settings",
        MessageKeys.Nav.PLAYER_SETTINGS,
        null,
        (player, ref, store, playerRef, faction, guiManager) ->
            new PlayerSettingsPage(playerRef, factionManager.get(),
                plugin.get().getPlayerStorage(), guiManager),
        false, // NOT in nav bar (rendered separately on far right)
        false, // Doesn't require faction
        99
    ));

    // Admin page (requires permission) - accessed via /f admin, not in main nav bar
    registry.registerEntry(new Entry(
        "admin",
        MessageKeys.Nav.ADMIN,
        Permissions.ADMIN,
        (player, ref, store, playerRef, faction, guiManager) ->
            new AdminMainPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
        false,  // Not in main nav bar - separate admin GUI
        false,
        13
    ));

    Logger.debug("[GUI] Registered %d pages with FactionPageRegistry", registry.getEntries().size());
  }

  /**
   * Registers all New Player GUI pages with the NewPlayerPageRegistry.
   * These pages are shown to players who are NOT in a faction.
   *
   * <p>Nav bar order: BROWSE | CREATE | INVITES | MAP | LEADERBOARD | HELP
   */
  private void registerNewPlayerPages() {
    NewPlayerPageRegistry registry = NewPlayerPageRegistry.getInstance();

    // Browse Factions (default landing page)
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "browse",
        MessageKeys.Nav.BROWSER,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new NewPlayerBrowsePage(playerRef, factionManager.get(), powerManager.get(),
                inviteManager.get(), guiManager),
        true,
        0
    ));

    // Create Faction (permission checked on actual create action, not nav visibility)
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "create",
        MessageKeys.Nav.CREATE,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new CreateFactionPage(playerRef, factionManager.get(), guiManager),
        true,
        1
    ));

    // My Invites
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "invites",
        MessageKeys.Nav.INVITES,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new InvitesPage(playerRef, factionManager.get(), powerManager.get(),
                inviteManager.get(), joinRequestManager.get(), guiManager),
        true,
        2
    ));

    // Territory Map (read-only for new players, always accessible)
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "map",
        MessageKeys.Nav.MAP,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new NewPlayerMapPage(playerRef, factionManager.get(), claimManager.get(),
                relationManager.get(), zoneManager.get(), guiManager),
        true,
        3
    ));

    // Leaderboard (accessible to all players)
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "leaderboard",
        MessageKeys.Nav.LEADERBOARD,
        null,
        (player, ref, store, playerRef, guiManager) -> {
          EconomyManager econ = plugin.get().isTreasuryEnabled() ? plugin.get().getEconomyManager() : null;
          return new FactionLeaderboardPage(playerRef, factionManager.get(), powerManager.get(), econ, plugin.get().getFactionKDCache(), guiManager);
        },
        true,
        4
    ));

    // Help Page
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "help",
        MessageKeys.Nav.HELP,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new HelpMainPage(playerRef, guiManager, factionManager.get()),
        true,
        5
    ));

    // Player Settings page (registered but NOT in nav bar — rendered separately on far right)
    registry.registerEntry(new NewPlayerPageRegistry.Entry(
        "player_settings",
        MessageKeys.Nav.PLAYER_SETTINGS,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new PlayerSettingsPage(playerRef, factionManager.get(),
                plugin.get().getPlayerStorage(), guiManager),
        false,
        99
    ));

    Logger.debug("[GUI] Registered %d pages with NewPlayerPageRegistry", registry.getEntries().size());
  }

  /**
   * Registers all Admin GUI pages with the AdminPageRegistry.
   * These pages are shown in the admin navigation bar.
   *
   * <p>Nav bar order: DASHBOARD | FACTIONS | PLAYERS | ECONOMY | ZONES | CONFIG | BACKUPS | LOG | UPDATES | HELP
   */
  private void registerAdminPages() {
    AdminPageRegistry registry = AdminPageRegistry.getInstance();

    // Dashboard (server-wide stats overview)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "dashboard",
        MessageKeys.AdminNav.DASHBOARD,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminDashboardPage(playerRef, plugin.get(), factionManager.get(), powerManager.get(),
                zoneManager.get(), guiManager),
        true,
        0
    ));

    // Actions page (server-wide quick actions)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "actions",
        MessageKeys.AdminNav.ACTIONS,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminActionsPage(playerRef, plugin.get().getPlayerStorage(), guiManager, plugin.get()),
        true,
        1
    ));

    // Factions page (faction management with expanding rows)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "factions",
        MessageKeys.AdminNav.FACTIONS,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminFactionsPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
        true,
        2
    ));

    // Players page (server-wide player management)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "players",
        MessageKeys.AdminNav.PLAYERS,
        Permissions.ADMIN_POWER,
        (player, ref, store, playerRef, guiManager) ->
            new AdminPlayersPage(playerRef, factionManager.get(), powerManager.get(),
                plugin.get().getPlayerStorage(), guiManager),
        true,
        3
    ));

    // Economy page (conditional - only if economy is enabled)
    if (plugin.get().isTreasuryEnabled()) {
      registry.registerEntry(new AdminPageRegistry.Entry(
          "economy",
          MessageKeys.AdminNav.ECONOMY,
          Permissions.ADMIN_ECONOMY,
          (player, ref, store, playerRef, guiManager) ->
              new AdminEconomyPage(playerRef, factionManager.get(),
                  plugin.get().getEconomyManager(), guiManager),
          true,
          4
      ));
    }

    // Zones page
    registry.registerEntry(new AdminPageRegistry.Entry(
        "zones",
        MessageKeys.AdminNav.ZONES,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminZonePage(playerRef, zoneManager.get(), guiManager, "all", 0),
        true,
        5
    ));

    // Config page (placeholder)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "config",
        MessageKeys.AdminNav.CONFIG,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminConfigPage(playerRef, guiManager),
        true,
        6
    ));

    // Backups page (placeholder)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "backups",
        MessageKeys.AdminNav.BACKUPS,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminBackupsPage(playerRef, guiManager),
        true,
        7
    ));

    // Activity Log page (global log aggregation)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "log",
        MessageKeys.AdminNav.LOG,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminActivityLogPage(playerRef, factionManager.get(), guiManager),
        true,
        8
    ));

    // Updates page (placeholder)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "updates",
        MessageKeys.AdminNav.UPDATES,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminUpdatesPage(playerRef, guiManager),
        true,
        9
    ));

    // Help page (placeholder)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "help",
        MessageKeys.AdminNav.HELP,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminHelpPage(playerRef, guiManager),
        true,
        10
    ));

    // Version page (mod versions and integration status)
    registry.registerEntry(new AdminPageRegistry.Entry(
        "version",
        MessageKeys.AdminNav.VERSION,
        null,
        (player, ref, store, playerRef, guiManager) ->
            new AdminVersionPage(playerRef, plugin.get(), guiManager),
        true,
        11
    ));

    Logger.debug("[GUI] Registered %d pages with AdminPageRegistry", registry.getEntries().size());
  }

  // === Page Opener Delegations ===

  /** Opens the main menu page. */
  public void openMainMenu(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openMainMenu(player, ref, store, playerRef);
  }

  /** Opens the faction main page. */
  public void openFactionMain(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openFactionMain(player, ref, store, playerRef);
  }

  /** Opens the faction members page. */
  public void openFactionMembers(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    factionPageOpener.openFactionMembers(player, ref, store, playerRef, faction);
  }

  /** Opens the chunk map page. */
  public void openChunkMap(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openChunkMap(player, ref, store, playerRef);
  }

  public void openFactionRelations(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openFactionRelations(player, ref, store, playerRef, faction);
  }

  /** Opens the faction relations page. */
  public void openFactionRelations(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, String tab) {
    factionPageOpener.openFactionRelations(player, ref, store, playerRef, faction, tab);
  }

  public void openCreateFaction(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openCreateFaction(player, ref, store, playerRef);
  }

  /** Opens the faction browser page. */
  public void openFactionBrowser(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openFactionBrowser(player, ref, store, playerRef);
  }

  /** Opens the leaderboard page. */
  public void openLeaderboard(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openLeaderboard(player, ref, store, playerRef);
  }

  /** Opens the faction invites page. */
  public void openFactionInvites(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    factionPageOpener.openFactionInvites(player, ref, store, playerRef, faction);
  }

  /** Opens the faction settings page. */
  public void openFactionSettings(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openFactionSettings(player, ref, store, playerRef, faction);
  }

  /** Opens the set relation modal page. */
  public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openSetRelationModal(player, ref, store, playerRef, faction);
  }

  /** Opens the set relation modal page. */
  public void openSetRelationModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, String searchQuery, int page) {
    factionPageOpener.openSetRelationModal(player, ref, store, playerRef, faction, searchQuery, page);
  }

  /** Opens the tag modal page. */
  public void openTagModal(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef,
              Faction faction) {
    factionPageOpener.openTagModal(player, ref, store, playerRef, faction);
  }

  public void openDescriptionModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openDescriptionModal(player, ref, store, playerRef, faction);
  }

  /** Opens the rename modal page. */
  public void openRenameModal(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    factionPageOpener.openRenameModal(player, ref, store, playerRef, faction);
  }

  /** Opens the admin tag modal page. */
  public void openAdminTagModal(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    adminPageOpener.openAdminTagModal(player, ref, store, playerRef, faction);
  }

  /** Opens the admin description modal page. */
  public void openAdminDescriptionModal(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     Faction faction) {
    adminPageOpener.openAdminDescriptionModal(player, ref, store, playerRef, faction);
  }

  /** Opens the admin rename modal page. */
  public void openAdminRenameModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    adminPageOpener.openAdminRenameModal(player, ref, store, playerRef, faction);
  }

  /** Opens the disband confirm page. */
  public void openDisbandConfirm(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    factionPageOpener.openDisbandConfirm(player, ref, store, playerRef, faction);
  }

  /** Opens the transfer confirm page. */
  public void openTransferConfirm(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction, UUID targetUuid, String targetName) {
    factionPageOpener.openTransferConfirm(player, ref, store, playerRef, faction, targetUuid, targetName);
  }

  /** Opens the player settings page. */
  public void openPlayerSettings(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openPlayerSettings(player, ref, store, playerRef);
  }

  /** Opens the faction dashboard page. */
  public void openFactionDashboard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openFactionDashboard(player, ref, store, playerRef, faction);
  }

  public void openLeaveConfirm(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    factionPageOpener.openLeaveConfirm(player, ref, store, playerRef, faction);
  }

  /** Opens the leader leave confirm page. */
  public void openLeaderLeaveConfirm(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    Faction faction) {
    factionPageOpener.openLeaderLeaveConfirm(player, ref, store, playerRef, faction);
  }

  /** Opens the faction chat page. */
  public void openFactionChat(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction faction) {
    factionPageOpener.openFactionChat(player, ref, store, playerRef, faction);
  }

  /** Opens the faction modules page. */
  public void openFactionModules(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef,
                 Faction faction) {
    factionPageOpener.openFactionModules(player, ref, store, playerRef, faction);
  }

  /** Opens the faction treasury page. */
  public void openFactionTreasury(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  Faction faction) {
    factionPageOpener.openFactionTreasury(player, ref, store, playerRef, faction);
  }

  /** Opens the treasury deposit modal page. */
  public void openTreasuryDepositModal(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     Faction faction, String mode) {
    factionPageOpener.openTreasuryDepositModal(player, ref, store, playerRef, faction, mode);
  }

  /** Opens the treasury transfer search page. */
  public void openTreasuryTransferSearch(Player player, Ref<EntityStore> ref,
                      Store<EntityStore> store, PlayerRef playerRef,
                      Faction faction) {
    factionPageOpener.openTreasuryTransferSearch(player, ref, store, playerRef, faction);
  }

  /** Opens the treasury transfer confirm page. */
  public void openTreasuryTransferConfirm(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       Faction faction, String targetId,
                       String targetName, String targetType) {
    factionPageOpener.openTreasuryTransferConfirm(player, ref, store, playerRef, faction, targetId, targetName, targetType);
  }

  /** Opens the treasury settings page. */
  public void openTreasurySettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   Faction faction) {
    factionPageOpener.openTreasurySettings(player, ref, store, playerRef, faction);
  }

  public void openAdminMain(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminMain(player, ref, store, playerRef);
  }

  /** Opens the admin dashboard page. */
  public void openAdminDashboard(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminDashboard(player, ref, store, playerRef);
  }

  /** Opens the admin actions page. */
  public void openAdminActions(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminActions(player, ref, store, playerRef);
  }

  /** Opens the admin factions page. */
  public void openAdminFactions(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminFactions(player, ref, store, playerRef);
  }

  /** Opens the admin players page. */
  public void openAdminPlayers(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminPlayers(player, ref, store, playerRef);
  }

  /** Opens the admin disband confirm page. */
  public void openAdminDisbandConfirm(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId, String factionName) {
    adminPageOpener.openAdminDisbandConfirm(player, ref, store, playerRef, factionId, factionName);
  }

  /** Opens the admin faction info page. */
  public void openAdminFactionInfo(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID factionId) {
    adminPageOpener.openAdminFactionInfo(player, ref, store, playerRef, factionId);
  }

  /** Opens the admin economy page. */
  public void openAdminEconomy(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminEconomy(player, ref, store, playerRef);
  }

  /** Opens the admin economy adjust page. */
  public void openAdminEconomyAdjust(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID factionId) {
    adminPageOpener.openAdminEconomyAdjust(player, ref, store, playerRef, factionId);
  }

  /** Opens the admin bulk economy adjust page. */
  public void openAdminBulkEconomy(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminBulkEconomy(player, ref, store, playerRef);
  }

  /** Opens the admin faction members page. */
  public void openAdminFactionMembers(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId) {
    adminPageOpener.openAdminFactionMembers(player, ref, store, playerRef, factionId);
  }

  /** Opens the admin player info page. */
  public void openAdminPlayerInfo(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID targetPlayerUuid, String targetPlayerName,
                  UUID factionId,
                  AdminPlayerInfoPage.Origin origin) {
    adminPageOpener.openAdminPlayerInfo(player, ref, store, playerRef, targetPlayerUuid, targetPlayerName, factionId, origin);
  }

  /** Opens the admin faction relations page. */
  public void openAdminFactionRelations(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     UUID factionId) {
    adminPageOpener.openAdminFactionRelations(player, ref, store, playerRef, factionId);
  }

  /** Opens the admin faction settings page. */
  public void openAdminFactionSettings(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID factionId) {
    adminPageOpener.openAdminFactionSettings(player, ref, store, playerRef, factionId);
  }

  /** Opens the admin unclaim all confirm page. */
  public void openAdminUnclaimAllConfirm(Player player, Ref<EntityStore> ref,
                     Store<EntityStore> store, PlayerRef playerRef,
                     UUID factionId, String factionName, int claimCount) {
    adminPageOpener.openAdminUnclaimAllConfirm(player, ref, store, playerRef, factionId, factionName, claimCount);
  }

  /** Opens the admin zone page. */
  public void openAdminZone(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminZone(player, ref, store, playerRef);
  }

  /** Opens the admin zone page. */
  public void openAdminZone(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               String tab, int page) {
    adminPageOpener.openAdminZone(player, ref, store, playerRef, tab, page);
  }

  /** Opens the admin config page. */
  public void openAdminConfig(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminConfig(player, ref, store, playerRef);
  }

  /** Opens the admin backups page. */
  public void openAdminBackups(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminBackups(player, ref, store, playerRef);
  }

  public void openAdminActivityLog(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminActivityLog(player, ref, store, playerRef);
  }

  /** Opens the admin updates page. */
  public void openAdminUpdates(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminUpdates(player, ref, store, playerRef);
  }

  /** Opens the admin help page. */
  public void openAdminHelp(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminHelp(player, ref, store, playerRef);
  }

  /** Opens the admin version page. */
  public void openAdminVersion(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openAdminVersion(player, ref, store, playerRef);
  }

  /** Opens the create zone wizard page. */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    adminPageOpener.openCreateZoneWizard(player, ref, store, playerRef);
  }

  /** Opens the create zone wizard page. */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType) {
    adminPageOpener.openCreateZoneWizard(player, ref, store, playerRef, selectedType);
  }

  /** Opens the create zone wizard page. */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType,
                  String preservedName) {
    adminPageOpener.openCreateZoneWizard(player, ref, store, playerRef, selectedType, preservedName);
  }

  /** Opens the create zone wizard page. */
  public void openCreateZoneWizard(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  com.hyperfactions.data.ZoneType selectedType,
                  String preservedName,
                  CreateZoneWizardPage.ClaimMethod claimMethod,
                  int selectedRadius,
                  boolean customizeFlags) {
    adminPageOpener.openCreateZoneWizard(player, ref, store, playerRef, selectedType, preservedName, claimMethod, selectedRadius, customizeFlags);
  }

  /** Opens the admin zone map page. */
  public void openAdminZoneMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Zone zone) {
    adminPageOpener.openAdminZoneMap(player, ref, store, playerRef, zone);
  }

  /** Opens the admin zone map page. */
  public void openAdminZoneMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Zone zone, boolean openFlagsAfter) {
    adminPageOpener.openAdminZoneMap(player, ref, store, playerRef, zone, openFlagsAfter);
  }

  /** Opens the admin zone properties page. */
  public void openAdminZoneProperties(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, String currentTab, int currentPage) {
    adminPageOpener.openAdminZoneProperties(player, ref, store, playerRef, zoneId, currentTab, currentPage);
  }

  /** Opens the admin zone settings page. */
  public void openAdminZoneSettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID zoneId) {
    adminPageOpener.openAdminZoneSettings(player, ref, store, playerRef, zoneId);
  }

  /** Opens the admin zone settings page. */
  public void openAdminZoneSettings(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef,
                   UUID zoneId, String backTarget,
                   String currentTab, int currentPage) {
    adminPageOpener.openAdminZoneSettings(player, ref, store, playerRef, zoneId, backTarget, currentTab, currentPage);
  }

  /** Opens the admin zone integration flags page. */
  public void openAdminZoneIntegrationFlags(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       UUID zoneId) {
    adminPageOpener.openAdminZoneIntegrationFlags(player, ref, store, playerRef, zoneId);
  }

  /** Opens the zone rename modal page. */
  public void openZoneRenameModal(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  UUID zoneId, String currentTab, int currentPage) {
    adminPageOpener.openZoneRenameModal(player, ref, store, playerRef, zoneId, currentTab, currentPage);
  }

  /** Opens the zone change type modal page. */
  public void openZoneChangeTypeModal(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, String currentTab, int currentPage) {
    adminPageOpener.openZoneChangeTypeModal(player, ref, store, playerRef, zoneId, currentTab, currentPage);
  }

  /** Opens the zone change type modal page. */
  public void openZoneChangeTypeModal(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef,
                    UUID zoneId, boolean returnToSettings,
                    String currentTab, int currentPage) {
    adminPageOpener.openZoneChangeTypeModal(player, ref, store, playerRef, zoneId, returnToSettings, currentTab, currentPage);
  }

  /** Opens the logs viewer page. */
  public void openLogsViewer(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               Faction faction) {
    factionPageOpener.openLogsViewer(player, ref, store, playerRef, faction);
  }

  /** Opens the faction info page. */
  public void openFactionInfo(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction targetFaction) {
    factionPageOpener.openFactionInfo(player, ref, store, playerRef, targetFaction);
  }

  /** Opens the faction info page. */
  public void openFactionInfo(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef,
                Faction targetFaction, String sourcePage) {
    factionPageOpener.openFactionInfo(player, ref, store, playerRef, targetFaction, sourcePage);
  }

  public void openFactionInfoFromPlayerInfo(Player player, Ref<EntityStore> ref,
                       Store<EntityStore> store, PlayerRef playerRef,
                       Faction targetFaction,
                       UUID sourcePlayerUuid, String sourcePlayerName,
                       String playerInfoSourcePage) {
    factionPageOpener.openFactionInfoFromPlayerInfo(player, ref, store, playerRef, targetFaction, sourcePlayerUuid, sourcePlayerName, playerInfoSourcePage);
  }

  /** Opens the player info page. */
  public void openPlayerInfo(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               UUID targetUuid, String targetName) {
    factionPageOpener.openPlayerInfo(player, ref, store, playerRef, targetUuid, targetName);
  }

  /** Opens the player info page. */
  public void openPlayerInfo(Player player, Ref<EntityStore> ref,
               Store<EntityStore> store, PlayerRef playerRef,
               UUID targetUuid, String targetName, String sourcePage) {
    factionPageOpener.openPlayerInfo(player, ref, store, playerRef, targetUuid, targetName, sourcePage);
  }

  /** Opens the new player browse page. */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef) {
    newPlayerPageOpener.openNewPlayerBrowse(player, ref, store, playerRef);
  }

  /** Opens the new player browse page. */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  int page, String sortBy) {
    newPlayerPageOpener.openNewPlayerBrowse(player, ref, store, playerRef, page, sortBy);
  }

  /** Opens the new player browse page. */
  public void openNewPlayerBrowse(Player player, Ref<EntityStore> ref,
                  Store<EntityStore> store, PlayerRef playerRef,
                  int page, String sortBy, String searchQuery) {
    newPlayerPageOpener.openNewPlayerBrowse(player, ref, store, playerRef, page, sortBy, searchQuery);
  }

  /** Opens the create faction wizard page. */
  public void openCreateFactionWizard(Player player, Ref<EntityStore> ref,
                    Store<EntityStore> store, PlayerRef playerRef) {
    newPlayerPageOpener.openCreateFactionWizard(player, ref, store, playerRef);
  }

  /** Opens the invites page page. */
  public void openInvitesPage(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    newPlayerPageOpener.openInvitesPage(player, ref, store, playerRef);
  }

  /** Opens the new player map page. */
  public void openNewPlayerMap(Player player, Ref<EntityStore> ref,
                Store<EntityStore> store, PlayerRef playerRef) {
    newPlayerPageOpener.openNewPlayerMap(player, ref, store, playerRef);
  }

  /** Opens the help page page. */
  public void openHelpPage(Player player, Ref<EntityStore> ref,
              Store<EntityStore> store, PlayerRef playerRef) {
    newPlayerPageOpener.openHelpPage(player, ref, store, playerRef);
  }

  /** Opens the help page. */
  public void openHelp(Player player, Ref<EntityStore> ref,
            Store<EntityStore> store, PlayerRef playerRef,
            HelpCategory category) {
    newPlayerPageOpener.openHelp(player, ref, store, playerRef, category);
  }

  /** Opens the button test page. */
  public void openButtonTestPage(Player player, Ref<EntityStore> ref,
                 Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openButtonTestPage(player, ref, store, playerRef);
  }

  /** Opens the markdown rendering test page. */
  public void openMarkdownTestPage(Player player, Ref<EntityStore> ref,
                   Store<EntityStore> store, PlayerRef playerRef) {
    factionPageOpener.openMarkdownTestPage(player, ref, store, playerRef);
  }

  /**
   * Closes the current page.
   *
   * @param player The Player entity
   * @param ref    The entity reference
   * @param store  The entity store
   */
  public void closePage(Player player, Ref<EntityStore> ref, Store<EntityStore> store) {
    player.getPageManager().setPage(ref, store,
        com.hypixel.hytale.protocol.packets.interface_.Page.None);
  }

  // === Getters ===

  /** Returns the plugin. */
  public Supplier<HyperFactions> getPlugin() {
    return plugin;
  }

  /** Returns the faction manager. */
  public Supplier<FactionManager> getFactionManager() {
    return factionManager;
  }

  /** Returns the claim manager. */
  public Supplier<ClaimManager> getClaimManager() {
    return claimManager;
  }

  /** Returns the power manager. */
  public Supplier<PowerManager> getPowerManager() {
    return powerManager;
  }

  /** Returns the relation manager. */
  public Supplier<RelationManager> getRelationManager() {
    return relationManager;
  }

  /** Returns the zone manager. */
  public Supplier<ZoneManager> getZoneManager() {
    return zoneManager;
  }

  /** Returns the teleport manager. */
  public Supplier<TeleportManager> getTeleportManager() {
    return teleportManager;
  }

  /** Returns the invite manager. */
  public Supplier<InviteManager> getInviteManager() {
    return inviteManager;
  }

  /** Returns the join request manager. */
  public JoinRequestManager getJoinRequestManager() {
    return joinRequestManager.get();
  }

  /** Returns the data dir. */
  public Supplier<Path> getDataDir() {
    return dataDir;
  }

  /**
   * Gets the active page tracker for real-time GUI updates.
   *
   * @return The active page tracker, or null if not initialized
   */
  public ActivePageTracker getActivePageTracker() {
    return activePageTracker;
  }

  /**
   * Sets the active page tracker.
   *
   * @param tracker The active page tracker
   */
  public void setActivePageTracker(ActivePageTracker tracker) {
    this.activePageTracker = tracker;
  }

  /**
   * Sets the chat manager supplier for the chat page.
   *
   * @param chatManager The chat manager supplier
   */
  public void setChatManagerSupplier(Supplier<ChatManager> chatManager) {
    this.chatManagerSupplier = chatManager;
  }

  /**
   * Sets the chat history manager supplier for the chat page.
   *
   * @param chatHistoryManager The chat history manager supplier
   */
  public void setChatHistoryManagerSupplier(Supplier<ChatHistoryManager> chatHistoryManager) {
    this.chatHistoryManagerSupplier = chatHistoryManager;
  }

  public Supplier<ChatManager> getChatManagerSupplier() {
    return chatManagerSupplier;
  }

  /** Returns the chat history manager supplier. */
  public Supplier<ChatHistoryManager> getChatHistoryManagerSupplier() {
    return chatHistoryManagerSupplier;
  }
}
