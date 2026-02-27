package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.gui.GuiColors;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.TimeUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

/**
 * Logs Viewer page - paginated view of faction activity logs with filtering.
 * Uses NavBarHelper for navigation integration.
 */
public class LogsViewerPage extends InteractiveCustomUIPage<FactionPageData> {

  private static final String PAGE_ID = "logs";

  private static final int LOGS_PER_PAGE = 10;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private int currentPage = 0;

  private FactionLog.LogType filterType = null;

  /** Creates a new LogsViewerPage. */
  public LogsViewerPage(PlayerRef playerRef,
             FactionManager factionManager,
             GuiManager guiManager,
             Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.LOGS_VIEWER);

    // Setup navigation bar
    Faction viewerFaction = factionManager.getPlayerFaction(playerRef.getUuid());
    if (viewerFaction != null) {
      NavBarHelper.setupBar(playerRef, viewerFaction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Set title with faction name
    cmd.set("#LogsTitle.Text", faction.name() + " - Activity Logs");

    buildLogList(cmd, events);
  }

  private void buildLogList(UICommandBuilder cmd, UIEventBuilder events) {
    // Get fresh faction data
    Faction currentFaction = factionManager.getFaction(faction.id());
    if (currentFaction == null) {
      currentFaction = faction;
    }

    // Filter logs
    List<FactionLog> allLogs = new ArrayList<>(currentFaction.logs());
    if (filterType != null) {
      allLogs = allLogs.stream()
          .filter(log -> log.type() == filterType)
          .collect(Collectors.toList());
    }

    // Sort by timestamp (newest first)
    allLogs.sort(Comparator.comparing(FactionLog::timestamp).reversed());

    // Calculate pagination
    int totalLogs = allLogs.size();
    int totalPages = Math.max(1, (int) Math.ceil((double) totalLogs / LOGS_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    if (currentPage < 0) {
      currentPage = 0;
    }

    int startIndex = currentPage * LOGS_PER_PAGE;
    int endIndex = Math.min(startIndex + LOGS_PER_PAGE, totalLogs);

    // Log count
    cmd.set("#LogCount.Text", totalLogs + " entries");

    // Filter dropdown
    List<DropdownEntryInfo> filterOptions = new ArrayList<>();
    filterOptions.add(new DropdownEntryInfo(LocalizableString.fromString("All Types"), "ALL"));
    for (FactionLog.LogType type : FactionLog.LogType.values()) {
      filterOptions.add(new DropdownEntryInfo(LocalizableString.fromString(type.getDisplayName()), type.name()));
    }
    cmd.set("#FilterDropdown.Entries", filterOptions);
    cmd.set("#FilterDropdown.Value", filterType != null ? filterType.name() : "ALL");

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#FilterDropdown",
        EventData.of("Button", "FilterChanged")
            .append("@SortMode", "#FilterDropdown.Value"),
        false
    );

    // Clear and build log entries
    cmd.clear("#LogsList");

    if (totalLogs == 0) {
      cmd.appendInline("#LogsList",
          "Label { Text: \""
          + (filterType != null ? "No logs of this type." : "No activity logs yet.") +
          "\"; Style: (FontSize: 11, TextColor: #555555); Anchor: (Height: 30); }");
    } else {
      for (int i = startIndex; i < endIndex; i++) {
        FactionLog log = allLogs.get(i);
        String sel = "#LogsList[" + (i - startIndex) + "]";

        cmd.append("#LogsList", UIPaths.LOG_ENTRY);

        // Time
        cmd.set(sel + " #LogTime.Text", TimeUtil.formatRelative(log.timestamp()));

        // Type badge with color
        cmd.set(sel + " #LogType.Text", log.type().getDisplayName());
        cmd.set(sel + " #LogType.Style.TextColor", GuiColors.forLogType(log.type()));

        // Message
        cmd.set(sel + " #LogMessage.Text", log.message());
      }
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

  /** Handles data event. */
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

    Faction viewerFaction = factionManager.getPlayerFaction(playerRef.getUuid());

    // Handle navigation
    if (viewerFaction != null) {
      if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, viewerFaction, guiManager)) {
        return;
      }
    } else {
      if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
        return;
      }
    }

    switch (data.button) {
      case "FilterChanged" -> {
        if (data.sortMode != null) {
          if ("ALL".equals(data.sortMode)) {
            filterType = null;
          } else {
            try {
              filterType = FactionLog.LogType.valueOf(data.sortMode);
            } catch (IllegalArgumentException e) {
              filterType = null;
            }
          }
        }
        currentPage = 0;
        rebuildList();
      }

      case "PrevPage" -> {
        currentPage = Math.max(0, data.page);
        rebuildList();
      }

      case "NextPage" -> {
        currentPage = data.page;
        rebuildList();
      }

      default -> sendUpdate();
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildLogList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
