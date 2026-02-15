package com.hyperfactions.gui.page.admin;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionMembersData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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
 * Admin Faction Members page - displays member list with search, sort, and pagination.
 * Follows AdminFactionsPage gold standard pattern (700x500, Container, IndexCards).
 */
public class AdminFactionMembersPage extends InteractiveCustomUIPage<AdminFactionMembersData> {

    private static final int MEMBERS_PER_PAGE = 8;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(ZoneId.systemDefault());

    private final PlayerRef playerRef;
    private final UUID factionId;
    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final GuiManager guiManager;

    private Set<UUID> expandedMembers = new HashSet<>();
    private SortMode sortMode = SortMode.ROLE;
    private String searchQuery = "";
    private int currentPage = 0;

    private enum SortMode {
        ROLE,
        ONLINE,
        NAME,
        POWER
    }

    public AdminFactionMembersPage(PlayerRef playerRef,
                                   UUID factionId,
                                   FactionManager factionManager,
                                   PowerManager powerManager,
                                   GuiManager guiManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionMembersData.CODEC);
        this.playerRef = playerRef;
        this.factionId = factionId;
        this.factionManager = factionManager;
        this.powerManager = powerManager;
        this.guiManager = guiManager;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        cmd.append("HyperFactions/admin/admin_faction_members.ui");

        // Setup admin nav bar
        AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

