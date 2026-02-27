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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction info.", COLOR_RED)));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    Faction faction;
    if (fctx.hasArgs()) {
      String factionName = fctx.joinArgs();
      faction = hyperFactions.getFactionManager().getFactionByName(factionName);
      if (faction == null) {
        ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
        return;
      }
    } else {
      faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
      if (faction == null) {
        ctx.sendMessage(MessageUtil.error("You are not in a faction. Use /f info <faction>"));
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

    ctx.sendMessage(msg("=== " + faction.name() + " ===", COLOR_CYAN).bold(true));
    ctx.sendMessage(msg("Leader: ", COLOR_GRAY).insert(msg(leader != null ? leader.username() : "None", COLOR_YELLOW)));
    ctx.sendMessage(msg("Members: ", COLOR_GRAY).insert(msg(faction.getMemberCount() + "/" + ConfigManager.get().getMaxMembers(), COLOR_WHITE)));
    ctx.sendMessage(msg("Power: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f", stats.currentPower(), stats.maxPower()), COLOR_WHITE)));
    ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(stats.currentClaims() + "/" + stats.maxClaims(), COLOR_WHITE)));
    if (stats.isRaidable()) {
      ctx.sendMessage(msg("RAIDABLE!", COLOR_RED).bold(true));
    }

    // Relation info
    var relationManager = hyperFactions.getRelationManager();
    int allyCount = relationManager.getAllies(faction.id()).size();
    int enemyCount = relationManager.getEnemies(faction.id()).size();
    ctx.sendMessage(msg("Allies: ", COLOR_GRAY).insert(msg(String.valueOf(allyCount), COLOR_GREEN)));
    ctx.sendMessage(msg("Enemies: ", COLOR_GRAY).insert(msg(String.valueOf(enemyCount), COLOR_RED)));

    // Show bidirectional relation if viewer is in a different faction
    Faction viewerFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
    if (viewerFaction != null && !viewerFaction.id().equals(faction.id())) {
      RelationType theyThinkOfUs = relationManager.getRelation(faction.id(), viewerFaction.id());
      RelationType weThinkOfThem = relationManager.getRelation(viewerFaction.id(), faction.id());
      ctx.sendMessage(msg("They consider you: ", COLOR_GRAY)
        .insert(msg(theyThinkOfUs.name(), relationColor(theyThinkOfUs))));
      ctx.sendMessage(msg("You consider them: ", COLOR_GRAY)
        .insert(msg(weThinkOfThem.name(), relationColor(weThinkOfThem))));
    }
  }

  private String relationColor(RelationType type) {
    return switch (type) {
      case ALLY, OWN -> COLOR_GREEN;
      case ENEMY -> COLOR_RED;
      case NEUTRAL -> COLOR_GRAY;
    };
  }
}
