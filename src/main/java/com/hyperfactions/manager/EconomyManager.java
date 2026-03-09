package com.hyperfactions.manager;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy.TreasuryLimits;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.storage.JsonEconomyStorage;
import com.hyperfactions.util.Logger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages faction treasury and economy operations.
 * Integrates with VaultUnlocked for player wallet transactions and
 * JsonEconomyStorage for persistent economy data.
 */
public class EconomyManager implements EconomyAPI {

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final FactionManager factionManager;

  private final VaultEconomyProvider vaultProvider;

  private final JsonEconomyStorage storage;

  // Cache for economy data
  private final Map<UUID, FactionEconomy> economyCache = new ConcurrentHashMap<>();

  /** Creates a new EconomyManager. */
  public EconomyManager(@NotNull FactionManager factionManager,
             @NotNull VaultEconomyProvider vaultProvider,
             @NotNull JsonEconomyStorage storage) {
    this.factionManager = factionManager;
    this.vaultProvider = vaultProvider;
    this.storage = storage;
  }

  /**
   * Initializes economy data for a new faction with starting balance from config.
   *
   * @param factionId the faction ID
   */
  public void initializeFaction(@NotNull UUID factionId) {
    if (!economyCache.containsKey(factionId)) {
      BigDecimal startingBalance = ConfigManager.get().getEconomyStartingBalance();
      TreasuryLimits defaultLimits = ConfigManager.get().getDefaultTreasuryLimits();
      boolean autoPayDefault = ConfigManager.get().isUpkeepAutoPayDefault();
      FactionEconomy economy;
      if (startingBalance.compareTo(BigDecimal.ZERO) > 0) {
        economy = new FactionEconomy(startingBalance, new ArrayList<>(), defaultLimits, 0L, autoPayDefault, 0L, 0);
      } else {
        economy = new FactionEconomy(BigDecimal.ZERO, new ArrayList<>(), defaultLimits, 0L, autoPayDefault, 0L, 0);
      }
      economyCache.put(factionId, economy);
      storage.save(factionId, economy);
      Logger.debugEconomy("Initialized economy for faction %s (starting balance: %s)",
          factionId, formatCurrency(startingBalance));
    }
  }

  /**
   * Cleans up economy data when a faction is disbanded.
   *
   * @param factionId the faction ID
   */
  public void removeFaction(@NotNull UUID factionId) {
    economyCache.remove(factionId);
    storage.delete(factionId);
  }

  /**
   * Gets the economy data for a faction.
   *
   * @param factionId the faction ID
   * @return the economy data, or null if faction not found
   */
  @Nullable
  public FactionEconomy getEconomy(@NotNull UUID factionId) {
    return economyCache.get(factionId);
  }

  /**
   * Gets the VaultUnlocked economy provider.
   *
   * @return the vault economy provider
   */
  @NotNull
  public VaultEconomyProvider getVaultProvider() {
    return vaultProvider;
  }

  /**
   * Loads economy data from storage.
   * Should be called after FactionManager loads.
   */
  public void loadAll() {
    Map<UUID, FactionEconomy> stored = storage.loadAll().join();
    economyCache.putAll(stored);

    // Initialize economy for factions that don't have stored economy data
    for (Faction faction : factionManager.getAllFactions()) {
      if (!economyCache.containsKey(faction.id())) {
        economyCache.put(faction.id(), FactionEconomy.empty());
      }
    }

    Logger.info("[Startup] Loaded economy data for %d factions", economyCache.size());
  }

  /**
   * Saves all economy data to storage.
   */
  public void saveAll() {
    storage.saveAll(new HashMap<>(economyCache)).join();
  }

  /**
   * Updates the full economy data for a faction.
   *
   * @param factionId the faction ID
   * @param economy   the updated economy data
   */
  public void updateEconomy(@NotNull UUID factionId, @NotNull FactionEconomy economy) {
    economyCache.put(factionId, economy);
    storage.save(factionId, economy);
  }

