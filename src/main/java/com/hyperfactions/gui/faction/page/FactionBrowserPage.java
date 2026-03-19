package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionPageData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
import com.hyperfactions.util.MessageUtil;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Faction Browser page - displays a paginated list of all factions.
 * Uses IndexCards pattern with expandable entries like AdminFactionsPage.
 */
public class FactionBrowserPage extends InteractiveCustomUIPage<FactionPageData> {

  private static final String PAGE_ID = "browser";

  private static final int FACTIONS_PER_PAGE = 8;

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
      .withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final GuiManager guiManager;

  private int currentPage = 0;

  private SortMode sortMode = SortMode.POWER;

  private String searchQuery = "";

  private Set<UUID> expandedFactions = new HashSet<>();

  private enum SortMode {
    POWER,
    NAME,
    MEMBERS
  }

  /** Creates a new FactionBrowserPage. */
  public FactionBrowserPage(PlayerRef playerRef,
               FactionManager factionManager,
               PowerManager powerManager,
               GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    UUID viewerUuid = playerRef.getUuid();
    Faction viewerFaction = factionManager.getPlayerFaction(viewerUuid);
    boolean hasFaction = viewerFaction != null;

    // Load the main template
    cmd.append(UIPaths.FACTION_BROWSER);

    // Localize static labels
    cmd.set("#BrowserTitle.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.TITLE));
    cmd.set("#SearchLabel.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.SEARCH));
    cmd.set("#SortLabel.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.SORT));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.NEXT));

    // Setup navigation bar - use new player nav when no faction
    if (viewerFaction != null) {
      NavBarHelper.setupBar(playerRef, viewerFaction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Build faction list
    buildFactionList(cmd, events, viewerFaction);
  }

  private void buildFactionList(UICommandBuilder cmd, UIEventBuilder events, Faction viewerFaction) {
    // Get all factions sorted and filtered
    List<FactionEntry> entries = buildFactionEntryList();

    cmd.set("#FactionCount.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.FACTION_COUNT, entries.size()));

    // Sort dropdown
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.GuiCommon.SORT_POWER)), "POWER"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.BrowserGui.SORT_NAME)), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, GuiKeys.GuiCommon.SORT_MEMBERS)), "MEMBERS")
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

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / FACTIONS_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    int startIdx = currentPage * FACTIONS_PER_PAGE;

    // Clear FactionList, then create IndexCards container inside it
    cmd.clear("#FactionList");
    cmd.appendInline("#FactionList", "Group #IndexCards { LayoutMode: Top; }");

    // Build entries
    int i = 0;
    for (int idx = startIdx; idx < Math.min(startIdx + FACTIONS_PER_PAGE, entries.size()); idx++) {
      FactionEntry entry = entries.get(idx);
      buildFactionEntry(cmd, events, i, entry, viewerFaction);
      i++;
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

  private List<FactionEntry> buildFactionEntryList() {
    List<FactionEntry> entries = new ArrayList<>();
    String lowerQuery = searchQuery.toLowerCase();

    for (Faction faction : factionManager.getAllFactions()) {
      // Apply search filter
      if (!searchQuery.isEmpty()) {
        boolean matches = faction.name().toLowerCase().contains(lowerQuery);
        if (!matches && faction.tag() != null) {
          matches = faction.tag().toLowerCase().contains(lowerQuery);
        }
        if (!matches) {
          continue;
        }
      }

      PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());
      FactionMember leader = faction.getLeader();
      entries.add(new FactionEntry(
          faction.id(),
          faction.name(),
          faction.color() != null ? faction.color() : "#00FFFF",
          faction.members().size(),
          stats.currentPower(),
          stats.maxPower(),
          faction.claims().size(),
          leader != null ? leader.username() : HFMessages.get(playerRef, CommonKeys.Common.NONE),
          faction.open(),
          faction.description(),
          faction.createdAt()
      ));
    }

    // Sort
    switch (sortMode) {
      case POWER -> entries.sort(Comparator.comparingDouble(FactionEntry::power).reversed());
      case MEMBERS -> entries.sort(Comparator.comparingInt(FactionEntry::memberCount).reversed());
      case NAME -> entries.sort(Comparator.comparing(FactionEntry::name, String.CASE_INSENSITIVE_ORDER));
      default -> throw new IllegalStateException("Unexpected value");
    }

    return entries;
  }

