package com.hyperfactions.gui.faction.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiColors;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.data.PlayerInfoData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.storage.PlayerStorage;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.TimeUtil;
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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Player Info page - shows detailed information about a player including
 * faction membership, power stats, combat stats, and membership history.
 *
 * <p>
 * Uses a static {@code player_info.ui} template with unique per-field IDs
 * and dynamic {@code history_entry.ui} entries via the IndexCards pattern.
 */
public class PlayerInfoPage extends InteractiveCustomUIPage<PlayerInfoData> {

  private final PlayerRef viewerRef;

  private final UUID targetPlayerUuid;

  private final String targetPlayerName;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final PlayerStorage playerStorage;

  private final GuiManager guiManager;

  private final String sourcePage;

  private PlayerData cachedPlayerData;

  /** Creates a new PlayerInfoPage. */
  public PlayerInfoPage(PlayerRef viewerRef,
             UUID targetPlayerUuid,
             String targetPlayerName,
             FactionManager factionManager,
             PowerManager powerManager,
             PlayerStorage playerStorage,
             GuiManager guiManager) {
    this(viewerRef, targetPlayerUuid, targetPlayerName, factionManager, powerManager, playerStorage, guiManager, null);
  }

  /**
   * Constructor with source page tracking.
   * @param sourcePage The page to return to when back is clicked:
   *                   "members" - FactionMembersPage
   *                   "browser" - FactionBrowserPage
   *                   null - just close the page
   */
  public PlayerInfoPage(PlayerRef viewerRef,
             UUID targetPlayerUuid,
             String targetPlayerName,
             FactionManager factionManager,
             PowerManager powerManager,
             PlayerStorage playerStorage,
             GuiManager guiManager,
             String sourcePage) {
    super(viewerRef, CustomPageLifetime.CanDismiss, PlayerInfoData.CODEC);
    this.viewerRef = viewerRef;
    this.targetPlayerUuid = targetPlayerUuid;
    this.targetPlayerName = targetPlayerName;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.playerStorage = playerStorage;
    this.guiManager = guiManager;
    this.sourcePage = sourcePage;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the player info template
    cmd.append(UIPaths.PLAYER_INFO);

    // === Header ===
    cmd.set("#PlayerName.Text", targetPlayerName);

    // Check if target is online
    PlayerRef targetRef = Universe.get().getPlayer(targetPlayerUuid);
    boolean isOnline = targetRef != null && targetRef.isValid();
    cmd.set("#OnlineIndicator.Text", isOnline ? "Online" : "Offline");
    cmd.set("#OnlineIndicator.Style.TextColor", GuiColors.forOnlineStatus(isOnline));

    // === First Joined / Last Online ===
    loadPlayerDataSync();
    if (cachedPlayerData != null && cachedPlayerData.getFirstJoined() > 0) {
      cmd.set("#FirstJoinedValue.Text", TimeUtil.formatDate(cachedPlayerData.getFirstJoined()));
    } else {
      cmd.set("#FirstJoinedValue.Text", "Unknown");
    }
    if (isOnline) {
      cmd.set("#LastOnlineValue.Text", "Now");
      cmd.set("#LastOnlineValue.Style.TextColor", "#55FF55");
    } else if (cachedPlayerData != null && cachedPlayerData.getLastOnline() > 0) {
      cmd.set("#LastOnlineValue.Text", TimeUtil.formatRelative(cachedPlayerData.getLastOnline()));
    } else {
      cmd.set("#LastOnlineValue.Text", "Unknown");
    }

    // === Faction Section ===
    Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
    FactionMember member = faction != null ? faction.getMember(targetPlayerUuid) : null;

    if (faction != null && member != null) {
      cmd.set("#FactionNameValue.Text", faction.name());
      cmd.set("#FactionRoleValue.Text", ConfigManager.get().getRoleDisplayName(member.role()));
      cmd.set("#FactionJoinedValue.Text", TimeUtil.formatRelative(member.joinedAt()));

      // Show faction rows, hide no-faction label
      cmd.set("#FactionNameRow.Visible", true);
      cmd.set("#FactionRoleRow.Visible", true);
      cmd.set("#FactionJoinedRow.Visible", true);
      cmd.set("#NoFactionLabel.Visible", false);

      // View Faction button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#ViewFactionBtn",
          EventData.of("Button", "ViewFaction")
              .append("PlayerUuid", faction.id().toString()),
          false
      );
    } else {
      // Hide faction detail rows, show no-faction label
      cmd.set("#FactionNameRow.Visible", false);
      cmd.set("#FactionRoleRow.Visible", false);
      cmd.set("#FactionJoinedRow.Visible", false);
      cmd.set("#NoFactionLabel.Visible", true);
      cmd.set("#ViewFactionBtn.Visible", false);
    }