  /**
   * Updates treasury limits for a faction.
   *
   * @param factionId the faction ID
   * @param limits    the new limits
   */
  public void updateLimits(@NotNull UUID factionId, @NotNull TreasuryLimits limits) {
    FactionEconomy economy = economyCache.get(factionId);
    if (economy != null) {
      FactionEconomy updated = economy.withLimits(limits);
      economyCache.put(factionId, updated);
      storage.save(factionId, updated);
    }
  }

  // === Limit Enforcement ===

  /**
   * Checks if a withdrawal would exceed configured limits.
   *
   * @param factionId the faction ID
   * @param amount    the withdrawal amount
   * @return null if within limits, or a human-readable reason if exceeded
   */
  @Nullable
  public String checkWithdrawLimits(@NotNull UUID factionId, @NotNull BigDecimal amount) {
    FactionEconomy economy = economyCache.get(factionId);
    if (economy == null) {
      return null;
    }

    TreasuryLimits limits = economy.limits();

    // Per-transaction limit
    if (limits.maxWithdrawAmount().compareTo(BigDecimal.ZERO) > 0
        && amount.compareTo(limits.maxWithdrawAmount()) > 0) {
      return String.format("Exceeds per-transaction limit of %s",
          formatCurrency(limits.maxWithdrawAmount()));
    }

    // Period cumulative limit
    if (limits.maxWithdrawPerPeriod().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal recentTotal = sumRecentTransactions(economy, EconomyAPI.TransactionType.WITHDRAW,
          limits.periodHours());
      if (recentTotal.add(amount).compareTo(limits.maxWithdrawPerPeriod()) > 0) {
        return String.format("Exceeds %d-hour withdrawal limit of %s (used: %s)",
            limits.periodHours(), formatCurrency(limits.maxWithdrawPerPeriod()),
            formatCurrency(recentTotal));
      }
    }

    return null;
  }

  /**
   * Checks if a transfer would exceed configured limits.
   *
   * @param factionId the faction ID
   * @param amount    the transfer amount
   * @return null if within limits, or a human-readable reason if exceeded
   */
  @Nullable
  public String checkTransferLimits(@NotNull UUID factionId, @NotNull BigDecimal amount) {
    FactionEconomy economy = economyCache.get(factionId);
    if (economy == null) {
      return null;
    }

    TreasuryLimits limits = economy.limits();

    // Per-transaction limit
    if (limits.maxTransferAmount().compareTo(BigDecimal.ZERO) > 0
        && amount.compareTo(limits.maxTransferAmount()) > 0) {
      return String.format("Exceeds per-transaction limit of %s",
          formatCurrency(limits.maxTransferAmount()));
    }

    // Period cumulative limit
    if (limits.maxTransferPerPeriod().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal recentTotal = sumRecentTransactions(economy, EconomyAPI.TransactionType.TRANSFER_OUT,
          limits.periodHours());
      if (recentTotal.add(amount).compareTo(limits.maxTransferPerPeriod()) > 0) {
        return String.format("Exceeds %d-hour transfer limit of %s (used: %s)",
            limits.periodHours(), formatCurrency(limits.maxTransferPerPeriod()),
            formatCurrency(recentTotal));
      }
    }

    return null;
  }

  /**
   * Sums transaction amounts of a given type within a time window.
   */
  @NotNull
  private BigDecimal sumRecentTransactions(@NotNull FactionEconomy economy,
                       @NotNull EconomyAPI.TransactionType type,
                       int periodHours) {
    long cutoff = System.currentTimeMillis() - (periodHours * 3600L * 1000L);
    BigDecimal total = BigDecimal.ZERO;
    for (EconomyAPI.Transaction tx : economy.transactionHistory()) {
      if (tx.timestamp() < cutoff) { // History is sorted most-recent-first
        break;
      }
      if (tx.type() == type) {
        total = total.add(tx.amount());
      }
    }
    return total;
  }

  // === Fee Calculation ===

  /**
   * Calculates the fee amount for a given transaction.
   *
   * @param amount the transaction amount
   * @param type   the transaction type
   * @return the fee amount (0 if no fee configured)
   */
  @NotNull
  public BigDecimal calculateFee(@NotNull BigDecimal amount, @NotNull EconomyAPI.TransactionType type) {
    BigDecimal percent = switch (type) {
      case DEPOSIT -> ConfigManager.get().getDepositFeePercent();
      case WITHDRAW -> ConfigManager.get().getWithdrawFeePercent();
      case TRANSFER_OUT, PLAYER_TRANSFER_OUT -> ConfigManager.get().getTransferFeePercent();
      default -> BigDecimal.ZERO;
    };
    return amount.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
  }

