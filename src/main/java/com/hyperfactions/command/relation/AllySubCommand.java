package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.RelationManager;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to manage alliances.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("Usage: /f ally <faction>", COLOR_RED)));
      return;
    }

    String factionName = fctx.joinArgs();
    Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (targetFaction == null) {
      ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
      return;
    }

    RelationManager.RelationResult result = hyperFactions.getRelationManager().requestAlly(player.getUuid(), targetFaction.id());

    switch (result) {
      case REQUEST_SENT -> {
        ctx.sendMessage(prefix().insert(msg("Ally request sent to ", COLOR_GREEN))
          .insert(msg(targetFaction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
      }
      case REQUEST_ACCEPTED -> {
        ctx.sendMessage(prefix().insert(msg("You are now allies with ", COLOR_GREEN))
          .insert(msg(targetFaction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
      }
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
      case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
      case CANNOT_RELATE_SELF -> ctx.sendMessage(prefix().insert(msg("You cannot ally with yourself.", COLOR_RED)));
      case ALREADY_ALLY -> ctx.sendMessage(prefix().insert(msg("You are already allied with that faction.", COLOR_RED)));
      case ALLY_LIMIT_REACHED -> ctx.sendMessage(prefix().insert(msg("You have reached the maximum number of allies.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to send ally request.", COLOR_RED)));
    }
  }
}
