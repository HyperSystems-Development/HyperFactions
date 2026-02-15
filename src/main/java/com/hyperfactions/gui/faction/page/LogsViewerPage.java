package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.faction.data.LogsViewerData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.TimeUtil;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Logs Viewer page - paginated view of faction activity logs with filtering.
 */
public class LogsViewerPage extends InteractiveCustomUIPage<LogsViewerData> {

    private static final int LOGS_PER_PAGE = 10;

    private final PlayerRef playerRef;
    private final FactionManager factionManager;
    private final GuiManager guiManager;
    private final Faction faction;
    private int currentPage = 0;
    private FactionLog.LogType filterType = null;

    public LogsViewerPage(PlayerRef playerRef,
                          FactionManager factionManager,
                          GuiManager guiManager,
                          Faction faction) {
        super(playerRef, CustomPageLifetime.CanDismiss, LogsViewerData.CODEC);
        this.playerRef = playerRef;
        this.factionManager = factionManager;
        this.guiManager = guiManager;
        this.faction = faction;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the logs viewer template
        cmd.append("HyperFactions/faction/logs_viewer.ui");

        // Set title
        cmd.set("#PageTitle.Text", faction.name() + " - Activity Logs");

        // Filter logs
        List<FactionLog> allLogs = new ArrayList<>(faction.logs());
        if (filterType != null) {
            allLogs = allLogs.stream()
                    .filter(log -> log.type() == filterType)
                    .collect(Collectors.toList());
        }

        // Sort by timestamp (newest first)
        allLogs.sort(Comparator.comparing(FactionLog::timestamp).reversed());

        // Calculate pagination
        int totalLogs = allLogs.size();
        int totalPages = (int) Math.ceil((double) totalLogs / LOGS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int startIndex = currentPage * LOGS_PER_PAGE;
        int endIndex = Math.min(startIndex + LOGS_PER_PAGE, totalLogs);

        // Display page indicator
        cmd.set("#PageIndicator.Text", String.format("Page %d of %d (%d total)",
                currentPage + 1, totalPages, totalLogs));

        // === Filter Section ===
        cmd.append("#FilterSection", "HyperFactions/faction/filter_section.ui");
        cmd.set("#FilterSection #FilterLabel.Text", "Filter:");

        // All logs filter
        cmd.append("#FilterSection #FilterButtons", "HyperFactions/faction/filter_button.ui");
        cmd.set("#FilterSection #FilterButtons #FilterBtn.Text", "All");
        if (filterType == null) {
            cmd.set("#FilterSection #FilterButtons #FilterBtn.BorderColor", "#55FF55");
        }
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FilterSection #FilterButtons #FilterBtn",
                EventData.of("Button", "Filter").append("FilterType", "ALL"),
                false
        );

        // Type-specific filters
        for (FactionLog.LogType type : FactionLog.LogType.values()) {
            cmd.append("#FilterSection #FilterButtons", "HyperFactions/faction/filter_button.ui");
            cmd.set("#FilterSection #FilterButtons #FilterBtn.Text", type.getDisplayName());
            if (filterType == type) {
                cmd.set("#FilterSection #FilterButtons #FilterBtn.BorderColor", "#55FF55");
            }
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    "#FilterSection #FilterButtons #FilterBtn",
                    EventData.of("Button", "Filter").append("FilterType", type.name()),
                    false
            );
        }

        // === Logs List ===
        if (totalLogs == 0) {
            cmd.append("#LogsList", "HyperFactions/faction/empty_state.ui");
            cmd.set("#LogsList #EmptyMessage.Text",
                    filterType != null ? "No logs of this type." : "No logs yet.");
        } else {
            for (int i = startIndex; i < endIndex; i++) {
                FactionLog log = allLogs.get(i);
                cmd.append("#LogsList", "HyperFactions/faction/log_entry.ui");

                // Log type badge
                cmd.set("#LogsList #LogType.Text", log.type().getDisplayName());
                cmd.set("#LogsList #LogType.BackgroundColor", getLogTypeColor(log.type()));

                // Log message
                cmd.set("#LogsList #LogMessage.Text", log.message());

                // Timestamp
                cmd.set("#LogsList #LogTimestamp.Text", TimeUtil.formatRelative(log.timestamp()));

                // Actor (if not system log)
                if (!log.isSystemLog() && log.actorUuid() != null) {
                    // Would need username lookup - for now show UUID
                    cmd.set("#LogsList #LogActor.Text", "by Player");
                    cmd.set("#LogsList #LogActor.Visible", "true");
                } else {
                    cmd.set("#LogsList #LogActor.Visible", "false");
                }
            }
        }

        // === Pagination Controls ===
        if (totalPages > 1) {
            // Previous button
            if (currentPage > 0) {
                cmd.set("#PrevBtn.Visible", "true");
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#PrevBtn",
                        EventData.of("Button", "PrevPage"),
                        false
                );
            } else {
                cmd.set("#PrevBtn.Visible", "false");
            }

            // Next button
            if (currentPage < totalPages - 1) {
                cmd.set("#NextBtn.Visible", "true");
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#NextBtn",
                        EventData.of("Button", "NextPage"),
                        false
                );
            } else {
                cmd.set("#NextBtn.Visible", "false");
            }
        } else {
            cmd.set("#PrevBtn.Visible", "false");
            cmd.set("#NextBtn.Visible", "false");
        }

        // Back button
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#BackBtn",
                EventData.of("Button", "Back"),
                false
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                LogsViewerData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null || data.button == null) {
            return;
        }

        switch (data.button) {
            case "Filter" -> {
                if (data.filterType != null) {
                    if ("ALL".equals(data.filterType)) {
                        filterType = null;
                    } else {
                        try {
                            filterType = FactionLog.LogType.valueOf(data.filterType);
                        } catch (IllegalArgumentException e) {
                            filterType = null;
                        }
                    }
                    currentPage = 0; // Reset to first page when filtering
                    guiManager.openLogsViewer(player, ref, store, playerRef, faction);
                }
            }

            case "PrevPage" -> {
                if (currentPage > 0) {
                    currentPage--;
                    guiManager.openLogsViewer(player, ref, store, playerRef, faction);
                }
            }

            case "NextPage" -> {
                currentPage++;
                guiManager.openLogsViewer(player, ref, store, playerRef, faction);
            }

            case "Back" -> guiManager.openFactionMain(player, ref, store, playerRef);
        }
    }

    /**
     * Gets color for log type badge.
     */
    private String getLogTypeColor(FactionLog.LogType type) {
        return switch (type) {
            case MEMBER_JOIN -> "#55FF55";     // Green
            case MEMBER_LEAVE, MEMBER_KICK -> "#FF5555";  // Red
            case MEMBER_PROMOTE -> "#FFD700";   // Gold
            case MEMBER_DEMOTE -> "#FFAA00";    // Orange
            case CLAIM, OVERCLAIM -> "#5555FF"; // Blue
            case UNCLAIM -> "#AAAAAA";          // Gray
            case HOME_SET -> "#55FFFF";         // Cyan
            case RELATION_ALLY -> "#55FF55";    // Green
            case RELATION_ENEMY -> "#FF5555";   // Red
            case RELATION_NEUTRAL -> "#FFFF55"; // Yellow
            case LEADER_TRANSFER -> "#FF55FF";  // Magenta
            case SETTINGS_CHANGE -> "#AA00FF";  // Purple
            case POWER_CHANGE -> "#FFAA00";     // Orange
            case ECONOMY -> "#FFD700";          // Gold
            case ADMIN_POWER -> "#FF55FF";      // Magenta (admin action)
        };
    }
}
