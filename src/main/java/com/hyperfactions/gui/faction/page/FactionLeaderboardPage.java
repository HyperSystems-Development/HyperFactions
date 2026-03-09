package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.gui.GuiColors;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionKDCache;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.UuidUtil;
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
import java.math.BigDecimal;
import java.util.*;
import org.jetbrains.annotations.Nullable;

/**
 * Faction Leaderboard page - displays ranked list of factions by various criteria.
 * Sort modes: POWER, TERRITORY, BALANCE, MEMBERS.
 */
public class FactionLeaderboardPage extends InteractiveCustomUIPage<FactionPageData> {

  private static final String PAGE_ID = "leaderboard";

  private static final int ENTRIES_PER_PAGE = 10;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final @Nullable EconomyManager economyManager;

  private final @Nullable FactionKDCache factionKDCache;

  private final GuiManager guiManager;

  private int currentPage = 0;

  private SortMode sortMode = SortMode.KD;

  private enum SortMode {
    KD(MessageKeys.LeaderboardGui.SORT_KD),
    POWER(MessageKeys.GuiCommon.SORT_POWER),
    TERRITORY(MessageKeys.LeaderboardGui.SORT_TERRITORY),
    BALANCE(MessageKeys.LeaderboardGui.SORT_BALANCE),
    MEMBERS(MessageKeys.GuiCommon.SORT_MEMBERS);

    private final String displayKey;

    SortMode(String displayKey) {
      this.displayKey = displayKey;
    }
  }

  /** Creates a new FactionLeaderboardPage. */
  public FactionLeaderboardPage(PlayerRef playerRef,
                 FactionManager factionManager,
                 PowerManager powerManager,
                 @Nullable EconomyManager economyManager,
                 @Nullable FactionKDCache factionKDCache,
                 GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.economyManager = economyManager;
    this.factionKDCache = factionKDCache;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    UUID viewerUuid = playerRef.getUuid();
    Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);

    cmd.append(UIPaths.FACTION_LEADERBOARD);

