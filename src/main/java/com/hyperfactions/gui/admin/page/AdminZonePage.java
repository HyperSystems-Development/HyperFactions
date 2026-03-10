package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminZoneData;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.Value;
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
 * Admin Zone page - manage safe zones and war zones.
 * Uses expanding row pattern for zone entries.
 */
public class AdminZonePage extends InteractiveCustomUIPage<AdminZoneData> {

  private static final int ZONES_PER_PAGE = 8;

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
      .withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;

  private final ZoneManager zoneManager;

  private final GuiManager guiManager;

  private String currentTab = "all"; // all, safe, war
  private int currentPage = 0;

  private Set<UUID> expandedZones = new HashSet<>();

  private ZoneSortMode zoneSortMode = ZoneSortMode.NAME;

  private enum ZoneSortMode {
    NAME,
    TYPE,
    CHUNKS,
    WORLD
  }

  /** Creates a new AdminZonePage. */
  public AdminZonePage(PlayerRef playerRef,
            ZoneManager zoneManager,
            GuiManager guiManager) {
    this(playerRef, zoneManager, guiManager, "all", 0);
  }

  /** Creates a new AdminZonePage. */
  public AdminZonePage(PlayerRef playerRef,
            ZoneManager zoneManager,
            GuiManager guiManager,
            String tab,
            int page) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminZoneData.CODEC);
    this.playerRef = playerRef;
    this.zoneManager = zoneManager;
    this.guiManager = guiManager;
    this.currentTab = tab != null ? tab : "all";
    this.currentPage = page;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    // Load the main template first
    cmd.append(UIPaths.ADMIN_ZONES);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "zones", cmd, events);

    // Localize page title and common labels
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_ZONES));
    cmd.set("#TabAll.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ALL));
    cmd.set("#TabSafe.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SAFE));
    cmd.set("#TabWar.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_WAR));
    cmd.set("#CreateZoneBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_CREATE_ZONE));
    cmd.set("#SortLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_SORT));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_NEXT));

    // Build zone list
    buildZoneList(cmd, events);
  }

  private void buildZoneList(UICommandBuilder cmd, UIEventBuilder events) {
    // Tab buttons - active tab gets cyan text style, inactive keeps original color
    cmd.set("#TabAll.Style", Value.ref(UIPaths.STYLES,
        currentTab.equals("all") ? "CyanButtonStyle" : "ButtonStyle"));
    cmd.set("#TabSafe.Style", Value.ref(UIPaths.STYLES,
        currentTab.equals("safe") ? "CyanButtonStyle" : "SmallTealButtonStyle"));
    cmd.set("#TabWar.Style", Value.ref(UIPaths.STYLES,
        currentTab.equals("war") ? "CyanButtonStyle" : "SmallPurpleButtonStyle"));

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TabAll",
        EventData.of("Button", "TabAll").append("ZoneType", "all"),
        false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TabSafe",
        EventData.of("Button", "TabSafe").append("ZoneType", "safe"),
        false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#TabWar",
        EventData.of("Button", "TabWar").append("ZoneType", "war"),
        false
    );

    // Sort dropdown
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZONE_SORT_NAME)), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZONE_SORT_TYPE)), "TYPE"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZONE_SORT_CHUNKS)), "CHUNKS"),
        new DropdownEntryInfo(LocalizableString.fromString(HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZONE_SORT_WORLD)), "WORLD")
    ));
    cmd.set("#SortDropdown.Value", zoneSortMode.name());
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value"),
        false
    );

    // Get zones based on current tab filter
    List<Zone> zones;
    if (currentTab.equals("all")) {
      zones = new ArrayList<>(zoneManager.getAllZones().stream()
          .sorted(getZoneSortComparator())
          .toList());
    } else {
      ZoneType targetType = currentTab.equals("safe") ? ZoneType.SAFE : ZoneType.WAR;
      zones = new ArrayList<>(zoneManager.getAllZones().stream()
          .filter(z -> z.type() == targetType)
          .sorted(getZoneSortComparator())
          .toList());
    }

    // Zone count (with total chunks)
    int totalChunks = zones.stream().mapToInt(Zone::getChunkCount).sum();
    String tabLabel = currentTab.equals("all") ? "" : currentTab + " ";
    cmd.set("#ZoneCount.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_ZONE_COUNT_FORMAT, zones.size(), tabLabel, totalChunks));

    // Create zone button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CreateZoneBtn",
        EventData.of("Button", "CreateZone")
            .append("ZoneType", currentTab),
        false
    );

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) zones.size() / ZONES_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    int startIdx = currentPage * ZONES_PER_PAGE;

    // Clear list and create IndexCards container
    cmd.clear("#ZoneList");
    cmd.appendInline("#ZoneList", "Group #IndexCards { LayoutMode: Top; }");

    // Build zone entries
    int displayIndex = 0;
    for (int i = startIdx; i < Math.min(startIdx + ZONES_PER_PAGE, zones.size()); i++) {
      Zone zone = zones.get(i);
      buildZoneEntry(cmd, events, displayIndex, zone);
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

  private void buildZoneEntry(UICommandBuilder cmd, UIEventBuilder events, int index, Zone zone) {
    boolean isExpanded = expandedZones.contains(zone.id());

    // Append entry template to IndexCards
    cmd.append("#IndexCards", UIPaths.ADMIN_ZONE_ENTRY);

    // Use indexed selector
    String idx = "#IndexCards[" + index + "]";

    // Type indicator color
    String typeColor = zone.isSafeZone() ? "#55FF55" : "#FF5555";
    cmd.set(idx + " #TypeIndicator.Background.Color", typeColor);

    // Zone info
    cmd.set(idx + " #ZoneName.Text", zone.name());
    cmd.set(idx + " #ZoneType.Text", zone.type().name() + " (" + zone.getChunkCount() + " chunks)");
    cmd.set(idx + " #ZoneWorld.Text", zone.world());

    // Inline stats (visible in collapsed row)
    cmd.set(idx + " #InlineChunks.Text", String.valueOf(zone.getChunkCount()));

    // Expansion state
    cmd.set(idx + " #ExpandIcon.Visible", !isExpanded);
    cmd.set(idx + " #CollapseIcon.Visible", isExpanded);
    cmd.set(idx + " #ExtendedInfo.Visible", isExpanded);

    // Header click to toggle expansion
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        idx + " #Header",
        EventData.of("Button", "ToggleExpanded")
            .append("ZoneUuid", zone.id().toString()),
        false
    );

    // Extended info (only bind events if expanded)
    if (isExpanded) {
      // Chunk count
      cmd.set(idx + " #ChunkCount.Text", String.valueOf(zone.getChunkCount()));

      // Bounds
      if (!zone.chunks().isEmpty()) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkKey chunk : zone.chunks()) {
          minX = Math.min(minX, chunk.chunkX());
          maxX = Math.max(maxX, chunk.chunkX());
          minZ = Math.min(minZ, chunk.chunkZ());
          maxZ = Math.max(maxZ, chunk.chunkZ());
        }
        cmd.set(idx + " #Bounds.Text",
            String.format("(%d,%d) to (%d,%d)", minX, minZ, maxX, maxZ));
      } else {
        cmd.set(idx + " #Bounds.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.ZONE_NO_CHUNKS));
      }

      // Created date
      String createdDate = DATE_FORMAT.format(Instant.ofEpochMilli(zone.createdAt()));
      cmd.set(idx + " #CreatedDate.Text", createdDate);

      // Edit Map button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #EditMapBtn",
          EventData.of("Button", "EditMap")
              .append("ZoneId", zone.id().toString())
              .append("ZoneName", zone.name()),
          false
      );

      // Settings button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #SettingsBtn",
          EventData.of("Button", "Settings")
              .append("ZoneId", zone.id().toString()),
          false
      );

      // Settings button (name, type, notifications)
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #SettingsBtn2",
          EventData.of("Button", "ZoneProperties")
              .append("ZoneId", zone.id().toString()),
          false
      );

      // Delete button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "DeleteZone")
              .append("ZoneId", zone.id().toString())
              .append("ZoneName", zone.name()),
          false
      );
    }
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminZoneData data) {
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
        if (data.zoneUuid != null) {
          UUID uuid = UuidUtil.parseOrNull(data.zoneUuid);
          if (uuid == null) {
            sendUpdate();
            return;
          }
          if (expandedZones.contains(uuid)) {
            expandedZones.remove(uuid);
          } else {
            expandedZones.add(uuid);
          }
          rebuildList();
        }
      }

      case "SortChanged" -> {
        try {
          if (data.sortMode != null) {
            zoneSortMode = ZoneSortMode.valueOf(data.sortMode);
          }
        } catch (IllegalArgumentException ignored) {}
        currentPage = 0;
        expandedZones.clear();
        rebuildList();
      }

      case "TabAll", "TabSafe", "TabWar" -> {
        String newTab = data.zoneType != null ? data.zoneType : "all";
        currentTab = newTab;
        currentPage = 0;
        expandedZones.clear();
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

      case "CreateZone" -> {
        ZoneType type = data.zoneType != null && data.zoneType.equals("war") ? ZoneType.WAR : ZoneType.SAFE;
        guiManager.openCreateZoneWizard(player, ref, store, playerRef, type);
      }

      case "EditMap" -> {
        if (data.zoneId != null) {
          UUID zoneId = UuidUtil.parseOrNull(data.zoneId);
          if (zoneId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_INVALID_ID));
            return;
          }
          Zone zone = zoneManager.getZoneById(zoneId);
          if (zone != null) {
            guiManager.openAdminZoneMap(player, ref, store, playerRef, zone);
          } else {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_NOT_FOUND));
            rebuildList();
          }
        }
      }

      case "Settings" -> {
        if (data.zoneId != null) {
          UUID zoneId = UuidUtil.parseOrNull(data.zoneId);
          if (zoneId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_INVALID_ID));
            return;
          }
          guiManager.openAdminZoneSettings(player, ref, store, playerRef, zoneId);
        }
      }

      case "ZoneProperties" -> {
        if (data.zoneId != null) {
          UUID zoneId = UuidUtil.parseOrNull(data.zoneId);
          if (zoneId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_INVALID_ID));
            return;
          }
          guiManager.openAdminZoneProperties(player, ref, store, playerRef,
              zoneId, currentTab, currentPage);
        }
      }

      case "DeleteZone" -> {
        if (data.zoneId != null) {
          UUID zoneId = UuidUtil.parseOrNull(data.zoneId);
          if (zoneId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_INVALID_ID));
            return;
          }
          ZoneManager.ZoneResult result = zoneManager.removeZone(zoneId);
          if (result == ZoneManager.ZoneResult.SUCCESS) {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_DELETED, data.zoneName));
            expandedZones.remove(zoneId);
          } else {
            player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.ZONE_DELETE_FAILED, result));
          }
          rebuildList();
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private Comparator<Zone> getZoneSortComparator() {
    return switch (zoneSortMode) {
      case TYPE -> Comparator.<Zone, String>comparing(z -> z.type().name())
          .thenComparing(Zone::name, String.CASE_INSENSITIVE_ORDER);
      case CHUNKS -> Comparator.<Zone>comparingInt(Zone::getChunkCount).reversed()
          .thenComparing(Zone::name, String.CASE_INSENSITIVE_ORDER);
      case WORLD -> Comparator.comparing(Zone::world, String.CASE_INSENSITIVE_ORDER)
          .thenComparing(Zone::name, String.CASE_INSENSITIVE_ORDER);
      default -> Comparator.comparing(Zone::name, String.CASE_INSENSITIVE_ORDER); // NAME
    };
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildZoneList(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
