package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionsData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Admin Factions page - provides admin controls for faction management.
 * Uses IndexCards pattern with expanding rows like FactionMembersPage.
 */
public class AdminFactionsPage extends InteractiveCustomUIPage<AdminFactionsData> {

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

  /** Creates a new AdminFactionsPage. */
  public AdminFactionsPage(PlayerRef playerRef,
              FactionManager factionManager,
              PowerManager powerManager,
              GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionsData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the main template first
    cmd.append(UIPaths.ADMIN_FACTIONS);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

    // Localize page title and common labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_FACTIONS));
    cmd.set("#SearchLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SEARCH));
    cmd.set("#SortLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SORT));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_NEXT));

    // Build faction list
    buildFactionList(cmd, events);
  }

  private void buildFactionList(UICommandBuilder cmd, UIEventBuilder events) {
    // Get all factions sorted
    List<Faction> factions = getSortedFactions();

    cmd.set("#FactionCount.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTIONS_SUFFIX, factions.size()));

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
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.SORT_POWER)), "POWER"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.SORT_NAME)), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.SORT_MEMBERS)), "MEMBERS")
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

    // Clear FactionList, then create IndexCards container inside it
    cmd.clear("#FactionList");
    cmd.appendInline("#FactionList", "Group #IndexCards { LayoutMode: Top; }");

    // Build entries
    int i = 0;
    for (int idx = startIdx; idx < Math.min(startIdx + FACTIONS_PER_PAGE, factions.size()); idx++) {
      Faction faction = factions.get(idx);
      buildFactionEntry(cmd, events, i, faction);
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

  private void buildFactionEntry(UICommandBuilder cmd, UIEventBuilder events, int index, Faction faction) {
    boolean isExpanded = expandedFactions.contains(faction.id());
    PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());

    // Append entry template to IndexCards
    cmd.append("#IndexCards", UIPaths.ADMIN_FACTION_ENTRY);

    // Use indexed selector like FactionMembersPage does
    String idx = "#IndexCards[" + index + "]";

    // Basic info
    cmd.set(idx + " #FactionName.Text", faction.name());

    // Leader info
    FactionMember leader = faction.getLeader();
    String leaderName = leader != null ? leader.username() : HFMessages.get(playerRef, MessageKeys.Common.NONE);
    cmd.set(idx + " #LeaderName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.LEADER_PREFIX, leaderName));

    // Stats
    cmd.set(idx + " #PowerDisplay.Text", String.format("%.0f/%.0f", stats.currentPower(), stats.maxPower()));
    cmd.set(idx + " #ClaimsDisplay.Text", String.valueOf(faction.claims().size()));
    cmd.set(idx + " #MemberCount.Text", String.valueOf(faction.members().size()));

    // Localize stat labels
    cmd.set(idx + " #PowerLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_POWER));
    cmd.set(idx + " #ClaimsLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_CLAIMS));
    cmd.set(idx + " #MembersLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_MEMBERS));

    // Expansion state
    cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
    cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
    cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

    // Bind to #Header TextButton inside the indexed element
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        idx + " #Header",
        EventData.of("Button", "ToggleExpanded")
            .append("FactionUuid", faction.id().toString()),
        false
    );

    // Extended info (only set values if expanded)
    if (isExpanded) {
      // Localize expanded labels
      cmd.set(idx + " #CreatedLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_CREATED));
      cmd.set(idx + " #HomeLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_HOME));

      // Localize button texts
      cmd.set(idx + " #TpHomeBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_TP_HOME));
      cmd.set(idx + " #ViewInfoBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_VIEW_INFO));
      cmd.set(idx + " #MembersBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_MEMBERS_BTN));
      cmd.set(idx + " #SettingsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_SETTINGS));
      cmd.set(idx + " #UnclaimAllBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_UNCLAIM_ALL));
      cmd.set(idx + " #DisbandBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ENTRY_DISBAND));

      // Created date
      String createdDate = DATE_FORMAT.format(Instant.ofEpochMilli(faction.createdAt()));
      cmd.set(idx + " #CreatedDate.Text", createdDate);

      // Home location
      if (faction.hasHome()) {
        Faction.FactionHome home = faction.home();
        cmd.set(idx + " #HomeLocation.Text",
            String.format("%s (%.0f, %.0f, %.0f)", home.world(), home.x(), home.y(), home.z()));
        cmd.set(idx + " #TpHomeBtn.Visible", true);
      } else {
        cmd.set(idx + " #HomeLocation.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.NOT_SET));
        cmd.set(idx + " #TpHomeBtn.Visible", false);
      }

      // TP Home button
      if (faction.hasHome()) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #TpHomeBtn",
            EventData.of("Button", "TpHome")
                .append("FactionId", faction.id().toString())
                .append("FactionName", faction.name()),
            false
        );
      }

      // View Info button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #ViewInfoBtn",
          EventData.of("Button", "ViewInfo")
              .append("FactionId", faction.id().toString()),
          false
      );

      // Members button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #MembersBtn",
          EventData.of("Button", "ViewMembers")
              .append("FactionId", faction.id().toString()),
          false
      );

      // Settings button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #SettingsBtn",
          EventData.of("Button", "ViewSettings")
              .append("FactionId", faction.id().toString()),
          false
      );

      // Unclaim All button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #UnclaimAllBtn",
          EventData.of("Button", "UnclaimAll")
              .append("FactionId", faction.id().toString())
              .append("FactionName", faction.name()),
          false
      );

      // Disband button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DisbandBtn",
          EventData.of("Button", "Disband")
              .append("FactionId", faction.id().toString())
              .append("FactionName", faction.name()),
          false
      );
    }
  }

  private List<Faction> getSortedFactions() {
    List<Faction> factions = new ArrayList<>(factionManager.getAllFactions());

    // Filter by search query
    if (!searchQuery.isEmpty()) {
      String lowerQuery = searchQuery.toLowerCase();
      factions.removeIf(f -> {
        if (f.name().toLowerCase().contains(lowerQuery)) {
          return false;
        }
        FactionMember leader = f.getLeader();
        if (leader != null && leader.username().toLowerCase().contains(lowerQuery)) {
          return false;
        }
        return true;
      });
    }

    switch (sortMode) {
      case POWER -> factions.sort((a, b) -> {
        double powerA = powerManager.getFactionPowerStats(a.id()).currentPower();
        double powerB = powerManager.getFactionPowerStats(b.id()).currentPower();
        return Double.compare(powerB, powerA);
      });
      case NAME -> factions.sort(Comparator.comparing(Faction::name, String.CASE_INSENSITIVE_ORDER));
      case MEMBERS -> factions.sort((a, b) ->
          Integer.compare(b.members().size(), a.members().size()));
      default -> throw new IllegalStateException("Unexpected value");
    }

    return factions;
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminFactionsData data) {
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
      case "ToggleExpanded" -> {
        if (data.factionUuid != null) {
          UUID uuid = UuidUtil.parseOrNull(data.factionUuid);
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

      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery : "";
        currentPage = 0;
        expandedFactions.clear();
        rebuildList();
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

      case "TpHome" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null && faction.hasHome()) {
            Faction.FactionHome home = faction.home();
            guiManager.closePage(player, ref, store);

            // Get target world
            World targetWorld = Universe.get().getWorld(home.world());
            if (targetWorld == null) {
              player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.FAC_WORLD_NOT_FOUND));
              return;
            }

            // Execute teleport on the target world's thread using createForPlayer for proper player teleportation
            targetWorld.execute(() -> {
              Vector3d position = new Vector3d(home.x(), home.y(), home.z());
              Vector3f rotation = new Vector3f(home.pitch(), home.yaw(), 0);
              Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
              store.addComponent(ref, Teleport.getComponentType(), teleport);
            });

            player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.FAC_TELEPORTED, "#00FFFF", faction.name()));
          } else {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.FAC_NO_HOME));
          }
        }
      }

      case "ViewInfo" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            // Use admin version to maintain admin nav context
            guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
          }
        }
      }

      case "ViewMembers" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            guiManager.openAdminFactionMembers(player, ref, store, playerRef, factionId);
          }
        }
      }

      case "ViewSettings" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            guiManager.openAdminFactionSettings(player, ref, store, playerRef, factionId);
          }
        }
      }

      case "Disband" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          guiManager.openAdminDisbandConfirm(player, ref, store, playerRef, factionId, data.factionName);
        }
      }

      case "UnclaimAll" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            int claimCount = faction.claims().size();
            // Open confirmation modal instead of chat message
            guiManager.openAdminUnclaimAllConfirm(player, ref, store, playerRef,
                factionId, data.factionName, claimCount);
          }
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildFactionList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
