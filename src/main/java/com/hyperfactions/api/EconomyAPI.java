package com.hyperfactions.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for HyperFactions economy system.
 * Provides access to faction treasury operations.
 */
public interface EconomyAPI {

    /**
     * Result of an economy transaction.
     */
    enum TransactionResult {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        INVALID_AMOUNT,
        FACTION_NOT_FOUND,
        PLAYER_NOT_FOUND,
        NOT_IN_FACTION,
        NO_PERMISSION,
        LIMIT_EXCEEDED,
        ERROR
    }

    /**
     * Represents a transaction in faction treasury.
     */
    record Transaction(
        @NotNull UUID factionId,
        @Nullable UUID actorId,
        @NotNull TransactionType type,
        double amount,
        double balanceAfter,
        long timestamp,
        @NotNull String description
    ) {}

    /**
     * Types of transactions.
     */
    enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER_IN,
        TRANSFER_OUT,
        UPKEEP,
        TAX_COLLECTION,
        WAR_COST,
        RAID_COST,
        SPOILS,
        PLAYER_TRANSFER_OUT,
        ADMIN_ADJUSTMENT
    }

    // === Balance Queries ===

    /**
     * Gets a faction's treasury balance.
     *
     * @param factionId the faction ID
     * @return the balance, or 0.0 if faction not found
     */
    double getFactionBalance(@NotNull UUID factionId);

    /**
     * Checks if a faction has sufficient funds.
     *
     * @param factionId the faction ID
     * @param amount    the amount to check
     * @return true if faction has at least this amount
     */
    boolean hasFunds(@NotNull UUID factionId, double amount);

    // === Transactions ===

    /**
     * Deposits money into a faction's treasury.
     *
     * @param factionId   the faction ID
     * @param amount      the amount to deposit
     * @param actorId     the player making the deposit (null for system)
     * @param description description for transaction log
     * @return the transaction result
     */
    @NotNull
    CompletableFuture<TransactionResult> deposit(
        @NotNull UUID factionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    );

    /**
     * Withdraws money from a faction's treasury.
     *
     * @param factionId   the faction ID
     * @param amount      the amount to withdraw
     * @param actorId     the player making the withdrawal
     * @param description description for transaction log
     * @return the transaction result
     */
    @NotNull
    CompletableFuture<TransactionResult> withdraw(
        @NotNull UUID factionId,
        double amount,
        @NotNull UUID actorId,
        @NotNull String description
    );

    /**
     * Transfers money between two factions.
     *
     * @param fromFactionId source faction ID
     * @param toFactionId   target faction ID
     * @param amount        the amount to transfer
     * @param actorId       the player initiating the transfer
     * @param description   description for transaction log
     * @return the transaction result
     */
    @NotNull
    CompletableFuture<TransactionResult> transfer(
        @NotNull UUID fromFactionId,
        @NotNull UUID toFactionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    );

    // === Transaction History ===

    /**
     * Gets recent transactions for a faction.
     *
     * @param factionId the faction ID
     * @param limit     maximum number of transactions to return
     * @return list of transactions, most recent first
     */
    @NotNull
    List<Transaction> getTransactionHistory(@NotNull UUID factionId, int limit);

    // === Currency Formatting ===

    /**
     * Gets the currency name (singular).
     *
     * @return the currency name (e.g., "dollar", "coin")
     */
    @NotNull
    String getCurrencyName();

    /**
     * Gets the currency name (plural).
     *
     * @return the plural currency name (e.g., "dollars", "coins")
     */
    @NotNull
    String getCurrencyNamePlural();

    /**
     * Formats an amount as a currency string.
     *
     * @param amount the amount
     * @return formatted string (e.g., "$1,234.56")
     */
    @NotNull
    String formatCurrency(double amount);

    // === Status ===

    /**
     * Checks if the economy system is enabled.
     *
     * @return true if economy features are available
     */
    boolean isEnabled();
}
