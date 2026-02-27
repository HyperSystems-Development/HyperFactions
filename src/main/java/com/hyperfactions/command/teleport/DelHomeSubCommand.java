package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f delhome
 * Deletes the faction home location.
 */
public class DelHomeSubCommand extends FactionSubCommand {

  /** Creates a new DelHomeSubCommand. */
  public DelHomeSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("delhome", "Delete faction home", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.DELHOME)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to delete faction home.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    if (faction.home() == null) {
      ctx.sendMessage(prefix().insert(msg("Your faction does not have a home set.", COLOR_YELLOW)));
      return;
    }

    FactionManager.FactionResult result = hyperFactions.getFactionManager().setHome(faction.id(), null, player.getUuid());

    if (result == FactionManager.FactionResult.SUCCESS) {
      ctx.sendMessage(prefix().insert(msg("Faction home deleted!", COLOR_GREEN)));
      broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
        .insert(msg(" deleted the faction home.", COLOR_GREEN)));
    } else if (result == FactionManager.FactionResult.NOT_OFFICER) {
      ctx.sendMessage(prefix().insert(msg("You must be an officer to delete the home.", COLOR_RED)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed to delete home.", COLOR_RED)));
    }
  }
}
