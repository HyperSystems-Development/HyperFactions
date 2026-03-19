package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
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
 * Subcommand: /f enemy {@code <faction>}
 * Declares another faction as an enemy.
 */
public class EnemySubCommand extends FactionSubCommand {

  /** Creates a new EnemySubCommand. */
  public EnemySubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("enemy", "Declare enemy", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.ENEMY)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.ENEMY_NO_PERMISSION));
      return;
    }

    Faction myFaction = requireFaction(ctx, player);
    if (myFaction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open SetRelationModal when no faction specified
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openSetRelationModal(playerEntity, ref, store, player, myFaction);
        return;
      }
    }

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.ENEMY_USAGE));
      return;
    }

    String factionName = fctx.joinArgs();
    Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (targetFaction == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.FACTION_NOT_FOUND));
      return;
    }

    RelationManager.RelationResult result = hyperFactions.getRelationManager().setEnemy(player.getUuid(), targetFaction.id());

    switch (result) {
      case SUCCESS -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.ENEMY_DECLARED, targetFaction.name()));
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      case NOT_OFFICER -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.MUST_BE_OFFICER));
      case ALREADY_ENEMY -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.ALREADY_ENEMY));
      case ENEMY_LIMIT_REACHED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.MAX_ENEMIES));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Relation.ENEMY_FAILED));
    }
  }
}
