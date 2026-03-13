package com.hyperfactions.command.admin.handler;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.AdminKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.HelpKeys;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Handles /f admin economy commands.
 */
public class AdminEconomyHandler {

  private final HyperFactions hyperFactions;

  private static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  private static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  private static final String COLOR_RED = CommandUtil.COLOR_RED;

  private static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  private static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  private static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;

  private static final String COLOR_GOLD = "#FFD700";

  private static Message prefix() {
    return CommandUtil.prefix();
  }

  private static Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  private boolean hasPermission(@Nullable PlayerRef player, String permission) {
    if (player == null) {
      return true;
    }
    return CommandUtil.hasPermission(player, permission);
  }

  /** Creates a new AdminEconomyHandler. */
  public AdminEconomyHandler(HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Handles admin economy. */
  public void handleAdminEconomy(CommandContext ctx, @Nullable PlayerRef player, UUID senderUuid, String[] args) {
    if (!hasPermission(player, Permissions.ADMIN_ECONOMY)) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.NO_PERMISSION), COLOR_RED)));
      return;
    }

    EconomyManager econ = hyperFactions.getEconomyManager();
    if (econ == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, CommonKeys.Common.ECONOMY_DISABLED), COLOR_RED)));
      return;
    }

    if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
      showAdminEconomyHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();

    switch (subCmd) {
      case "balance", "bal" -> handleBalance(ctx, econ, args);
      case "set" -> handleSet(ctx, econ, senderUuid, args);
      case "add" -> handleAdd(ctx, econ, senderUuid, args);
      case "take", "remove" -> handleTake(ctx, econ, senderUuid, args);
      case "total" -> handleTotal(ctx, econ);
      case "reset" -> handleReset(ctx, econ, senderUuid, args);
      case "upkeep" -> handleUpkeep(ctx, senderUuid);
      default -> ctx.sendMessage(prefix().insert(msg(HFMessages.get(player, AdminKeys.AdminCmd.ECON_UNKNOWN_CMD), COLOR_RED)));
    }
  }

  private void showAdminEconomyHelp(CommandContext ctx, @Nullable PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();
    commands.add(new CommandHelp("/f admin economy balance <faction>", HelpKeys.Help.ECONOMY_CMD_BALANCE));
    commands.add(new CommandHelp("/f admin economy set <faction> <amount>", HelpKeys.Help.ECONOMY_CMD_SET));
    commands.add(new CommandHelp("/f admin economy add <faction> <amount>", HelpKeys.Help.ECONOMY_CMD_ADD));
    commands.add(new CommandHelp("/f admin economy take <faction> <amount>", HelpKeys.Help.ECONOMY_CMD_TAKE));
    commands.add(new CommandHelp("/f admin economy total", HelpKeys.Help.ECONOMY_CMD_TOTAL));
    commands.add(new CommandHelp("/f admin economy reset <faction>", HelpKeys.Help.ECONOMY_CMD_RESET));
    commands.add(new CommandHelp("/f admin economy upkeep", HelpKeys.Help.ECONOMY_CMD_UPKEEP));
    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.ECONOMY_TITLE, HelpKeys.Help.ECONOMY_DESCRIPTION, commands, null, player));
  }

  // /f admin economy balance <faction>
  private void handleBalance(CommandContext ctx, EconomyManager econ, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy balance <faction>", COLOR_YELLOW)));
      return;
    }
    String factionName = args[1];
    Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (faction == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.FACTION_NOT_FOUND, factionName), COLOR_RED)));
      return;
    }

    BigDecimal balance = econ.getFactionBalance(faction.id());
    ctx.sendMessage(prefix().insert(msg(faction.name(), COLOR_CYAN))
        .insert(msg("'s treasury: ", COLOR_GRAY))
        .insert(msg(econ.formatCurrency(balance), COLOR_GOLD)));
  }

  // /f admin economy set <faction> <amount>
  private void handleSet(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy set <faction> <amount>", COLOR_YELLOW)));
      return;
    }
    Faction faction = resolveFaction(ctx, args[1]);
    if (faction == null) {
      return;
    }

    BigDecimal amount = parseBigDecimal(ctx, args[2]);
    if (amount == null) {
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.BALANCE_NOT_NEGATIVE), COLOR_RED)));
      return;
    }

    BigDecimal oldBalance = econ.getFactionBalance(faction.id());
    Logger.debugEconomy("Admin set balance: faction=%s amount=%s oldBalance=%s by=%s",
        faction.name(), amount.toPlainString(), oldBalance.toPlainString(), senderUuid);

    econ.setBalance(faction.id(), amount, senderUuid).thenAccept(result -> {
      if (result == EconomyAPI.TransactionResult.SUCCESS) {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_SET, faction.name(), econ.formatCurrency(amount)), COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_FAILED, result.name()), COLOR_RED)));
      }
    }).exceptionally(ex -> {
      ErrorHandler.report(String.format("Admin economy set balance failed for %s", faction.name()), ex);
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ERROR_GENERIC, ex.getMessage()), COLOR_RED)));
      return null;
    });
  }

  // /f admin economy add <faction> <amount>
  private void handleAdd(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy add <faction> <amount>", COLOR_YELLOW)));
      return;
    }
    Faction faction = resolveFaction(ctx, args[1]);
    if (faction == null) {
      return;
    }

    BigDecimal amount = parseBigDecimal(ctx, args[2]);
    if (amount == null) {
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.AMOUNT_POSITIVE), COLOR_RED)));
      return;
    }

    String desc = "Admin added " + econ.formatCurrency(amount);
    Logger.debugEconomy("Admin add: faction=%s amount=%s by=%s", faction.name(), amount.toPlainString(), senderUuid);

    econ.adminAdjust(faction.id(), amount, senderUuid, desc).thenAccept(result -> {
      if (result == EconomyAPI.TransactionResult.SUCCESS) {
        BigDecimal newBalance = econ.getFactionBalance(faction.id());
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_ADDED, econ.formatCurrency(amount), faction.name(), econ.formatCurrency(newBalance)), COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_FAILED, result.name()), COLOR_RED)));
      }
    }).exceptionally(ex -> {
      ErrorHandler.report(String.format("Admin economy add failed for %s", faction.name()), ex);
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ERROR_GENERIC, ex.getMessage()), COLOR_RED)));
      return null;
    });
  }

  // /f admin economy take <faction> <amount>
  private void handleTake(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
    if (args.length < 3) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy take <faction> <amount>", COLOR_YELLOW)));
      return;
    }
    Faction faction = resolveFaction(ctx, args[1]);
    if (faction == null) {
      return;
    }

    BigDecimal amount = parseBigDecimal(ctx, args[2]);
    if (amount == null) {
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.AMOUNT_POSITIVE), COLOR_RED)));
      return;
    }

    String desc = "Admin deducted " + econ.formatCurrency(amount);
    Logger.debugEconomy("Admin take: faction=%s amount=%s by=%s", faction.name(), amount.toPlainString(), senderUuid);

    econ.adminAdjust(faction.id(), amount.negate(), senderUuid, desc).thenAccept(result -> {
      if (result == EconomyAPI.TransactionResult.SUCCESS) {
        BigDecimal newBalance = econ.getFactionBalance(faction.id());
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_DEDUCTED, econ.formatCurrency(amount), faction.name(), econ.formatCurrency(newBalance)), COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_FAILED, result.name()), COLOR_RED)));
      }
    }).exceptionally(ex -> {
      ErrorHandler.report(String.format("Admin economy take failed for %s", faction.name()), ex);
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ERROR_GENERIC, ex.getMessage()), COLOR_RED)));
      return null;
    });
  }

  // /f admin economy total
  private void handleTotal(CommandContext ctx, EconomyManager econ) {
    BigDecimal total = econ.getServerTotalBalance();
    int count = econ.getFactionEconomyCount();
    BigDecimal avg = count > 0 ? total.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_TOTAL_HEADER), COLOR_CYAN)));
    ctx.sendMessage(msg("  Total Balance: ", COLOR_GRAY)
        .insert(msg(econ.formatCurrency(total), COLOR_GOLD)));
    ctx.sendMessage(msg("  Factions: ", COLOR_GRAY)
        .insert(msg(String.valueOf(count), COLOR_WHITE)));
    ctx.sendMessage(msg("  Average Balance: ", COLOR_GRAY)
        .insert(msg(econ.formatCurrency(avg), COLOR_WHITE)));
  }

  // /f admin economy reset <faction>
  private void handleReset(CommandContext ctx, EconomyManager econ, UUID senderUuid, String[] args) {
    if (args.length < 2) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f admin economy reset <faction>", COLOR_YELLOW)));
      return;
    }
    Faction faction = resolveFaction(ctx, args[1]);
    if (faction == null) {
      return;
    }

    BigDecimal oldBalance = econ.getFactionBalance(faction.id());
    Logger.debugEconomy("Admin reset: faction=%s oldBalance=%s by=%s",
        faction.name(), oldBalance.toPlainString(), senderUuid);

    econ.setBalance(faction.id(), BigDecimal.ZERO, senderUuid).thenAccept(result -> {
      if (result == EconomyAPI.TransactionResult.SUCCESS) {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_RESET, faction.name()), COLOR_GREEN)));
      } else {
        ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_FAILED, result.name()), COLOR_RED)));
      }
    }).exceptionally(ex -> {
      ErrorHandler.report(String.format("Admin economy reset failed for %s", faction.name()), ex);
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ERROR_GENERIC, ex.getMessage()), COLOR_RED)));
      return null;
    });
  }

  // /f admin economy upkeep
  private void handleUpkeep(CommandContext ctx, UUID senderUuid) {
    com.hyperfactions.economy.UpkeepProcessor processor = hyperFactions.getUpkeepProcessor();
    if (processor == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_UPKEEP_DISABLED), COLOR_RED)));
      return;
    }

    ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_UPKEEP_TRIGGER), COLOR_CYAN)));
    Logger.info("[Admin] %s manually triggered upkeep collection", senderUuid);

    try {
      processor.processUpkeep();
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_UPKEEP_COMPLETE), COLOR_GREEN)));
    } catch (Exception e) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.ECON_UPKEEP_FAILED, e.getMessage()), COLOR_RED)));
      ErrorHandler.report("[Admin] Manual upkeep collection failed", e);
    }
  }

  @Nullable
  private Faction resolveFaction(CommandContext ctx, String name) {
    Faction faction = hyperFactions.getFactionManager().getFactionByName(name);
    if (faction == null) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.FACTION_NOT_FOUND, name), COLOR_RED)));
    }
    return faction;
  }

  @Nullable
  private BigDecimal parseBigDecimal(CommandContext ctx, String value) {
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      ctx.sendMessage(prefix().insert(msg(HFMessages.get((PlayerRef) null, AdminKeys.AdminCmd.INVALID_NUMBER, value), COLOR_RED)));
      return null;
    }
  }
}
