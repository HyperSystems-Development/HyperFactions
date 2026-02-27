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
 * Subcommand: /f withdraw {@code <amount>}
 * Withdraws money from faction treasury into player wallet.
 */
public class WithdrawSubCommand extends FactionSubCommand {

  /** Creates a new WithdrawSubCommand. */
  public WithdrawSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("withdraw", "Withdraw money from faction treasury", hyperFactions, plugin);
    addAliases("wd");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {
    String[] args = CommandUtil.parseRawArgs(ctx.getInputString(), 2);

    TreasuryCommandHandler.handleWithdraw(ctx, player, hyperFactions, args);
  }
}
