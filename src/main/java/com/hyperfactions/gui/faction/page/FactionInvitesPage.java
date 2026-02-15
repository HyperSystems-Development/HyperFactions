package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.nav.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.*;

/**
 * Faction Invites Management page - manages outgoing invites and incoming join requests.
 * Uses tab-based filtering and expandable entries like AdminZonePage.
 * Only visible to officers and above.
 */
public class FactionInvitesPage extends InteractiveCustomUIPage<FactionPageData> implements RefreshablePage {

    private static final String PAGE_ID = "invites";
    private static final int ITEMS_PER_PAGE = 8;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final InviteManager inviteManager;
    private final JoinRequestManager joinRequestManager;
    private final GuiManager guiManager;
    private final HyperFactions plugin;
    private final Faction faction;

    private Tab currentTab = Tab.OUTGOING;
    private int currentPage = 0;
    private Set<String> expandedItems = new HashSet<>();

    private enum Tab {
        OUTGOING,
        REQUESTS
    }

    public FactionInvitesPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              InviteManager inviteManager,
                              JoinRequestManager joinRequestManager,
                              GuiManager guiManager,
                              HyperFactions plugin,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.inviteManager = inviteManager;
        this.joinRequestManager = joinRequestManager;
        this.guiManager = guiManager;
        this.plugin = plugin;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main template
        cmd.append("HyperFactions/faction/faction_invites.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), PAGE_ID, faction.id(), this);
        }

