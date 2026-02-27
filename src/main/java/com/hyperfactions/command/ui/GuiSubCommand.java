package com.hyperfactions.command.ui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
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
 * Subcommand: /f gui
 * Opens the faction main GUI.
 * Aliases: menu
 */
public class GuiSubCommand extends FactionSubCommand {

  /** Creates a new GuiSubCommand. */
  public GuiSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("gui", "Open faction GUI", hyperFactions, plugin);
    addAliases("menu");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef playerRef,
             @NotNull World currentWorld) {

    if (!hasPermission(playerRef, Permissions.USE)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
      return;
    }

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      ctx.sendMessage(prefix().insert(msg("Could not find player entity.", COLOR_RED)));
      return;
    }

    hyperFactions.getGuiManager().openFactionMain(player, ref, store, playerRef);
  }
}