        // Get the faction
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            cmd.set("#FactionName.Text", "Faction Not Found");
            cmd.set("#MemberCount.Text", "0 members");
            return;
        }

        // Set faction name
        cmd.set("#FactionName.Text", faction.name());

        // Build member list
        buildMemberList(cmd, events, faction);
    }

    private void buildMemberList(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
        List<FactionMember> allMembers = getFilteredSortedMembers(faction);

        // Count display
        if (searchQuery.isEmpty()) {
            cmd.set("#MemberCount.Text", allMembers.size() + " members");
        } else {
            cmd.set("#MemberCount.Text", allMembers.size() + " found");
        }

        // Sort dropdown
        cmd.set("#SortDropdown.Entries", List.of(
                new DropdownEntryInfo(LocalizableString.fromString("Role"), "ROLE"),
                new DropdownEntryInfo(LocalizableString.fromString("Online"), "ONLINE"),
                new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
                new DropdownEntryInfo(LocalizableString.fromString("Power"), "POWER")
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

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back")
                        .append("FactionId", factionId.toString()),
                false
        );

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) allMembers.size() / MEMBERS_PER_PAGE));
        currentPage = Math.min(currentPage, totalPages - 1);
        int startIdx = currentPage * MEMBERS_PER_PAGE;

        // Clear and rebuild list
        cmd.clear("#MembersList");
        cmd.appendInline("#MembersList", "Group #IndexCards { LayoutMode: Top; }");

        // Build paginated entries
        int i = 0;
        for (int idx = startIdx; idx < Math.min(startIdx + MEMBERS_PER_PAGE, allMembers.size()); idx++) {
            buildMemberEntry(cmd, events, i, allMembers.get(idx));
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

    private void buildMemberEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                                   FactionMember member) {
        boolean isExpanded = expandedMembers.contains(member.uuid());
        boolean memberIsOnline = isOnline(member);

        cmd.append("#IndexCards", "HyperFactions/admin/admin_faction_members_entry.ui");

        String idx = "#IndexCards[" + index + "]";

        // Basic info
        cmd.set(idx + " #MemberName.Text", member.username());
        cmd.set(idx + " #MemberRole.Text", formatRole(member.role()));
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

        // Bind header for expansion toggle
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                idx + " #Header",
                EventData.of("Button", "ToggleExpanded")
                        .append("MemberUuid", member.uuid().toString()),
                false
        );

        // Extended info
        if (isExpanded) {
            // Power info
            PlayerPower power = powerManager.getPlayerPower(member.uuid());
            String powerText = String.format("%.0f/%.0f", power.power(), power.getEffectiveMaxPower());
            cmd.set(idx + " #PowerValue.Text", powerText);
            int powerPercent = power.getPowerPercent();
            String powerColor = powerPercent >= 80 ? "#55FF55" : powerPercent >= 40 ? "#FFAA00" : "#FF5555";
            cmd.set(idx + " #PowerValue.Style.TextColor", powerColor);

            // Joined date
            String joinedDate = member.joinedAt() > 0
                    ? DATE_FORMAT.format(Instant.ofEpochMilli(member.joinedAt()))
                    : "Unknown";
            cmd.set(idx + " #JoinedDate.Text", joinedDate);

            // Last death
            String lastDeathText = power.lastDeath() > 0
                    ? TimeUtil.formatDuration(System.currentTimeMillis() - power.lastDeath()) + " ago"
                    : "Never";
            cmd.set(idx + " #LastDeath.Text", lastDeathText);

            // UUID (admin-only info)
            cmd.set(idx + " #UuidValue.Text", member.uuid().toString());

            // Action buttons visibility based on role
            boolean canPromote = member.role() != FactionRole.LEADER;
            boolean canDemote = member.role() != FactionRole.MEMBER;
            boolean canKick = member.role() != FactionRole.LEADER;

            cmd.set(idx + " #ViewInfoBtn.Visible", true);
            cmd.set(idx + " #TeleportBtn.Visible", true);
            cmd.set(idx + " #PromoteBtn.Visible", canPromote);
            cmd.set(idx + " #DemoteBtn.Visible", canDemote);
            cmd.set(idx + " #KickBtn.Visible", canKick);

            // View Info button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    idx + " #ViewInfoBtn",
                    EventData.of("Button", "ViewPlayerInfo")
                            .append("MemberUuid", member.uuid().toString())
                            .append("MemberName", member.username())
                            .append("FactionId", factionId.toString()),
                    false
            );

            // Teleport button
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    idx + " #TeleportBtn",
                    EventData.of("Button", "Teleport")
                            .append("MemberUuid", member.uuid().toString())
                            .append("MemberName", member.username()),
                    false
            );

            // Bind action buttons
            if (canPromote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #PromoteBtn",
                        EventData.of("Button", "Promote")
                                .append("MemberUuid", member.uuid().toString())
                                .append("MemberName", member.username()),
                        false
                );
            }

            if (canDemote) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #DemoteBtn",
                        EventData.of("Button", "Demote")
                                .append("MemberUuid", member.uuid().toString())
                                .append("MemberName", member.username()),
                        false
                );
            }

            if (canKick) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        idx + " #KickBtn",
                        EventData.of("Button", "Kick")
                                .append("MemberUuid", member.uuid().toString())
                                .append("MemberName", member.username()),
                        false
                );
            }
        }
    }

    private List<FactionMember> getFilteredSortedMembers(Faction faction) {
        return faction.members().values().stream()
                .filter(m -> searchQuery.isEmpty() ||
                        m.username().toLowerCase().contains(searchQuery.toLowerCase()))
                .sorted(getSortComparator())
                .toList();
    }

    private Comparator<FactionMember> getSortComparator() {
        return switch (sortMode) {
            case ONLINE -> Comparator
                    .<FactionMember>comparingLong(m -> isOnline(m) ? Long.MAX_VALUE : m.lastOnline())
                    .reversed()
                    .thenComparing(FactionMember::username);
            case NAME -> Comparator.comparing(FactionMember::username, String.CASE_INSENSITIVE_ORDER);
            case POWER -> Comparator
                    .<FactionMember>comparingDouble(m -> powerManager.getPlayerPower(m.uuid()).power())
                    .reversed()
                    .thenComparing(FactionMember::username);
            default -> Comparator // ROLE
                    .<FactionMember>comparingInt(m -> -m.role().getLevel())
                    .thenComparing(FactionMember::username);
        };
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
                                AdminFactionMembersData data) {
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
                if (data.memberUuid != null) {
                    try {
                        UUID uuid = UUID.fromString(data.memberUuid);
                        if (expandedMembers.contains(uuid)) {
                            expandedMembers.remove(uuid);
                        } else {
                            expandedMembers.add(uuid);
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
                expandedMembers.clear();
                rebuildList();
            }

            case "SearchChanged" -> {
                searchQuery = data.searchQuery != null ? data.searchQuery : "";
                currentPage = 0;
                expandedMembers.clear();
                rebuildList();
            }

            case "PrevPage" -> {
                currentPage = Math.max(0, data.page);
                expandedMembers.clear();
                rebuildList();
            }

            case "NextPage" -> {
                currentPage = data.page;
                expandedMembers.clear();
                rebuildList();
            }

            case "Back" -> {
                guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
            }

            case "Teleport" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID memberUuid = UUID.fromString(data.memberUuid);
                        PlayerRef targetPlayer = Universe.get().getPlayer(memberUuid);
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
                                var teleport = com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.createForPlayer(
                                        targetWorld, targetPos, targetRot);
                                store.addComponent(ref, com.hypixel.hytale.server.core.modules.entity.teleport.Teleport.getComponentType(), teleport);
                            });
                            player.sendMessage(Message.raw("[Admin] Teleported to ").color("#55FF55")
                                    .insert(Message.raw(data.memberName != null ? data.memberName : "player").color("#00FFFF"))
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

            case "Promote" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID memberUuid = UUID.fromString(data.memberUuid);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            FactionMember member = faction.getMember(memberUuid);
                            if (member != null && member.role() != FactionRole.LEADER) {
                                FactionRole newRole = member.role() == FactionRole.MEMBER
                                        ? FactionRole.OFFICER : FactionRole.LEADER;
                                factionManager.adminSetMemberRole(factionId, memberUuid, newRole);
                                player.sendMessage(Message.raw("[Admin] Promoted ").color("#55FF55")
                                        .insert(Message.raw(data.memberName != null ? data.memberName : "player").color("#00FFFF"))
                                        .insert(Message.raw(" to ").color("#55FF55"))
                                        .insert(Message.raw(formatRole(newRole)).color("#FFD700"))
                                        .insert(Message.raw(".").color("#55FF55")));
                                rebuildList();
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "Demote" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID memberUuid = UUID.fromString(data.memberUuid);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            FactionMember member = faction.getMember(memberUuid);
                            if (member != null && member.role() != FactionRole.MEMBER) {
                                FactionRole newRole = member.role() == FactionRole.LEADER
                                        ? FactionRole.OFFICER : FactionRole.MEMBER;
                                factionManager.adminSetMemberRole(factionId, memberUuid, newRole);
                                player.sendMessage(Message.raw("[Admin] Demoted ").color("#FFAA00")
                                        .insert(Message.raw(data.memberName != null ? data.memberName : "player").color("#00FFFF"))
                                        .insert(Message.raw(" to ").color("#FFAA00"))
                                        .insert(Message.raw(formatRole(newRole)).color("#888888"))
                                        .insert(Message.raw(".").color("#FFAA00")));
                                rebuildList();
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "Kick" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID memberUuid = UUID.fromString(data.memberUuid);
                        Faction faction = factionManager.getFaction(factionId);
                        if (faction != null) {
                            FactionMember member = faction.getMember(memberUuid);
                            if (member != null && member.role() != FactionRole.LEADER) {
                                factionManager.adminRemoveMember(factionId, memberUuid);
                                player.sendMessage(Message.raw("[Admin] Kicked ").color("#FF5555")
                                        .insert(Message.raw(data.memberName != null ? data.memberName : "player").color("#00FFFF"))
                                        .insert(Message.raw(" from the faction.").color("#FF5555")));
                                rebuildList();
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            case "ViewPlayerInfo" -> {
                if (data.memberUuid != null) {
                    try {
                        UUID memberUuid = UUID.fromString(data.memberUuid);
                        String memberName = data.memberName != null ? data.memberName : "Unknown";
                        guiManager.openAdminPlayerInfo(player, ref, store, playerRef, memberUuid, memberName, factionId);
                    } catch (IllegalArgumentException e) {
                        sendUpdate();
                    }
                }
            }

            default -> sendUpdate();
        }
    }

    private void rebuildList() {
        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            sendUpdate();
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        buildMemberList(cmd, events, faction);

        sendUpdate(cmd, events, false);
    }
}