        // Build the list
        buildList(cmd, events);
    }

    private void buildList(UICommandBuilder cmd, UIEventBuilder events) {
        // Tab buttons - active tab gets cyan text style and is disabled
        cmd.set("#TabOutgoing.Style", Value.ref("HyperFactions/shared/styles.ui",
                currentTab == Tab.OUTGOING ? "CyanButtonStyle" : "ButtonStyle"));
        cmd.set("#TabRequests.Style", Value.ref("HyperFactions/shared/styles.ui",
                currentTab == Tab.REQUESTS ? "CyanButtonStyle" : "ButtonStyle"));
        cmd.set("#TabOutgoing.Disabled", currentTab == Tab.OUTGOING);
        cmd.set("#TabRequests.Disabled", currentTab == Tab.REQUESTS);

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabOutgoing",
                EventData.of("Button", "Tab").append("Tab", "OUTGOING"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabRequests",
                EventData.of("Button", "Tab").append("Tab", "REQUESTS"),
                false
        );

        // Get items based on current tab
        List<InviteItem> items = currentTab == Tab.OUTGOING
                ? getOutgoingInvites()
                : getJoinRequests();

        // Count
        String countText = items.size() + (currentTab == Tab.OUTGOING ? " invites" : " requests");
        cmd.set("#ItemCount.Text", countText);

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * ITEMS_PER_PAGE;

        // Clear list, create IndexCards container
        cmd.clear("#ItemList");
        cmd.appendInline("#ItemList", "Group #IndexCards { LayoutMode: Top; }");

        // Build entries
        int i = 0;
        for (int idx = startIdx; idx < Math.min(startIdx + ITEMS_PER_PAGE, items.size()); idx++) {
            InviteItem item = items.get(idx);
            buildEntry(cmd, events, i, item);
            i++;
        }

        // Show empty message if no items
        if (items.isEmpty()) {
            cmd.clear("#ItemList");
            cmd.appendInline("#ItemList", "Group { LayoutMode: Top; Padding: (Top: 20); " +
                    "Label #EmptyText { Text: \"" + getEmptyMessage() + "\"; " +
                    "Style: (FontSize: 12, TextColor: #666666, HorizontalAlignment: Center); } }");
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

    private List<InviteItem> getOutgoingInvites() {
        List<InviteItem> items = new ArrayList<>();
        Set<UUID> invitedPlayers = inviteManager.getFactionInvites(faction.id());

        for (UUID playerUuid : invitedPlayers) {
            PendingInvite invite = inviteManager.getInvite(faction.id(), playerUuid);
            if (invite == null || invite.isExpired()) {
                continue;
            }

            String playerName = getPlayerName(playerUuid);
            String inviterName = getPlayerName(invite.invitedBy());

            items.add(new InviteItem(
                    playerUuid.toString(),
                    playerName,
                    true,
                    "Invited by: " + inviterName,
                    null,
                    invite.getRemainingSeconds()
            ));
        }

        // Sort by remaining time (expiring soonest first)
        items.sort(Comparator.comparingInt(InviteItem::remainingSeconds));
        return items;
    }

    private List<InviteItem> getJoinRequests() {
        List<InviteItem> items = new ArrayList<>();
        List<JoinRequest> requests = joinRequestManager.getFactionRequests(faction.id());

        for (JoinRequest request : requests) {
            String message = request.message();
            if (message == null || message.isBlank()) {
                message = "No message";
            } else if (message.length() > 50) {
                message = message.substring(0, 47) + "...";
            }

            items.add(new InviteItem(
                    request.playerUuid().toString(),
                    request.playerName(),
                    false,
                    null,
                    message,
                    request.getRemainingHours() * 3600 // Convert to seconds for consistency
            ));
        }

        // Sort by remaining time (expiring soonest first)
        items.sort(Comparator.comparingInt(InviteItem::remainingSeconds));
        return items;
    }

    private void buildEntry(UICommandBuilder cmd, UIEventBuilder events, int index, InviteItem item) {
        boolean isExpanded = expandedItems.contains(item.id);

        // Append entry template
        cmd.append("#IndexCards", "HyperFactions/faction/faction_invite_entry.ui");

        String idx = "#IndexCards[" + index + "]";

        // Basic info
        cmd.set(idx + " #PlayerName.Text", item.playerName);
        cmd.set(idx + " #StatusInfo.Text", "Expires: " + formatTime(item.remainingSeconds));

        // Type badge
        if (item.isOutgoing) {
            cmd.set(idx + " #TypeLabel.Text", "Outgoing");
            cmd.set(idx + " #TypeLabel.Style.TextColor", "#55FFFF");
        } else {
            cmd.set(idx + " #TypeLabel.Text", "Request");
            cmd.set(idx + " #TypeLabel.Style.TextColor", "#FFAA00");
        }

        // Expansion state
        cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
        cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
        cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

        // Bind header click
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("PlayerUuid", item.id),
                false
        );

        // Extended info (only set if expanded)
        if (isExpanded) {
            if (item.isOutgoing) {
                // Outgoing invite - show inviter info
                cmd.set(idx + " #InfoLabel.Text", "Invited by:");
                cmd.set(idx + " #InfoValue.Text", item.inviterInfo);
                cmd.set(idx + " #MessageRow.Visible", false);

                // Show cancel button, hide accept/decline
                cmd.set(idx + " #CancelBtn.Visible", true);
                cmd.set(idx + " #AcceptBtn.Visible", false);
                cmd.set(idx + " #DeclineBtn.Visible", false);

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #CancelBtn",
                        EventData.of("Button", "CancelInvite")
                                .append("PlayerUuid", item.id),
                        false
                );
            } else {
                // Join request - show message
                cmd.set(idx + " #InfoRow.Visible", false);
                cmd.set(idx + " #MessageRow.Visible", true);
                cmd.set(idx + " #MessageValue.Text", "\"" + item.message + "\"");

                // Show accept/decline buttons, hide cancel
                cmd.set(idx + " #CancelBtn.Visible", false);
                cmd.set(idx + " #AcceptBtn.Visible", true);
                cmd.set(idx + " #DeclineBtn.Visible", true);

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #AcceptBtn",
                        EventData.of("Button", "AcceptRequest")
                                .append("PlayerUuid", item.id),
                        false
                );
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #DeclineBtn",
                        EventData.of("Button", "DeclineRequest")
                                .append("PlayerUuid", item.id),
                        false
                );
            }
        }
    }

    private String getEmptyMessage() {
        if (currentTab == Tab.OUTGOING) {
            return "No outgoing invites. Use /f invite <player> to invite someone.";
        } else {
            return "No join requests. Players can request to join with /f request.";
        }
    }

    private String getPlayerName(UUID playerUuid) {
        // Check faction members across all factions
        for (Faction f : factionManager.getAllFactions()) {
            FactionMember member = f.getMember(playerUuid);
            if (member != null) {
                return member.username();
            }
        }
        return "Unknown";
    }

    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            return (seconds / 60) + "m";
        } else {
            return (seconds / 3600) + "h";
        }
    }

    private record InviteItem(String id, String playerName, boolean isOutgoing,
                              String inviterInfo, String message, int remainingSeconds) {}

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

        Faction freshFaction = factionManager.getFaction(faction.id());

        // Handle navigation
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, freshFaction, guiManager)) {
            return;
        }

        switch (data.button) {
            case "Tab" -> {
                if (data.tab != null) {
                    currentTab = "REQUESTS".equals(data.tab) ? Tab.REQUESTS : Tab.OUTGOING;
                    currentPage = 0;
                    expandedItems.clear();
                    rebuildList();
                }
            }

            case "ToggleExpanded" -> {
                if (data.playerUuid != null) {
                    if (expandedItems.contains(data.playerUuid)) {
                        expandedItems.remove(data.playerUuid);
                    } else {
                        expandedItems.add(data.playerUuid);
                    }
                    rebuildList();
                }
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                expandedItems.clear();
                rebuildList();
            }

            case "NextPage" -> {
                currentPage = data.page;
                expandedItems.clear();
                rebuildList();
            }

            case "CancelInvite" -> handleCancelInvite(player, data);

            case "AcceptRequest" -> handleAcceptRequest(player, ref, store, playerRef, data);

            case "DeclineRequest" -> handleDeclineRequest(player, data);

            default -> sendUpdate();
        }
    }

    private void handleCancelInvite(Player player, FactionPageData data) {
        if (data.playerUuid == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            inviteManager.removeInvite(faction.id(), targetUuid);

            String playerName = getPlayerName(targetUuid);
            player.sendMessage(Message.raw("Cancelled invite to " + playerName + ".").color("#AAAAAA"));

            expandedItems.remove(data.playerUuid);
            rebuildList();
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleAcceptRequest(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                     PlayerRef playerRef, FactionPageData data) {
        if (data.playerUuid == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            JoinRequest request = joinRequestManager.acceptRequest(faction.id(), targetUuid);

            if (request != null) {
                // Add player to faction
                FactionManager.FactionResult result = factionManager.addMember(
                        faction.id(), targetUuid, request.playerName()
                );

                if (result == FactionManager.FactionResult.SUCCESS) {
                    // Clear player's other requests since they joined a faction
                    joinRequestManager.clearPlayerRequests(targetUuid);
                    player.sendMessage(Message.raw(request.playerName() + " has joined the faction!").color("#55FF55"));
                } else if (result == FactionManager.FactionResult.FACTION_FULL) {
                    player.sendMessage(Message.raw("Faction is full. Cannot accept request.").color("#FF5555"));
                } else {
                    player.sendMessage(Message.raw("Failed to add player to faction.").color("#FF5555"));
                }
            } else {
                player.sendMessage(Message.raw("Request not found or expired.").color("#FF5555"));
            }

            expandedItems.remove(data.playerUuid);
            rebuildList();
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
            sendUpdate();
        }
    }

    private void handleDeclineRequest(Player player, FactionPageData data) {
        if (data.playerUuid == null) {
            sendUpdate();
            return;
        }

        try {
            UUID targetUuid = UUID.fromString(data.playerUuid);
            JoinRequest request = joinRequestManager.getRequest(faction.id(), targetUuid);
            String playerName = request != null ? request.playerName() : "Unknown";

            joinRequestManager.declineRequest(faction.id(), targetUuid);

            player.sendMessage(Message.raw("Declined join request from " + playerName + ".").color("#AAAAAA"));

            expandedItems.remove(data.playerUuid);
            rebuildList();
        } catch (IllegalArgumentException e) {
            player.sendMessage(Message.raw("Invalid player.").color("#FF5555"));
            sendUpdate();
        }
    }

    @Override
    public void refreshContent() {
        rebuildList();
    }

    private void rebuildList() {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildList(cmd, events);

        sendUpdate(cmd, events, false);
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        super.onDismiss(ref, store);
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.unregister(playerRef.getUuid());
        }
    }
}
