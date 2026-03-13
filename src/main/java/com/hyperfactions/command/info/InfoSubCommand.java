package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f info [faction]
 * Views faction information.
 * Aliases: show
 */
public class InfoSubCommand extends FactionSubCommand {

  /** Creates a new InfoSubCommand. */
  public InfoSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("info", "View faction info", hyperFactions, plugin);
    addAliases("show");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.INFO)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.NO_PERMISSION));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    Faction faction;
    if (fctx.hasArgs()) {
      String factionName = fctx.joinArgs();
      faction = hyperFactions.getFactionManager().getFactionByName(factionName);
      if (faction == null) {
        ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.FACTION_NOT_FOUND, factionName));
        return;
      }
    } else {
      faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
      if (faction == null) {
        ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.NOT_IN_FACTION_HINT));
        return;
      }
    }

    // GUI mode: open FactionInfoPage (default when no args and no --text flag)
    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionInfo(playerEntity, ref, store, player, faction);
        return;
      }
    }

    // Text mode: output to chat
    PowerManager.FactionPowerStats stats = hyperFactions.getPowerManager().getFactionPowerStats(faction.id());
    FactionMember leader = faction.getLeader();

    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.FACTION_HEADER, faction.name()), COLOR_CYAN).bold(true));
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.LEADER, leader != null ? leader.username() : HFMessages.get(player, CommonKeys.Common.NONE)), COLOR_GRAY));
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.MEMBERS, faction.getMemberCount(), ConfigManager.get().getMaxMembers()), COLOR_GRAY));
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.POWER, String.format("%.1f/%.1f", stats.currentPower(), stats.maxPower())), COLOR_GRAY));
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.CLAIMS, stats.currentClaims() + "/" + stats.maxClaims()), COLOR_GRAY));
    if (stats.isRaidable()) {
      ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.RAIDABLE), COLOR_RED).bold(true));
    }

    // Relation info
    var relationManager = hyperFactions.getRelationManager();
    int allyCount = relationManager.getAllies(faction.id()).size();
    int enemyCount = relationManager.getEnemies(faction.id()).size();
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.ALLIES, allyCount), COLOR_GRAY));
    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.ENEMIES, enemyCount), COLOR_GRAY));

    // Show bidirectional relation if viewer is in a different faction
    Faction viewerFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
    if (viewerFaction != null && !viewerFaction.id().equals(faction.id())) {
      RelationType theyThinkOfUs = relationManager.getRelation(faction.id(), viewerFaction.id());
      RelationType weThinkOfThem = relationManager.getRelation(viewerFaction.id(), faction.id());
      ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.THEY_CONSIDER, theyThinkOfUs.name()), COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.YOU_CONSIDER, weThinkOfThem.name()), COLOR_GRAY));
    }
  }
}
