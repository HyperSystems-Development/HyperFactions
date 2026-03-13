package com.hyperfactions.command.teleport;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.MessageUtil;
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
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.DELHOME_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    if (faction.home() == null) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Home.DELHOME_NO_HOME, COLOR_YELLOW));
      return;
    }

    FactionManager.FactionResult result = hyperFactions.getFactionManager().setHome(faction.id(), null, player.getUuid());

    if (result == FactionManager.FactionResult.SUCCESS) {
      ctx.sendMessage(MessageUtil.success(player, CommandKeys.Home.DELETED));
      broadcastToFaction(faction.id(), MessageUtil.success(player, CommandKeys.Home.DELHOME_BROADCAST, player.getUsername()));
    } else if (result == FactionManager.FactionResult.NOT_OFFICER) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.DELHOME_NOT_OFFICER));
    } else {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Home.DELHOME_FAILED));
    }
  }
}