    // === Stats Section ===
    // Power display — hardcore mode shows faction power, normal mode shows player power
    double currentPower, maxPower;
    if (ConfigManager.get().isHardcoreMode() && faction != null) {
      currentPower = powerManager.getFactionPower(faction.id());
      maxPower = powerManager.getFactionMaxPower(faction.id());
    } else if (ConfigManager.get().isHardcoreMode()) {
      // Factionless in hardcore mode
      currentPower = 0;
      maxPower = 0;
    } else {
      PlayerPower power = powerManager.getPlayerPower(targetPlayerUuid);
      currentPower = power.power();
      maxPower = power.getEffectiveMaxPower();
    }
    cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", currentPower, maxPower));

    // Power bar
    float powerRatio = maxPower > 0 ? (float) (currentPower / maxPower) : 0f;
    cmd.set("#PowerBar.Value", powerRatio);
    int powerPercent = maxPower > 0 ? (int) ((currentPower / maxPower) * 100) : 0;
    String powerColor = GuiColors.forPowerLevel(powerPercent);
    cmd.set("#PowerBar.Bar.Color", powerColor);

    // Combat stats from PlayerData
    loadPlayerDataSync();
    int kills = cachedPlayerData != null ? cachedPlayerData.getKills() : 0;
    int deaths = cachedPlayerData != null ? cachedPlayerData.getDeaths() : 0;
    double kdr = deaths > 0 ? (double) kills / deaths : kills;

    cmd.set("#KillsValue.Text", String.valueOf(kills));
    cmd.set("#DeathsValue.Text", String.valueOf(deaths));
    cmd.set("#KDRValue.Text", String.format("%.2f", kdr));

    // === Membership History ===
    if (cachedPlayerData != null && !cachedPlayerData.getMembershipHistory().isEmpty()) {
      // Show history newest-first (reverse of storage order)
      List<MembershipRecord> history = new java.util.ArrayList<>(cachedPlayerData.getMembershipHistory());
      Collections.reverse(history);

      cmd.set("#HistoryCount.Text", history.size() + " records");
      cmd.appendInline("#HistoryList", "Group #HistoryCards { LayoutMode: Top; }");

      for (int i = 0; i < history.size(); i++) {
        cmd.append("#HistoryCards", UIPaths.HISTORY_ENTRY);
        String idx = "#HistoryCards[" + i + "]";
        MembershipRecord rec = history.get(i);

        cmd.set(idx + " #HFactionName.Text", rec.factionName());
        cmd.set(idx + " #HRole.Text", ConfigManager.get().getRoleDisplayName(rec.highestRole()));
        cmd.set(idx + " #HJoined.Text", "Joined: " + TimeUtil.formatDate(rec.joinedAt()));
        cmd.set(idx + " #HLeft.Text", rec.isActive() ? "Current" : "Left: " + TimeUtil.formatDate(rec.leftAt()));
        cmd.set(idx + " #HReason.Text", formatReason(rec.reason()));
        cmd.set(idx + " #HReason.Style.TextColor", GuiColors.forLeaveReason(rec.reason()));
        cmd.set(idx + " #RoleBar.Background.Color", GuiColors.forRole(rec.highestRole()));
      }
    } else {
      cmd.set("#HistoryCount.Text", "");
      cmd.appendInline("#HistoryList",
          "Label { Text: \"No membership history\"; Style: (FontSize: 11, TextColor: #555555); }");
    }

    // Back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                PlayerInfoData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null || data.button == null) {
      return;
    }

    switch (data.button) {
      case "ViewFaction" -> {
        if (data.playerUuid != null) {
          UUID factionId = UuidUtil.parseOrNull(data.playerUuid);
          if (factionId == null) {
            player.sendMessage(MessageUtil.errorText("Invalid faction ID."));
            return;
          }

          Faction faction = factionManager.getFaction(factionId);
          if (faction != null) {
            guiManager.openFactionInfoFromPlayerInfo(player, ref, store, playerRef, faction,
                targetPlayerUuid, targetPlayerName, sourcePage);
          } else {
            player.sendMessage(MessageUtil.errorText("Faction no longer exists."));
          }
        }
      }

      case "Back" -> handleBack(player, ref, store, playerRef);
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  private void handleBack(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
    if (sourcePage == null) {
      guiManager.closePage(player, ref, store);
      return;
    }

    switch (sourcePage) {
      case "members" -> {
        Faction faction = factionManager.getPlayerFaction(viewerRef.getUuid());
        if (faction != null) {
          guiManager.openFactionMembers(player, ref, store, playerRef, faction);
        } else {
          guiManager.closePage(player, ref, store);
        }
      }
      case "browser" -> guiManager.openFactionBrowser(player, ref, store, playerRef);
      default -> guiManager.closePage(player, ref, store);
    }
  }

  private void loadPlayerDataSync() {
    if (cachedPlayerData == null) {
      try {
        cachedPlayerData = playerStorage.loadPlayerData(targetPlayerUuid).join().orElse(null);
      } catch (Exception e) {
        Logger.debug("Failed to load player data for %s: %s", targetPlayerUuid, e.getMessage());
      }
    }
  }

  private String formatReason(MembershipRecord.LeaveReason reason) {
    return switch (reason) {
      case ACTIVE -> "ACTIVE";
      case LEFT -> "LEFT";
      case KICKED -> "KICKED";
      case DISBANDED -> "DISBANDED";
    };
  }
}
