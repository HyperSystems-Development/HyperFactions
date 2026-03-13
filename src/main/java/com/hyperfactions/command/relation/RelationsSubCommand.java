package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
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
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f relations
 * Views faction diplomatic relations.
 */
public class RelationsSubCommand extends FactionSubCommand {

  /** Creates a new RelationsSubCommand. */
  public RelationsSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("relations", "View faction relations", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.RELATIONS)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.VIEW_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open FactionRelationsPage
    if (fctx.shouldOpenGui()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionRelations(playerEntity, ref, store, player, faction);
        return;
      }
    }

    // Text mode: list relations
    List<UUID> allies = hyperFactions.getRelationManager().getAllies(faction.id());
    List<UUID> enemies = hyperFactions.getRelationManager().getEnemies(faction.id());

    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Relation.HEADER), COLOR_CYAN).bold(true));

    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Relation.ALLIES_COUNT, allies.size()), COLOR_GREEN));
    if (allies.isEmpty()) {
      ctx.sendMessage(msg("  (" + HFMessages.get(player, CommonKeys.Common.NONE) + ")", COLOR_GRAY));
    } else {
      for (UUID allyId : allies) {
        Faction ally = hyperFactions.getFactionManager().getFaction(allyId);
        if (ally != null) {
          ctx.sendMessage(msg("  ", COLOR_GRAY).insert(msg(HFMessages.get(player, CommandKeys.Relation.LIST_ENTRY, ally.name()), COLOR_GREEN)));
        }
      }
    }

    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Relation.ENEMIES_COUNT, enemies.size()), COLOR_RED));
    if (enemies.isEmpty()) {
      ctx.sendMessage(msg("  (" + HFMessages.get(player, CommonKeys.Common.NONE) + ")", COLOR_GRAY));
    } else {
      for (UUID enemyId : enemies) {
        Faction enemy = hyperFactions.getFactionManager().getFaction(enemyId);
        if (enemy != null) {
          ctx.sendMessage(msg("  ", COLOR_GRAY).insert(msg(HFMessages.get(player, CommandKeys.Relation.LIST_ENTRY, enemy.name()), COLOR_RED)));
        }
      }
    }
  }
}
