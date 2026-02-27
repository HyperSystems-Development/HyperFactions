package com.hyperfactions.command.info;

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
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f leaderboard (alias: /f top)
 * Opens the faction leaderboard GUI.
 */
public class LeaderboardSubCommand extends FactionSubCommand {

  /** Creates a new LeaderboardSubCommand. */
  public LeaderboardSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("leaderboard", "View faction leaderboard", hyperFactions, plugin);
    addAliases("top");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> store,
             @NotNull Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openLeaderboard(playerEntity, ref, store, player);
    }
  }
}
