package com.hyperfactions.command.economy;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.manager.EconomyManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Shared logic for all treasury commands.
 * Avoids duplication between MoneySubCommand and top-level shortcuts.
 */
public final class TreasuryCommandHandler {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd HH:mm")
      .withZone(ZoneId.systemDefault());

  private TreasuryCommandHandler() {}

  /**
   * Handles /f balance [faction].
   */
  public static void handleBalance(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                   @NotNull HyperFactions hf, String[] args) {
    if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_BALANCE)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.BALANCE_NO_PERMISSION));
      return;
    }

    EconomyManager econ = hf.getEconomyManager();
    if (econ == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TREASURY_UNAVAILABLE));
      return;
    }

    Faction faction;
    if (args.length > 0) {
      faction = hf.getFactionManager().getFactionByName(args[0]);
      if (faction == null) {
        ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.FACTION_NOT_FOUND));
        return;
      }
    } else {
      faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
      if (faction == null) {
        ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
        return;
      }
    }

    BigDecimal balance = econ.getFactionBalance(faction.id());
    ctx.sendMessage(MessageUtil.success(player, CommandKeys.Economy.BALANCE_DISPLAY,
        faction.name(), econ.formatCurrency(balance)));
  }

  /**
   * Handles /f deposit {@code <amount>}.
   */
  public static void handleDeposit(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                   @NotNull HyperFactions hf, String[] args) {
    if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_DEPOSIT)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.DEPOSIT_NO_PERMISSION));
      return;
    }

    EconomyManager econ = hf.getEconomyManager();
    VaultEconomyProvider vault = econ != null ? econ.getVaultProvider() : null;
    if (econ == null || vault == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TREASURY_UNAVAILABLE));
      return;
    }

    Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      return;
    }

    // Check faction permission
    FactionMember member = faction.getMember(player.getUuid());
    if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_DEPOSIT)
        && !member.isOfficerOrHigher()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.DEPOSIT_FACTION_DENIED));
      return;
    }

    if (args.length < 1) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Economy.DEPOSIT_USAGE, MessageUtil.COLOR_YELLOW));
      return;
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(args[0]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.INVALID_AMOUNT, args[0]));
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.AMOUNT_POSITIVE));
      return;
    }

    // Check player has enough in wallet
    if (!vault.has(player.getUuid(), amount)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WALLET_INSUFFICIENT,
          econ.formatCurrency(vault.getBalanceBigDecimal(player.getUuid()))));
      return;
    }

    // Withdraw from player wallet
    if (!vault.withdraw(player.getUuid(), amount)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WALLET_WITHDRAW_FAILED));
      return;
    }

    // Deposit to faction treasury
    EconomyAPI.TransactionResult result = econ.deposit(
        faction.id(), amount, player.getUuid(), "Player deposit").join();

    if (result != EconomyAPI.TransactionResult.SUCCESS) {
      // Rollback: return money to player
      vault.deposit(player.getUuid(), amount);
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.DEPOSIT_FAILED));
      return;
    }

    ctx.sendMessage(MessageUtil.success(player, CommandKeys.Economy.DEPOSITED, econ.formatCurrency(amount)));
  }

  /**
   * Handles /f withdraw {@code <amount>}.
   */
  public static void handleWithdraw(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                   @NotNull HyperFactions hf, String[] args) {
    if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_WITHDRAW)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WITHDRAW_NO_PERMISSION));
      return;
    }

    EconomyManager econ = hf.getEconomyManager();
    VaultEconomyProvider vault = econ != null ? econ.getVaultProvider() : null;
    if (econ == null || vault == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TREASURY_UNAVAILABLE));
      return;
    }

    Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      return;
    }

    // Check faction permission
    FactionMember member = faction.getMember(player.getUuid());
    if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_WITHDRAW)
        && !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WITHDRAW_FACTION_DENIED));
      return;
    }

    if (args.length < 1) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Economy.WITHDRAW_USAGE, MessageUtil.COLOR_YELLOW));
      return;
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(args[0]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.INVALID_AMOUNT, args[0]));
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.AMOUNT_POSITIVE));
      return;
    }

    // Check limits before attempting
    String limitReason = econ.checkWithdrawLimits(faction.id(), amount);
    if (limitReason != null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WITHDRAW_LIMIT_DENIED, limitReason));
      return;
    }

    // Withdraw from faction treasury
    EconomyAPI.TransactionResult result = econ.withdraw(
        faction.id(), amount, player.getUuid(), "Player withdrawal").join();

    switch (result) {
      case SUCCESS -> {
        // Deposit to player wallet
        if (!vault.deposit(player.getUuid(), amount)) {
          // Rollback is complex — log the error
          ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WALLET_DEPOSIT_FAILED));
          return;
        }
        ctx.sendMessage(MessageUtil.success(player, CommandKeys.Economy.WITHDRAWN, econ.formatCurrency(amount)));
      }
      case INSUFFICIENT_FUNDS -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.INSUFFICIENT));
      case LIMIT_EXCEEDED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WITHDRAW_LIMIT_EXCEEDED));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.WITHDRAW_FAILED, result));
    }
  }

  /**
   * Handles /f money transfer {@code <faction>} {@code <amount>}.
   */
  public static void handleTransfer(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                   @NotNull HyperFactions hf, String[] args) {
    if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_TRANSFER)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_NO_PERMISSION));
      return;
    }

    EconomyManager econ = hf.getEconomyManager();
    if (econ == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TREASURY_UNAVAILABLE));
      return;
    }

    Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      return;
    }

    // Check faction permission
    FactionMember member = faction.getMember(player.getUuid());
    if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_TRANSFER)
        && !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_FACTION_DENIED));
      return;
    }

    if (args.length < 2) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Economy.TRANSFER_USAGE, MessageUtil.COLOR_YELLOW));
      return;
    }

    Faction target = hf.getFactionManager().getFactionByName(args[0]);
    if (target == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.FACTION_NOT_FOUND));
      return;
    }

    if (target.id().equals(faction.id())) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_SELF));
      return;
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(args[1]);
    } catch (NumberFormatException e) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.INVALID_AMOUNT, args[1]));
      return;
    }

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.AMOUNT_POSITIVE));
      return;
    }

    // Check limits
    String limitReason = econ.checkTransferLimits(faction.id(), amount);
    if (limitReason != null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_LIMIT_DENIED, limitReason));
      return;
    }

    EconomyAPI.TransactionResult result = econ.transfer(
        faction.id(), target.id(), amount, player.getUuid(), "Player transfer").join();

    switch (result) {
      case SUCCESS -> ctx.sendMessage(MessageUtil.success(player, CommandKeys.Economy.TRANSFERRED,
          econ.formatCurrency(amount), target.name()));
      case INSUFFICIENT_FUNDS -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.INSUFFICIENT));
      case LIMIT_EXCEEDED -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_LIMIT_EXCEEDED));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TRANSFER_FAILED, result));
    }
  }

  /**
   * Handles /f money log [page] [type].
   */
  public static void handleLog(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                 @NotNull HyperFactions hf, String[] args) {
    if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_LOG)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.LOG_NO_PERMISSION));
      return;
    }

    EconomyManager econ = hf.getEconomyManager();
    if (econ == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Economy.TREASURY_UNAVAILABLE));
      return;
    }

    Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NOT_IN_FACTION));
      return;
    }

    int page = 1;
    String typeFilter = null;
    if (args.length >= 1) {
      try {
        page = Integer.parseInt(args[0]);
      } catch (NumberFormatException e) {
        typeFilter = args[0].toUpperCase();
      }
    }
    if (args.length >= 2) {
      typeFilter = args[1].toUpperCase();
    }

    List<EconomyAPI.Transaction> all = econ.getTransactionHistory(faction.id(), FactionEconomy.MAX_HISTORY);
    if (typeFilter != null) {
      String filter = typeFilter;
      all = all.stream().filter(tx -> tx.type().name().equals(filter)).toList();
    }

    int perPage = 8;
    int totalPages = Math.max(1, (all.size() + perPage - 1) / perPage);
    page = Math.max(1, Math.min(page, totalPages));

    ctx.sendMessage(MessageUtil.info(player, CommandKeys.Economy.LOG_HEADER, MessageUtil.COLOR_CYAN, page, totalPages));

    int start = (page - 1) * perPage;
    int end = Math.min(start + perPage, all.size());
    for (int i = start; i < end; i++) {
      EconomyAPI.Transaction tx = all.get(i);
      String time = TIME_FORMAT.format(Instant.ofEpochMilli(tx.timestamp()));
      String typeColor = switch (tx.type()) {
        case DEPOSIT, TRANSFER_IN, SPOILS -> CommandUtil.COLOR_GREEN;
        case WITHDRAW, TRANSFER_OUT, UPKEEP, WAR_COST, RAID_COST -> CommandUtil.COLOR_RED;
        default -> CommandUtil.COLOR_YELLOW;
      };
      String sign = switch (tx.type()) {
        case DEPOSIT, TRANSFER_IN, SPOILS, TAX_COLLECTION -> "+";
        case WITHDRAW, TRANSFER_OUT, UPKEEP, WAR_COST, RAID_COST -> "-";
        default -> "";
      };

      ctx.sendMessage(
          CommandUtil.msg("[" + time + "] ", CommandUtil.COLOR_GRAY)
              .insert(CommandUtil.msg(tx.type().name() + " ", typeColor))
              .insert(CommandUtil.msg(sign + econ.formatCurrency(tx.amount()) + " ", CommandUtil.COLOR_WHITE))
              .insert(CommandUtil.msg(tx.description(), CommandUtil.COLOR_GRAY)));
    }

    if (all.isEmpty()) {
      ctx.sendMessage(CommandUtil.msg("  " + HFMessages.get(player, CommandKeys.Economy.LOG_EMPTY), CommandUtil.COLOR_GRAY));
    }
  }
}
