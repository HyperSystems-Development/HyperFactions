package com.hyperfactions.gui.newplayer.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.gui.newplayer.data.NewPlayerPageData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.MessageUtil;
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
import java.util.*;

/**
 * Enhanced Browse Factions page for new players.
 * Uses IndexCards pattern with expandable entries like AdminFactionsPage.
 * Default landing page showing all factions with JOIN/REQUEST buttons.
 */
public class NewPlayerBrowsePage extends InteractiveCustomUIPage<NewPlayerPageData> {

  private static final String PAGE_ID = "browse";

  private static final int FACTIONS_PER_PAGE = 8;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final InviteManager inviteManager;

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

  /** Creates a new NewPlayerBrowsePage. */
  public NewPlayerBrowsePage(PlayerRef playerRef,
               FactionManager factionManager,
               PowerManager powerManager,
               InviteManager inviteManager,
               GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, NewPlayerPageData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.inviteManager = inviteManager;
    this.guiManager = guiManager;
  }

  /**
   * Constructor with custom page and sort state.
   */
  public NewPlayerBrowsePage(PlayerRef playerRef,
               FactionManager factionManager,
               PowerManager powerManager,
               InviteManager inviteManager,
               GuiManager guiManager,
               int page,
               String sortBy) {
    this(playerRef, factionManager, powerManager, inviteManager, guiManager);
    this.currentPage = page;
    if (sortBy != null) {
      this.sortMode = switch (sortBy) {
        case "name" -> SortMode.NAME;
        case "members" -> SortMode.MEMBERS;
        default -> SortMode.POWER;
      };
    }
  }

