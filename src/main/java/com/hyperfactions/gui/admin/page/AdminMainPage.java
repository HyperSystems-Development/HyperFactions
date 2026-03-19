package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminMainData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.AdminGuiKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
import com.hyperfactions.util.UuidUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.*;

/**
 * Admin Main page - provides admin controls for faction management.
 */
public class AdminMainPage extends InteractiveCustomUIPage<AdminMainData> {

  private static final int FACTIONS_PER_PAGE = 6;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final GuiManager guiManager;

  private int currentPage = 0;

  /** Creates a new AdminMainPage. */
  public AdminMainPage(PlayerRef playerRef,
            FactionManager factionManager,
            PowerManager powerManager,
            GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminMainData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the main template first (nav bar elements must exist before setupBar)
    cmd.append(UIPaths.ADMIN_MAIN);

    // Setup admin nav bar (must be after template load)
    AdminNavBarHelper.setupBar(playerRef, "dashboard", cmd, events);


    // Localize page title and buttons
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_TITLE_MAIN));
    cmd.set("#ZonesBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_ZONES_BTN));
    cmd.set("#ReloadBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_RELOAD_BTN));
    cmd.set("#PrevBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_PREV));
    cmd.set("#NextBtn.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.GUI_NEXT));

    // Stats overview
    Collection<Faction> allFactions = factionManager.getAllFactions();
    int totalFactions = allFactions.size();
    int totalMembers = allFactions.stream()
        .mapToInt(f -> f.members().size())
        .sum();
    int totalClaims = allFactions.stream()
        .mapToInt(f -> f.claims().size())
        .sum();

    cmd.set("#TotalFactions.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.DASH_FACTIONS_PREFIX, totalFactions));
    cmd.set("#TotalMembers.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.DASH_MEMBERS_PREFIX, totalMembers));
    cmd.set("#TotalClaims.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.DASH_CLAIMS_PREFIX, totalClaims));

    // Navigation buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ZonesBtn",
        EventData.of("Button", "Zones"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ReloadBtn",
        EventData.of("Button", "Reload"),
        false
    );

    // Get all factions sorted by power
    List<Faction> factions = new ArrayList<>(allFactions);
    factions.sort((a, b) -> {
      double powerA = powerManager.getFactionPowerStats(a.id()).currentPower();
      double powerB = powerManager.getFactionPowerStats(b.id()).currentPower();
      return Double.compare(powerB, powerA);
    });

    // Calculate pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) factions.size() / FACTIONS_PER_PAGE));
    currentPage = Math.min(currentPage, totalPages - 1);
    int startIdx = currentPage * FACTIONS_PER_PAGE;

    // Build faction entries
    for (int i = 0; i < FACTIONS_PER_PAGE; i++) {
      String entryId = "#FactionEntry" + i;
      int factionIdx = startIdx + i;

      if (factionIdx < factions.size()) {
        Faction faction = factions.get(factionIdx);
        PowerManager.FactionPowerStats stats = powerManager.getFactionPowerStats(faction.id());

        cmd.append(entryId, UIPaths.ADMIN_FACTION_ENTRY);

        String prefix = entryId + " ";

        // Faction info
        String colorHex = faction.color() != null ? faction.color() : "#00FFFF";
        cmd.set(prefix + "#FactionName.Text", faction.name());
        cmd.set(prefix + "#MemberCount.Text", HFMessages.get(playerRef, CommonKeys.Common.MEMBER_COUNT, faction.members().size()));
        cmd.set(prefix + "#PowerCount.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.POWER_FORMAT, String.format("%.0f", stats.currentPower()), String.format("%.0f", stats.maxPower())));
        cmd.set(prefix + "#ClaimCount.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.CLAIMS_SUFFIX, faction.claims().size()));

        // Leader info
        FactionMember leader = faction.getLeader();
        String leaderName = leader != null ? leader.username() : HFMessages.get(playerRef, CommonKeys.Common.NONE);
        cmd.set(prefix + "#LeaderName.Text", HFMessages.get(playerRef, AdminGuiKeys.AdminGui.LEADER_PREFIX, leaderName));

        // Action buttons
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            prefix + "#DisbandBtn",
            EventData.of("Button", "Disband")
                .append("FactionId", faction.id().toString())
                .append("FactionName", faction.name()),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            prefix + "#UnclaimAllBtn",
            EventData.of("Button", "UnclaimAll")
                .append("FactionId", faction.id().toString())
                .append("FactionName", faction.name()),
            false
        );
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
                AdminMainData data) {
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
      case "Zones" -> guiManager.openAdminZone(player, ref, store, playerRef);

      case "Reload" -> {
        guiManager.closePage(player, ref, store);
        player.sendMessage(MessageUtil.text(playerRef, AdminGuiKeys.AdminGui.MAIN_RELOAD_HINT, "#00FFFF"));
      }

      case "PrevPage" -> {
        currentPage = Math.max(0, data.page);
        sendUpdate(); // Refresh current page
      }

      case "NextPage" -> {
        currentPage = data.page;
        sendUpdate(); // Refresh current page
      }

      case "Disband" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, AdminGuiKeys.AdminGui.INVALID_FACTION));
            return;
          }

          // Open confirmation modal
          guiManager.openAdminDisbandConfirm(player, ref, store, playerRef, factionId, data.factionName);
        }
      }

      case "UnclaimAll" -> {
        if (data.factionId != null) {
          UUID factionId = UuidUtil.parseOrNull(data.factionId);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText(playerRef, AdminGuiKeys.AdminGui.INVALID_FACTION));
            return;
          }
          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            int claimCount = faction.claims().size();
            // Admin unclaim - prompt for command
            guiManager.closePage(player, ref, store);
            player.sendMessage(MessageUtil.text(playerRef, AdminGuiKeys.AdminGui.MAIN_UNCLAIM_HINT, MessageUtil.COLOR_GOLD, data.factionName, claimCount));
          }
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
