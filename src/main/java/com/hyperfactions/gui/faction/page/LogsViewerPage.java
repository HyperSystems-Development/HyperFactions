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
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.GuiKeys;
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
import java.util.concurrent.TimeUnit;
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
    cmd.set("#LogsTitle.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.TITLE, faction.name()));

    // Localize static labels
    cmd.set("#FilterLabel.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.FILTER_LABEL));
    cmd.set("#ColTimeLabel.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.COL_TIME));
    cmd.set("#ColTypeLabel.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.COL_TYPE));
    cmd.set("#ColMessageLabel.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.COL_MESSAGE));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.NEXT));

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
    cmd.set("#LogCount.Text", HFMessages.get(playerRef, GuiKeys.LogsGui.ENTRY_COUNT, totalLogs));

    // Filter dropdown
    List<DropdownEntryInfo> filterOptions = new ArrayList<>();
    filterOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.LogsGui.ALL_TYPES)), "ALL"));
    for (FactionLog.LogType type : FactionLog.LogType.values()) {
      filterOptions.add(new DropdownEntryInfo(LocalizableString.fromString(getLocalizedTypeName(type)), type.name()));
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
      String emptyText = filterType != null
          ? HFMessages.get(playerRef, GuiKeys.LogsGui.NO_LOGS_TYPE)
          : HFMessages.get(playerRef, GuiKeys.LogsGui.NO_LOGS);
      cmd.appendInline("#LogsList",
          "Label { Text: \"" + emptyText +
          "\"; Style: (FontSize: 11, TextColor: #555555); Anchor: (Height: 30); }");
    } else {
      for (int i = startIndex; i < endIndex; i++) {
        FactionLog log = allLogs.get(i);
        String sel = "#LogsList[" + (i - startIndex) + "]";

        cmd.append("#LogsList", UIPaths.LOG_ENTRY);

        // Time (localized)
        cmd.set(sel + " #LogTime.Text", formatRelativeTime(log.timestamp()));

        // Type badge with color (localized)
        cmd.set(sel + " #LogType.Text", getLocalizedTypeName(log.type()));
        cmd.set(sel + " #LogType.Style.TextColor", GuiColors.forLogType(log.type()));

        // Message (localized if key available, else English fallback)
        cmd.set(sel + " #LogMessage.Text", HFMessages.resolveLogMessage(playerRef, log));
      }
    }

    // Pagination
    cmd.set("#PageInfo.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.PAGE_FORMAT, currentPage + 1, totalPages));

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

  /** Returns a localized relative time string for the given timestamp. */
  private String formatRelativeTime(long timestamp) {
    long diff = System.currentTimeMillis() - timestamp;
    if (diff < 60_000) {
      return HFMessages.get(playerRef, GuiKeys.LogsGui.TIME_JUST_NOW);
    } else if (diff < 3600_000) {
      long m = TimeUnit.MILLISECONDS.toMinutes(diff);
      return HFMessages.get(playerRef, m == 1 ? GuiKeys.LogsGui.TIME_MINUTE : GuiKeys.LogsGui.TIME_MINUTES, m);
    } else if (diff < 86400_000) {
      long h = TimeUnit.MILLISECONDS.toHours(diff);
      return HFMessages.get(playerRef, h == 1 ? GuiKeys.LogsGui.TIME_HOUR : GuiKeys.LogsGui.TIME_HOURS, h);
    } else if (diff < 604800_000) {
      long d = TimeUnit.MILLISECONDS.toDays(diff);
      return HFMessages.get(playerRef, d == 1 ? GuiKeys.LogsGui.TIME_DAY : GuiKeys.LogsGui.TIME_DAYS, d);
    } else if (diff < 2592000_000L) {
      long w = TimeUnit.MILLISECONDS.toDays(diff) / 7;
      return HFMessages.get(playerRef, w == 1 ? GuiKeys.LogsGui.TIME_WEEK : GuiKeys.LogsGui.TIME_WEEKS, w);
    } else {
      return TimeUtil.formatDate(timestamp);
    }
  }

  /** Returns the localized display name for a log type. */
  private String getLocalizedTypeName(FactionLog.LogType type) {
    return HFMessages.get(playerRef, GuiKeys.LogsGui.typeKey(type.name()));
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildLogList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
