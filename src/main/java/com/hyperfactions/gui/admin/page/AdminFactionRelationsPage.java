package com.hyperfactions.gui.admin.page;

import com.hyperfactions.data.*;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminFactionRelationsData;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Admin Faction Relations page - view and manage relations for any faction.
 * Can force-set allies/enemies without requiring mutual approval.
 */
public class AdminFactionRelationsPage extends InteractiveCustomUIPage<AdminFactionRelationsData> {

  private final PlayerRef playerRef;

  private final UUID factionId;

  private final FactionManager factionManager;

  private final RelationManager relationManager;

  private final GuiManager guiManager;

  /** Creates a new AdminFactionRelationsPage. */
  public AdminFactionRelationsPage(PlayerRef playerRef, UUID factionId, FactionManager factionManager, RelationManager relationManager, GuiManager guiManager) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminFactionRelationsData.CODEC);
    this.playerRef = playerRef;
    this.factionId = factionId;
    this.factionManager = factionManager;
    this.relationManager = relationManager;
    this.guiManager = guiManager;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_FACTION_RELATIONS);
    AdminNavBarHelper.setupBar(playerRef, "factions", cmd, events);
    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      cmd.set("#FactionName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.FACTION_NOT_FOUND_LABEL));
      return;
    }
    cmd.set("#FactionName.Text", faction.name());
    events.addEventBinding(CustomUIEventBindingType.Activating, "#BackBtn", EventData.of("Button", "Back").append("FactionId", factionId.toString()), false);
    List<RelationEntry> allies = getRelationsOfType(faction, RelationType.ALLY);
    List<RelationEntry> enemies = getRelationsOfType(faction, RelationType.ENEMY);
    cmd.set("#AlliesHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.REL_ALLIES_HEADER, allies.size()));
    cmd.clear("#AlliesList");
    if (allies.isEmpty()) { cmd.appendInline("#AlliesList", "Label { Text: \"" + HFMessages.get(playerRef, MessageKeys.AdminGui.REL_NO_ALLIES) + "\"; Style: (FontSize: 11, TextColor: #666666); Anchor: (Height: 24); }"); }
    else {
      for (int i = 0;
      i < allies.size();
      i++) buildRelationEntry(cmd, events, "#AlliesList", i, allies.get(i), "ally");
    }
    cmd.set("#EnemiesHeader.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.REL_ENEMIES_HEADER, enemies.size()));
    cmd.clear("#EnemiesList");
    if (enemies.isEmpty()) { cmd.appendInline("#EnemiesList", "Label { Text: \"" + HFMessages.get(playerRef, MessageKeys.AdminGui.REL_NO_ENEMIES) + "\"; Style: (FontSize: 11, TextColor: #666666); Anchor: (Height: 24); }"); }
    else {
      for (int i = 0;
      i < enemies.size();
      i++) buildRelationEntry(cmd, events, "#EnemiesList", i, enemies.get(i), "enemy");
    }
    buildSetRelationSection(cmd, events, faction);
  }

  private void buildRelationEntry(UICommandBuilder cmd, UIEventBuilder events, String container, int index, RelationEntry entry, String type) {
    cmd.append(container, UIPaths.ADMIN_FACTION_RELATIONS_ENTRY);
    String idx = container + "[" + index + "]";
    cmd.set(idx + " #FactionName.Text", entry.factionName);
    cmd.set(idx + " #LeaderName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.LEADER_PREFIX, entry.leaderName));
    cmd.set(idx + " #DateEstablished.Text", formatDate(entry.sinceMillis));
    if ("ally".equals(type)) {
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetNeutralBtn", EventData.of("Button", "AdminSetNeutral").append("TargetFactionId", entry.factionId.toString()), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetEnemyBtn", EventData.of("Button", "AdminSetEnemy").append("TargetFactionId", entry.factionId.toString()), false);
    } else {
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetNeutralBtn", EventData.of("Button", "AdminSetNeutral").append("TargetFactionId", entry.factionId.toString()), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetAllyBtn", EventData.of("Button", "AdminSetAlly").append("TargetFactionId", entry.factionId.toString()), false);
    }
  }

  private void buildSetRelationSection(UICommandBuilder cmd, UIEventBuilder events, Faction faction) {
    List<Faction> neutralFactions = new ArrayList<>();
    for (Faction other : factionManager.getAllFactions()) {
      if (!other.id().equals(factionId)) {
        RelationType relation = relationManager.getRelation(factionId, other.id());
        if (relation == RelationType.NEUTRAL) {
          neutralFactions.add(other);
        }
      }
    }
    int count = Math.min(5, neutralFactions.size());
    cmd.set("#NeutralCount.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.REL_NEUTRAL_COUNT, neutralFactions.size()));
    cmd.clear("#NeutralList");
    for (int i = 0; i < count; i++) {
      Faction other = neutralFactions.get(i);
      cmd.append("#NeutralList", UIPaths.ADMIN_FACTION_RELATIONS_ENTRY);
      String idx = "#NeutralList[" + i + "]";
      FactionMember leader = other.getLeader();
      String leaderName = leader != null ? leader.username() : "Unknown";
      cmd.set(idx + " #FactionName.Text", other.name());
      cmd.set(idx + " #LeaderName.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.LEADER_PREFIX, leaderName));
      cmd.set(idx + " #DateEstablished.Text", "");
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetAllyBtn", EventData.of("Button", "AdminSetAlly").append("TargetFactionId", other.id().toString()), false);
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SetEnemyBtn", EventData.of("Button", "AdminSetEnemy").append("TargetFactionId", other.id().toString()), false);
    }
  }

  private String formatDate(long sinceMillis) {
    long daysSince = ChronoUnit.DAYS.between(Instant.ofEpochMilli(sinceMillis), Instant.now());
    if (daysSince == 0) {
      return HFMessages.get(playerRef, MessageKeys.AdminGui.REL_SINCE_TODAY);
    } else if (daysSince == 1) {
      return HFMessages.get(playerRef, MessageKeys.AdminGui.REL_SINCE_ONE_DAY);
    } else {
      return HFMessages.get(playerRef, MessageKeys.AdminGui.REL_SINCE_DAYS, daysSince);
    }
  }

  private List<RelationEntry> getRelationsOfType(Faction faction, RelationType targetType) {
    List<RelationEntry> entries = new ArrayList<>();
    for (FactionRelation relation : faction.relations().values()) {
      if (relation.type() == targetType) {
        Faction other = factionManager.getFaction(relation.targetFactionId());
        if (other != null) {
          FactionMember leader = other.getLeader();
          String leaderName = leader != null ? leader.username() : "Unknown";
          entries.add(new RelationEntry(other.id(), other.name(), leaderName, relation.since()));
        }
      }
    }
    entries.sort(Comparator.comparing(e -> e.factionName));
    return entries;
  }

  private record RelationEntry(UUID factionId, String factionName, String leaderName, long sinceMillis) {}

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, AdminFactionRelationsData data) {
    super.handleDataEvent(ref, store, data);
    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || playerRef == null) {
      sendUpdate();
      return;
    }
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }
    if (data.button == null) {
      sendUpdate();
      return;
    }
    switch (data.button) {
      case "Back" -> guiManager.openAdminFactionInfo(player, ref, store, playerRef, factionId);
      case "AdminSetAlly" -> { if (data.targetFactionId != null) { UUID targetId = UuidUtil.parseOrNull(data.targetFactionId); if (targetId == null) { player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION)); return; } Faction target = factionManager.getFaction(targetId); String targetName = target != null ? target.name() : "Unknown"; RelationManager.RelationResult result = relationManager.adminSetRelation(factionId, targetId, RelationType.ALLY); if (result == RelationManager.RelationResult.SUCCESS) player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.REL_SET_ALLY, MessageUtil.COLOR_BLUE, targetName)); else player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.REL_FAILED, result)); refresh(player, ref, store, playerRef); } }
      case "AdminSetEnemy" -> { if (data.targetFactionId != null) { UUID targetId = UuidUtil.parseOrNull(data.targetFactionId); if (targetId == null) { player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION)); return; } Faction target = factionManager.getFaction(targetId); String targetName = target != null ? target.name() : "Unknown"; RelationManager.RelationResult result = relationManager.adminSetRelation(factionId, targetId, RelationType.ENEMY); if (result == RelationManager.RelationResult.SUCCESS) player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.REL_SET_ENEMY, targetName)); else player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.REL_FAILED, result)); refresh(player, ref, store, playerRef); } }
      case "AdminSetNeutral" -> { if (data.targetFactionId != null) { UUID targetId = UuidUtil.parseOrNull(data.targetFactionId); if (targetId == null) { player.sendMessage(MessageUtil.errorText(playerRef, MessageKeys.AdminGui.INVALID_FACTION)); return; } Faction target = factionManager.getFaction(targetId); String targetName = target != null ? target.name() : "Unknown"; RelationManager.RelationResult result = relationManager.adminSetRelation(factionId, targetId, RelationType.NEUTRAL); if (result == RelationManager.RelationResult.SUCCESS) player.sendMessage(MessageUtil.text(playerRef, MessageKeys.AdminGui.REL_SET_NEUTRAL, "#888888", targetName)); else player.sendMessage(MessageUtil.adminError(playerRef, MessageKeys.AdminGui.REL_FAILED, result)); refresh(player, ref, store, playerRef); } }
      default -> sendUpdate();
    }
  }

  private void refresh(Player player, Ref<EntityStore> ref, Store<EntityStore> store, PlayerRef playerRef) {
    guiManager.openAdminFactionRelations(player, ref, store, playerRef, factionId);
  }
}
