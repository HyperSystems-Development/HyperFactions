package com.hyperfactions.gui.faction.page;

import com.hyperfactions.Permissions;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.gui.faction.data.FactionMembersData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Faction Members page - displays member list with expandable entries.
 * Uses AdminUI pattern exactly.
 */
public class FactionMembersPage extends InteractiveCustomUIPage<FactionMembersData> implements RefreshablePage {

    private static final String PAGE_ID = "members";
    private static final int ITEMS_PER_PAGE = 8;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault());

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;
    private Faction faction;

    private Set<UUID> expandedMembers = new HashSet<>();
    private SortMode sortMode = SortMode.ROLE;
    private String searchQuery = "";
    private int currentPage = 0;
    private Ref<EntityStore> lastRef;
    private Store<EntityStore> lastStore;

    private enum SortMode {
        ROLE,
        LAST_ONLINE
    }

    public FactionMembersPage(PlayerRef playerRef,
                              FactionManager factionManager,
                              PowerManager powerManager,
                              GuiManager guiManager,
                              Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, FactionMembersData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {
        this.lastRef = ref;
        this.lastStore = store;

        // Load the main template
        cmd.append("HyperFactions/faction/faction_members.ui");

        // Setup navigation bar
        NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

        // Build member list
        buildMemberList(cmd, events);

        // Register with active page tracker for real-time updates
        ActivePageTracker activeTracker = guiManager.getActivePageTracker();
        if (activeTracker != null) {
            activeTracker.register(playerRef.getUuid(), PAGE_ID, faction.id(), this);
        }
    }

    @Override
    public void refreshContent() {
        if (lastRef != null && lastStore != null) {
            rebuildList(lastRef, lastStore);
        }
    }

    private void buildMemberList(UICommandBuilder cmd, UIEventBuilder events) {
        List<FactionMember> allMembers = getFilteredSortedMembers();
        int totalMembers = allMembers.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalMembers / ITEMS_PER_PAGE));

        // Clamp current page
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }
        if (currentPage < 0) {
            currentPage = 0;
        }

        // Get members for current page
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalMembers);
        List<FactionMember> pageMembers = allMembers.subList(startIdx, endIdx);

        cmd.set("#MemberCount.Text", totalMembers + " members");

        // Sort dropdown
        cmd.set("#SortDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Role"), "ROLE"),
                new DropdownEntryInfo(LocalizableString.fromString("Last Online"), "LAST_ONLINE")
        ));
        cmd.set("#SortDropdown.Value", sortMode.name());
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SortDropdown",
                EventData.of("Button", "SortChanged")
                        .append("@SortMode", "#SortDropdown.Value"),
                false
        );

        // Search binding - real-time filtering via ValueChanged
        if (!searchQuery.isEmpty()) {
            cmd.set("#SearchInput.Value", searchQuery);
        }
        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#SearchInput",
                EventData.of("Button", "Search").append("@SearchQuery", "#SearchInput.Value"),
                false
        );

        // Pagination controls
        boolean hasPrev = currentPage > 0;
        boolean hasNext = currentPage < totalPages - 1;

        cmd.set("#PrevBtn.Visible", totalPages > 1);
        cmd.set("#NextBtn.Visible", totalPages > 1);
        cmd.set("#PageIndicator.Visible", totalPages > 1);
        cmd.set("#PrevBtn.Disabled", !hasPrev);
        cmd.set("#NextBtn.Disabled", !hasNext);
        cmd.set("#PageIndicator.Text", (currentPage + 1) + " / " + totalPages);

        if (hasPrev) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#PrevBtn",
                    EventData.of("Button", "PrevPage"),
                    false
            );
        }
        if (hasNext) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#NextBtn",
                    EventData.of("Button", "NextPage"),
                    false
            );
        }

        // Clear MembersList (exists in template), then create IndexCards container inside it
        cmd.clear("#MembersList");
        cmd.appendInline("#MembersList", "Group #IndexCards { LayoutMode: Top; }");

        // Build entries for current page
        int i = 0;
        for (FactionMember member : pageMembers) {
            buildMemberEntry(cmd, events, i, member);
            i++;
        }
    }

    private void buildMemberEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                                   FactionMember member) {
        boolean isExpanded = expandedMembers.contains(member.uuid());
        boolean memberIsOnline = isOnline(member);

        // Append entry template to IndexCards
        cmd.append("#IndexCards", "HyperFactions/faction/member_entry.ui");

        // Use indexed selector like NavBarHelper does
        String idx = "#IndexCards[" + index + "]";

        // Basic info
        cmd.set(idx + " #MemberName.Text", member.username());
        cmd.set(idx + " #MemberRole.Text", formatRole(member.role()));

        // Role indicator color
        cmd.set(idx + " #RoleIndicator.Background.Color", getRoleColor(member.role()));

        // Online status
        if (memberIsOnline) {
            cmd.set(idx + " #OnlineStatus.Text", "Online");
            cmd.set(idx + " #OnlineStatus.Style.TextColor", "#55FF55");
            cmd.set(idx + " #LastOnline.Text", "");
        } else {
            cmd.set(idx + " #OnlineStatus.Text", "Offline");
            cmd.set(idx + " #OnlineStatus.Style.TextColor", "#888888");
            cmd.set(idx + " #LastOnline.Text", formatLastOnline(member.lastOnline()));
        }

        // Expansion state
        cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
        cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
        cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

        // Bind to #Header TextButton inside the indexed element (like NavBarHelper pattern)
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("PlayerUuid", member.uuid().toString()),
                false
        );

        // Extended info
        if (isExpanded) {
            // Power info
            PlayerPower power = powerManager.getPlayerPower(member.uuid());
            String powerText = String.format("%.0f/%.0f", power.power(), power.getEffectiveMaxPower());
            cmd.set(idx + " #PowerValue.Text", powerText);
            // Color based on power percentage
            int powerPercent = power.getPowerPercent();
            String powerColor = powerPercent >= 80 ? "#55FF55" : powerPercent >= 40 ? "#FFAA00" : "#FF5555";
            cmd.set(idx + " #PowerValue.Style.TextColor", powerColor);

            // Joined date
            String joinedDate = member.joinedAt() > 0
                    ? DATE_FORMAT.format(Instant.ofEpochMilli(member.joinedAt()))
                    : "Unknown";
            cmd.set(idx + " #JoinedDate.Text", joinedDate);

            // Last death (relative format)
            String lastDeathText = power.lastDeath() > 0
                    ? TimeUtil.formatDuration(System.currentTimeMillis() - power.lastDeath()) + " ago"
                    : "Never";
            cmd.set(idx + " #LastDeath.Text", lastDeathText);

            // Determine what actions the viewer can take on this member
            FactionMember viewer = faction.members().get(playerRef.getUuid());
            boolean isSelf = member.uuid().equals(playerRef.getUuid());
            boolean viewerIsLeader = viewer != null && viewer.role() == FactionRole.LEADER;
            boolean viewerIsOfficer = viewer != null && viewer.role() == FactionRole.OFFICER;
            boolean targetIsMember = member.role() == FactionRole.MEMBER;
            boolean targetIsOfficer = member.role() == FactionRole.OFFICER;

            // Show "(You)" label for self
            cmd.set(idx + " #SelfLabel.Visible", isSelf);

            // Profile button - always visible when expanded
            cmd.set(idx + " #ProfileBtn.Visible", true);
            cmd.set(idx + " #ProfileSpacer.Visible", true);
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    idx + " #ProfileBtn",
                    EventData.of("Button", "ViewProfile")
                            .append("PlayerUuid", member.uuid().toString())
                            .append("Target", member.username()),
                    false
            );

            // Promote: Leader can promote Members to Officer (requires PROMOTE permission)
            boolean canPromote = viewerIsLeader && targetIsMember && !isSelf
                    && PermissionManager.get().hasPermission(playerRef.getUuid(), Permissions.PROMOTE);
            cmd.set(idx + " #PromoteBtn.Visible", canPromote);
            cmd.set(idx + " #PromoteSpacer.Visible", canPromote);
            if (canPromote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #PromoteBtn",
                        EventData.of("Button", "Promote")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Demote: Leader can demote Officers to Member (requires DEMOTE permission)
            boolean canDemote = viewerIsLeader && targetIsOfficer && !isSelf
                    && PermissionManager.get().hasPermission(playerRef.getUuid(), Permissions.DEMOTE);
            cmd.set(idx + " #DemoteBtn.Visible", canDemote);
            cmd.set(idx + " #DemoteSpacer.Visible", canDemote);
            if (canDemote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #DemoteBtn",
                        EventData.of("Button", "Demote")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Kick: Leader can kick anyone, Officer can kick Members (requires KICK permission)
            boolean canKick = !isSelf && (viewerIsLeader || (viewerIsOfficer && targetIsMember))
                    && PermissionManager.get().hasPermission(playerRef.getUuid(), Permissions.KICK);
            cmd.set(idx + " #KickBtn.Visible", canKick);
            cmd.set(idx + " #KickSpacer.Visible", canKick);
            if (canKick) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #KickBtn",
                        EventData.of("Button", "Kick")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }

            // Transfer: Leader can transfer leadership to anyone else (requires TRANSFER permission)
            boolean canTransfer = viewerIsLeader && !isSelf
                    && PermissionManager.get().hasPermission(playerRef.getUuid(), Permissions.TRANSFER);
            cmd.set(idx + " #TransferBtn.Visible", canTransfer);
            if (canTransfer) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #TransferBtn",
                        EventData.of("Button", "Transfer")
                                .append("PlayerUuid", member.uuid().toString()),
                        false
                );
            }
        }
    }

    private List<FactionMember> getFilteredSortedMembers() {
        var stream = faction.members().values().stream();
        if (!searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            stream = stream.filter(m -> m.username().toLowerCase().contains(query));
        }
        return stream.sorted(getSortComparator()).toList();
    }

    private Comparator<FactionMember> getSortComparator() {
        if (sortMode == SortMode.LAST_ONLINE) {
            return Comparator
                    .<FactionMember>comparingLong(m -> isOnline(m) ? Long.MAX_VALUE : m.lastOnline())
                    .reversed()
                    .thenComparing(FactionMember::username);
        } else {
            return Comparator
                    .<FactionMember>comparingInt(m -> -m.role().getLevel())
                    .thenComparing(FactionMember::username);
        }
    }

    private boolean isOnline(FactionMember member) {
        PlayerRef onlinePlayer = Universe.get().getPlayer(member.uuid());
        return onlinePlayer != null && onlinePlayer.isValid();
    }

    private String formatRole(FactionRole role) {
        return switch (role) {
            case LEADER -> "Leader";
            case OFFICER -> "Officer";
            case MEMBER -> "Member";
        };
    }

    private String getRoleColor(FactionRole role) {
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#00AAFF";
            case MEMBER -> "#888888";
        };
    }

    private String formatLastOnline(long lastOnlineMs) {
        if (lastOnlineMs <= 0) {
            return "";
        }
        long diffMs = System.currentTimeMillis() - lastOnlineMs;
        if (diffMs < 60000) {
            return "just now";
        }
        return TimeUtil.formatDuration(diffMs) + " ago";
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                FactionMembersData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            sendUpdate();
            return;
        }

        // Handle navigation - FactionMembersData now implements NavAwareData
        if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
            return;
        }

        if (data.button == null) {
            sendUpdate();
            return;
        }

        switch (data.button) {
            case "ToggleExpanded" -> {
                if (data.playerUuid != null) {
                    UUID uuid = UuidUtil.parseOrNull(data.playerUuid);
                    if (uuid == null) {
                        sendUpdate();
                        return;
                    }
                    if (expandedMembers.contains(uuid)) {
                        expandedMembers.remove(uuid);
                    } else {
                        expandedMembers.add(uuid);
                    }
                    rebuildList(ref, store);
                }
            }

            case "Search" -> {
                searchQuery = data.searchQuery != null ? data.searchQuery : "";
                currentPage = 0;
                rebuildList(ref, store);
            }

            case "SortChanged" -> {
                try {
                    if (data.sortMode != null) {
                        sortMode = SortMode.valueOf(data.sortMode);
                    }
                } catch (IllegalArgumentException ignored) {}
                currentPage = 0;
                rebuildList(ref, store);
            }

            case "PrevPage" -> {
                if (currentPage > 0) {
                    currentPage--;
                    rebuildList(ref, store);
                }
            }

            case "NextPage" -> {
                currentPage++;
                rebuildList(ref, store);
            }

            case "ViewProfile" -> {
                if (data.playerUuid != null) {
                    UUID uuid = UuidUtil.parseOrNull(data.playerUuid);
                    if (uuid == null) {
                        sendUpdate();
                        return;
                    }
                    String targetName = data.target != null ? data.target : "Unknown";
                    guiManager.openPlayerInfo(player, ref, store, playerRef, uuid, targetName, "members");
                }
            }

            case "Promote" -> handlePromote(player, ref, store, data.playerUuid);
            case "Demote" -> handleDemote(player, ref, store, data.playerUuid);
            case "Kick" -> handleKick(player, ref, store, data.playerUuid);
            case "Transfer" -> handleTransfer(player, ref, store, data.playerUuid);

            default -> sendUpdate();
        }
    }

    private void handlePromote(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        UUID targetUuid = UuidUtil.parseOrNull(targetUuidStr);
        if (targetUuid == null) {
            sendUpdate();
            return;
        }
        FactionMember target = faction.members().get(targetUuid);
        if (target == null) {
            player.sendMessage(MessageUtil.errorText("Member not found."));
            sendUpdate();
            return;
        }

        var result = factionManager.promoteMember(faction.id(), targetUuid, playerRef.getUuid());
        if (result == FactionManager.FactionResult.SUCCESS) {
            player.sendMessage(Message.raw("Promoted " + target.username() + " to Officer.").color("#55FF55"));
        } else {
            player.sendMessage(MessageUtil.errorText("Failed to promote: " + result.name()));
        }
        rebuildList(ref, store);
    }

    private void handleDemote(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        UUID targetUuid = UuidUtil.parseOrNull(targetUuidStr);
        if (targetUuid == null) {
            sendUpdate();
            return;
        }
        FactionMember target = faction.members().get(targetUuid);
        if (target == null) {
            player.sendMessage(MessageUtil.errorText("Member not found."));
            sendUpdate();
            return;
        }

        var result = factionManager.demoteMember(faction.id(), targetUuid, playerRef.getUuid());
        if (result == FactionManager.FactionResult.SUCCESS) {
            player.sendMessage(Message.raw("Demoted " + target.username() + " to Member.").color("#55FF55"));
        } else {
            player.sendMessage(MessageUtil.errorText("Failed to demote: " + result.name()));
        }
        rebuildList(ref, store);
    }

    private void handleKick(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        UUID targetUuid = UuidUtil.parseOrNull(targetUuidStr);
        if (targetUuid == null) {
            sendUpdate();
            return;
        }
        FactionMember target = faction.members().get(targetUuid);
        if (target == null) {
            player.sendMessage(MessageUtil.errorText("Member not found."));
            sendUpdate();
            return;
        }

        var result = factionManager.removeMember(faction.id(), targetUuid, playerRef.getUuid(), true);
        if (result == FactionManager.FactionResult.SUCCESS) {
            player.sendMessage(Message.raw("Kicked " + target.username() + " from the faction.").color("#55FF55"));
        } else {
            player.sendMessage(MessageUtil.errorText("Failed to kick: " + result.name()));
        }
        rebuildList(ref, store);
    }

    private void handleTransfer(Player player, Ref<EntityStore> ref, Store<EntityStore> store, String targetUuidStr) {
        if (targetUuidStr == null) {
            sendUpdate();
            return;
        }

        UUID targetUuid = UuidUtil.parseOrNull(targetUuidStr);
        if (targetUuid == null) {
            sendUpdate();
            return;
        }
        FactionMember target = faction.members().get(targetUuid);
        if (target == null) {
            player.sendMessage(MessageUtil.errorText("Member not found."));
            sendUpdate();
            return;
        }

        // Open confirmation modal instead of directly transferring
        guiManager.openTransferConfirm(player, ref, store, playerRef, faction, targetUuid, target.username());
    }

    private void rebuildList(Ref<EntityStore> ref, Store<EntityStore> store) {
        Faction freshFaction = factionManager.getFaction(faction.id());
        if (freshFaction != null) {
            this.faction = freshFaction;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildMemberList(cmd, events);

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
