package com.hyperfactions.data;

import com.hyperfactions.api.EconomyAPI;
import java.math.BigDecimal;
import java.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a faction's economy data.
 */
public record FactionEconomy(
  @NotNull BigDecimal balance,
  @NotNull List<EconomyAPI.Transaction> transactionHistory,
  @NotNull TreasuryLimits limits,
  long lastUpkeepTimestamp,
  boolean upkeepAutoPay,
  long upkeepGraceStartTimestamp,
  int consecutiveMissedPayments
) {
  /**
   * Maximum number of transactions to keep in history.
   */
  public static final int MAX_HISTORY = 50;

  /**
   * Configurable limits for treasury withdrawals and transfers.
   *
   * @param maxWithdrawAmount   max per-transaction withdrawal (0 = unlimited)
   * @param maxWithdrawPerPeriod max total withdrawals in timeframe (0 = unlimited)
   * @param maxTransferAmount   max per-transaction transfer (0 = unlimited)
   * @param maxTransferPerPeriod max total transfers in timeframe (0 = unlimited)
   * @param periodHours         timeframe duration in hours (default: 24)
   */
  public record TreasuryLimits(
    @NotNull BigDecimal maxWithdrawAmount,
    @NotNull BigDecimal maxWithdrawPerPeriod,
    @NotNull BigDecimal maxTransferAmount,
    @NotNull BigDecimal maxTransferPerPeriod,
    int periodHours
  ) {
    /** Defaults. */
    public static TreasuryLimits defaults() {
      return new TreasuryLimits(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 24);
    }
  }

  /**
   * Creates an empty economy with zero balance.
   *
   * @return a new empty FactionEconomy
   */
  public static FactionEconomy empty() {
    return new FactionEconomy(BigDecimal.ZERO, new ArrayList<>(), TreasuryLimits.defaults(), 0L, true, 0L, 0);
  }

  /**
   * Creates an economy with a starting balance.
   *
   * @param startingBalance the starting balance
   * @return a new FactionEconomy
   */
  public static FactionEconomy withStartingBalance(@NotNull BigDecimal startingBalance) {
    return new FactionEconomy(startingBalance, new ArrayList<>(), TreasuryLimits.defaults(), 0L, true, 0L, 0);
  }

  /**
   * Defensive copy constructor.
   */
  public FactionEconomy {
    // Defensive copy
    transactionHistory = new ArrayList<>(transactionHistory);
  }

  /**
   * Returns a copy with updated balance.
   *
   * @param newBalance the new balance
   * @return a new FactionEconomy with the updated balance
   */
  public FactionEconomy withBalance(@NotNull BigDecimal newBalance) {
    return new FactionEconomy(newBalance, transactionHistory, limits, lastUpkeepTimestamp, upkeepAutoPay,
        upkeepGraceStartTimestamp, consecutiveMissedPayments);
  }

  /**
   * Returns a copy with added transaction.
   *
   * @param transaction the transaction to add
   * @return a new FactionEconomy with the transaction added
   */
  public FactionEconomy withTransaction(@NotNull EconomyAPI.Transaction transaction) {
    List<EconomyAPI.Transaction> newHistory = new ArrayList<>(transactionHistory);
    newHistory.add(0, transaction); // Add to front (most recent first)

    // Trim if exceeds max history
    while (newHistory.size() > MAX_HISTORY) {
      newHistory.remove(newHistory.size() - 1);
    }

    return new FactionEconomy(balance, newHistory, limits, lastUpkeepTimestamp, upkeepAutoPay,
        upkeepGraceStartTimestamp, consecutiveMissedPayments);
  }

  /**
   * Returns a copy with updated balance and added transaction.
   *
   * @param newBalance  the new balance
   * @param transaction the transaction to add
   * @return a new FactionEconomy
   */
  public FactionEconomy withBalanceAndTransaction(@NotNull BigDecimal newBalance,
                          @NotNull EconomyAPI.Transaction transaction) {
    return withBalance(newBalance).withTransaction(transaction);
  }

  /**
   * Returns a copy with updated treasury limits.
   *
   * @param newLimits the new limits
   * @return a new FactionEconomy with updated limits
   */
  public FactionEconomy withLimits(@NotNull TreasuryLimits newLimits) {
    return new FactionEconomy(balance, transactionHistory, newLimits, lastUpkeepTimestamp, upkeepAutoPay,
        upkeepGraceStartTimestamp, consecutiveMissedPayments);
  }

  /** With Last Upkeep Timestamp. */
  public FactionEconomy withLastUpkeepTimestamp(long timestamp) {
    return new FactionEconomy(balance, transactionHistory, limits, timestamp, upkeepAutoPay,
        upkeepGraceStartTimestamp, consecutiveMissedPayments);
  }

  /** With Upkeep Auto Pay. */
  public FactionEconomy withUpkeepAutoPay(boolean autoPay) {
    return new FactionEconomy(balance, transactionHistory, limits, lastUpkeepTimestamp, autoPay,
        upkeepGraceStartTimestamp, consecutiveMissedPayments);
  }

  /** With Upkeep Grace Start Timestamp. */
  public FactionEconomy withUpkeepGraceStartTimestamp(long timestamp) {
    return new FactionEconomy(balance, transactionHistory, limits, lastUpkeepTimestamp, upkeepAutoPay,
        timestamp, consecutiveMissedPayments);
  }

  /** With Consecutive Missed Payments. */
  public FactionEconomy withConsecutiveMissedPayments(int count) {
    return new FactionEconomy(balance, transactionHistory, limits, lastUpkeepTimestamp, upkeepAutoPay,
        upkeepGraceStartTimestamp, count);
  }

  /** Resets grace state (grace timestamp and missed payments to 0). */
  public FactionEconomy withGraceReset() {
    return new FactionEconomy(balance, transactionHistory, limits, lastUpkeepTimestamp, upkeepAutoPay, 0L, 0);
  }

  /**
   * Checks if there are sufficient funds for a withdrawal.
   *
   * @param amount the amount to check
   * @return true if balance >= amount
   */
  public boolean hasFunds(@NotNull BigDecimal amount) {
    return balance.compareTo(amount) >= 0;
  }

  /**
   * Gets the recent transaction history.
   *
   * @param limit maximum number of transactions to return
   * @return list of transactions (most recent first)
   */
  @NotNull
  public List<EconomyAPI.Transaction> getRecentTransactions(int limit) {
    if (limit <= 0) {
      return Collections.emptyList();
    }
    if (limit >= transactionHistory.size()) {
      return Collections.unmodifiableList(transactionHistory);
    }
    return Collections.unmodifiableList(transactionHistory.subList(0, limit));
  }

  /**
   * Gets an unmodifiable view of the transaction history.
   *
   * @return unmodifiable list of transactions
   */
  @NotNull
  public List<EconomyAPI.Transaction> transactionHistory() {
    return Collections.unmodifiableList(transactionHistory);
  }
}