  /**
   * Gets the net amount after fee deduction.
   *
   * @param amount the gross amount
   * @param type   the transaction type
   * @return the net amount (amount - fee)
   */
  @NotNull
  public BigDecimal getNetAfterFee(@NotNull BigDecimal amount, @NotNull EconomyAPI.TransactionType type) {
    return amount.subtract(calculateFee(amount, type));
  }

  // === EconomyAPI Implementation ===

  /** Returns the faction balance. */
  @Override
  @NotNull
  public BigDecimal getFactionBalance(@NotNull UUID factionId) {
    FactionEconomy economy = economyCache.get(factionId);
    return economy != null ? economy.balance() : BigDecimal.ZERO;
  }

  /** Checks if funds. */
  @Override
  public boolean hasFunds(@NotNull UUID factionId, @NotNull BigDecimal amount) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return true;
    }
    FactionEconomy economy = economyCache.get(factionId);
    return economy != null && economy.hasFunds(amount);
  }

  /** Deposit. */
  @Override
  @NotNull
  public CompletableFuture<TransactionResult> deposit(
    @NotNull UUID factionId,
    @NotNull BigDecimal amount,
    @Nullable UUID actorId,
    @NotNull String description
  ) {
    return CompletableFuture.supplyAsync(() -> {
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        return TransactionResult.INVALID_AMOUNT;
      }

      Faction faction = factionManager.getFaction(factionId);
      if (faction == null) {
        return TransactionResult.FACTION_NOT_FOUND;
      }

      FactionEconomy economy = economyCache.get(factionId);
      if (economy == null) {
        economy = FactionEconomy.empty();
      }

      BigDecimal newBalance = economy.balance().add(amount);
      Transaction transaction = new Transaction(
        factionId,
        actorId,
        TransactionType.DEPOSIT,
        amount,
        newBalance,
        System.currentTimeMillis(),
        description
      );

      FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
      economyCache.put(factionId, updated);
      storage.save(factionId, updated);

      // Log to faction
      String logMessage = String.format("Deposit: %s (+%s)",
        formatCurrency(newBalance), formatCurrency(amount));
      Faction updatedFaction = faction.withLog(
        FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, actorId)
      );
      factionManager.updateFaction(updatedFaction);

      Logger.debugEconomy("Deposit to %s: %s (new balance: %s)",
        faction.name(), formatCurrency(amount), formatCurrency(newBalance));

      return TransactionResult.SUCCESS;
    });
  }

  /** Withdraw. */
  @Override
  @NotNull
  public CompletableFuture<TransactionResult> withdraw(
    @NotNull UUID factionId,
    @NotNull BigDecimal amount,
    @NotNull UUID actorId,
    @NotNull String description
  ) {
    return withdraw(factionId, amount, actorId, description, TransactionType.WITHDRAW);
  }

  /**
   * Withdraw with explicit transaction type (e.g., TRANSFER_OUT, PLAYER_TRANSFER_OUT).
   */
  public CompletableFuture<TransactionResult> withdraw(
    @NotNull UUID factionId,
    @NotNull BigDecimal amount,
    @NotNull UUID actorId,
    @NotNull String description,
    @NotNull TransactionType transactionType
  ) {
    return CompletableFuture.supplyAsync(() -> {
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        return TransactionResult.INVALID_AMOUNT;
      }

      Faction faction = factionManager.getFaction(factionId);
      if (faction == null) {
        return TransactionResult.FACTION_NOT_FOUND;
      }

      FactionEconomy economy = economyCache.get(factionId);
      if (economy == null || !economy.hasFunds(amount)) {
        return TransactionResult.INSUFFICIENT_FUNDS;
      }

      // Check limits (use transfer limits for transfer types)
      boolean isTransfer = transactionType == TransactionType.TRANSFER_OUT
          || transactionType == TransactionType.PLAYER_TRANSFER_OUT;
      String limitReason = isTransfer
          ? checkTransferLimits(factionId, amount)
          : checkWithdrawLimits(factionId, amount);
      if (limitReason != null) {
        return TransactionResult.LIMIT_EXCEEDED;
      }

      BigDecimal newBalance = economy.balance().subtract(amount);
      Transaction transaction = new Transaction(
        factionId,
        actorId,
        transactionType,
        amount,
        newBalance,
        System.currentTimeMillis(),
        description
      );

      FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
      economyCache.put(factionId, updated);
      storage.save(factionId, updated);

      // Log to faction
      String logMessage = String.format("Withdrawal: %s (-%s)",
        formatCurrency(newBalance), formatCurrency(amount));
      Faction updatedFaction = faction.withLog(
        FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, actorId)
      );
      factionManager.updateFaction(updatedFaction);

      Logger.debugEconomy("Withdrawal from %s: %s (new balance: %s)",
        faction.name(), formatCurrency(amount), formatCurrency(newBalance));

      return TransactionResult.SUCCESS;
    });
  }

  /** Transfer. */
  @Override
  @NotNull
  public CompletableFuture<TransactionResult> transfer(
    @NotNull UUID fromFactionId,
    @NotNull UUID toFactionId,
    @NotNull BigDecimal amount,
    @Nullable UUID actorId,
    @NotNull String description
  ) {
    return CompletableFuture.supplyAsync(() -> {
      if (amount.compareTo(BigDecimal.ZERO) <= 0) {
        return TransactionResult.INVALID_AMOUNT;
      }

      Faction fromFaction = factionManager.getFaction(fromFactionId);
      Faction toFaction = factionManager.getFaction(toFactionId);

      if (fromFaction == null || toFaction == null) {
        return TransactionResult.FACTION_NOT_FOUND;
      }

      FactionEconomy fromEconomy = economyCache.get(fromFactionId);
      if (fromEconomy == null || !fromEconomy.hasFunds(amount)) {
        return TransactionResult.INSUFFICIENT_FUNDS;
      }

      // Check limits
      String limitReason = checkTransferLimits(fromFactionId, amount);
      if (limitReason != null) {
        return TransactionResult.LIMIT_EXCEEDED;
      }

      FactionEconomy toEconomy = economyCache.get(toFactionId);
      if (toEconomy == null) {
        toEconomy = FactionEconomy.empty();
      }

      // Perform transfer
      BigDecimal fromNewBalance = fromEconomy.balance().subtract(amount);
      BigDecimal toNewBalance = toEconomy.balance().add(amount);

      Transaction fromTransaction = new Transaction(
        fromFactionId,
        actorId,
        TransactionType.TRANSFER_OUT,
        amount,
        fromNewBalance,
        System.currentTimeMillis(),
        "Transfer to " + toFaction.name() + ": " + description
      );

      Transaction toTransaction = new Transaction(
        toFactionId,
        actorId,
        TransactionType.TRANSFER_IN,
        amount,
        toNewBalance,
        System.currentTimeMillis(),
        "Transfer from " + fromFaction.name() + ": " + description
      );

      // Update both economies
      FactionEconomy updatedFrom = fromEconomy.withBalanceAndTransaction(fromNewBalance, fromTransaction);
      FactionEconomy updatedTo = toEconomy.withBalanceAndTransaction(toNewBalance, toTransaction);
      economyCache.put(fromFactionId, updatedFrom);
      economyCache.put(toFactionId, updatedTo);
      storage.save(fromFactionId, updatedFrom);
      storage.save(toFactionId, updatedTo);

      Logger.debugEconomy("Transfer from %s to %s: %s",
        fromFaction.name(), toFaction.name(), formatCurrency(amount));

      return TransactionResult.SUCCESS;
    });
  }

  /** Returns the transaction history. */
  @Override
  @NotNull
  public List<Transaction> getTransactionHistory(@NotNull UUID factionId, int limit) {
    FactionEconomy economy = economyCache.get(factionId);
    if (economy == null) {
      return Collections.emptyList();
    }
    return economy.getRecentTransactions(limit);
  }

  /** Returns the currency name. */
  @Override
  @NotNull
  public String getCurrencyName() {
    return ConfigManager.get().getEconomyCurrencyName();
  }

  /** Returns the currency name plural. */
  @Override
  @NotNull
  public String getCurrencyNamePlural() {
    return ConfigManager.get().getEconomyCurrencyNamePlural();
  }

  /** Formats currency. */
  @Override
  @NotNull
  public String formatCurrency(@NotNull BigDecimal amount) {
    String symbol = ConfigManager.get().getEconomyCurrencySymbol();
    return symbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  /**
   * Formats currency in compact form for stat cards (e.g. $1.2K, $3.5M, $1.1B).
   * Uses full format for amounts under 10,000.
   *
   * @param amount the amount to format
   * @return compact formatted string
   */
  @NotNull
  public String formatCurrencyCompact(@NotNull BigDecimal amount) {
    String symbol = ConfigManager.get().getEconomyCurrencySymbol();
    BigDecimal abs = amount.abs();
    String sign = amount.compareTo(BigDecimal.ZERO) < 0 ? "-" : "";

    BigDecimal billion = new BigDecimal("1000000000");
    BigDecimal million = new BigDecimal("1000000");
    BigDecimal tenThousand = new BigDecimal("10000");
    BigDecimal thousand = new BigDecimal("1000");

    if (abs.compareTo(billion) >= 0) {
      return String.format("%s%s%.1fB", sign, symbol, abs.divide(billion, 1, RoundingMode.HALF_UP).doubleValue());
    } else if (abs.compareTo(million) >= 0) {
      return String.format("%s%s%.1fM", sign, symbol, abs.divide(million, 1, RoundingMode.HALF_UP).doubleValue());
    } else if (abs.compareTo(tenThousand) >= 0) {
      return String.format("%s%s%.1fK", sign, symbol, abs.divide(thousand, 1, RoundingMode.HALF_UP).doubleValue());
    } else {
      return sign + symbol + abs.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
  }

  /** Checks if enabled. */
  @Override
  public boolean isEnabled() {
    return true; // If EconomyManager exists, treasury is enabled
  }

  // === Admin Methods ===

  /**
   * Admin balance adjustment — bypasses limits, records as ADMIN_ADJUSTMENT.
   * Amount can be positive (add) or negative (deduct).
   *
   * @param factionId   the faction ID
   * @param amount      positive to add, negative to deduct
   * @param adminId     the admin who performed the action (null for console)
   * @param description description of the adjustment
   * @return the result
   */
  @NotNull
  public CompletableFuture<TransactionResult> adminAdjust(
      @NotNull UUID factionId, @NotNull BigDecimal amount,
      @Nullable UUID adminId, @NotNull String description) {
    return CompletableFuture.supplyAsync(() -> {
      if (amount.compareTo(BigDecimal.ZERO) == 0) {
        return TransactionResult.INVALID_AMOUNT;
      }

      Faction faction = factionManager.getFaction(factionId);
      if (faction == null) {
        return TransactionResult.FACTION_NOT_FOUND;
      }

      FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());

      // For negative adjustments, check if balance would go below 0
      BigDecimal newBalance = economy.balance().add(amount);
      if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
        newBalance = BigDecimal.ZERO;
      }

      Transaction transaction = new Transaction(
          factionId,
          adminId,
          TransactionType.ADMIN_ADJUSTMENT,
          amount.abs(),
          newBalance,
          System.currentTimeMillis(),
          description
      );

      FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
      economyCache.put(factionId, updated);
      storage.save(factionId, updated);

      // Log to faction
      String logMessage = String.format("Admin %s: %s (balance: %s)",
          amount.compareTo(BigDecimal.ZERO) >= 0 ? "added" : "deducted",
          formatCurrency(amount.abs()), formatCurrency(newBalance));
      Faction updatedFaction = faction.withLog(
          FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, adminId)
      );
      factionManager.updateFaction(updatedFaction);

      Logger.debugEconomy("Admin adjust %s: %s%s (new balance: %s)",
          faction.name(), amount.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
          formatCurrency(amount), formatCurrency(newBalance));

      return TransactionResult.SUCCESS;
    });
  }

  /**
   * Admin set balance — replaces balance entirely, records as ADMIN_ADJUSTMENT.
   *
   * @param factionId  the faction ID
   * @param newBalance the exact balance to set
   * @param adminId    the admin who performed the action (null for console)
   * @return the result
   */
  @NotNull
  public CompletableFuture<TransactionResult> setBalance(
      @NotNull UUID factionId, @NotNull BigDecimal newBalance, @Nullable UUID adminId) {
    return CompletableFuture.supplyAsync(() -> {
      if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
        return TransactionResult.INVALID_AMOUNT;
      }

      Faction faction = factionManager.getFaction(factionId);
      if (faction == null) {
        return TransactionResult.FACTION_NOT_FOUND;
      }

      FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());
      BigDecimal oldBalance = economy.balance();

      Transaction transaction = new Transaction(
          factionId,
          adminId,
          TransactionType.ADMIN_ADJUSTMENT,
          newBalance.subtract(oldBalance).abs(),
          newBalance,
          System.currentTimeMillis(),
          String.format("Admin set balance: %s -> %s",
              formatCurrency(oldBalance), formatCurrency(newBalance))
      );

      FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
      economyCache.put(factionId, updated);
      storage.save(factionId, updated);

      // Log to faction
      String logMessage = String.format("Admin set balance to %s (was %s)",
          formatCurrency(newBalance), formatCurrency(oldBalance));
      Faction updatedFaction = faction.withLog(
          FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, adminId)
      );
      factionManager.updateFaction(updatedFaction);

      Logger.debugEconomy("Admin set balance %s: %s -> %s",
          faction.name(), formatCurrency(oldBalance), formatCurrency(newBalance));

      return TransactionResult.SUCCESS;
    });
  }

  /**
   * Gets the total balance across all factions.
   *
   * @return total server economy balance
   */
  @NotNull
  public BigDecimal getServerTotalBalance() {
    return economyCache.values().stream()
        .map(FactionEconomy::balance)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Gets the number of factions with economy data.
   *
   * @return count of factions with economy
   */
  public int getFactionEconomyCount() {
    return economyCache.size();
  }

  /**
   * Gets the economy cache (read-only view for admin pages).
   *
   * @return unmodifiable map of faction economies
   */
  @NotNull
  public Map<UUID, FactionEconomy> getAllEconomies() {
    return Collections.unmodifiableMap(economyCache);
  }

  /**
   * Performs a system deposit (no actor, for rewards/adjustments).
   *
   * @param factionId   the faction ID
   * @param amount      the amount
   * @param type        the transaction type
   * @param description description
   * @return the result
   */
  @NotNull
  public TransactionResult systemDeposit(
    @NotNull UUID factionId,
    @NotNull BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull String description
  ) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return TransactionResult.INVALID_AMOUNT;
    }

    Faction faction = factionManager.getFaction(factionId);
    if (faction == null) {
      return TransactionResult.FACTION_NOT_FOUND;
    }

    FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());
    BigDecimal newBalance = economy.balance().add(amount);

    Transaction transaction = new Transaction(
      factionId,
      null, // System
      type,
      amount,
      newBalance,
      System.currentTimeMillis(),
      description
    );

    FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
    economyCache.put(factionId, updated);
    storage.save(factionId, updated);
    return TransactionResult.SUCCESS;
  }

  /**
   * Performs a system withdrawal (no actor, for upkeep/costs).
   *
   * @param factionId   the faction ID
   * @param amount      the amount
   * @param type        the transaction type
   * @param description description
   * @return the result
   */
  @NotNull
  public TransactionResult systemWithdraw(
    @NotNull UUID factionId,
    @NotNull BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull String description
  ) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      return TransactionResult.INVALID_AMOUNT;
    }

    FactionEconomy economy = economyCache.get(factionId);
    if (economy == null || !economy.hasFunds(amount)) {
      return TransactionResult.INSUFFICIENT_FUNDS;
    }

    BigDecimal newBalance = economy.balance().subtract(amount);
    Transaction transaction = new Transaction(
      factionId,
      null, // System
      type,
      amount,
      newBalance,
      System.currentTimeMillis(),
      description
    );

    FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
    economyCache.put(factionId, updated);
    storage.save(factionId, updated);
    return TransactionResult.SUCCESS;
  }
}
