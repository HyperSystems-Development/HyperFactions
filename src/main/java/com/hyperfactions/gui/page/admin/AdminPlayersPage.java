package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminPlayersData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin Players page - browse ALL known players on the server.
 * Follows AdminFactionsPage gold standard pattern (700x500, Container, IndexCards).
 */
public class AdminPlayersPage extends InteractiveCustomUIPage<AdminPlayersData> {

    private static final int PLAYERS_PER_PAGE = 8;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault());

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final PlayerStorage playerStorage;
    private final GuiManager guiManager;

    private Set<UUID> expandedPlayers = new HashSet<>();
    private SortMode sortMode = SortMode.NAME;
    private String searchQuery = "";
    private int currentPage = 0;

    /** Cached player info records — loaded once on page open */
    private List<PlayerInfo> cachedPlayers;

    private enum SortMode {
        NAME,
        POWER,
        LAST_ONLINE,
        FACTION,
        ONLINE
    }

    /** Lightweight record for display in the player list */
    private record PlayerInfo(
            UUID uuid,
            String name,
            double power,
            double maxPower,
            String factionName,
            String factionRole,
            long lastOnline,
            long firstJoined,
            int kills,
            int deaths,
            boolean isOnline
    ) {}

    public AdminPlayersPage(PlayerRef playerRef,
                            FactionManager factionManager,
                            PowerManager powerManager,
                            PlayerStorage playerStorage,
                            GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminPlayersData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.playerStorage = playerStorage;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/admin/admin_players.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "players", cmd, events);

        // Load player data (synchronous for initial build)
        loadPlayerCache();

        // Build player list
        buildPlayerList(cmd, events);
    }

    private void loadPlayerCache() {
        cachedPlayers = new ArrayList<>();

        // Get all known player UUIDs from all factions + power manager
        Set<UUID> allUuids = new HashSet<>();

        // Collect from all factions
        for (Faction faction : factionManager.getAllFactions()) {
            allUuids.addAll(faction.members().keySet());
        }

        // Also get from player storage (catches factionless players)
        try {
            Set<UUID> storedUuids = playerStorage.getAllPlayerUuids().join();
            allUuids.addAll(storedUuids);
        } catch (Exception e) {
            // Continue with faction-sourced UUIDs
        }

        // Build info records
        for (UUID uuid : allUuids) {
            PlayerPower power = powerManager.getPlayerPower(uuid);
            PlayerRef onlineRef = Universe.get().getPlayer(uuid);
            boolean isOnline = onlineRef != null && onlineRef.isValid();

            // Find faction membership
            String factionName = null;
            String factionRole = null;
            for (Faction faction : factionManager.getAllFactions()) {
                FactionMember member = faction.getMember(uuid);
                if (member != null) {
                    factionName = faction.name();
                    factionRole = formatRole(member.role());
                    break;
                }
            }

            // Get player name — from online player, power cache, or faction member
            String name = null;
            if (isOnline) {
                name = onlineRef.getUsername();
            }
            if (name == null) {
                // Try to get from faction membership
                for (Faction faction : factionManager.getAllFactions()) {
                    FactionMember member = faction.getMember(uuid);
                    if (member != null) {
                        name = member.username();
                        break;
                    }
                }
            }
            if (name == null) {
                // Try cached username from PlayerData
                try {
                    var playerData = playerStorage.loadPlayerData(uuid).join();
                    if (playerData.isPresent() && playerData.get().getUsername() != null) {
                        name = playerData.get().getUsername();
                    }
                } catch (Exception ignored) {}
            }
            if (name == null) {
                name = uuid.toString().substring(0, 8) + "...";
            }

            // Get extended data
            long lastOnline = 0;
            long firstJoined = 0;
            int kills = 0;
            int deaths = 0;
            try {
                var playerData = playerStorage.loadPlayerData(uuid).join();
                if (playerData.isPresent()) {
                    PlayerData pd = playerData.get();
                    lastOnline = pd.getLastOnline();
                    firstJoined = pd.getFirstJoined();
                    kills = pd.getKills();
                    deaths = pd.getDeaths();
                }
            } catch (Exception ignored) {}

            cachedPlayers.add(new PlayerInfo(
                    uuid, name, power.power(), power.getEffectiveMaxPower(),
                    factionName, factionRole, lastOnline, firstJoined,
                    kills, deaths, isOnline
            ));
        }
    }

    private void buildPlayerList(UICommandBuilder cmd, UIEventBuilder events) {
        List<PlayerInfo> filtered = getFilteredSortedPlayers();

        // Count display
        if (searchQuery.isEmpty()) {
            cmd.set("#PlayerCount.Text", filtered.size() + " players");
        } else {
            cmd.set("#PlayerCount.Text", filtered.size() + " found");
        }

        // Sort dropdown
        cmd.set("#SortDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
                new DropdownEntryInfo(LocalizableString.fromString("Power"), "POWER"),
                new DropdownEntryInfo(LocalizableString.fromString("Last Online"), "LAST_ONLINE"),
                new DropdownEntryInfo(LocalizableString.fromString("Faction"), "FACTION"),
                new DropdownEntryInfo(LocalizableString.fromString("Online"), "ONLINE")
        ));
        cmd.set("#SortDropdown.Value", sortMode.name());
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SortDropdown",
                EventData.of("Button", "SortChanged")
                        .append("@SortMode", "#SortDropdown.Value"),
                false
        );

        // Search field
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SearchInput",
                EventData.of("Button", "SearchChanged")
                        .append("@SearchQuery", "#SearchInput.Value"),
                false
        );

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) filtered.size() / PLAYERS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * PLAYERS_PER_PAGE;

        // Clear and rebuild list
        cmd.clear("#PlayerList");
        cmd.appendInline("#PlayerList", "Group #IndexCards { LayoutMode: Top; }");

        // Build paginated entries
        int i = 0;
        for (int idx = startIdx; idx < Math.min(startIdx + PLAYERS_PER_PAGE, filtered.size()); idx++) {
            buildPlayerEntry(cmd, events, i, filtered.get(idx));
            i++;
        }

        // Pagination
        cmd.set("#PageInfo.Text", (currentPage + 1) + "/" + totalPages);

        if (currentPage > 0) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PrevBtn",
                    EventData.of("Button", "PrevPage")
                            .append("Page", String.valueOf(currentPage - 1)),
                    false
            );
        }

        if (currentPage < totalPages - 1) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextBtn",
                    EventData.of("Button", "NextPage")
                            .append("Page", String.valueOf(currentPage + 1)),
                    false
            );
        }
    }

    private void buildPlayerEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                                   PlayerInfo info) {
        boolean isExpanded = expandedPlayers.contains(info.uuid());

        cmd.append("#IndexCards", "HyperFactions/admin/admin_player_entry.ui");

        String idx = "#IndexCards[" + index + "]";

        // Online indicator bar
        cmd.set(idx + " #OnlineBar.Background.Color", info.isOnline() ? "#55FF55" : "#555555");

        // Player name
        cmd.set(idx + " #PlayerName.Text", info.name());
        cmd.set(idx + " #PlayerName.Style.TextColor", info.isOnline() ? "#00FFFF" : "#CCCCCC");

        // Online status
        cmd.set(idx + " #OnlineStatus.Text", info.isOnline() ? "Online" : "Offline");
        cmd.set(idx + " #OnlineStatus.Style.TextColor", info.isOnline() ? "#55FF55" : "#888888");

        // Faction name
        if (info.factionName() != null) {
            cmd.set(idx + " #FactionName.Text", info.factionName());
            cmd.set(idx + " #FactionName.Style.TextColor", "#AAAAAA");
        } else {
            cmd.set(idx + " #FactionName.Text", "No Faction");
            cmd.set(idx + " #FactionName.Style.TextColor", "#666666");
        }

        // Power display
        cmd.set(idx + " #PowerDisplay.Text", String.format("%.0f/%.0f", info.power(), info.maxPower()));
        int powerPercent = info.maxPower() > 0 ? (int) (info.power() / info.maxPower() * 100) : 0;
        String powerColor = powerPercent >= 80 ? "#55FF55" : powerPercent >= 40 ? "#FFAA00" : "#FF5555";
        cmd.set(idx + " #PowerDisplay.Style.TextColor", powerColor);

        // Expansion state
        cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
        cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
        cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

        // Bind header for expansion toggle
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("PlayerUuid", info.uuid().toString()),
                false
        );

        // Extended info
        if (isExpanded) {
            // Role
            cmd.set(idx + " #RoleValue.Text", info.factionRole() != null ? info.factionRole() : "N/A");

            // First joined
            String joinedDate = info.firstJoined() > 0
                    ? DATE_FORMAT.format(Instant.ofEpochMilli(info.firstJoined()))
                    : "Unknown";
            cmd.set(idx + " #JoinedDate.Text", joinedDate);

            // Last online
            String lastOnlineText;
            if (info.isOnline()) {
                lastOnlineText = "Now";
            } else if (info.lastOnline() > 0) {
                lastOnlineText = TimeUtil.formatDuration(System.currentTimeMillis() - info.lastOnline()) + " ago";
            } else {
                lastOnlineText = "Unknown";
            }
            cmd.set(idx + " #LastOnline.Text", lastOnlineText);

            // Combat stats
            double kdr = info.deaths() > 0 ? (double) info.kills() / info.deaths() : info.kills();
            cmd.set(idx + " #KdrValue.Text", info.kills() + "/" + info.deaths() + "/" + String.format("%.2f", kdr));

            // Power detail
            cmd.set(idx + " #PowerDetail.Text", String.format("%.1f/%.1f", info.power(), info.maxPower()));
            cmd.set(idx + " #PowerDetail.Style.TextColor", powerColor);

            // UUID
            cmd.set(idx + " #UuidValue.Text", info.uuid().toString());

            // Action buttons
            cmd.set(idx + " #ViewInfoBtn.Visible", true);
            cmd.set(idx + " #TeleportBtn.Visible", info.isOnline());

            // View Info button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    idx + " #ViewInfoBtn",
                    EventData.of("Button", "ViewPlayerInfo")
                            .append("PlayerUuid", info.uuid().toString())
                            .append("PlayerName", info.name()),
                    false
            );

            // Teleport button (only for online players)
            if (info.isOnline()) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #TeleportBtn",
                        EventData.of("Button", "Teleport")
                                .append("PlayerUuid", info.uuid().toString())
                                .append("PlayerName", info.name()),
                        false
                );
            }
        }
    }

    private List<PlayerInfo> getFilteredSortedPlayers() {
        if (cachedPlayers == null) return List.of();

        return cachedPlayers.stream()
                .filter(p -> searchQuery.isEmpty() ||
                        p.name().toLowerCase().contains(searchQuery.toLowerCase()))
                .sorted(getSortComparator())
                .toList();
    }

    private Comparator<PlayerInfo> getSortComparator() {
        return switch (sortMode) {
            case POWER -> Comparator.<PlayerInfo>comparingDouble(PlayerInfo::power).reversed()
                    .thenComparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);
            case LAST_ONLINE -> Comparator
                    .<PlayerInfo>comparingLong(p -> p.isOnline() ? Long.MAX_VALUE : p.lastOnline())
                    .reversed()
                    .thenComparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);
            case FACTION -> Comparator
                    .<PlayerInfo, String>comparing(p -> p.factionName() != null ? p.factionName() : "\uFFFF",
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);
            case ONLINE -> Comparator
                    .<PlayerInfo>comparingInt(p -> p.isOnline() ? 0 : 1)
                    .thenComparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(PlayerInfo::name, String.CASE_INSENSITIVE_ORDER); // NAME
        };
    }

    private String formatRole(FactionRole role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            case MEMBER -> "Member";
        };
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                AdminPlayersData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            sendUpdate();
            return;
        }

        // Handle admin nav bar navigation
        if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
            return;
        }

        if (data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "ToggleExpanded" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID uuid = UUID.fromString(data.playerUuid);
                        if (expandedPlayers.contains(uuid)) {
                            expandedPlayers.remove(uuid);
                        } else {
                            expandedPlayers.add(uuid);
                        }
                        rebuildList();
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "SortChanged" -> {
                try {
                    if (data.sortMode != null) {
                        sortMode = SortMode.valueOf(data.sortMode);
                    }
                } catch (IllegalArgumentException ignored) {}
                currentPage = 0;
                expandedPlayers.clear();
                rebuildList();
            }

            case "SearchChanged" -> {
                searchQuery = data.searchQuery != null ? data.searchQuery : "";
                currentPage = 0;
                expandedPlayers.clear();
                rebuildList();
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                expandedPlayers.clear();
                rebuildList();
            }

            case "NextPage" -> {
                currentPage = data.page;
                expandedPlayers.clear();
                rebuildList();
            }

            case "ViewPlayerInfo" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.playerUuid);
                        String targetName = data.playerName != null ? data.playerName : "Unknown";
                        // Find the player's faction for context
                        UUID factionId = null;
                        for (Faction faction : factionManager.getAllFactions()) {
                            if (faction.getMember(targetUuid) != null) {
                                factionId = faction.id();
                                break;
                            }
                        }
                        guiManager.openAdminPlayerInfo(player, ref, store, playerRef, targetUuid, targetName, factionId);
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "Teleport" -> {
                if (data.playerUuid != null) {
                    try {
                        UUID targetUuid = UUID.fromString(data.playerUuid);
                        PlayerRef targetPlayer = Universe.get().getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isValid()) {
                            guiManager.closePage(player, ref, store);
                            var targetWorld = Universe.get().getWorld(targetPlayer.getWorldUuid());
                            if (targetWorld == null) {
                                player.sendMessage(Message.raw("Target world not found.").color("#FF5555"));
                                return;
                            }
                            var targetTransform = targetPlayer.getTransform();
                            var targetPos = targetTransform.getPosition();
                            var targetRot = targetTransform.getRotation();
                            targetWorld.execute(() -> {
                                var teleport = Teleport.createForPlayer(
                                        targetWorld, targetPos, targetRot);
                                store.addComponent(ref, Teleport.getComponentType(), teleport);
                            });
                            player.sendMessage(Message.raw("[Admin] Teleported to ").color("#55FF55")
                                    .insert(Message.raw(data.playerName != null ? data.playerName : "player").color("#00FFFF"))
                                    .insert(Message.raw(".").color("#55FF55")));
                        } else {
                            player.sendMessage(Message.raw("Player is not online.").color("#FF5555"));
                            sendUpdate();
                        }
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            default -> sendUpdate();
        }
    }

    private void rebuildList() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildPlayerList(cmd, events);

        sendUpdate(cmd, events, false);
    }
}
