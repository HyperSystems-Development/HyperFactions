package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to create factions.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("Usage: /f create <name>", COLOR_RED)));
      return;
    }

    String name = fctx.joinArgs();
    FactionManager.FactionResult result = hyperFactions.getFactionManager().createFaction(
      name, player.getUuid(), player.getUsername()
    );

    switch (result) {
      case SUCCESS -> {
        ctx.sendMessage(prefix().insert(msg("Faction '", COLOR_GREEN))
          .insert(msg(name, COLOR_CYAN)).insert(msg("' created!", COLOR_GREEN)));
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
          ctx.sendMessage(prefix().insert(msg("You are already in ", COLOR_RED))
            .insert(msg(existingFaction.name(), COLOR_CYAN))
            .insert(msg(".", COLOR_RED)));
          ctx.sendMessage(prefix().insert(msg("Use /f leave first if you want to create a new faction.", COLOR_YELLOW)));
        } else {
          ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
        }
      }
      case NAME_TAKEN -> ctx.sendMessage(prefix().insert(msg("That faction name is already taken.", COLOR_RED)));
      case NAME_TOO_SHORT -> ctx.sendMessage(prefix().insert(msg("Faction name is too short.", COLOR_RED)));
      case NAME_TOO_LONG -> ctx.sendMessage(prefix().insert(msg("Faction name is too long.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to create faction.", COLOR_RED)));
    }
  }
}
