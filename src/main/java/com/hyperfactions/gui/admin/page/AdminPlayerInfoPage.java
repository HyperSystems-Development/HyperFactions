package com.hyperfactions.gui.admin.page;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.MembershipRecord;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.gui.GuiColors;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminPlayerInfoData;
import com.hyperfactions.manager.FactionManager.FactionResult;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.TimeUtil;
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
 * Admin Player Info page - displays detailed player information with admin power controls.
 * Allows admin to set/adjust/reset power, set max override, toggle bypass flags.
 */
public class AdminPlayerInfoPage extends InteractiveCustomUIPage<AdminPlayerInfoData> {

  /** Where the user navigated from, so Back returns to the correct page. */
  public enum Origin {
    /** Opened from the admin players list. */
    PLAYERS_LIST,
    /** Opened from a faction's member list. */
    FACTION_MEMBERS
  }

  private final PlayerRef playerRef;

  private final UUID targetPlayerUuid;

  private final String targetPlayerName;

  private final UUID factionId;

  private final Origin origin;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final GuiManager guiManager;

  /** Creates a new AdminPlayerInfoPage. */
  public AdminPlayerInfoPage(PlayerRef playerRef,
               UUID targetPlayerUuid,
               String targetPlayerName,
               UUID factionId,
               Origin origin,
               FactionManager factionManager,
               PowerManager powerManager,
               GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPlayerInfoData.CODEC);
    this.playerRef = playerRef;
    this.targetPlayerUuid = targetPlayerUuid;
    this.targetPlayerName = targetPlayerName;
    this.factionId = factionId;
    this.origin = origin;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_PLAYER_INFO);
    AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);
    buildContent(cmd, events);
  }

  private void buildContent(UICommandBuilder cmd, UIEventBuilder events) {
    // === Header ===
    cmd.set("#PlayerName.Text", targetPlayerName);

    // Online status
    boolean isOnline = isOnline(targetPlayerUuid);
    cmd.set("#OnlineStatus.Text", isOnline ? "Online" : "Offline");
    cmd.set("#OnlineStatus.Style.TextColor", GuiColors.forOnlineStatus(isOnline));

    // Load player data once for all sections
    PlayerData cachedData = loadPlayerDataSync();

    // First Joined / Last Online
    if (cachedData != null && cachedData.getFirstJoined() > 0) {
      cmd.set("#FirstJoinedValue.Text", TimeUtil.formatDate(cachedData.getFirstJoined()));
    } else {
      cmd.set("#FirstJoinedValue.Text", "Unknown");
    }
    if (isOnline) {
      cmd.set("#LastOnlineValue.Text", "Now");
      cmd.set("#LastOnlineValue.Style.TextColor", "#55FF55");
    } else if (cachedData != null && cachedData.getLastOnline() > 0) {
      cmd.set("#LastOnlineValue.Text", TimeUtil.formatRelative(cachedData.getLastOnline()));
    } else {
      cmd.set("#LastOnlineValue.Text", "Unknown");
    }

    // === Faction Card ===
    Faction faction = factionId != null ? factionManager.getFaction(factionId) : null;
    if (faction == null) {
      faction = factionManager.getPlayerFaction(targetPlayerUuid);
    }
    FactionMember member = faction != null ? faction.getMember(targetPlayerUuid) : null;

    if (faction != null) {
      cmd.set("#FactionName.Text", faction.name());
    } else {
      cmd.set("#FactionName.Text", "No Faction");
      cmd.set("#FactionName.Style.TextColor", "#888888");
    }

    if (faction != null && member != null) {
      cmd.set("#FactionNameValue.Text", faction.name());
      cmd.set("#FactionRoleValue.Text", ConfigManager.get().getRoleDisplayName(member.role()));

      cmd.set("#FactionInfo.Visible", true);
      cmd.set("#NoFactionLabel.Visible", false);

      // View Faction button
      events.addEventBinding(CustomUIEventBindingType.Activating, "#ViewFactionBtn",
          EventData.of("Button", "ViewFaction"), false);
    } else {
      cmd.set("#FactionInfo.Visible", false);
      cmd.set("#NoFactionLabel.Visible", true);
    }

    // === Power Management ===
    PlayerPower power = powerManager.getPlayerPower(targetPlayerUuid);
    double effectiveMax = power.getEffectiveMaxPower();

    // Power display
    cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", power.power(), effectiveMax));
    int powerPercent = power.getPowerPercent();
    String powerColor = GuiColors.forPowerLevel(powerPercent);
    cmd.set("#PowerValue.Style.TextColor", powerColor);

    // Max override indicator
    if (power.maxPowerOverride() != null) {
      cmd.set("#MaxOverrideLabel.Text", "(custom max)");
    } else {
      cmd.set("#MaxOverrideLabel.Text", "(default max)");
      cmd.set("#MaxOverrideLabel.Style.TextColor", "#666666");
    }

    // Power bar
    float powerRatio = effectiveMax > 0 ? (float) (power.power() / effectiveMax) : 0f;
    cmd.set("#PowerBar.Value", powerRatio);
    cmd.set("#PowerBar.Bar.Color", powerColor);

    // Max power display
    cmd.set("#MaxPowerValue.Text", String.format("%.1f", effectiveMax));

    // === Bypass Toggles ===
    cmd.set("#NoLossCheck #CheckBox.Value", power.powerLossDisabled());
    cmd.set("#NoDecayCheck #CheckBox.Value", power.claimDecayExempt());

    // === Stats ===
    if (cachedData != null) {
      cmd.set("#KillsValue.Text", String.valueOf(cachedData.getKills()));
      cmd.set("#DeathsValue.Text", String.valueOf(cachedData.getDeaths()));
      double kdr = cachedData.getDeaths() > 0 ? (double) cachedData.getKills() / cachedData.getDeaths() : cachedData.getKills();
      cmd.set("#KDRValue.Text", String.format("%.2f", kdr));
    }

    cmd.set("#UuidValue.Text", targetPlayerUuid.toString());

    // === Membership History ===
    if (cachedData != null && !cachedData.getMembershipHistory().isEmpty()) {
      List<MembershipRecord> history = new java.util.ArrayList<>(cachedData.getMembershipHistory());
      Collections.reverse(history);

      cmd.set("#HistoryCount.Text", history.size() + " records");
      cmd.appendInline("#HistoryList", "Group #HistoryCards { LayoutMode: Top; }");

      for (int i = 0; i < history.size(); i++) {
        cmd.append("#HistoryCards", UIPaths.ADMIN_HISTORY_ENTRY);
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
          "Label { Text: \"No membership history\"; Style: (FontSize: 10, TextColor: #555555); }");
    }

    // === Kick button ===
    if (faction == null) {
      cmd.set("#KickBtn.Disabled", true);
    } else {
      FactionMember targetMember = faction.getMember(targetPlayerUuid);
      if (targetMember != null && targetMember.isLeader() && faction.getMemberCount() == 1) {
        cmd.set("#KickBtn.Text", "Disband Faction");
      } else if (targetMember != null && targetMember.isLeader()) {
        cmd.set("#KickBtn.Text", "Kick Leader");
      }
    }

    // === Event Bindings ===
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SubFive",
        EventData.of("Button", "AdjustPower").append("Delta", "-5"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SubOne",
        EventData.of("Button", "AdjustPower").append("Delta", "-1"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#AddOne",
        EventData.of("Button", "AdjustPower").append("Delta", "1"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#AddFive",
        EventData.of("Button", "AdjustPower").append("Delta", "5"), false);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#SetPowerBtn",
        EventData.of("Button", "SetPower").append("@PowerInput", "#PowerInput.Value"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetPowerBtn",
        EventData.of("Button", "ResetPower"), false);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#SetMaxBtn",
        EventData.of("Button", "SetMax").append("@PowerInput", "#MaxPowerInput.Value"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetMaxBtn",
        EventData.of("Button", "ResetMax"), false);

    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NoLossCheck #CheckBox",
        EventData.of("Button", "ToggleNoLoss"), false);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NoDecayCheck #CheckBox",
        EventData.of("Button", "ToggleNoDecay"), false);

    events.addEventBinding(CustomUIEventBindingType.Activating, "#KickBtn",
        EventData.of("Button", "Kick"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetKDBtn",
        EventData.of("Button", "ResetKD"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn",
        EventData.of("Button", "Back"), false);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminPlayerInfoData data) {
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

    UUID adminUuid = playerRef.getUuid();

    switch (data.button) {
      case "AdjustPower" -> {
        double delta = parseDoubleOrZero(data.delta);
        if (delta == 0) {
          return;
        }
        double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
        double newPower = powerManager.adjustPlayerPower(targetPlayerUuid, delta);
        logAdminPowerChange(adminUuid,
          "Admin adjusted " + targetPlayerName + "'s power by " + String.format("%.1f", delta)
          + " (" + String.format("%.1f", oldPower) + " -> " + String.format("%.1f", newPower) + ")");
        reopenPage(player, ref, store, playerRef);
      }

      case "SetPower" -> {
        double amount = parseDoubleOrNaN(data.powerInput);
        if (Double.isNaN(amount)) {
          player.sendMessage(MessageUtil.adminError("Enter a valid number."));
          return;
        }
        double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
        double newPower = powerManager.setPlayerPower(targetPlayerUuid, amount);
        logAdminPowerChange(adminUuid,
          "Admin set " + targetPlayerName + "'s power to " + String.format("%.1f", newPower)
          + " (was " + String.format("%.1f", oldPower) + ")");
        reopenPage(player, ref, store, playerRef);
      }

      case "ResetPower" -> {
        double oldPower = powerManager.getPlayerPower(targetPlayerUuid).power();
        double newPower = powerManager.resetPlayerPower(targetPlayerUuid);
        logAdminPowerChange(adminUuid,
          "Admin reset " + targetPlayerName + "'s power to " + String.format("%.1f", newPower)
          + " (was " + String.format("%.1f", oldPower) + ")");
        reopenPage(player, ref, store, playerRef);
      }

      case "SetMax" -> {
        double amount = parseDoubleOrNaN(data.powerInput);
        if (Double.isNaN(amount) || amount <= 0) {
          player.sendMessage(MessageUtil.adminError("Enter a valid positive number."));
          return;
        }
        PlayerPower old = powerManager.getPlayerPower(targetPlayerUuid);
        double oldMax = old.getEffectiveMaxPower();
        powerManager.setPlayerMaxPower(targetPlayerUuid, amount);
        logAdminPowerChange(adminUuid,
          "Admin set " + targetPlayerName + "'s max power to " + String.format("%.1f", amount)
          + " (was " + String.format("%.1f", oldMax) + ")");
        reopenPage(player, ref, store, playerRef);
      }

      case "ResetMax" -> {
        PlayerPower old = powerManager.getPlayerPower(targetPlayerUuid);
        double oldMax = old.getEffectiveMaxPower();
        powerManager.resetPlayerMaxPower(targetPlayerUuid);
        logAdminPowerChange(adminUuid,
          "Admin reset " + targetPlayerName + "'s max power to global default");
        reopenPage(player, ref, store, playerRef);
      }

      case "ToggleNoLoss" -> {
        // Toggle from current server state (don't read checkbox value from EventData)
        PlayerPower current = powerManager.getPlayerPower(targetPlayerUuid);
        boolean newState = !current.powerLossDisabled();
        powerManager.setPlayerPowerLossDisabled(targetPlayerUuid, newState);
        logAdminPowerChange(adminUuid,
          "Admin " + (newState ? "disabled" : "enabled") + " power loss for " + targetPlayerName);
        reopenPage(player, ref, store, playerRef);
      }

      case "ToggleNoDecay" -> {
        // Toggle from current server state (don't read checkbox value from EventData)
        PlayerPower current = powerManager.getPlayerPower(targetPlayerUuid);
        boolean newState = !current.claimDecayExempt();
        powerManager.setPlayerClaimDecayExempt(targetPlayerUuid, newState);
        logAdminPowerChange(adminUuid,
          "Admin " + (newState ? "enabled" : "disabled") + " claim decay exemption for " + targetPlayerName);
        reopenPage(player, ref, store, playerRef);
      }

      case "ResetKD" -> {
        guiManager.getPlugin().get().getPlayerStorage().updatePlayerData(targetPlayerUuid, data2 -> {
          data2.setKills(0);
          data2.setDeaths(0);
        });
        Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
        if (faction != null) {
          Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER,
              "Admin reset K/D for " + targetPlayerName, adminUuid));
          factionManager.updateFaction(updated);
        }
        player.sendMessage(MessageUtil.adminSuccess("Reset K/D for " + targetPlayerName + "."));
        reopenPage(player, ref, store, playerRef);
      }

      case "Kick" -> {
        Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
        if (faction == null) {
          return;
        }

        FactionMember targetMember = faction.getMember(targetPlayerUuid);
        if (targetMember == null) {
          return;
        }

        if (targetMember.isLeader()) {
          if (faction.getMemberCount() == 1) {
            // Last member — disband the faction
            factionManager.forceDisband(faction.id(),
              "[Admin] Disbanded via admin kick of last member " + targetPlayerName);
            player.sendMessage(MessageUtil.text("[Admin] Faction '" + faction.name()
              + "' disbanded (last member kicked).", MessageUtil.COLOR_GOLD));
            // Navigate back to factions list since faction no longer exists
            guiManager.openAdminFactions(player, ref, store, playerRef);
          } else {
            // Leader with other members — transfer leadership then kick
            FactionMember successor = faction.findSuccessor();
            if (successor != null) {
              // Transfer leadership
              FactionMember promoted = successor.withRole(FactionRole.LEADER);
              FactionMember demoted = targetMember.withRole(FactionRole.MEMBER);
              Faction updated = faction.withMember(promoted).withMember(demoted)
                .withLog(FactionLog.create(FactionLog.LogType.LEADER_TRANSFER,
                  "[Admin] Leadership transferred from " + targetPlayerName
                  + " to " + successor.username() + " (admin kick)",
                  adminUuid));
              factionManager.updateFaction(updated);

              // Now kick the demoted member
              factionManager.adminRemoveMember(faction.id(), targetPlayerUuid);
              player.sendMessage(MessageUtil.adminSuccess("Kicked leader " + targetPlayerName
                + ". Leadership transferred to " + successor.username() + "."));
            }
            reopenPage(player, ref, store, playerRef);
          }
        } else {
          // Normal kick
          FactionResult result = factionManager.adminRemoveMember(faction.id(), targetPlayerUuid);
          if (result == FactionResult.SUCCESS) {
            player.sendMessage(MessageUtil.adminSuccess("Kicked " + targetPlayerName
              + " from " + faction.name() + "."));
          }
          reopenPage(player, ref, store, playerRef);
        }
      }

      case "ViewFaction" -> {
        Faction viewFaction = factionId != null ? factionManager.getFaction(factionId)
            : factionManager.getPlayerFaction(targetPlayerUuid);
        if (viewFaction != null) {
          guiManager.openAdminFactionInfo(player, ref, store, playerRef, viewFaction.id());
        } else {
          player.sendMessage(MessageUtil.adminError("Faction no longer exists."));
        }
      }

      case "Back" -> {
        if (origin == Origin.FACTION_MEMBERS && factionId != null) {
          guiManager.openAdminFactionMembers(player, ref, store, playerRef, factionId);
        } else {
          guiManager.openAdminPlayers(player, ref, store, playerRef);
        }
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }

  /**
   * Reopens the page with fresh data via GuiManager (avoids sendUpdate state issues).
   */
  private void reopenPage(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
    guiManager.openAdminPlayerInfo(player, ref, store, playerRef,
        targetPlayerUuid, targetPlayerName, factionId, origin);
  }

  private void logAdminPowerChange(UUID adminUuid, String message) {
    Faction faction = factionManager.getPlayerFaction(targetPlayerUuid);
    if (faction != null) {
      Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER, message, adminUuid));
      factionManager.updateFaction(updated);
    }
  }

  private PlayerData loadPlayerDataSync() {
    try {
      return guiManager.getPlugin().get().getPlayerStorage()
          .loadPlayerData(targetPlayerUuid).join().orElse(null);
    } catch (Exception e) {
      Logger.debug("Failed to load player data for %s: %s", targetPlayerUuid, e.getMessage());
      return null;
    }
  }

  private boolean isOnline(UUID uuid) {
    try {
      PlayerRef ref = Universe.get().getPlayer(uuid);
      return ref != null && ref.isValid();
    } catch (Exception e) {
      return false;
    }
  }

  private String formatRole(FactionRole role) {
    return ConfigManager.get().getRoleDisplayName(role);
  }

  private String formatReason(MembershipRecord.LeaveReason reason) {
    return switch (reason) {
      case ACTIVE -> "ACTIVE";
      case LEFT -> "LEFT";
      case KICKED -> "KICKED";
      case DISBANDED -> "DISBANDED";
    };
  }

  private double parseDoubleOrZero(String value) {
    if (value == null || value.isEmpty()) {
      return 0;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private double parseDoubleOrNaN(String value) {
    if (value == null || value.isEmpty()) {
      return Double.NaN;
    }
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }
}
