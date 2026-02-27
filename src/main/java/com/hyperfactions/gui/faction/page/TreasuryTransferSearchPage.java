package com.hyperfactions.gui.faction.page;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.TransferSearchData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.PlayerResolver;
import com.hyperfactions.util.UiUtil;
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
import java.math.BigDecimal;
import java.util.*;

/**
 * Transfer search modal — unified search for players and factions.
 * Selecting a target navigates to the transfer confirmation page.
 */
public class TreasuryTransferSearchPage extends InteractiveCustomUIPage<TransferSearchData> {

  private static final int RESULTS_PER_PAGE = 5;

  private static final int SEARCH_DEBOUNCE_TICKS = 10; // 500ms at 20 TPS

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  private final HyperFactions plugin;

  private final Faction faction;

  private String searchQuery = "";

  private int currentPage = 0;

  private int pendingSearchTaskId = -1;

  /** Creates a new TreasuryTransferSearchPage. */
  public TreasuryTransferSearchPage(PlayerRef playerRef,
                   FactionManager factionManager,
                   EconomyManager economyManager,
                   GuiManager guiManager,
                   HyperFactions plugin,
                   Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, TransferSearchData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
    this.plugin = plugin;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.TREASURY_TRANSFER_SEARCH);
    buildResultsContent(cmd, events);
  }

  private void buildResultsContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Search binding
    if (!searchQuery.isEmpty()) {
      cmd.set("#SearchInput.Value", searchQuery);
    }
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
        EventData.of("Button", "Search").append("@SearchQuery", "#SearchInput.Value"), false);

    // Cancel button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#CancelBtn",
        EventData.of("Button", "Cancel"), false);

    // Clear previous results
    cmd.clear("#ResultsList");

    List<SearchResult> results = getSearchResults();

    if (results.isEmpty()) {
      if (searchQuery.isEmpty()) {
        cmd.set("#EmptyText.Text", "Search for a player or faction");
      } else {
        cmd.set("#EmptyText.Text", "No results for '" + UiUtil.sanitize(searchQuery) + "'");
      }
      cmd.set("#PageInfo.Text", "0/0");
    } else {
      cmd.set("#EmptyText.Text", "");

      int totalPages = Math.max(1, (int) Math.ceil((double) results.size() / RESULTS_PER_PAGE));
      currentPage = Math.min(currentPage, totalPages - 1);
      int startIdx = currentPage * RESULTS_PER_PAGE;

      // Build result cards using separate .ui entry file
      for (int i = 0; i < RESULTS_PER_PAGE; i++) {
        int idx = startIdx + i;
        if (idx >= results.size()) {
          break;
        }

        SearchResult result = results.get(idx);
        String tagColor = "player".equals(result.type) ? "#55FF55" : "#00AAFF";
        String typeTag = "player".equals(result.type) ? "[Player]" : "[Faction]";

        cmd.append("#ResultsList", UIPaths.TRANSFER_SEARCH_ENTRY);
        String prefix = "#ResultsList[" + i + "] ";

        cmd.set(prefix + "#Name.Text", result.name);
        cmd.set(prefix + "#TypeTag.Text", typeTag);
        cmd.set(prefix + "#TypeTag.Style.TextColor", tagColor);
        cmd.set(prefix + "#Subtitle.Text", result.subtitle);

        events.addEventBinding(CustomUIEventBindingType.Activating,
            prefix + "#SelectBtn",
            EventData.of("Button", "Select")
                .append("TargetId", result.id)
                .append("TargetName", result.name)
                .append("TargetType", result.type),
            false);
      }

      // Pagination
      cmd.set("#PageInfo.Text", (currentPage + 1) + "/" + totalPages);

      if (currentPage > 0) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
            EventData.of("Button", "Page").append("Page", String.valueOf(currentPage - 1)), false);
      }
      if (currentPage < totalPages - 1) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
            EventData.of("Button", "Page").append("Page", String.valueOf(currentPage + 1)), false);
      }
    }
  }

  private List<SearchResult> getSearchResults() {
    if (searchQuery.isEmpty()) {
      return Collections.emptyList();
    }

    List<SearchResult> results = new ArrayList<>();
    UUID selfUuid = playerRef.getUuid();
    String query = searchQuery.toLowerCase();

    // Search players (online + offline faction members + PlayerDB fallback)
    List<PlayerResolver.ResolvedPlayer> players = PlayerResolver.search(plugin, searchQuery, selfUuid);
    for (PlayerResolver.ResolvedPlayer p : players) {
      String subtitle = switch (p.source()) {
        case ONLINE -> "Online" + (p.factionName() != null ? " - " + p.factionName() : "");
        case FACTION_MEMBER -> "Offline - " + p.factionName();
        case PLAYER_DB -> "Hytale player";
      };
      results.add(new SearchResult(p.uuid().toString(), p.username(), "player", subtitle));
    }

    // Search factions
    for (Faction f : factionManager.getAllFactions()) {
      if (f.id().equals(faction.id())) {
        continue;
      }

      boolean matches = f.name().toLowerCase().contains(query);
      if (f.tag() != null) {
        matches = matches || f.tag().toLowerCase().contains(query);
      }
      if (!matches) {
        continue;
      }

      FactionEconomy econ = economyManager.getEconomy(f.id());
      BigDecimal bal = econ != null ? econ.balance() : BigDecimal.ZERO;
      String subtitle = f.members().size() + " members, " + economyManager.formatCurrency(bal);
      results.add(new SearchResult(f.id().toString(), f.name(), "faction", subtitle));
    }

    // Sort: exact matches first, then alphabetical
    results.sort((a, b) -> {
      boolean aExact = a.name.equalsIgnoreCase(searchQuery);
      boolean bExact = b.name.equalsIgnoreCase(searchQuery);
      if (aExact != bExact) {
        return aExact ? -1 : 1;
      }
      return a.name.compareToIgnoreCase(b.name);
    });

    return results;
  }

  private record SearchResult(String id, String name, String type, String subtitle) {}

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                TransferSearchData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Cancel" -> {
        Faction fresh = factionManager.getFaction(faction.id());
        if (fresh != null) {
          guiManager.openFactionTreasury(player, ref, store, playerRef, fresh);
        }
      }
      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery.trim() : "";
        currentPage = 0;
        // Cancel any pending debounced search
        if (pendingSearchTaskId != -1) {
          plugin.cancelTask(pendingSearchTaskId);
          pendingSearchTaskId = -1;
        }

        // Debounce: delay the search to avoid spamming PlayerDB on every keystroke
        pendingSearchTaskId = plugin.scheduleDelayedTask(SEARCH_DEBOUNCE_TICKS, () -> {
          pendingSearchTaskId = -1;
          rebuildResults();
        });
      }
      case "Page" -> {
        currentPage = data.page;
        rebuildResults();
      }
      case "Select" -> {
        if (data.targetId != null && data.targetName != null && data.targetType != null) {
          guiManager.openTreasuryTransferConfirm(player, ref, store, playerRef, faction,
              data.targetId, data.targetName, data.targetType);
        } else {
          sendUpdate();
        }
      }
      default -> sendUpdate();
    }
  }

  private void rebuildResults() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildResultsContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

}
