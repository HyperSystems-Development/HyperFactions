package com.hyperfactions.gui.admin.page;

import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionInfoData;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.manager.RelationManager;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin Faction Info page - displays detailed information about a faction.
 * Uses admin navigation context so Back returns to AdminFactionsPage.
 */
public class AdminFactionInfoPage extends InteractiveCustomUIPage<AdminFactionInfoData> {

  private final PlayerRef playerRef;

  private final UUID factionId;

  private final FactionManager factionManager;

  private final PowerManager powerManager;

  private final RelationManager relationManager;

  private final EconomyManager economyManager;

  private final GuiManager guiManager;

  /** Creates a new AdminFactionInfoPage. */
  public AdminFactionInfoPage(PlayerRef playerRef,
                UUID factionId,
                FactionManager factionManager,
                PowerManager powerManager,
                RelationManager relationManager,
                EconomyManager economyManager,
                GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionInfoData.CODEC);
    this.playerRef = playerRef;
    this.factionId = factionId;
    this.factionManager = factionManager;
    this.powerManager = powerManager;
    this.relationManager = relationManager;
    this.economyManager = economyManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the admin faction info template
    cmd.append(UIPaths.ADMIN_FACTION_INFO);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);

    // Localize page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_FACTION_INFO));

    // Localize stat card labels
    cmd.set("#PowerCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_POWER));
    cmd.set("#PowerSubLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_CURRENT_MAX));
    cmd.set("#ClaimsCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_CLAIMS));
    cmd.set("#ClaimsSubLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_CLAIMED_MAX));
    cmd.set("#MembersCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_MEMBERS));
    cmd.set("#RelationsCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_RELATIONS));
    cmd.set("#RelationsSubLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ALLY_ENEMY));
    cmd.set("#StatusCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_STATUS));
    cmd.set("#InfoCardLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_INFO));
    cmd.set("#TreasurySubLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_TREASURY_BALANCE));

    // Localize section headers
    cmd.set("#LeadershipHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_LEADERSHIP));
    cmd.set("#LeaderLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_LEADER_LABEL));
    cmd.set("#OfficersLabel.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_OFFICERS_LABEL));
    cmd.set("#PowerMgmtHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_POWER_MANAGEMENT));
    cmd.set("#EconMgmtHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ECON_MGMT));
    cmd.set("#DangerZoneHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_DANGER_ZONE));

    // Localize button labels
    cmd.set("#PowerResetAll.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_RESET_ALL_POWER));
    cmd.set("#EconAdjustBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_ECON_ADJUST));
    cmd.set("#EconViewLogBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_VIEW_TREASURY));
    cmd.set("#DisbandBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_DISBAND));
    cmd.set("#ViewMembersBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_VIEW_MEMBERS));
    cmd.set("#ViewRelationsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_VIEW_RELATIONS));
    cmd.set("#ViewSettingsBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_FAC_VIEW_SETTINGS));
    cmd.set("#BackBtn.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_BACK));

    // Get the faction
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      cmd.set("#FactionName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTION_NOT_FOUND_LABEL));
      cmd.set("#FactionDescription.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.INFO_FACTION_GONE));
      return;
    }

    // === Header Section ===
    cmd.set("#FactionName.Text", faction.name());

    // Tag (if set)
    String tag = faction.tag();
    if (tag != null && !tag.isEmpty()) {
      cmd.set("#FactionTag.Text", "[" + tag + "]");
    } else {
      cmd.set("#FactionTag.Text", "");
    }

    // Description
    String description = faction.description();
    cmd.set("#FactionDescription.Text",
        description != null && !description.isEmpty() ? description : HFMessages.get(playerRef, MessageKeys.AdminGui.NO_DESCRIPTION));

    // Open/Closed status indicator
    cmd.set("#StatusIndicator.Text", faction.open() ? HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN) : HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY));

    // === Stats Section ===
    PowerManager.FactionPowerStats powerStats = powerManager.getFactionPowerStats(faction.id());

    // Power
    cmd.set("#PowerValue.Text", String.format("%.1f / %.1f", powerStats.currentPower(), powerStats.maxPower()));

    // Claims
    cmd.set("#ClaimsValue.Text", String.format("%d / %d", powerStats.currentClaims(), powerStats.maxClaims()));

    // Members
    int memberCount = faction.getMemberCount();
    int maxMembers = ConfigManager.get().getMaxMembers();
    cmd.set("#MembersValue.Text", String.format("%d / %d", memberCount, maxMembers));

    // Recruitment status
    cmd.set("#RecruitmentValue.Text", faction.open() ? HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_OPEN) : HFMessages.get(playerRef, MessageKeys.FactionInfoGui.STATUS_INVITE_ONLY));

    // Founded date
    cmd.set("#FoundedValue.Text", TimeUtil.formatRelative(faction.createdAt()));

    // Relations count
    int allyCount = relationManager.getAllies(faction.id()).size();
    int enemyCount = relationManager.getEnemies(faction.id()).size();
    cmd.set("#AlliesValue.Text", String.valueOf(allyCount));
    cmd.set("#EnemiesValue.Text", String.valueOf(enemyCount));

    // Raidable status
    if (powerStats.isRaidable()) {
      cmd.set("#RaidableValue.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.RAIDABLE));
    } else {
      cmd.set("#RaidableValue.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.PROTECTED));
    }

    // === Leadership Section ===
    FactionMember leader = faction.getLeader();
    cmd.set("#LeaderName.Text", leader != null ? leader.username() : HFMessages.get(playerRef, MessageKeys.Common.UNKNOWN));

    // Officers
    List<FactionMember> officers = faction.getMembersSorted().stream()
        .filter(m -> m.role() == FactionRole.OFFICER)
        .toList();
    if (officers.isEmpty()) {
      cmd.set("#OfficersValue.Text", HFMessages.get(playerRef, MessageKeys.Common.NONE));
    } else {
      String officerNames = officers.stream()
          .map(FactionMember::username)
          .limit(3)
          .collect(Collectors.joining(", "));
      if (officers.size() > 3) {
        officerNames += " " + HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_INFO_MORE, officers.size() - 3);
      }
      cmd.set("#OfficersValue.Text", officerNames);
    }

    // === Event Bindings ===
    // View Members button - opens admin members page
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ViewMembersBtn",
        EventData.of("Button", "ViewMembers")
            .append("FactionId", factionId.toString()),
        false
    );

    // View Relations button - opens admin relations page
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ViewRelationsBtn",
        EventData.of("Button", "ViewRelations")
            .append("FactionId", factionId.toString()),
        false
    );

    // View Settings button - opens admin faction settings page
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#ViewSettingsBtn",
        EventData.of("Button", "ViewSettings")
            .append("FactionId", factionId.toString()),
        false
    );

    // === Power Management Buttons ===
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerSubFive",
        EventData.of("Button", "BulkPower").append("Amount", "-5").append("FactionId", factionId.toString()), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerSubOne",
        EventData.of("Button", "BulkPower").append("Amount", "-1").append("FactionId", factionId.toString()), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerAddOne",
        EventData.of("Button", "BulkPower").append("Amount", "1").append("FactionId", factionId.toString()), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerAddFive",
        EventData.of("Button", "BulkPower").append("Amount", "5").append("FactionId", factionId.toString()), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PowerResetAll",
        EventData.of("Button", "ResetAllPower").append("FactionId", factionId.toString()), false);

    // === Economy Section (conditional) ===
    if (economyManager != null) {
      // Show treasury balance in stats
      cmd.set("#TreasuryRow.Visible", true);
      java.math.BigDecimal balance = economyManager.getFactionBalance(factionId);
      cmd.set("#TreasuryValue.Text", economyManager.formatCurrencyCompact(balance));

      // Show economy management section
      cmd.set("#EconomyManagement.Visible", true);

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#EconAdjustBtn",
          EventData.of("Button", "EconAdjust")
              .append("FactionId", factionId.toString()),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#EconViewLogBtn",
          EventData.of("Button", "EconViewLog")
              .append("FactionId", factionId.toString()),
          false
      );
    }

    // Disband button (danger zone)
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DisbandBtn",
        EventData.of("Button", "Disband")
            .append("FactionId", factionId.toString()),
        false
    );

    // Back button - returns to admin factions list
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
                AdminFactionInfoData data) {
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
      case "ViewMembers" -> {
        guiManager.openAdminFactionMembers(player, ref, store, playerRef, factionId);
      }

      case "ViewRelations" -> {
        guiManager.openAdminFactionRelations(player, ref, store, playerRef, factionId);
      }

      case "ViewSettings" -> {
        guiManager.openAdminFactionSettings(player, ref, store, playerRef, factionId);
      }

      case "BulkPower" -> {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null && data.amount != null) {
          try {
            double delta = Double.parseDouble(data.amount);
            for (UUID memberUuid : faction.members().keySet()) {
              powerManager.adjustPlayerPower(memberUuid, delta);
            }
            Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER,
                "Admin adjusted all " + faction.getMemberCount() + " members' power by " + String.format("%.1f", delta),
                playerRef.getUuid(),
                MessageKeys.LogsGui.MSG_ADMIN_POWER_ADJUSTED_ALL, String.valueOf(faction.getMemberCount()), String.format("%.1f", delta)));
            factionManager.updateFaction(updated);
            // Rebuild page to show updated stats
            guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
          } catch (NumberFormatException ignored) {}
        }
      }

      case "ResetAllPower" -> {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null) {
          for (UUID memberUuid : faction.members().keySet()) {
            powerManager.resetPlayerPower(memberUuid);
          }
          Faction updated = faction.withLog(FactionLog.create(FactionLog.LogType.ADMIN_POWER,
              "Admin reset power for all " + faction.getMemberCount() + " members",
              playerRef.getUuid(),
              MessageKeys.LogsGui.MSG_ADMIN_POWER_RESET_ALL, String.valueOf(faction.getMemberCount())));
          factionManager.updateFaction(updated);
          guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
        }
      }

      case "EconAdjust" -> {
        guiManager.openAdminEconomyAdjust(player, ref, store, playerRef, factionId);
      }

      case "EconViewLog" -> {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null) {
          guiManager.openFactionTreasury(player, ref, store, playerRef, faction);
        }
      }

      case "Disband" -> {
        Faction faction = factionManager.getFaction(factionId);
        if (faction != null) {
          factionManager.forceDisband(faction.id(),
              "[Admin] Force disbanded faction '" + faction.name() + "'");
          player.sendMessage(com.hyperfactions.util.MessageUtil.adminSuccess(
              "Faction '" + faction.name() + "' has been disbanded."));
          guiManager.openAdminFactions(player, ref, store, playerRef);
        }
      }

      case "Back" -> {
        guiManager.openAdminFactions(player, ref, store, playerRef);
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
