package com.hyperfactions.gui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.Zone;
import com.hyperfactions.gui.admin.AdminPageRegistry;
import com.hyperfactions.gui.faction.*;
import com.hyperfactions.gui.faction.page.*;
import com.hyperfactions.gui.help.HelpCategory;
import com.hyperfactions.gui.help.page.HelpMainPage;
import com.hyperfactions.gui.shared.page.*;
import com.hyperfactions.gui.admin.page.*;
import com.hyperfactions.gui.newplayer.NewPlayerPageRegistry;
import com.hyperfactions.gui.newplayer.page.*;
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
import java.util.function.Supplier;

import static com.hyperfactions.gui.faction.FactionPageRegistry.Entry;

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
                "Dashboard",
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
                "Chat",
                Permissions.CHAT_FACTION,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    if (chatManagerSupplier == null || chatHistoryManagerSupplier == null) return null;
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
                "Members",
                Permissions.MEMBERS,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionMembersPage(playerRef, factionManager.get(), powerManager.get(), guiManager, faction);
                },
                true,
                true, // Requires faction
                2
        ));

        // Invites page (officers+ only) - shows outgoing invites and incoming join requests
        registry.registerEntry(new Entry(
                "invites",
                "Invites",
                Permissions.INVITE,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
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
                "Browse",
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
                "Map",
                Permissions.MAP,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new ChunkMapPage(playerRef, factionManager.get(), claimManager.get(),
                                relationManager.get(), zoneManager.get(), guiManager),
                true,
                false,
                5
        ));

        // Relations page
        registry.registerEntry(new Entry(
                "relations",
                "Relations",
                Permissions.RELATIONS,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionRelationsPage(playerRef, factionManager.get(),
                            relationManager.get(), guiManager, faction, null);
                },
                true,
                true,
                6
        ));

        // Treasury page (conditional - only if economy is enabled)
        if (plugin.get().isTreasuryEnabled()) {
            registry.registerEntry(new Entry(
                    "treasury",
                    "Treasury",
                    Permissions.ECONOMY_BALANCE,
                    (player, ref, store, playerRef, faction, guiManager) -> {
                        if (faction == null) return null;
                        EconomyManager econ = plugin.get().getEconomyManager();
                        if (econ == null) return null;
                        return new TreasuryPage(playerRef, factionManager.get(), econ, guiManager, plugin.get(), faction);
                    },
                    true, // Show in nav bar
                    true, // Requires faction
                    7
            ));
        }

        // Settings page (officers+) - unified two-column layout
        registry.registerEntry(new Entry(
                "settings",
                "Settings",
                null,
                (player, ref, store, playerRef, faction, guiManager) -> {
                    if (faction == null) return null;
                    return new FactionSettingsPage(playerRef, factionManager.get(), claimManager.get(), guiManager, plugin.get(), faction);
                },
                true, // Show in nav bar
                true, // Requires faction
                8
        ));

        // Help page (available to all players in faction nav bar)
        registry.registerEntry(new Entry(
                "help",
                "Help",
                null,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new HelpMainPage(playerRef, guiManager, factionManager.get()),
                true, // Show in nav bar
                false, // Doesn't require faction
                9
        ));

        // Admin page (requires permission) - accessed via /f admin, not in main nav bar
        registry.registerEntry(new Entry(
                "admin",
                "Admin",
                Permissions.ADMIN,
                (player, ref, store, playerRef, faction, guiManager) ->
                        new AdminMainPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
                false,  // Not in main nav bar - separate admin GUI
                false,
                11
        ));

        Logger.debug("[GUI] Registered %d pages with FactionPageRegistry", registry.getEntries().size());
    }

    /**
     * Registers all New Player GUI pages with the NewPlayerPageRegistry.
     * These pages are shown to players who are NOT in a faction.
     *
     * Nav bar order: BROWSE | CREATE | INVITES | MAP | HELP
     */
    private void registerNewPlayerPages() {
        NewPlayerPageRegistry registry = NewPlayerPageRegistry.getInstance();

        // Browse Factions (default landing page)
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "browse",
                "Browse",
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
                "Create",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new CreateFactionPage(playerRef, factionManager.get(), guiManager),
                true,
                1
        ));

        // My Invites
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "invites",
                "Invites",
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
                "Map",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new NewPlayerMapPage(playerRef, factionManager.get(), claimManager.get(),
                                zoneManager.get(), guiManager),
                true,
                3
        ));

        // Help Page
        registry.registerEntry(new NewPlayerPageRegistry.Entry(
                "help",
                "Help",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new HelpMainPage(playerRef, guiManager, factionManager.get()),
                true,
                4
        ));

        Logger.debug("[GUI] Registered %d pages with NewPlayerPageRegistry", registry.getEntries().size());
    }

    /**
     * Registers all Admin GUI pages with the AdminPageRegistry.
     * These pages are shown in the admin navigation bar.
     *
     * Nav bar order: DASHBOARD | FACTIONS | ZONES | CONFIG | BACKUPS | UPDATES | HELP
     */
    private void registerAdminPages() {
        AdminPageRegistry registry = AdminPageRegistry.getInstance();

        // Dashboard (server-wide stats overview)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "dashboard",
                "Dashboard",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminDashboardPage(playerRef, plugin.get(), factionManager.get(), powerManager.get(),
                                zoneManager.get(), guiManager),
                true,
                0
        ));

        // Factions page (faction management with expanding rows)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "factions",
                "Factions",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminFactionsPage(playerRef, factionManager.get(), powerManager.get(), guiManager),
                true,
                1
        ));

        // Players page (server-wide player management)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "players",
                "Players",
                Permissions.ADMIN_POWER,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminPlayersPage(playerRef, factionManager.get(), powerManager.get(),
                                plugin.get().getPlayerStorage(), guiManager),
                true,
                2
        ));

        // Economy page (conditional - only if economy is enabled)
        if (plugin.get().isTreasuryEnabled()) {
            registry.registerEntry(new AdminPageRegistry.Entry(
                    "economy",
                    "Economy",
                    Permissions.ADMIN_ECONOMY,
                    (player, ref, store, playerRef, guiManager) ->
                            new AdminEconomyPage(playerRef, factionManager.get(),
                                    plugin.get().getEconomyManager(), guiManager),
                    true,
                    3
            ));
        }

        // Zones page
        registry.registerEntry(new AdminPageRegistry.Entry(
                "zones",
                "Zones",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminZonePage(playerRef, zoneManager.get(), guiManager, "all", 0),
                true,
                4
        ));

        // Config page (placeholder)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "config",
                "Config",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminConfigPage(playerRef, guiManager),
                true,
                5
        ));

        // Backups page (placeholder)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "backups",
                "Backups",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminBackupsPage(playerRef, guiManager),
                true,
                6
        ));

        // Updates page (placeholder)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "updates",
                "Updates",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminUpdatesPage(playerRef, guiManager),
                true,
                7
        ));

        // Help page (placeholder)
        registry.registerEntry(new AdminPageRegistry.Entry(
                "help",
                "Help",
                null,
                (player, ref, store, playerRef, guiManager) ->
                        new AdminHelpPage(playerRef, guiManager),
                true,
                8
        ));

        Logger.debug("[GUI] Registered %d pages with AdminPageRegistry", registry.getEntries().size());
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
                factionManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] MainMenuPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open MainMenuPage: %s", e.getMessage());
            e.printStackTrace();
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
        Faction faction = factionManager.get().getPlayerFaction(uuid);

        if (faction != null) {
            // Player has a faction - open enhanced dashboard
            Logger.debug("[GUI] Opening FactionDashboardPage for %s (faction: %s)",
                    playerRef.getUsername(), faction.name());
            try {
                PageManager pageManager = player.getPageManager();
                FactionDashboardPage page = new FactionDashboardPage(
                    playerRef,
                    factionManager.get(),
                    claimManager.get(),
                    powerManager.get(),
                    teleportManager.get(),
                    this,
                    plugin.get(),
                    faction
                );
                pageManager.openCustomPage(ref, store, page);
                Logger.debug("[GUI] FactionDashboardPage opened successfully");
            } catch (Exception e) {
                Logger.severe("[GUI] Failed to open FactionDashboardPage: %s", e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Player has no faction - redirect to new player browse page
            Logger.debug("[GUI] Player %s has no faction, redirecting to NewPlayerBrowsePage",
                    playerRef.getUsername());
            openNewPlayerBrowse(player, ref, store, playerRef);
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
                factionManager.get(),
                powerManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionMembersPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionMembersPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                claimManager.get(),
                relationManager.get(),
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] ChunkMapPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open ChunkMapPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                relationManager.get(),
                this,
                faction,
                tab
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionRelationsPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionRelationsPage: %s", e.getMessage());
            e.printStackTrace();
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
        openCreateFactionWizard(player, ref, store, playerRef);
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
                factionManager.get(),
                powerManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionBrowserPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionBrowserPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                inviteManager.get(),
                joinRequestManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionInvitesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionInvitesPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                claimManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionSettingsPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionSettingsPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                this,
                faction,
                searchQuery,
                page
            );
            pageManager.openCustomPage(ref, store, modalPage);
            Logger.debug("[GUI] SetRelationModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open SetRelationModalPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction,
                plugin.get().getWorldMapService()
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] TagModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TagModalPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] DescriptionModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open DescriptionModalPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction,
                plugin.get().getWorldMapService()
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] RenameModalPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open RenameModalPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    // Admin Modal Methods (bypass permission checks)
    // ============================================================

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
                factionManager.get(),
                this,
                faction,
                plugin.get().getWorldMapService(),
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
                factionManager.get(),
                this,
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
                factionManager.get(),
                this,
                faction,
                plugin.get().getWorldMapService(),
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
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] DisbandConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open DisbandConfirmPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction,
                targetUuid,
                targetName
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] TransferConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TransferConfirmPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                claimManager.get(),
                powerManager.get(),
                teleportManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionDashboardPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionDashboardPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] LeaveConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open LeaveConfirmPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] LeaderLeaveConfirmPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open LeaderLeaveConfirmPage: %s", e.getMessage());
            e.printStackTrace();
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
            if (chatManagerSupplier == null || chatHistoryManagerSupplier == null) {
                Logger.warn("[GUI] Chat managers not initialized, cannot open chat page");
                return;
            }
            PageManager pageManager = player.getPageManager();
            FactionChatPage page = new FactionChatPage(
                playerRef,
                factionManager.get(),
                chatManagerSupplier.get(),
                chatHistoryManagerSupplier.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionChatPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionChatPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionModulesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionModulesPage: %s", e.getMessage());
            e.printStackTrace();
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
                return;
            }
            PageManager pageManager = player.getPageManager();
            TreasuryPage page = new TreasuryPage(
                playerRef,
                factionManager.get(),
                econ,
                this,
                plugin.get(),
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] TreasuryPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TreasuryPage: %s", e.getMessage());
            e.printStackTrace();
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
                return;
            }
            var page = new TreasuryDepositModalPage(playerRef, factionManager.get(), econ,
                    this, plugin.get(), faction, mode);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TreasuryDepositModal: %s", e.getMessage());
            e.printStackTrace();
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
                return;
            }
            var page = new TreasuryTransferSearchPage(playerRef, factionManager.get(), econ,
                    this, plugin.get(), faction);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TreasuryTransferSearch: %s", e.getMessage());
            e.printStackTrace();
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
                return;
            }
            var page = new TreasuryTransferConfirmPage(playerRef, factionManager.get(), econ,
                    this, plugin.get(), faction, targetId, targetName, targetType);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TreasuryTransferConfirm: %s", e.getMessage());
            e.printStackTrace();
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Treasury is not available."));
                return;
            }
            var page = new TreasurySettingsPage(playerRef, factionManager.get(), econ, this, faction);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open TreasurySettings: %s", e.getMessage());
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
                plugin.get(),
                factionManager.get(),
                powerManager.get(),
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] AdminDashboardPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open AdminDashboardPage: %s", e.getMessage());
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
                factionManager.get(),
                powerManager.get(),
                this
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
                factionManager.get(),
                powerManager.get(),
                plugin.get().getPlayerStorage(),
                this
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
                factionManager.get(),
                this,
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
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                plugin.get().getEconomyManager(),
                this
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Economy system is not enabled."));
                return;
            }
            PageManager pageManager = player.getPageManager();
            AdminEconomyPage page = new AdminEconomyPage(
                playerRef,
                factionManager.get(),
                econ,
                this
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
            EconomyManager econ = plugin.get().getEconomyManager();
            if (econ == null) {
                player.sendMessage(com.hyperfactions.util.MessageUtil.errorText("Economy system is not enabled."));
                return;
            }
            PageManager pageManager = player.getPageManager();
            AdminEconomyAdjustPage page = new AdminEconomyAdjustPage(
                playerRef,
                factionManager.get(),
                econ,
                this,
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
                factionManager.get(),
                powerManager.get(),
                this
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
     * @param factionId         The faction ID for back navigation
     */
    public void openAdminPlayerInfo(Player player, Ref<EntityStore> ref,
                                    Store<EntityStore> store, PlayerRef playerRef,
                                    UUID targetPlayerUuid, String targetPlayerName,
                                    UUID factionId) {
        Logger.debug("[GUI] Opening AdminPlayerInfoPage for %s (target: %s)", playerRef.getUsername(), targetPlayerName);
        try {
            PageManager pageManager = player.getPageManager();
            AdminPlayerInfoPage page = new AdminPlayerInfoPage(
                playerRef,
                targetPlayerUuid,
                targetPlayerName,
                factionId,
                factionManager.get(),
                powerManager.get(),
                this
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
                factionManager.get(),
                relationManager.get(),
                this
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
                factionManager.get(),
                this
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
                claimManager.get(),
                this,
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

    public void openAdminZone(Player player, Ref<EntityStore> ref,
                              Store<EntityStore> store, PlayerRef playerRef,
                              String tab, int page) {
        Logger.debug("[GUI] Opening AdminZonePage for %s (tab=%s, page=%d)", playerRef.getUsername(), tab, page);
        try {
            PageManager pageManager = player.getPageManager();
            AdminZonePage zonePage = new AdminZonePage(
                playerRef,
                zoneManager.get(),
                this,
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
            AdminConfigPage page = new AdminConfigPage(playerRef, this);
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
            AdminBackupsPage page = new AdminBackupsPage(playerRef, this);
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] AdminBackupsPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open AdminBackupsPage: %s", e.getMessage());
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
            AdminUpdatesPage page = new AdminUpdatesPage(playerRef, this);
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
            AdminHelpPage page = new AdminHelpPage(playerRef, this);
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] AdminHelpPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open AdminHelpPage: %s", e.getMessage());
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
                zoneManager.get(),
                this,
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
                zoneManager.get(),
                claimManager.get(),
                this,
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
     * Opens the Admin Zone Settings page for configuring zone flags.
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
                zoneManager.get(),
                this
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
            var gs = plugin.get().getProtectionChecker().getGravestoneIntegration();
            AdminZoneIntegrationFlagsPage page = new AdminZoneIntegrationFlagsPage(
                playerRef,
                zoneId,
                zoneManager.get(),
                this,
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
                zoneManager.get(),
                this,
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
        Logger.debug("[GUI] Opening ZoneChangeTypeModalPage for %s (zone: %s)", playerRef.getUsername(), zoneId);
        try {
            PageManager pageManager = player.getPageManager();
            ZoneChangeTypeModalPage page = new ZoneChangeTypeModalPage(
                playerRef,
                zoneManager.get(),
                this,
                zoneId,
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
                factionManager.get(),
                this,
                faction
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] LogsViewerPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open LogsViewerPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                plugin.get().getEconomyManager(),
                this,
                sourcePage
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionInfoPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionInfoPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                powerManager.get(),
                relationManager.get(),
                plugin.get().getEconomyManager(),
                this,
                "player_info",
                sourcePlayerUuid,
                sourcePlayerName,
                playerInfoSourcePage
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] FactionInfoPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open FactionInfoPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                powerManager.get(),
                plugin.get().getPlayerStorage(),
                this,
                sourcePage
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] PlayerInfoPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open PlayerInfoPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // NEW PLAYER GUI METHODS
    // =========================================================================

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
                factionManager.get(),
                powerManager.get(),
                inviteManager.get(),
                this,
                page,
                sortBy,
                searchQuery
            );
            pageManager.openCustomPage(ref, store, browsePage);
            Logger.debug("[GUI] NewPlayerBrowsePage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open NewPlayerBrowsePage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] CreateFactionPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open CreateFactionPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                powerManager.get(),
                inviteManager.get(),
                joinRequestManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] InvitesPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open InvitesPage: %s", e.getMessage());
            e.printStackTrace();
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
                factionManager.get(),
                claimManager.get(),
                zoneManager.get(),
                this
            );
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] NewPlayerMapPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open NewPlayerMapPage: %s", e.getMessage());
            e.printStackTrace();
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
            HelpMainPage page = new HelpMainPage(playerRef, this, factionManager.get(), category);
            pageManager.openCustomPage(ref, store, page);
            Logger.debug("[GUI] HelpMainPage opened successfully");
        } catch (Exception e) {
            Logger.severe("[GUI] Failed to open HelpMainPage: %s", e.getMessage());
            e.printStackTrace();
        }
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

    public Supplier<HyperFactions> getPlugin() {
        return plugin;
    }

    public Supplier<FactionManager> getFactionManager() {
        return factionManager;
    }

    public Supplier<ClaimManager> getClaimManager() {
        return claimManager;
    }

    public Supplier<PowerManager> getPowerManager() {
        return powerManager;
    }

    public Supplier<RelationManager> getRelationManager() {
        return relationManager;
    }

    public Supplier<ZoneManager> getZoneManager() {
        return zoneManager;
    }

    public Supplier<TeleportManager> getTeleportManager() {
        return teleportManager;
    }

    public Supplier<InviteManager> getInviteManager() {
        return inviteManager;
    }

    public JoinRequestManager getJoinRequestManager() {
        return joinRequestManager.get();
    }

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
            Logger.severe("[GUI] Failed to open ButtonTestPage: %s", e.getMessage());
            e.printStackTrace();
        }
    }
}
