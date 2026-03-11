package com.hyperfactions.gui.admin.page;

import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.gui.GuiColors;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminActivityLogData;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Activity Log page - global view of all faction activity with filtering.
 * Aggregates logs from all factions and provides type, time, and player filters.
 */
public class AdminActivityLogPage extends InteractiveCustomUIPage<AdminActivityLogData> {

  private static final int LOGS_PER_PAGE = 10;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private int currentPage = 0;

  private FactionLog.LogType filterType = null;

  private TimeFilter timeFilter = TimeFilter.ALL;

  private String playerFilter = "";

  /**
   * Wraps a FactionLog with faction context for display.
   */
  private record GlobalLogEntry(
      FactionLog log,
      String factionName,
      @Nullable String factionTag,
      String factionColor,
      UUID factionId
  ) {}

  private enum TimeFilter {
    HOUR_1("1h", 3600_000L),
    HOUR_24("24h", 86400_000L),
    DAY_7("7d", 604800_000L),
    ALL("All", Long.MAX_VALUE);

    private final String displayName;

    private final long millis;

    TimeFilter(String displayName, long millis) {
      this.displayName = displayName;
      this.millis = millis;
    }
  }

  /** Creates a new AdminActivityLogPage. */
  public AdminActivityLogPage(PlayerRef playerRef,
                FactionManager factionManager,
                GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminActivityLogData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.ADMIN_ACTIVITY_LOG);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "log", cmd, events);

    // Localize page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_ACTIVITY_LOG));

    // Localize filter labels
    cmd.set("#TypeLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_LOG_TYPE));
    cmd.set("#TimeLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_LOG_TIME));
    cmd.set("#PlayerLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_LOG_PLAYER));

    // Localize column headers
    cmd.set("#ColTime.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_COL_TIME));
    cmd.set("#ColType.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_COL_TYPE));
    cmd.set("#ColFaction.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_COL_FACTION));
    cmd.set("#ColMessage.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_COL_MESSAGE));

    // Localize pagination buttons
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_NEXT));

    buildLogList(cmd, events);
  }

  private void buildLogList(UICommandBuilder cmd, UIEventBuilder events) {
    // === Filter Controls ===

    // Type filter dropdown
    List<DropdownEntryInfo> typeOptions = new ArrayList<>();
    typeOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.LOG_ALL_TYPES)), "ALL"));
    for (FactionLog.LogType type : FactionLog.LogType.values()) {
      typeOptions.add(new DropdownEntryInfo(LocalizableString.fromString(
          HFMessages.get(playerRef, MessageKeys.LogsGui.typeKey(type.name()))), type.name()));
    }
    cmd.set("#TypeDropdown.Entries", typeOptions);
    cmd.set("#TypeDropdown.Value", filterType != null ? filterType.name() : "ALL");

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#TypeDropdown",
        EventData.of("Button", "TypeFilter")
            .append("@FilterType", "#TypeDropdown.Value"),
        false
    );

    // Time filter dropdown
    List<DropdownEntryInfo> timeOptions = new ArrayList<>();
    for (TimeFilter tf : TimeFilter.values()) {
      timeOptions.add(new DropdownEntryInfo(LocalizableString.fromString(tf.displayName), tf.name()));
    }
    cmd.set("#TimeDropdown.Entries", timeOptions);
    cmd.set("#TimeDropdown.Value", timeFilter.name());

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#TimeDropdown",
        EventData.of("Button", "TimeFilter")
            .append("@TimeFilter", "#TimeDropdown.Value"),
        false
    );

    // Player search
    if (!playerFilter.isEmpty()) {
      cmd.set("#PlayerSearch.Value", playerFilter);
    }
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#PlayerSearch",
        EventData.of("Button", "PlayerFilter")
            .append("@PlayerFilter", "#PlayerSearch.Value"),
        false
    );

    // === Collect and filter logs ===
    List<GlobalLogEntry> allLogs = collectGlobalLogs();

    cmd.set("#LogCount.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ENTRIES_SUFFIX, allLogs.size()));

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) allLogs.size() / LOGS_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    if (currentPage < 0) {
      currentPage = 0;
    }
    int startIdx = currentPage * LOGS_PER_PAGE;

    // Clear and populate list
    cmd.clear("#LogList");

    int index = 0;
    for (int idx = startIdx; idx < Math.min(startIdx + LOGS_PER_PAGE, allLogs.size()); idx++) {
      GlobalLogEntry entry = allLogs.get(idx);
      String sel = "#LogList[" + index + "]";

      cmd.append("#LogList", UIPaths.ADMIN_ACTIVITY_LOG_ENTRY);

      // Time (localized)
      cmd.set(sel + " #LogTime.Text", formatRelativeTime(entry.log.timestamp()));

      // Type with color (localized)
      cmd.set(sel + " #LogType.Text", HFMessages.get(playerRef, MessageKeys.LogsGui.typeKey(entry.log.type().name())));
      cmd.set(sel + " #LogType.Style.TextColor", GuiColors.forLogType(entry.log.type()));

      // Faction name with color
      String factionDisplay = entry.factionName;
      if (entry.factionTag != null && !entry.factionTag.isEmpty()) {
        factionDisplay = "[" + entry.factionTag + "] " + entry.factionName;
      }
      cmd.set(sel + " #FactionName.Text", factionDisplay);
      cmd.set(sel + " #FactionName.Style.TextColor", entry.factionColor);

      // Message (localized if key available, else English fallback)
      cmd.set(sel + " #LogMessage.Text", HFMessages.resolveLogMessage(playerRef, entry.log()));

      index++;
    }

    // Empty state
    if (index == 0) {
      cmd.appendInline("#LogList",
          "Label { Text: \"" + HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_LOG_NO_LOGS) + "\"; "
          + "Style: (FontSize: 11, TextColor: #555555); Anchor: (Height: 30); }");
    }

    // Pagination
    cmd.set("#PageInfo.Text", HFMessages.get(playerRef, MessageKeys.GuiCommon.PAGE_FORMAT, currentPage + 1, totalPages));

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

  /**
   * Collects logs from all factions, applies filters, and sorts by timestamp.
   */
  private List<GlobalLogEntry> collectGlobalLogs() {
    List<GlobalLogEntry> result = new ArrayList<>();
    long now = System.currentTimeMillis();
    long cutoff = timeFilter.millis == Long.MAX_VALUE ? 0 : now - timeFilter.millis;

    for (Faction faction : factionManager.getAllFactions()) {
      String factionColor = faction.color() != null ? faction.color() : "#FFFFFF";

      for (FactionLog log : faction.logs()) {
        // Time filter
        if (log.timestamp() < cutoff) {
          continue;
        }

        // Type filter
        if (filterType != null && log.type() != filterType) {
          continue;
        }

        // Player filter - match actor UUID against faction member usernames
        if (!playerFilter.isEmpty() && log.actorUuid() != null) {
          String lowerFilter = playerFilter.toLowerCase();
          FactionMember member = faction.getMember(log.actorUuid());
          if (member == null || !member.username().toLowerCase().contains(lowerFilter)) {
            // Actor is not in this faction (may have left) or name doesn't match
            // Also check the log message itself for the player name
            if (!log.message().toLowerCase().contains(lowerFilter)) {
              continue;
            }
          }
        } else if (!playerFilter.isEmpty()) {
          // System log - check message for player name match
          if (!log.message().toLowerCase().contains(playerFilter.toLowerCase())) {
            continue;
          }
        }

        result.add(new GlobalLogEntry(log, faction.name(), faction.tag(), factionColor, faction.id()));
      }
    }

    // Sort by timestamp descending (newest first)
    result.sort(Comparator.comparingLong((GlobalLogEntry e) -> e.log.timestamp()).reversed());

    return result;
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminActivityLogData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      return;
    }

    // Handle admin nav bar navigation
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    if (data.button == null) {
      return;
    }

    switch (data.button) {
      case "TypeFilter" -> {
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
        }
        currentPage = 0;
        rebuildList();
      }

      case "TimeFilter" -> {
        if (data.timeFilter != null) {
          try {
            timeFilter = TimeFilter.valueOf(data.timeFilter);
          } catch (IllegalArgumentException e) {
            timeFilter = TimeFilter.ALL;
          }
        }
        currentPage = 0;
        rebuildList();
      }

      case "PlayerFilter" -> {
        playerFilter = data.playerFilter != null ? data.playerFilter : "";
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
      return HFMessages.get(playerRef, MessageKeys.LogsGui.TIME_JUST_NOW);
    } else if (diff < 3600_000) {
      long m = TimeUnit.MILLISECONDS.toMinutes(diff);
      return HFMessages.get(playerRef, m == 1 ? MessageKeys.LogsGui.TIME_MINUTE : MessageKeys.LogsGui.TIME_MINUTES, m);
    } else if (diff < 86400_000) {
      long h = TimeUnit.MILLISECONDS.toHours(diff);
      return HFMessages.get(playerRef, h == 1 ? MessageKeys.LogsGui.TIME_HOUR : MessageKeys.LogsGui.TIME_HOURS, h);
    } else if (diff < 604800_000) {
      long d = TimeUnit.MILLISECONDS.toDays(diff);
      return HFMessages.get(playerRef, d == 1 ? MessageKeys.LogsGui.TIME_DAY : MessageKeys.LogsGui.TIME_DAYS, d);
    } else if (diff < 2592000_000L) {
      long w = TimeUnit.MILLISECONDS.toDays(diff) / 7;
      return HFMessages.get(playerRef, w == 1 ? MessageKeys.LogsGui.TIME_WEEK : MessageKeys.LogsGui.TIME_WEEKS, w);
    } else {
      return TimeUtil.formatDate(timestamp);
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildLogList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
