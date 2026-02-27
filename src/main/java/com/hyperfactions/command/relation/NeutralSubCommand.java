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
 * Subcommand: /f neutral {@code <faction>}
 * Sets neutral relations with another faction.
 */
public class NeutralSubCommand extends FactionSubCommand {

  /** Creates a new NeutralSubCommand. */
  public NeutralSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("neutral", "Set neutral relation", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.NEUTRAL)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to set neutral relations.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("Usage: /f neutral <faction>", COLOR_RED)));
      return;
    }

    String factionName = fctx.joinArgs();
    Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (targetFaction == null) {
      ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
      return;
    }

    RelationManager.RelationResult result = hyperFactions.getRelationManager().setNeutral(player.getUuid(), targetFaction.id());

    switch (result) {
      case SUCCESS -> ctx.sendMessage(prefix().insert(msg("Your faction is now neutral with " + targetFaction.name() + ".", COLOR_GRAY)));
      case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
      case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
      case ALREADY_NEUTRAL -> ctx.sendMessage(prefix().insert(msg("You are already neutral with that faction.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to set neutral.", COLOR_RED)));
    }
  }
}
