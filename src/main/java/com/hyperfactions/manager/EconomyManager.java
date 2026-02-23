package com.hyperfactions.manager;

import com.hyperfactions.api.EconomyAPI;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionEconomy;
import com.hyperfactions.data.FactionEconomy.TreasuryLimits;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.integration.economy.VaultEconomyProvider;
import com.hyperfactions.storage.JsonEconomyStorage;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages faction treasury and economy operations.
 * Integrates with VaultUnlocked for player wallet transactions and
 * JsonEconomyStorage for persistent economy data.
 */
public class EconomyManager implements EconomyAPI {

    private final FactionManager factionManager;
    private final VaultEconomyProvider vaultProvider;
    private final JsonEconomyStorage storage;

    // Cache for economy data
    private final Map<UUID, FactionEconomy> economyCache = new ConcurrentHashMap<>();

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
            double startingBalance = ConfigManager.get().getEconomyStartingBalance();
            TreasuryLimits defaultLimits = ConfigManager.get().getDefaultTreasuryLimits();
            boolean autoPayDefault = ConfigManager.get().isUpkeepAutoPayDefault();
            FactionEconomy economy;
            if (startingBalance > 0) {
                economy = new FactionEconomy(startingBalance, new ArrayList<>(), defaultLimits, 0L, autoPayDefault);
            } else {
                economy = new FactionEconomy(0.0, new ArrayList<>(), defaultLimits, 0L, autoPayDefault);
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
    public String checkWithdrawLimits(@NotNull UUID factionId, double amount) {
        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null) return null;

        TreasuryLimits limits = economy.limits();

        // Per-transaction limit
        if (limits.maxWithdrawAmount() > 0 && amount > limits.maxWithdrawAmount()) {
            return String.format("Exceeds per-transaction limit of %s",
                    formatCurrency(limits.maxWithdrawAmount()));
        }

        // Period cumulative limit
        if (limits.maxWithdrawPerPeriod() > 0) {
            double recentTotal = sumRecentTransactions(economy, EconomyAPI.TransactionType.WITHDRAW,
                    limits.periodHours());
            if (recentTotal + amount > limits.maxWithdrawPerPeriod()) {
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
    public String checkTransferLimits(@NotNull UUID factionId, double amount) {
        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null) return null;

        TreasuryLimits limits = economy.limits();

        // Per-transaction limit
        if (limits.maxTransferAmount() > 0 && amount > limits.maxTransferAmount()) {
            return String.format("Exceeds per-transaction limit of %s",
                    formatCurrency(limits.maxTransferAmount()));
        }

        // Period cumulative limit
        if (limits.maxTransferPerPeriod() > 0) {
            double recentTotal = sumRecentTransactions(economy, EconomyAPI.TransactionType.TRANSFER_OUT,
                    limits.periodHours());
            if (recentTotal + amount > limits.maxTransferPerPeriod()) {
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
    private double sumRecentTransactions(@NotNull FactionEconomy economy,
                                         @NotNull EconomyAPI.TransactionType type,
                                         int periodHours) {
        long cutoff = System.currentTimeMillis() - (periodHours * 3600L * 1000L);
        double total = 0;
        for (EconomyAPI.Transaction tx : economy.transactionHistory()) {
            if (tx.timestamp() < cutoff) break; // History is sorted most-recent-first
            if (tx.type() == type) {
                total += tx.amount();
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
    public double calculateFee(double amount, @NotNull EconomyAPI.TransactionType type) {
        double percent = switch (type) {
            case DEPOSIT -> ConfigManager.get().getDepositFeePercent();
            case WITHDRAW -> ConfigManager.get().getWithdrawFeePercent();
            case TRANSFER_OUT, PLAYER_TRANSFER_OUT -> ConfigManager.get().getTransferFeePercent();
            default -> 0.0;
        };
        return amount * (percent / 100.0);
    }

    /**
     * Gets the net amount after fee deduction.
     *
     * @param amount the gross amount
     * @param type   the transaction type
     * @return the net amount (amount - fee)
     */
    public double getNetAfterFee(double amount, @NotNull EconomyAPI.TransactionType type) {
        return amount - calculateFee(amount, type);
    }

    // === EconomyAPI Implementation ===

    @Override
    public double getFactionBalance(@NotNull UUID factionId) {
        FactionEconomy economy = economyCache.get(factionId);
        return economy != null ? economy.balance() : 0.0;
    }

    @Override
    public boolean hasFunds(@NotNull UUID factionId, double amount) {
        if (amount <= 0) return true;
        FactionEconomy economy = economyCache.get(factionId);
        return economy != null && economy.hasFunds(amount);
    }

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> deposit(
        @NotNull UUID factionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
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

            double newBalance = economy.balance() + amount;
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

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> withdraw(
        @NotNull UUID factionId,
        double amount,
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
        double amount,
        @NotNull UUID actorId,
        @NotNull String description,
        @NotNull TransactionType transactionType
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
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

            double newBalance = economy.balance() - amount;
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

    @Override
    @NotNull
    public CompletableFuture<TransactionResult> transfer(
        @NotNull UUID fromFactionId,
        @NotNull UUID toFactionId,
        double amount,
        @Nullable UUID actorId,
        @NotNull String description
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount <= 0) {
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
            double fromNewBalance = fromEconomy.balance() - amount;
            double toNewBalance = toEconomy.balance() + amount;

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

    @Override
    @NotNull
    public List<Transaction> getTransactionHistory(@NotNull UUID factionId, int limit) {
        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null) {
            return Collections.emptyList();
        }
        return economy.getRecentTransactions(limit);
    }

    @Override
    @NotNull
    public String getCurrencyName() {
        return ConfigManager.get().getEconomyCurrencyName();
    }

    @Override
    @NotNull
    public String getCurrencyNamePlural() {
        return ConfigManager.get().getEconomyCurrencyNamePlural();
    }

    @Override
    @NotNull
    public String formatCurrency(double amount) {
        String symbol = ConfigManager.get().getEconomyCurrencySymbol();
        return String.format("%s%.2f", symbol, amount);
    }

    /**
     * Formats currency in compact form for stat cards (e.g. $1.2K, $3.5M, $1.1B).
     * Uses full format for amounts under 10,000.
     *
     * @param amount the amount to format
     * @return compact formatted string
     */
    @NotNull
    public String formatCurrencyCompact(double amount) {
        String symbol = ConfigManager.get().getEconomyCurrencySymbol();
        double abs = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";

        if (abs >= 1_000_000_000) {
            return String.format("%s%s%.1fB", sign, symbol, abs / 1_000_000_000);
        } else if (abs >= 1_000_000) {
            return String.format("%s%s%.1fM", sign, symbol, abs / 1_000_000);
        } else if (abs >= 10_000) {
            return String.format("%s%s%.1fK", sign, symbol, abs / 1_000);
        } else {
            return String.format("%s%s%.2f", sign, symbol, abs);
        }
    }

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
            @NotNull UUID factionId, double amount,
            @Nullable UUID adminId, @NotNull String description) {
        return CompletableFuture.supplyAsync(() -> {
            if (amount == 0) {
                return TransactionResult.INVALID_AMOUNT;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                return TransactionResult.FACTION_NOT_FOUND;
            }

            FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());

            // For negative adjustments, check if balance would go below 0
            double newBalance = economy.balance() + amount;
            if (newBalance < 0) {
                newBalance = 0;
            }

            Transaction transaction = new Transaction(
                    factionId,
                    adminId,
                    TransactionType.ADMIN_ADJUSTMENT,
                    Math.abs(amount),
                    newBalance,
                    System.currentTimeMillis(),
                    description
            );

            FactionEconomy updated = economy.withBalanceAndTransaction(newBalance, transaction);
            economyCache.put(factionId, updated);
            storage.save(factionId, updated);

            // Log to faction
            String logMessage = String.format("Admin %s: %s (balance: %s)",
                    amount >= 0 ? "added" : "deducted",
                    formatCurrency(Math.abs(amount)), formatCurrency(newBalance));
            Faction updatedFaction = faction.withLog(
                    FactionLog.create(FactionLog.LogType.ECONOMY, logMessage, adminId)
            );
            factionManager.updateFaction(updatedFaction);

            Logger.debugEconomy("Admin adjust %s: %s%s (new balance: %s)",
                    faction.name(), amount >= 0 ? "+" : "", formatCurrency(amount), formatCurrency(newBalance));

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
            @NotNull UUID factionId, double newBalance, @Nullable UUID adminId) {
        return CompletableFuture.supplyAsync(() -> {
            if (newBalance < 0) {
                return TransactionResult.INVALID_AMOUNT;
            }

            Faction faction = factionManager.getFaction(factionId);
            if (faction == null) {
                return TransactionResult.FACTION_NOT_FOUND;
            }

            FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());
            double oldBalance = economy.balance();

            Transaction transaction = new Transaction(
                    factionId,
                    adminId,
                    TransactionType.ADMIN_ADJUSTMENT,
                    Math.abs(newBalance - oldBalance),
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
    public double getServerTotalBalance() {
        return economyCache.values().stream().mapToDouble(FactionEconomy::balance).sum();
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
        double amount,
        @NotNull TransactionType type,
        @NotNull String description
    ) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        Faction faction = factionManager.getFaction(factionId);
        if (faction == null) {
            return TransactionResult.FACTION_NOT_FOUND;
        }

        FactionEconomy economy = economyCache.getOrDefault(factionId, FactionEconomy.empty());
        double newBalance = economy.balance() + amount;

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
        double amount,
        @NotNull TransactionType type,
        @NotNull String description
    ) {
        if (amount <= 0) {
            return TransactionResult.INVALID_AMOUNT;
        }

        FactionEconomy economy = economyCache.get(factionId);
        if (economy == null || !economy.hasFunds(amount)) {
            return TransactionResult.INSUFFICIENT_FUNDS;
        }

        double newBalance = economy.balance() - amount;
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