    // Setup navigation bar
    if (viewerFaction != null) {
      NavBarHelper.setupBar(playerRef, viewerFaction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    buildLeaderboard(cmd, events, viewerFaction);
  }

  private void buildLeaderboard(UICommandBuilder cmd, UIEventBuilder events,
                 @Nullable Faction viewerFaction) {
    List<LeaderboardEntry> entries = buildEntryList();

    cmd.set("#FactionCount.Text", HFMessages.get(playerRef, MessageKeys.GuiCommon.FACTION_COUNT, entries.size()));

    // Sort dropdown
    List<DropdownEntryInfo> sortOptions = new ArrayList<>();
    sortOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.LeaderboardGui.SORT_KD)), "KD"));
    sortOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.GuiCommon.SORT_POWER)), "POWER"));
    sortOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.LeaderboardGui.SORT_TERRITORY)), "TERRITORY"));
    if (economyManager != null) {
      sortOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.LeaderboardGui.SORT_BALANCE)), "BALANCE"));
    }
    sortOptions.add(new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.GuiCommon.SORT_MEMBERS)), "MEMBERS"));
    cmd.set("#SortDropdown.Entries", sortOptions);
    cmd.set("#SortDropdown.Value", sortMode.name());

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value"),
        false
    );

    // Update column header based on sort mode
    cmd.set("#StatHeader.Text", HFMessages.get(playerRef, sortMode.displayKey));

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    int startIdx = currentPage * ENTRIES_PER_PAGE;

    // Clear and create container
    cmd.clear("#LeaderboardList");
    cmd.appendInline("#LeaderboardList", "Group #IndexCards { LayoutMode: Top; }");

    // Build entries
    int displayIndex = 0;
    for (int idx = startIdx; idx < Math.min(startIdx + ENTRIES_PER_PAGE, entries.size()); idx++) {
      LeaderboardEntry entry = entries.get(idx);
      int rank = idx + 1;
      buildEntry(cmd, events, displayIndex, rank, entry, viewerFaction);
      displayIndex++;
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

  private List<LeaderboardEntry> buildEntryList() {
    List<LeaderboardEntry> entries = new ArrayList<>();

    for (Faction faction : factionManager.getAllFactions()) {
      PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
      FactionMember leader = faction.getLeader();
      BigDecimal balance = economyManager != null
          ? economyManager.getFactionBalance(faction.id())
          : BigDecimal.ZERO;

      // Get K/D stats from cache
      int totalKills = 0;
      int totalDeaths = 0;
      double kdr = 0.0;
      if (factionKDCache != null) {
        FactionKDCache.FactionKDStats kdStats = factionKDCache.getFactionKD(faction.id());
        totalKills = kdStats.totalKills();
        totalDeaths = kdStats.totalDeaths();
        kdr = kdStats.kdr();
      }

      entries.add(new LeaderboardEntry(
          faction.id(),
          faction.name(),
          faction.tag(),
          faction.color() != null ? faction.color() : "#00FFFF",
          leader != null ? leader.username() : HFMessages.get(playerRef, MessageKeys.Common.NONE),
          stats.currentPower(),
          stats.maxPower(),
          faction.getClaimCount(),
          faction.members().size(),
          balance,
          totalKills,
          totalDeaths,
          kdr
      ));
    }

    // Sort by selected mode (descending)
    switch (sortMode) {
      case KD -> entries.sort(Comparator.comparingDouble(LeaderboardEntry::kdr).reversed());
      case POWER -> entries.sort(Comparator.comparingDouble(LeaderboardEntry::power).reversed());
      case TERRITORY -> entries.sort(Comparator.comparingInt(LeaderboardEntry::claimCount).reversed());
      case BALANCE -> entries.sort((a, b) -> b.balance.compareTo(a.balance));
      case MEMBERS -> entries.sort(Comparator.comparingInt(LeaderboardEntry::memberCount).reversed());
      default -> throw new IllegalStateException("Unexpected value");
    }

    return entries;
  }

  private void buildEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
              int rank, LeaderboardEntry entry,
              @Nullable Faction viewerFaction) {
    cmd.append("#IndexCards", UIPaths.FACTION_LEADERBOARD_ENTRY);

    String idx = "#IndexCards[" + index + "]";

    // Rank display with color coding for top 3
    cmd.set(idx + " #Rank.Text", String.valueOf(rank));
    String rankColor = GuiColors.forRank(rank);
    cmd.set(idx + " #Rank.Style.TextColor", rankColor);

    // Faction name with color
    cmd.set(idx + " #FactionName.Text", entry.name);
    cmd.set(idx + " #FactionName.Style.TextColor", entry.color);

    // Tag
    if (entry.tag != null && !entry.tag.isEmpty()) {
      cmd.set(idx + " #FactionTag.Text", "[" + entry.tag + "]");
    }

    // Leader
    cmd.set(idx + " #LeaderName.Text", HFMessages.get(playerRef, MessageKeys.GuiCommon.LEADER_LABEL, entry.leaderName));

    // Primary stat value based on sort mode
    String statValue = switch (sortMode) {
      case KD -> String.format("%.2f", entry.kdr);
      case POWER -> String.format("%.0f/%.0f", entry.power, entry.maxPower);
      case TERRITORY -> String.valueOf(entry.claimCount);
      case BALANCE -> economyManager != null
          ? economyManager.formatCurrency(entry.balance)
          : HFMessages.get(playerRef, MessageKeys.Common.NA);
      case MEMBERS -> String.valueOf(entry.memberCount);
    };
    cmd.set(idx + " #StatValue.Text", statValue);

    // Secondary stats
    cmd.set(idx + " #ClaimsValue.Text", String.valueOf(entry.claimCount));
    cmd.set(idx + " #MembersValue.Text", String.valueOf(entry.memberCount));

    // Highlight own faction row
    boolean isOwnFaction = viewerFaction != null && viewerFaction.id().equals(entry.id);
    if (isOwnFaction) {
      cmd.set(idx + " #RowContent.Background.Color", "#1a3a2a");
    }

    // Click to view faction info
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        idx + " #RowButton",
        EventData.of("Button", "ViewFaction")
            .append("FactionId", entry.id.toString()),
        false
    );
  }

  private record LeaderboardEntry(
      UUID id,
      String name,
      String tag,
      String color,
      String leaderName,
      double power,
      double maxPower,
      int claimCount,
      int memberCount,
      BigDecimal balance,
      int totalKills,
      int totalDeaths,
      double kdr
  ) {}

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
      case "SortChanged" -> {
        try {
          if (data.sortMode != null) {
            sortMode = SortMode.valueOf(data.sortMode);
          }
        } catch (IllegalArgumentException ignored) {}
        currentPage = 0;
        rebuildList(viewerFaction);
      }

      case "PrevPage" -> {
        currentPage = Math.max(0, data.page);
        rebuildList(viewerFaction);
      }

      case "NextPage" -> {
        currentPage = data.page;
        rebuildList(viewerFaction);
      }

      case "ViewFaction" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId != null) {
            Faction faction = factionManager.getFaction(factionId);
            if (faction != null) {
              guiManager.openFactionInfo(player, ref, store, playerRef, faction, "browser");
            }
          }
        }
      }

      default -> sendUpdate();
    }
  }

  private void rebuildList(@Nullable Faction viewerFaction) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildLeaderboard(cmd, events, viewerFaction);

    sendUpdate(cmd, events, false);
  }
}
