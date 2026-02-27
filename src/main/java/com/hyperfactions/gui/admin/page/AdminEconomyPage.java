package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminEconomyData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.Logger;
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
import java.util.*;

/**
 * Admin Economy overview page - shows server economy stats and faction treasury list
 * with search, sort, and pagination.
 */
public class AdminEconomyPage extends InteractiveCustomUIPage<AdminEconomyData> {

  private static final int FACTIONS_PER_PAGE = 8;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  private int currentPage = 0;

  private SortMode sortMode = SortMode.BALANCE;

  private String searchQuery = "";

  private enum SortMode {
    BALANCE,
    NAME,
    MEMBERS
  }

  /** Creates a new AdminEconomyPage. */
  public AdminEconomyPage(PlayerRef playerRef,
              FactionManager factionManager,
              EconomyManager economyManager,
              GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminEconomyData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load template
    cmd.append(UIPaths.ADMIN_ECONOMY);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "economy", cmd, events);

    // === Server Economy Stats ===
    buildServerStats(cmd);

    // === Faction List with search/sort/pagination ===
    buildFactionList(cmd, events);
  }

  private void buildServerStats(UICommandBuilder cmd) {
    java.math.BigDecimal totalBalance = economyManager.getServerTotalBalance();
    int factionCount = economyManager.getFactionEconomyCount();
    java.math.BigDecimal avgBalance = factionCount > 0
        ? totalBalance.divide(java.math.BigDecimal.valueOf(factionCount), 2, java.math.RoundingMode.HALF_UP)
        : java.math.BigDecimal.ZERO;

    cmd.set("#TotalBalance.Text", economyManager.formatCurrencyCompact(totalBalance));
    cmd.set("#EconFactionCount.Text", String.valueOf(factionCount));
    cmd.set("#AvgBalance.Text", economyManager.formatCurrencyCompact(avgBalance));
  }

  private void buildFactionList(UICommandBuilder cmd, UIEventBuilder events) {
    // Get sorted/filtered factions
    List<FactionEntry> factions = getSortedFactions();

    cmd.set("#FactionCount.Text", factions.size() + " factions");

    // Search input
    if (!searchQuery.isEmpty()) {
      cmd.set("#SearchInput.Value", searchQuery);
    }
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#SearchInput",
        EventData.of("Button", "Search").append("@SearchQuery", "#SearchInput.Value"),
        false
    );

    // Sort dropdown
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Balance"), "BALANCE"),
        new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString("Members"), "MEMBERS")
    ));
    cmd.set("#SortDropdown.Value", sortMode.name());
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value"),
        false
    );

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) factions.size() / FACTIONS_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    int startIdx = currentPage * FACTIONS_PER_PAGE;

    // Clear list and populate
    cmd.clear("#FactionList");

    int index = 0;
    for (int idx = startIdx; idx < Math.min(startIdx + FACTIONS_PER_PAGE, factions.size()); idx++) {
      FactionEntry entry = factions.get(idx);
      String sel = "#FactionList[" + index + "]";

      cmd.append("#FactionList", UIPaths.ADMIN_ECONOMY_ENTRY);
      cmd.set(sel + " #FactionName.Text", entry.faction.name());
      cmd.set(sel + " #Balance.Text", economyManager.formatCurrencyCompact(entry.economy.balance()));
      cmd.set(sel + " #MemberCount.Text", String.valueOf(entry.faction.getMemberCount()));

      // Adjust button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          sel + " #AdjustBtn",
          EventData.of("Button", "Adjust")
              .append("FactionId", entry.faction.id().toString()),
          false
      );

      // View Info button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          sel + " #ViewBtn",
          EventData.of("Button", "ViewInfo")
              .append("FactionId", entry.faction.id().toString()),
          false
      );

      index++;
    }

    // Empty state
    if (index == 0) {
      cmd.appendInline("#FactionList",
          "Label { Text: \"No factions with economy data.\"; "
          + "Style: (FontSize: 11, TextColor: #555555); Anchor: (Height: 30); }");
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

  private List<FactionEntry> getSortedFactions() {
    Map<UUID, FactionEconomy> allEconomies = economyManager.getAllEconomies();
    List<FactionEntry> entries = new ArrayList<>();

    for (Map.Entry<UUID, FactionEconomy> e : allEconomies.entrySet()) {
      Faction faction = factionManager.getFaction(e.getKey());
      if (faction == null) {
        continue;
      }
      entries.add(new FactionEntry(faction, e.getValue()));
    }

    // Filter by search query
    if (!searchQuery.isEmpty()) {
      String lowerQuery = searchQuery.toLowerCase();
      entries.removeIf(e -> {
        if (e.faction.name().toLowerCase().contains(lowerQuery)) {
          return false;
        }
        FactionMember leader = e.faction.getLeader();
        if (leader != null && leader.username().toLowerCase().contains(lowerQuery)) {
          return false;
        }
        return true;
      });
    }

    // Sort
    switch (sortMode) {
      case BALANCE -> entries.sort((a, b) ->
          b.economy.balance().compareTo(a.economy.balance()));
      case NAME -> entries.sort(Comparator.comparing(
          e -> e.faction.name(), String.CASE_INSENSITIVE_ORDER));
      case MEMBERS -> entries.sort((a, b) ->
          Integer.compare(b.faction.getMemberCount(), a.faction.getMemberCount()));
      default -> throw new IllegalStateException("Unexpected value");
    }

    return entries;
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminEconomyData data) {
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
      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery : "";
        currentPage = 0;
        rebuildList();
      }

      case "SortChanged" -> {
        try {
          if (data.sortMode != null) {
            sortMode = SortMode.valueOf(data.sortMode);
          }
        } catch (IllegalArgumentException e) {
          Logger.debugEconomy("Invalid sort mode received: %s", data.sortMode);
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

      case "Adjust" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId != null) {
            guiManager.openAdminEconomyAdjust(player, ref, store, playerRef, factionId);
          }
        }
      }

      case "ViewInfo" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId != null) {
            guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
          }
        }
      }

      case "Back" -> {
        guiManager.openAdminDashboard(player, ref, store, playerRef);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildServerStats(cmd);
    buildFactionList(cmd, events);

    sendUpdate(cmd, events, false);
  }

  private record FactionEntry(Faction faction, FactionEconomy economy) {}
}
