package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the faction economy system.
 * <p>
 * Controls currency display and starting balances for factions.
 */
public class EconomyConfig extends ModuleConfig {

    private String currencyName = "dollar";
    private String currencyNamePlural = "dollars";
    private String currencySymbol = "$";
    private double startingBalance = 0.0;
    private boolean disbandRefundToLeader = true;

    // Default treasury limits for new factions
    private double defaultMaxWithdrawAmount = 0.0;
    private double defaultMaxWithdrawPerPeriod = 0.0;
    private double defaultMaxTransferAmount = 0.0;
    private double defaultMaxTransferPerPeriod = 0.0;
    private int defaultLimitPeriodHours = 24;

    // Fee configuration (percentage, 0 = no fee)
    private double depositFeePercent = 0.0;
    private double withdrawFeePercent = 0.0;
    private double transferFeePercent = 0.0;

    // Upkeep configuration
    private boolean upkeepEnabled = false;
    private double upkeepCostPerChunk = 10.0;
    private int upkeepIntervalHours = 24;
    private int upkeepGracePeriodHours = 48;
    private boolean upkeepAutoPayDefault = true;

    /**
     * Creates a new economy config.
     *
     * @param filePath path to config/economy.json
     */
    public EconomyConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "economy";
    }

    @Override
    protected void createDefaults() {
        enabled = true;
        currencyName = "dollar";
        currencyNamePlural = "dollars";
        currencySymbol = "$";
        startingBalance = 0.0;
        disbandRefundToLeader = true;
        defaultMaxWithdrawAmount = 0.0;
        defaultMaxWithdrawPerPeriod = 0.0;
        defaultMaxTransferAmount = 0.0;
        defaultMaxTransferPerPeriod = 0.0;
        defaultLimitPeriodHours = 24;
        depositFeePercent = 0.0;
        withdrawFeePercent = 0.0;
        transferFeePercent = 0.0;
        upkeepEnabled = false;
        upkeepCostPerChunk = 10.0;
        upkeepIntervalHours = 24;
        upkeepGracePeriodHours = 48;
        upkeepAutoPayDefault = true;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        currencyName = getString(root, "currencyName", currencyName);
        currencyNamePlural = getString(root, "currencyNamePlural", currencyNamePlural);
        currencySymbol = getString(root, "currencySymbol", currencySymbol);
        startingBalance = getDouble(root, "startingBalance", startingBalance);
        disbandRefundToLeader = getBool(root, "disbandRefundToLeader", disbandRefundToLeader);
        defaultMaxWithdrawAmount = getDouble(root, "defaultMaxWithdrawAmount", defaultMaxWithdrawAmount);
        defaultMaxWithdrawPerPeriod = getDouble(root, "defaultMaxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod);
        defaultMaxTransferAmount = getDouble(root, "defaultMaxTransferAmount", defaultMaxTransferAmount);
        defaultMaxTransferPerPeriod = getDouble(root, "defaultMaxTransferPerPeriod", defaultMaxTransferPerPeriod);
        defaultLimitPeriodHours = getInt(root, "defaultLimitPeriodHours", defaultLimitPeriodHours);
        depositFeePercent = getDouble(root, "depositFeePercent", depositFeePercent);
        withdrawFeePercent = getDouble(root, "withdrawFeePercent", withdrawFeePercent);
        transferFeePercent = getDouble(root, "transferFeePercent", transferFeePercent);
        upkeepEnabled = getBool(root, "upkeepEnabled", upkeepEnabled);
        upkeepCostPerChunk = getDouble(root, "upkeepCostPerChunk", upkeepCostPerChunk);
        upkeepIntervalHours = getInt(root, "upkeepIntervalHours", upkeepIntervalHours);
        upkeepGracePeriodHours = getInt(root, "upkeepGracePeriodHours", upkeepGracePeriodHours);
        upkeepAutoPayDefault = getBool(root, "upkeepAutoPayDefault", upkeepAutoPayDefault);
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("currencyName", currencyName);
        root.addProperty("currencyNamePlural", currencyNamePlural);
        root.addProperty("currencySymbol", currencySymbol);
        root.addProperty("startingBalance", startingBalance);
        root.addProperty("disbandRefundToLeader", disbandRefundToLeader);
        root.addProperty("defaultMaxWithdrawAmount", defaultMaxWithdrawAmount);
        root.addProperty("defaultMaxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod);
        root.addProperty("defaultMaxTransferAmount", defaultMaxTransferAmount);
        root.addProperty("defaultMaxTransferPerPeriod", defaultMaxTransferPerPeriod);
        root.addProperty("defaultLimitPeriodHours", defaultLimitPeriodHours);
        root.addProperty("depositFeePercent", depositFeePercent);
        root.addProperty("withdrawFeePercent", withdrawFeePercent);
        root.addProperty("transferFeePercent", transferFeePercent);
        root.addProperty("upkeepEnabled", upkeepEnabled);
        root.addProperty("upkeepCostPerChunk", upkeepCostPerChunk);
        root.addProperty("upkeepIntervalHours", upkeepIntervalHours);
        root.addProperty("upkeepGracePeriodHours", upkeepGracePeriodHours);
        root.addProperty("upkeepAutoPayDefault", upkeepAutoPayDefault);
    }

    // === Getters ===

    /**
     * Gets the singular currency name (e.g., "dollar").
     *
     * @return currency name
     */
    @NotNull
    public String getCurrencyName() {
        return currencyName;
    }

    /**
     * Gets the plural currency name (e.g., "dollars").
     *
     * @return plural currency name
     */
    @NotNull
    public String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    /**
     * Gets the currency symbol (e.g., "$").
     *
     * @return currency symbol
     */
    @NotNull
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Gets the starting balance for new factions.
     *
     * @return starting balance
     */
    public double getStartingBalance() {
        return startingBalance;
    }

    /**
     * Whether to refund the faction balance to the leader on disband.
     * If false, the balance is destroyed.
     *
     * @return true if refund to leader (default)
     */
    public boolean isDisbandRefundToLeader() {
        return disbandRefundToLeader;
    }

    public double getDefaultMaxWithdrawAmount() { return defaultMaxWithdrawAmount; }
    public double getDefaultMaxWithdrawPerPeriod() { return defaultMaxWithdrawPerPeriod; }
    public double getDefaultMaxTransferAmount() { return defaultMaxTransferAmount; }
    public double getDefaultMaxTransferPerPeriod() { return defaultMaxTransferPerPeriod; }
    public int getDefaultLimitPeriodHours() { return defaultLimitPeriodHours; }

    // Fee getters
    public double getDepositFeePercent() { return depositFeePercent; }
    public double getWithdrawFeePercent() { return withdrawFeePercent; }
    public double getTransferFeePercent() { return transferFeePercent; }

    // Upkeep getters
    public boolean isUpkeepEnabled() { return upkeepEnabled; }
    public double getUpkeepCostPerChunk() { return upkeepCostPerChunk; }
    public int getUpkeepIntervalHours() { return upkeepIntervalHours; }
    public int getUpkeepGracePeriodHours() { return upkeepGracePeriodHours; }
    public boolean isUpkeepAutoPayDefault() { return upkeepAutoPayDefault; }

    /**
     * Creates a TreasuryLimits instance from the server-configured defaults.
     *
     * @return default treasury limits from config
     */
    @NotNull
    public com.hyperfactions.data.FactionEconomy.TreasuryLimits getDefaultTreasuryLimits() {
        return new com.hyperfactions.data.FactionEconomy.TreasuryLimits(
            defaultMaxWithdrawAmount, defaultMaxWithdrawPerPeriod,
            defaultMaxTransferAmount, defaultMaxTransferPerPeriod,
            defaultLimitPeriodHours
        );
    }

    /**
     * Formats an amount with the currency symbol.
     *
     * @param amount the amount to format
     * @return formatted string (e.g., "$100.00")
     */
    @NotNull
    public String format(double amount) {
        return currencySymbol + String.format("%.2f", amount);
    }

    /**
     * Formats an amount with the currency name.
     *
     * @param amount the amount to format
     * @return formatted string (e.g., "100 dollars")
     */
    @NotNull
    public String formatWithName(double amount) {
        String name = amount == 1.0 ? currencyName : currencyNamePlural;
        return String.format("%.2f %s", amount, name);
    }

    // === Validation ===

    @Override
    @NotNull
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();

        // Starting balance must be >= 0
        startingBalance = validateMin(result, "startingBalance", startingBalance, 0.0, 0.0);

        // Currency names should not be empty
        if (currencyName.isBlank()) {
            result.addWarning(getConfigName(), "currencyName",
                    "Currency name should not be empty", currencyName, "dollar");
            currencyName = "dollar";
            needsSave = true;
        }
        if (currencyNamePlural.isBlank()) {
            result.addWarning(getConfigName(), "currencyNamePlural",
                    "Currency name plural should not be empty", currencyNamePlural, "dollars");
            currencyNamePlural = "dollars";
            needsSave = true;
        }

        // Fee percentages must be 0-100
        depositFeePercent = validateRange(result, "depositFeePercent", depositFeePercent, 0.0, 100.0, 0.0);
        withdrawFeePercent = validateRange(result, "withdrawFeePercent", withdrawFeePercent, 0.0, 100.0, 0.0);
        transferFeePercent = validateRange(result, "transferFeePercent", transferFeePercent, 0.0, 100.0, 0.0);

        // Upkeep settings
        upkeepCostPerChunk = validateMin(result, "upkeepCostPerChunk", upkeepCostPerChunk, 0.0, 10.0);
        upkeepIntervalHours = validateMin(result, "upkeepIntervalHours", upkeepIntervalHours, 1, 24);
        upkeepGracePeriodHours = validateMin(result, "upkeepGracePeriodHours", upkeepGracePeriodHours, 0, 48);

        return result;
    }
}
