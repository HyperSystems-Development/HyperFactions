package com.hyperfactions.gui.faction.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.SetRelationModalData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.RelationManager;
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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.*;

/**
 * Modal for searching and setting relations with other factions.
 * Supports search, pagination, and setting ally/enemy relations.
 */
public class SetRelationModalPage extends InteractiveCustomUIPage<SetRelationModalData> {

  private static final int FACTIONS_PER_PAGE = 4;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final RelationManager relationManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private String searchQuery = "";

  private int currentPage = 0;

  /** Creates a new SetRelationModalPage. */
  public SetRelationModalPage(PlayerRef playerRef,
                FactionManager factionManager,
                PowerManager powerManager,
                RelationManager relationManager,
                GuiManager guiManager,
                Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, SetRelationModalData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.relationManager = relationManager;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Creates a new SetRelationModalPage. */
  public SetRelationModalPage(PlayerRef playerRef,
                FactionManager factionManager,
                PowerManager powerManager,
                RelationManager relationManager,
                GuiManager guiManager,
                Faction faction,
                String searchQuery,
                int currentPage) {
    this(playerRef, factionManager, powerManager, relationManager, guiManager, faction);
    this.searchQuery = searchQuery != null ? searchQuery : "";
    this.currentPage = currentPage;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the modal template
    cmd.append(UIPaths.SET_RELATION_MODAL);

    // Build the results content
    buildResultsContent(cmd, events);
  }

  /**
   * Builds the results content (search binding, results list, pagination).
   * Called from build() for initial load and from rebuildResults() for partial updates.
   */
  private void buildResultsContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Search - real-time filtering via ValueChanged
    if (!searchQuery.isEmpty()) {
      cmd.set("#SearchInput.Value", searchQuery);
    }
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#SearchInput",
        EventData.of("Button", "Search").append("@SearchQuery", "#SearchInput.Value"),
        false
    );

    // Cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#CancelBtn",
        EventData.of("Button", "Cancel"),
        false
    );

    // Clear previous results
    cmd.clear("#ResultsList");

    // Build search results
    List<FactionEntry> results = getSearchResults();

    if (results.isEmpty()) {
      // Show empty state
      if (searchQuery.isEmpty()) {
        cmd.set("#EmptyText.Text", HFMessages.get(playerRef, GuiKeys.RelationsGui.SEARCH_HINT));
      } else {
        cmd.set("#EmptyText.Text", HFMessages.get(playerRef, GuiKeys.RelationsGui.NO_RESULTS, searchQuery));
      }
      cmd.set("#PageInfo.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.PAGE_FORMAT, 0, 0));
    } else {
      // Hide empty state by setting text to empty
      cmd.set("#EmptyText.Text", "");

      // Calculate pagination
      int totalPages = Math.max(1, (int) Math.ceil((double) results.size() / FACTIONS_PER_PAGE));
      currentPage = Math.min(currentPage, totalPages - 1);
      int startIdx = currentPage * FACTIONS_PER_PAGE;

      // Build faction cards
      buildFactionCards(cmd, events, results, startIdx);

      // Pagination
      cmd.set("#PageInfo.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.PAGE_FORMAT, currentPage + 1, totalPages));

      if (currentPage > 0) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#PrevBtn",
            EventData.of("Button", "Page").append("Page", String.valueOf(currentPage - 1)),
            false
        );
      }

      if (currentPage < totalPages - 1) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#NextBtn",
            EventData.of("Button", "Page").append("Page", String.valueOf(currentPage + 1)),
            false
        );
      }
    }
  }

  private List<FactionEntry> getSearchResults() {
    List<FactionEntry> entries = new ArrayList<>();

    for (Faction f : factionManager.getAllFactions()) {
      // Skip own faction
      if (f.id().equals(faction.id())) {
        continue;
      }

      // Filter by search query
      if (!searchQuery.isEmpty()) {
        boolean matches = f.name().toLowerCase().contains(searchQuery.toLowerCase());
        if (f.tag() != null) {
          matches = matches || f.tag().toLowerCase().contains(searchQuery.toLowerCase());
        }
        if (!matches) {
          continue;
        }
      }

      PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(f.id());
      FactionMember leader = f.getLeader();
      String leaderName = leader != null ? leader.username() : HFMessages.get(playerRef, CommonKeys.Common.UNKNOWN);

      entries.add(new FactionEntry(
          f.id(),
          f.name(),
          leaderName,
          stats.currentPower(),
          f.members().size()
      ));
    }

    // Sort by power (highest first)
    entries.sort(Comparator.comparingDouble(FactionEntry::power).reversed());

    return entries;
  }

  private void buildFactionCards(UICommandBuilder cmd, UIEventBuilder events,
                 List<FactionEntry> entries, int startIdx) {
    for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
      int factionIdx = startIdx + i;

      if (factionIdx < entries.size()) {
        FactionEntry entry = entries.get(factionIdx);

        cmd.append("#ResultsList", UIPaths.SET_RELATION_CARD);

        String prefix = "#ResultsList[" + i + "] ";

        // Faction info
        cmd.set(prefix + "#FactionName.Text", entry.name);
        cmd.set(prefix + "#LeaderName.Text", HFMessages.get(playerRef, GuiKeys.GuiCommon.LEADER_LABEL, entry.leaderName));
        cmd.set(prefix + "#PowerCount.Text", HFMessages.get(playerRef, GuiKeys.RelationsGui.POWER_DISPLAY, String.format("%.0f", entry.power)));
        cmd.set(prefix + "#MemberCount.Text", HFMessages.get(playerRef, CommonKeys.Common.MEMBER_COUNT, entry.memberCount));

        // Ally button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            prefix + "#AllyBtn",
            EventData.of("Button", "RequestAlly")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );

        // Enemy button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            prefix + "#EnemyBtn",
            EventData.of("Button", "SetEnemy")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );

        // View button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            prefix + "#ViewBtn",
            EventData.of("Button", "ViewFaction")
                .append("FactionId", entry.id.toString())
                .append("FactionName", entry.name),
            false
        );
      }
    }
  }

  private record FactionEntry(UUID id, String name, String leaderName, double power, int memberCount) {}

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                SetRelationModalData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    UUID uuid = playerRef.getUuid();
    FactionMember member = faction.getMember(uuid);

    // Verify officer permission for relation changes
    boolean canManage = member != null && member.role().getLevel() >= FactionRole.OFFICER.getLevel();

    switch (data.button) {
      case "Cancel" -> {
        guiManager.openFactionRelations(player, ref, store, playerRef,
            factionManager.getFaction(faction.id()));
      }

      case "Search" -> {
        searchQuery = data.searchQuery != null ? data.searchQuery.trim() : "";
        currentPage = 0;
        rebuildResults();
      }

      case "Page" -> {
        currentPage = data.page;
        rebuildResults();
      }

      case "RequestAlly" -> {
        if (!canManage) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.SettingsGui.NO_PERMISSION));
          sendUpdate();
          return;
        }

        if (data.factionId != null) {
          UUID targetId = UuidUtil.parseOrNull(data.factionId);
          if (targetId == null) {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.BrowserGui.INVALID_FACTION));
            sendUpdate();
            return;
          }

          RelationManager.RelationResult result = relationManager.requestAlly(uuid, targetId);

          if (result == RelationManager.RelationResult.REQUEST_SENT) {
            player.sendMessage(MessageUtil.info(playerRef, GuiKeys.RelationsGui.REQUEST_SENT, "#00AAFF", data.factionName));
            // Navigate to pending tab since a request was sent
            guiManager.openFactionRelations(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()), "pending");
          } else if (result == RelationManager.RelationResult.REQUEST_ACCEPTED) {
            player.sendMessage(MessageUtil.info(playerRef, GuiKeys.RelationsGui.NOW_ALLIED, "#00AAFF", data.factionName));
            // Navigate to relations tab since alliance is now active
            guiManager.openFactionRelations(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()), "relations");
          } else {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RelationsGui.FAILED, result));
            guiManager.openFactionRelations(player, ref, store, playerRef,
                factionManager.getFaction(faction.id()));
          }
        }
      }

      case "SetEnemy" -> {
        if (!canManage) {
          player.sendMessage(MessageUtil.error(playerRef, GuiKeys.SettingsGui.NO_PERMISSION));
          sendUpdate();
          return;
        }

        if (data.factionId != null) {
          UUID targetId = UuidUtil.parseOrNull(data.factionId);
          if (targetId == null) {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.BrowserGui.INVALID_FACTION));
            sendUpdate();
            return;
          }

          RelationManager.RelationResult result = relationManager.setEnemy(uuid, targetId);

          if (result == RelationManager.RelationResult.SUCCESS) {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RelationsGui.NOW_ENEMIES, data.factionName));
          } else {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.RelationsGui.FAILED, result));
          }

          guiManager.openFactionRelations(player, ref, store, playerRef,
              factionManager.getFaction(faction.id()), "relations");
        }
      }

      case "ViewFaction" -> {
        if (data.factionId != null) {
          UUID targetId = UuidUtil.parseOrNull(data.factionId);
          if (targetId == null) {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.BrowserGui.INVALID_FACTION));
            sendUpdate();
            return;
          }

          Faction targetFaction = factionManager.getFaction(targetId);

          if (targetFaction != null) {
            guiManager.openFactionInfo(player, ref, store, playerRef, targetFaction, "relations");
          } else {
            player.sendMessage(MessageUtil.error(playerRef, GuiKeys.PlayerInfoGui.FACTION_GONE));
            sendUpdate();
          }
        }
      }

      default -> sendUpdate();
    }
  }

  /**
   * Rebuild only the results portion of the page via partial update.
   * This preserves the text field focus so search typing isn't interrupted.
   */
  private void rebuildResults() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildResultsContent(cmd, events);

    sendUpdate(cmd, events, false);
  }
}