  private void buildFactionEntry(UICommandBuilder cmd, UIEventBuilder events, int index,
                 FactionEntry entry, Faction viewerFaction) {
    boolean isExpanded = expandedFactions.contains(entry.id);
    boolean isOwnFaction = viewerFaction != null && viewerFaction.id().equals(entry.id);

    // Append entry template to IndexCards
    cmd.append("#IndexCards", UIPaths.FACTION_BROWSE_ENTRY);

    // Use indexed selector
    String idx = "#IndexCards[" + index + "]";

    // Basic info
    cmd.set(idx + " #FactionName.Text", entry.name);
    cmd.set(idx + " #LeaderName.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.LEADER_LABEL, entry.leaderName));

    // Stats
    cmd.set(idx + " #PowerDisplay.Text", String.format("%.0f/%.0f", entry.power, entry.maxPower));
    cmd.set(idx + " #ClaimsDisplay.Text", String.valueOf(entry.claimCount));
    cmd.set(idx + " #MemberCount.Text", String.valueOf(entry.memberCount));

    // Localized stat labels
    cmd.set(idx + " #PowerLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_POWER));
    cmd.set(idx + " #ClaimsLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_CLAIMS));
    cmd.set(idx + " #MemberLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_MEMBERS));

    // Own faction indicator
    if (isOwnFaction) {
      cmd.set(idx + " #OwnIndicator.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.OWN_FACTION));
    }

    // Relation indicator (only for faction members viewing other factions)
    if (viewerFaction != null && !isOwnFaction) {
      RelationType relation = viewerFaction.getRelationType(entry.id);
      if (relation == RelationType.ALLY) {
        cmd.append(idx + " #RelationSlot", UIPaths.INDICATOR_ALLY);
      } else if (relation == RelationType.ENEMY) {
        cmd.append(idx + " #RelationSlot", UIPaths.INDICATOR_ENEMY);
      }
    }

    // Expansion state
    cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
    cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
    cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

    // Bind header click for expand/collapse
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        idx + " #Header",
        EventData.of("Button", "ToggleExpanded")
            .append("FactionId", entry.id.toString()),
        false
    );

    // Extended info (only set values if expanded)
    if (isExpanded) {
      // Localized extended labels
      cmd.set(idx + " #RecruitmentLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_RECRUITMENT));
      cmd.set(idx + " #CreatedLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_CREATED));
      cmd.set(idx + " #DescriptionLabel.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.LABEL_DESCRIPTION));
      cmd.set(idx + " #ViewInfoBtn.Text", HFMessages.get(playerRef, GuiKeys.BrowserGui.VIEW_INFO_BTN));

      // Recruitment status
      cmd.set(idx + " #RecruitmentStatus.Text", entry.isOpen
          ? HFMessages.get(playerRef, GuiKeys.FactionInfoGui.STATUS_OPEN)
          : HFMessages.get(playerRef, GuiKeys.FactionInfoGui.STATUS_INVITE_ONLY));
      cmd.set(idx + " #RecruitmentStatus.Style.TextColor", entry.isOpen ? "#44CC44" : "#FFAA00");

      // Created date
      String createdDate = DATE_FORMAT.format(Instant.ofEpochMilli(entry.createdAt));
      cmd.set(idx + " #CreatedDate.Text", createdDate);

      // Description
      if (entry.description != null && !entry.description.isEmpty()) {
        String desc = entry.description.length() > 60
            ? entry.description.substring(0, 57) + "..."
            : entry.description;
        cmd.set(idx + " #Description.Text", desc);
      } else {
        cmd.set(idx + " #Description.Text", HFMessages.get(playerRef, CommonKeys.Common.NO_DESCRIPTION));
      }

      // View Info button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #ViewInfoBtn",
          EventData.of("Button", "ViewFaction")
              .append("FactionId", entry.id.toString()),
          false
      );
    }
  }

  private record FactionEntry(
      UUID id,
      String name,
      String color,
      int memberCount,
      double power,
      double maxPower,
      int claimCount,
      String leaderName,
      boolean isOpen,
      String description,
      long createdAt
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

    // Handle navigation - use new player nav when no faction
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
      case "ToggleExpanded" -> {
        if (data.factionId != null) {
          UUID uuid = UuidUtil.parseOrNull(data.factionId);
          if (uuid == null) {
            sendUpdate();
            return;
          }
          if (expandedFactions.contains(uuid)) {
            expandedFactions.remove(uuid);
          } else {
            expandedFactions.add(uuid);
          }
          rebuildList(viewerFaction);
        }
      }

      case "SortChanged" -> {
        try {
          if (data.sortMode != null) {
            sortMode = SortMode.valueOf(data.sortMode);
          }
        } catch (IllegalArgumentException ignored) {}
        currentPage = 0;
        expandedFactions.clear();
        rebuildList(viewerFaction);
      }

      case "PrevPage" -> {
        currentPage = Math.max(0, data.page);
        expandedFactions.clear();
        rebuildList(viewerFaction);
      }

      case "NextPage" -> {
        currentPage = data.page;
        expandedFactions.clear();
        rebuildList(viewerFaction);
      }

      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery : "";
        currentPage = 0;
        expandedFactions.clear();
        rebuildList(viewerFaction);
      }

      case "ViewFaction" -> handleViewFaction(player, ref, store, playerRef, data);

      default -> sendUpdate();
    }
  }

  private void handleViewFaction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 PlayerRef playerRef, FactionPageData data) {
    if (data.factionId == null) {
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.error(playerRef, GuiKeys.BrowserGui.INVALID_FACTION));
      return;
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction != null) {
      // Open the FactionInfoPage GUI with source page tracking
      guiManager.openFactionInfo(player, ref, store, playerRef, faction, "browser");
    }
  }

  /**
   * Rebuild only the list portion of the page, not the entire template.
   * This avoids re-appending the whole page and breaking the nav bar.
   */
  private void rebuildList(Faction viewerFaction) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildFactionList(cmd, events, viewerFaction);

    sendUpdate(cmd, events, false);
  }
}