  /**
   * Constructor with custom page, sort, and search state.
   */
  public NewPlayerBrowsePage(PlayerRef playerRef,
               FactionManager factionManager,
               PowerManager powerManager,
               InviteManager inviteManager,
               GuiManager guiManager,
               int page,
               String sortBy,
               String searchQuery) {
    this(playerRef, factionManager, powerManager, inviteManager, guiManager, page, sortBy);
    this.searchQuery = searchQuery != null ? searchQuery : "";
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the main template
    cmd.append(UIPaths.NEWPLAYER_BROWSE);

    // Static labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BROWSE_TITLE));
    cmd.set("#SearchLabel.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.SEARCH_LABEL));
    cmd.set("#SortLabel.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.SORT_LABEL));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.PREV_BTN));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.NEXT_BTN));

    // Setup navigation bar for new players
    NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);

    // Build faction list
    buildFactionList(cmd, events);
  }

  private void buildFactionList(UICommandBuilder cmd, UIEventBuilder events) {
    UUID viewerUuid = playerRef.getUuid();

    // Get all factions sorted and filtered
    List<FactionEntry> entries = buildFactionEntryList();

    cmd.set("#FactionCount.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.FACTION_COUNT, entries.size()));
    cmd.set("#Subtitle.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BROWSE_SUBTITLE));

    // Sort dropdown
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.NewPlayerGui.SORT_POWER)), "POWER"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.NewPlayerGui.SORT_NAME)), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.NewPlayerGui.SORT_MEMBERS)), "MEMBERS")
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
      buildFactionEntry(cmd, events, i, entry, viewerUuid);
      i++;
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
          leader != null ? leader.username() : "None",
          faction.open(),
          faction.description()
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
                 FactionEntry entry, UUID viewerUuid) {
    boolean isExpanded = expandedFactions.contains(entry.id);

    // Check player's status with this faction
    boolean hasInvite = inviteManager.hasInvite(entry.id, viewerUuid);
    boolean hasRequest = guiManager.getJoinRequestManager().hasRequest(entry.id, viewerUuid);

    // Append entry template to IndexCards
    cmd.append("#IndexCards", UIPaths.NEWPLAYER_FACTION_ENTRY);

    // Use indexed selector
    String idx = "#IndexCards[" + index + "]";

    // Basic info
    cmd.set(idx + " #FactionName.Text", entry.name);

    // Recruitment badge
    if (entry.isOpen) {
      cmd.set(idx + " #RecruitmentBadge.Text", HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN));
      cmd.set(idx + " #RecruitmentBadge.Style.TextColor", "#44CC44");
    } else {
      cmd.set(idx + " #RecruitmentBadge.Text", HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY));
      cmd.set(idx + " #RecruitmentBadge.Style.TextColor", "#FFAA00");
    }

    // Stats
    cmd.set(idx + " #PowerDisplay.Text", String.format("%.0f/%.0f", entry.power, entry.maxPower));
    cmd.set(idx + " #MemberCount.Text", String.valueOf(entry.memberCount));

    // Localized stat labels
    cmd.set(idx + " #PowerLabel.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.LABEL_POWER));
    cmd.set(idx + " #MemberLabel.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.LABEL_MEMBERS));

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
      cmd.set(idx + " #LeaderLabel.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.LABEL_LEADER));
      cmd.set(idx + " #ClaimsLabel.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.LABEL_CLAIMS));
      cmd.set(idx + " #DescriptionLabel.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.LABEL_DESCRIPTION));
      cmd.set(idx + " #ViewInfoBtn.Text", HFMessages.get(playerRef, MessageKeys.BrowserGui.VIEW_INFO_BTN));

      // Leader and claims
      cmd.set(idx + " #LeaderName.Text", entry.leaderName);
      cmd.set(idx + " #ClaimsDisplay.Text", String.valueOf(entry.claimCount));

      // Description
      if (entry.description != null && !entry.description.isEmpty()) {
        String desc = entry.description.length() > 60
            ? entry.description.substring(0, 57) + "..."
            : entry.description;
        cmd.set(idx + " #Description.Text", desc);
      }

      // Action button - varies by player's status with faction
      // Note: TextButtons can't have Style.TextColor changed dynamically - use button text to convey state
      if (hasInvite) {
        // Player has pending invite - show ACCEPT button
        cmd.set(idx + " #ActionBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BTN_ACCEPT));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #ActionBtn",
            EventData.of("Button", "AcceptInvite")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );
      } else if (hasRequest) {
        // Player already requested - show PENDING button (goes to invites page)
        cmd.set(idx + " #ActionBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BTN_PENDING));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #ActionBtn",
            EventData.of("Button", "ViewRequests"),
            false
        );
      } else if (entry.isOpen) {
        // Open faction - JOIN button
        cmd.set(idx + " #ActionBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BTN_JOIN));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #ActionBtn",
            EventData.of("Button", "JoinFaction")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );
      } else {
        // Invite-only faction - REQUEST button
        cmd.set(idx + " #ActionBtn.Text", HFMessages.get(playerRef, MessageKeys.NewPlayerGui.BTN_REQUEST));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #ActionBtn",
            EventData.of("Button", "RequestJoin")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );
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
      String description
  ) {}

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                NewPlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
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
          rebuildList();
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
        rebuildList();
      }

      case "PrevPage" -> {
        currentPage = Math.max(0, data.page);
        expandedFactions.clear();
        rebuildList();
      }

      case "NextPage" -> {
        currentPage = data.page;
        expandedFactions.clear();
        rebuildList();
      }

      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery : "";
        currentPage = 0;
        expandedFactions.clear();
        rebuildList();
      }

      case "JoinFaction" -> handleJoinFaction(player, ref, store, playerRef, data);

      case "AcceptInvite" -> handleAcceptInvite(player, ref, store, playerRef, data);

      case "RequestJoin" -> handleRequestJoin(player, ref, store, playerRef, data);

      case "ViewRequests" -> guiManager.openInvitesPage(player, ref, store, playerRef);

      case "ViewFaction" -> handleViewFaction(player, ref, store, playerRef, data);

      default -> sendUpdate();
    }
  }

  private void handleViewFaction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVALID_FACTION));
      return;
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction != null) {
      guiManager.openFactionInfo(player, ref, store, playerRef, faction, "newplayer_browser");
    }
  }

  private void handleJoinFaction(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                 PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVALID_FACTION));
      sendUpdate();
      return;
    }

    Faction faction = factionManager.getFaction(factionId);

    if (faction == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.Common.FACTION_NOT_FOUND));
      sendUpdate();
      return;
    }

    if (!faction.open()) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVITE_ONLY_MSG));
      sendUpdate();
      return;
    }

    // Join the faction using addMember
    FactionManager.FactionResult result = factionManager.addMember(
        factionId,
        playerRef.getUuid(),
        playerRef.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        player.sendMessage(MessageUtil.successText(playerRef, MessageKeys.NewPlayerGui.JOINED, faction.name()));
        // Clear any pending invites
        inviteManager.clearPlayerInvites(playerRef.getUuid());
        // Open faction dashboard - use fresh faction data
        Faction freshFaction = factionManager.getPlayerFaction(playerRef.getUuid());
        if (freshFaction != null) {
          guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
        } else {
          // Fallback: use the faction we looked up earlier (shouldn't happen but safety net)
          Faction updatedFaction = factionManager.getFaction(factionId);
          if (updatedFaction != null) {
            guiManager.openFactionDashboard(player, ref, store, playerRef, updatedFaction);
          } else {
            // Last resort: close page and let them reopen
            player.sendMessage(MessageUtil.text(playerRef, MessageKeys.NewPlayerGui.WELCOME_HINT, MessageUtil.COLOR_GRAY));
            guiManager.closePage(player, ref, store);
          }
        }
      }
      case ALREADY_IN_FACTION -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.Common.ALREADY_IN_FACTION));
        sendUpdate();
      }
      case FACTION_NOT_FOUND -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.Common.FACTION_NOT_FOUND));
        sendUpdate();
      }
      case FACTION_FULL -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.FACTION_FULL));
        sendUpdate();
      }
      default -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.JOIN_FAILED));
        sendUpdate();
      }
    }
  }

  private void handleAcceptInvite(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVALID_FACTION));
      sendUpdate();
      return;
    }

    UUID playerUuid = playerRef.getUuid();

    // Check if invite exists
    if (!inviteManager.hasInvite(factionId, playerUuid)) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVITE_EXPIRED));
      sendUpdate();
      return;
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.FACTION_GONE));
      inviteManager.removeInvite(factionId, playerUuid);
      sendUpdate();
      return;
    }

    // Join the faction using addMember (invite allows joining closed factions)
    FactionManager.FactionResult result = factionManager.addMember(
        factionId,
        playerUuid,
        playerRef.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        player.sendMessage(MessageUtil.successText(playerRef, MessageKeys.NewPlayerGui.JOINED, faction.name()));
        // Clear invite and other pending invites
        inviteManager.clearPlayerInvites(playerUuid);
        // Open faction dashboard
        Faction freshFaction = factionManager.getPlayerFaction(playerUuid);
        if (freshFaction != null) {
          guiManager.openFactionDashboard(player, ref, store, playerRef, freshFaction);
        }
      }
      case ALREADY_IN_FACTION -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.Common.ALREADY_IN_FACTION));
        sendUpdate();
      }
      case FACTION_FULL -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.FACTION_FULL));
        sendUpdate();
      }
      default -> {
        player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.JOIN_FAILED));
        sendUpdate();
      }
    }
  }

  private void handleRequestJoin(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                  PlayerRef playerRef, NewPlayerPageData data) {
    if (data.factionId == null || data.factionName == null) {
      sendUpdate();
      return;
    }

    UUID factionId = UuidUtil.parseOrNull(data.factionId);
    if (factionId == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.NewPlayerGui.INVALID_FACTION));
      sendUpdate();
      return;
    }

    UUID playerUuid = playerRef.getUuid();

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.Common.FACTION_NOT_FOUND));
      sendUpdate();
      return;
    }

    // Check if faction is open (shouldn't happen, but just in case)
    if (faction.open()) {
      player.sendMessage(MessageUtil.text(playerRef, MessageKeys.NewPlayerGui.FACTION_OPEN_HINT, MessageUtil.COLOR_GOLD));
      sendUpdate();
      return;
    }

    // Check if player already has a pending request
    var joinRequestManager = guiManager.getJoinRequestManager();
    if (joinRequestManager.hasRequest(factionId, playerUuid)) {
      player.sendMessage(MessageUtil.text(playerRef, MessageKeys.NewPlayerGui.ALREADY_REQUESTED, MessageUtil.COLOR_GOLD));
      sendUpdate();
      return;
    }

    // Check if player already has an invite (they should accept it)
    if (inviteManager.hasInvite(factionId, playerUuid)) {
      player.sendMessage(MessageUtil.text(playerRef, MessageKeys.NewPlayerGui.HAS_INVITE_HINT, MessageUtil.COLOR_GOLD));
      sendUpdate();
      return;
    }

    // Create the join request
    joinRequestManager.createRequest(factionId, playerUuid, playerRef.getUsername(), null);

    player.sendMessage(MessageUtil.successText(playerRef, MessageKeys.NewPlayerGui.REQUEST_SENT, faction.name()));
    player.sendMessage(MessageUtil.text(playerRef, MessageKeys.NewPlayerGui.OFFICER_REVIEW, MessageUtil.COLOR_GRAY));

    // Rebuild list to show updated state (PENDING button)
    rebuildList();
  }

  /**
   * Rebuild only the list portion of the page, not the entire template.
   * This avoids re-appending the whole page and breaking the nav bar.
   */
  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildFactionList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
