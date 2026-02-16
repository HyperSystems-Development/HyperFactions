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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have permission to view balances.", CommandUtil.COLOR_RED)));
            return;
        }

        EconomyManager econ = hf.getEconomyManager();
        if (econ == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Treasury is not available.", CommandUtil.COLOR_RED)));
            return;
        }

        Faction faction;
        if (args.length > 0) {
            faction = hf.getFactionManager().getFactionByName(args[0]);
            if (faction == null) {
                ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                        "Faction '" + args[0] + "' not found.", CommandUtil.COLOR_RED)));
                return;
            }
        } else {
            faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
            if (faction == null) {
                ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                        "You are not in a faction.", CommandUtil.COLOR_RED)));
                return;
            }
        }

        double balance = econ.getFactionBalance(faction.id());
        ctx.sendMessage(CommandUtil.prefix()
                .insert(CommandUtil.msg(faction.name() + "'s treasury: ", CommandUtil.COLOR_CYAN))
                .insert(CommandUtil.msg(econ.formatCurrency(balance), CommandUtil.COLOR_GREEN)));
    }

    /**
     * Handles /f deposit <amount>.
     */
    public static void handleDeposit(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                                      @NotNull HyperFactions hf, String[] args) {
        if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_DEPOSIT)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have permission to deposit.", CommandUtil.COLOR_RED)));
            return;
        }

        EconomyManager econ = hf.getEconomyManager();
        VaultEconomyProvider vault = econ != null ? econ.getVaultProvider() : null;
        if (econ == null || vault == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Treasury is not available.", CommandUtil.COLOR_RED)));
            return;
        }

        Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You are not in a faction.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check faction permission
        FactionMember member = faction.getMember(player.getUuid());
        if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_DEPOSIT)
                && !member.isOfficerOrHigher()) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have faction permission to deposit.", CommandUtil.COLOR_RED)));
            return;
        }

        if (args.length < 1) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Usage: /f deposit <amount>", CommandUtil.COLOR_YELLOW)));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Invalid amount: " + args[0], CommandUtil.COLOR_RED)));
            return;
        }

        if (amount <= 0) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Amount must be positive.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check player has enough in wallet
        if (!vault.has(player.getUuid(), amount)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have enough money. Wallet: " + econ.formatCurrency(vault.getBalance(player.getUuid())),
                    CommandUtil.COLOR_RED)));
            return;
        }

        // Withdraw from player wallet
        if (!vault.withdraw(player.getUuid(), amount)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Failed to withdraw from your wallet.", CommandUtil.COLOR_RED)));
            return;
        }

        // Deposit to faction treasury
        EconomyAPI.TransactionResult result = econ.deposit(
                faction.id(), amount, player.getUuid(), "Player deposit").join();

        if (result != EconomyAPI.TransactionResult.SUCCESS) {
            // Rollback: return money to player
            vault.deposit(player.getUuid(), amount);
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Failed to deposit to faction treasury. Money returned.", CommandUtil.COLOR_RED)));
            return;
        }

        ctx.sendMessage(CommandUtil.prefix()
                .insert(CommandUtil.msg("Deposited ", CommandUtil.COLOR_GREEN))
                .insert(CommandUtil.msg(econ.formatCurrency(amount), CommandUtil.COLOR_CYAN))
                .insert(CommandUtil.msg(" into the faction treasury.", CommandUtil.COLOR_GREEN)));
    }

    /**
     * Handles /f withdraw <amount>.
     */
    public static void handleWithdraw(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                                       @NotNull HyperFactions hf, String[] args) {
        if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_WITHDRAW)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have permission to withdraw.", CommandUtil.COLOR_RED)));
            return;
        }

        EconomyManager econ = hf.getEconomyManager();
        VaultEconomyProvider vault = econ != null ? econ.getVaultProvider() : null;
        if (econ == null || vault == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Treasury is not available.", CommandUtil.COLOR_RED)));
            return;
        }

        Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You are not in a faction.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check faction permission
        FactionMember member = faction.getMember(player.getUuid());
        if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_WITHDRAW)
                && !member.isLeader()) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have faction permission to withdraw.", CommandUtil.COLOR_RED)));
            return;
        }

        if (args.length < 1) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Usage: /f withdraw <amount>", CommandUtil.COLOR_YELLOW)));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Invalid amount: " + args[0], CommandUtil.COLOR_RED)));
            return;
        }

        if (amount <= 0) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Amount must be positive.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check limits before attempting
        String limitReason = econ.checkWithdrawLimits(faction.id(), amount);
        if (limitReason != null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Withdrawal denied: " + limitReason, CommandUtil.COLOR_RED)));
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
                    ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                            "Warning: Failed to deposit to your wallet. Contact an admin.",
                            CommandUtil.COLOR_RED)));
                    return;
                }
                ctx.sendMessage(CommandUtil.prefix()
                        .insert(CommandUtil.msg("Withdrew ", CommandUtil.COLOR_GREEN))
                        .insert(CommandUtil.msg(econ.formatCurrency(amount), CommandUtil.COLOR_CYAN))
                        .insert(CommandUtil.msg(" from the faction treasury.", CommandUtil.COLOR_GREEN)));
            }
            case INSUFFICIENT_FUNDS -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Insufficient funds in faction treasury.", CommandUtil.COLOR_RED)));
            case LIMIT_EXCEEDED -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Withdrawal denied: limit exceeded.", CommandUtil.COLOR_RED)));
            default -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Withdrawal failed: " + result, CommandUtil.COLOR_RED)));
        }
    }

    /**
     * Handles /f money transfer <faction> <amount>.
     */
    public static void handleTransfer(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                                       @NotNull HyperFactions hf, String[] args) {
        if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_TRANSFER)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have permission to transfer.", CommandUtil.COLOR_RED)));
            return;
        }

        EconomyManager econ = hf.getEconomyManager();
        if (econ == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Treasury is not available.", CommandUtil.COLOR_RED)));
            return;
        }

        Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You are not in a faction.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check faction permission
        FactionMember member = faction.getMember(player.getUuid());
        if (member != null && !faction.getEffectivePermissions().get(FactionPermissions.TREASURY_TRANSFER)
                && !member.isLeader()) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have faction permission to transfer.", CommandUtil.COLOR_RED)));
            return;
        }

        if (args.length < 2) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Usage: /f money transfer <faction> <amount>", CommandUtil.COLOR_YELLOW)));
            return;
        }

        Faction target = hf.getFactionManager().getFactionByName(args[0]);
        if (target == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Faction '" + args[0] + "' not found.", CommandUtil.COLOR_RED)));
            return;
        }

        if (target.id().equals(faction.id())) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Cannot transfer to your own faction.", CommandUtil.COLOR_RED)));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Invalid amount: " + args[1], CommandUtil.COLOR_RED)));
            return;
        }

        if (amount <= 0) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Amount must be positive.", CommandUtil.COLOR_RED)));
            return;
        }

        // Check limits
        String limitReason = econ.checkTransferLimits(faction.id(), amount);
        if (limitReason != null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Transfer denied: " + limitReason, CommandUtil.COLOR_RED)));
            return;
        }

        EconomyAPI.TransactionResult result = econ.transfer(
                faction.id(), target.id(), amount, player.getUuid(), "Player transfer").join();

        switch (result) {
            case SUCCESS -> ctx.sendMessage(CommandUtil.prefix()
                    .insert(CommandUtil.msg("Transferred ", CommandUtil.COLOR_GREEN))
                    .insert(CommandUtil.msg(econ.formatCurrency(amount), CommandUtil.COLOR_CYAN))
                    .insert(CommandUtil.msg(" to " + target.name() + ".", CommandUtil.COLOR_GREEN)));
            case INSUFFICIENT_FUNDS -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Insufficient funds in faction treasury.", CommandUtil.COLOR_RED)));
            case LIMIT_EXCEEDED -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Transfer denied: limit exceeded.", CommandUtil.COLOR_RED)));
            default -> ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Transfer failed: " + result, CommandUtil.COLOR_RED)));
        }
    }

    /**
     * Handles /f money log [page] [type].
     */
    public static void handleLog(@NotNull CommandContext ctx, @NotNull PlayerRef player,
                                  @NotNull HyperFactions hf, String[] args) {
        if (!CommandUtil.hasPermission(player, Permissions.ECONOMY_LOG)) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You don't have permission to view the transaction log.", CommandUtil.COLOR_RED)));
            return;
        }

        EconomyManager econ = hf.getEconomyManager();
        if (econ == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "Treasury is not available.", CommandUtil.COLOR_RED)));
            return;
        }

        Faction faction = hf.getFactionManager().getPlayerFaction(player.getUuid());
        if (faction == null) {
            ctx.sendMessage(CommandUtil.prefix().insert(CommandUtil.msg(
                    "You are not in a faction.", CommandUtil.COLOR_RED)));
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

        ctx.sendMessage(CommandUtil.prefix()
                .insert(CommandUtil.msg("Transaction Log (page " + page + "/" + totalPages + ")", CommandUtil.COLOR_CYAN)));

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
            ctx.sendMessage(CommandUtil.msg("  No transactions found.", CommandUtil.COLOR_GRAY));
        }
    }
}
