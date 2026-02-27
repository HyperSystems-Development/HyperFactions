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
 * Container subcommand: /f money <sub>
 * Routes to balance, deposit, withdraw, transfer, log.
 */
public class MoneySubCommand extends FactionSubCommand {

  /** Creates a new MoneySubCommand. */
  public MoneySubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("money", "Faction treasury commands", hyperFactions, plugin);
    addAliases("treasury", "econ");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {
    // Parse: parts[0]=faction, parts[1]=money, parts[2]=subcommand, parts[3+]=args
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 3) {
      sendHelp(ctx);
      return;
    }

    String subCmd = parts[2].toLowerCase();
    String[] subArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 3);

    switch (subCmd) {
      case "balance", "bal" -> TreasuryCommandHandler.handleBalance(ctx, player, hyperFactions, subArgs);
      case "deposit", "dep" -> TreasuryCommandHandler.handleDeposit(ctx, player, hyperFactions, subArgs);
      case "withdraw", "wd" -> TreasuryCommandHandler.handleWithdraw(ctx, player, hyperFactions, subArgs);
      case "transfer", "send" -> TreasuryCommandHandler.handleTransfer(ctx, player, hyperFactions, subArgs);
      case "log", "history" -> TreasuryCommandHandler.handleLog(ctx, player, hyperFactions, subArgs);
      default -> sendHelp(ctx);
    }
  }

  private void sendHelp(CommandContext ctx) {
    ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg("Treasury Commands:", COLOR_CYAN)));
    ctx.sendMessage(CommandUtil.msg("  /f money balance [faction]", COLOR_YELLOW)
        .insert(CommandUtil.msg(" - View balance", COLOR_GRAY)));
    ctx.sendMessage(CommandUtil.msg("  /f money deposit <amount>", COLOR_YELLOW)
        .insert(CommandUtil.msg(" - Deposit into treasury", COLOR_GRAY)));
    ctx.sendMessage(CommandUtil.msg("  /f money withdraw <amount>", COLOR_YELLOW)
        .insert(CommandUtil.msg(" - Withdraw from treasury", COLOR_GRAY)));
    ctx.sendMessage(CommandUtil.msg("  /f money transfer <faction> <amount>", COLOR_YELLOW)
        .insert(CommandUtil.msg(" - Transfer between factions", COLOR_GRAY)));
    ctx.sendMessage(CommandUtil.msg("  /f money log [page] [type]", COLOR_YELLOW)
        .insert(CommandUtil.msg(" - View transaction history", COLOR_GRAY)));
  }
}
