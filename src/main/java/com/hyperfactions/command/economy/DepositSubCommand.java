package com.hyperfactions.command.economy;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f deposit {@code <amount>}
 * Deposits money from player wallet into faction treasury.
 */
public class DepositSubCommand extends FactionSubCommand {

  /** Creates a new DepositSubCommand. */
  public DepositSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("deposit", "Deposit money into faction treasury", hyperFactions, plugin);
    addAliases("dep");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {
    String[] args = CommandUtil.parseRawArgs(ctx.getInputString(), 2);

    TreasuryCommandHandler.handleDeposit(ctx, player, hyperFactions, args);
  }
}
