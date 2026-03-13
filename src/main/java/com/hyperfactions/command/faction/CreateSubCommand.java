package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
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
 * Subcommand: /f create {@code <name>}
 * Creates a new faction.
 */
public class CreateSubCommand extends FactionSubCommand {

  /** Creates a new CreateSubCommand. */
  public CreateSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("create", "Create a faction", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.CREATE)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.NO_PERMISSION));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open CreateFactionPage when no name provided
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openCreateFactionWizard(playerEntity, ref, store, player);
        return;
      }
    }

    // Text mode or with args: create directly
    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.USAGE));
      return;
    }

    String name = fctx.joinArgs();
    FactionManager.FactionResult result = hyperFactions.getFactionManager().createFaction(
      name, player.getUuid(), player.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        ctx.sendMessage(MessageUtil.success(player, MessageKeys.Create.SUCCESS, name));
        // Open dashboard after creation (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          Faction newFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
          if (playerEntity != null && newFaction != null) {
            hyperFactions.getGuiManager().openFactionDashboard(playerEntity, ref, store, player, newFaction);
          }
        }
      }
      case ALREADY_IN_FACTION -> {
        Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
        if (existingFaction != null) {
          ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.ALREADY_IN_NAMED, existingFaction.name()));
          ctx.sendMessage(MessageUtil.info(player, MessageKeys.Create.USE_LEAVE_FIRST, COLOR_YELLOW));
        } else {
          ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.ALREADY_IN_FACTION));
        }
      }
      case NAME_TAKEN -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.NAME_TAKEN));
      case NAME_TOO_SHORT -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.NAME_TOO_SHORT));
      case NAME_TOO_LONG -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.NAME_TOO_LONG));
      default -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Create.FAILED));
    }
  }
}
