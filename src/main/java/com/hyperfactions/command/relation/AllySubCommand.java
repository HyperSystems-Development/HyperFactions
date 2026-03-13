package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.MessageKeys;
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
 * Subcommand: /f ally {@code <faction>}
 * Requests or accepts an alliance with another faction.
 */
public class AllySubCommand extends FactionSubCommand {

  /** Creates a new AllySubCommand. */
  public AllySubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("ally", "Request alliance", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.ALLY)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.ALLY_NO_PERMISSION));
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
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.ALLY_USAGE));
      return;
    }

    String factionName = fctx.joinArgs();
    Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (targetFaction == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.FACTION_NOT_FOUND));
      return;
    }

    RelationManager.RelationResult result = hyperFactions.getRelationManager().requestAlly(player.getUuid(), targetFaction.id());

    switch (result) {
      case REQUEST_SENT -> ctx.sendMessage(MessageUtil.success(player, MessageKeys.Relation.ALLY_SENT, targetFaction.name()));
      case REQUEST_ACCEPTED -> ctx.sendMessage(MessageUtil.success(player, MessageKeys.Relation.ALLY_FORMED, targetFaction.name()));
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.NOT_IN_FACTION));
      case NOT_OFFICER -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.MUST_BE_OFFICER));
      case CANNOT_RELATE_SELF -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.CANNOT_SELF));
      case ALREADY_ALLY -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.ALREADY_ALLY));
      case ALLY_LIMIT_REACHED -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.MAX_ALLIES));
      default -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Relation.ALLY_FAILED));
    }
  }
}
