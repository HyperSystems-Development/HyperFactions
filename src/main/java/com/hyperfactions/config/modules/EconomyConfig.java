package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the faction economy system.
 *
 * <p>
 * Controls currency display and starting balances for factions.
 */
public class EconomyConfig extends ModuleConfig {

  private String currencyName = "dollar";

  private String currencyNamePlural = "dollars";

  private String currencySymbol = "$";

  private BigDecimal startingBalance = BigDecimal.ZERO;

  private boolean disbandRefundToLeader = true;

  // Default treasury limits for new factions
  private BigDecimal defaultMaxWithdrawAmount = BigDecimal.ZERO;

  private BigDecimal defaultMaxWithdrawPerPeriod = BigDecimal.ZERO;

  private BigDecimal defaultMaxTransferAmount = BigDecimal.ZERO;

  private BigDecimal defaultMaxTransferPerPeriod = BigDecimal.ZERO;

  private int defaultLimitPeriodHours = 24;

  // Fee configuration (percentage, 0 = no fee)
  private BigDecimal depositFeePercent = BigDecimal.ZERO;

  private BigDecimal withdrawFeePercent = BigDecimal.ZERO;

  private BigDecimal transferFeePercent = BigDecimal.ZERO;

  // Upkeep configuration
  private boolean upkeepEnabled = false;

  private BigDecimal upkeepCostPerChunk = new BigDecimal("10.0");

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

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "economy";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    currencyName = "dollar";
    currencyNamePlural = "dollars";
    currencySymbol = "$";
    startingBalance = BigDecimal.ZERO;
    disbandRefundToLeader = true;
    defaultMaxWithdrawAmount = BigDecimal.ZERO;
    defaultMaxWithdrawPerPeriod = BigDecimal.ZERO;
    defaultMaxTransferAmount = BigDecimal.ZERO;
    defaultMaxTransferPerPeriod = BigDecimal.ZERO;
    defaultLimitPeriodHours = 24;
    depositFeePercent = BigDecimal.ZERO;
    withdrawFeePercent = BigDecimal.ZERO;
    transferFeePercent = BigDecimal.ZERO;
    upkeepEnabled = false;
    upkeepCostPerChunk = new BigDecimal("10.0");
    upkeepIntervalHours = 24;
    upkeepGracePeriodHours = 48;
    upkeepAutoPayDefault = true;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    currencyName = getString(root, "currencyName", currencyName);
    currencyNamePlural = getString(root, "currencyNamePlural", currencyNamePlural);
    currencySymbol = getString(root, "currencySymbol", currencySymbol);
    startingBalance = getBigDecimal(root, "startingBalance", startingBalance);
    disbandRefundToLeader = getBool(root, "disbandRefundToLeader", disbandRefundToLeader);
    defaultMaxWithdrawAmount = getBigDecimal(root, "defaultMaxWithdrawAmount", defaultMaxWithdrawAmount);
    defaultMaxWithdrawPerPeriod = getBigDecimal(root, "defaultMaxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod);
    defaultMaxTransferAmount = getBigDecimal(root, "defaultMaxTransferAmount", defaultMaxTransferAmount);
    defaultMaxTransferPerPeriod = getBigDecimal(root, "defaultMaxTransferPerPeriod", defaultMaxTransferPerPeriod);
    defaultLimitPeriodHours = getInt(root, "defaultLimitPeriodHours", defaultLimitPeriodHours);
    depositFeePercent = getBigDecimal(root, "depositFeePercent", depositFeePercent);
    withdrawFeePercent = getBigDecimal(root, "withdrawFeePercent", withdrawFeePercent);
    transferFeePercent = getBigDecimal(root, "transferFeePercent", transferFeePercent);
    upkeepEnabled = getBool(root, "upkeepEnabled", upkeepEnabled);
    upkeepCostPerChunk = getBigDecimal(root, "upkeepCostPerChunk", upkeepCostPerChunk);
    upkeepIntervalHours = getInt(root, "upkeepIntervalHours", upkeepIntervalHours);
    upkeepGracePeriodHours = getInt(root, "upkeepGracePeriodHours", upkeepGracePeriodHours);
    upkeepAutoPayDefault = getBool(root, "upkeepAutoPayDefault", upkeepAutoPayDefault);
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("currencyName", currencyName);
    root.addProperty("currencyNamePlural", currencyNamePlural);
    root.addProperty("currencySymbol", currencySymbol);
    root.addProperty("startingBalance", startingBalance.toPlainString());
    root.addProperty("disbandRefundToLeader", disbandRefundToLeader);
    root.addProperty("defaultMaxWithdrawAmount", defaultMaxWithdrawAmount.toPlainString());
    root.addProperty("defaultMaxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod.toPlainString());
    root.addProperty("defaultMaxTransferAmount", defaultMaxTransferAmount.toPlainString());
    root.addProperty("defaultMaxTransferPerPeriod", defaultMaxTransferPerPeriod.toPlainString());
    root.addProperty("defaultLimitPeriodHours", defaultLimitPeriodHours);
    root.addProperty("depositFeePercent", depositFeePercent.toPlainString());
    root.addProperty("withdrawFeePercent", withdrawFeePercent.toPlainString());
    root.addProperty("transferFeePercent", transferFeePercent.toPlainString());
    root.addProperty("upkeepEnabled", upkeepEnabled);
    root.addProperty("upkeepCostPerChunk", upkeepCostPerChunk.toPlainString());
    root.addProperty("upkeepIntervalHours", upkeepIntervalHours);
    root.addProperty("upkeepGracePeriodHours", upkeepGracePeriodHours);
    root.addProperty("upkeepAutoPayDefault", upkeepAutoPayDefault);
  }

  // === BigDecimal JSON Helper ===

  /**
   * Reads a BigDecimal from a JsonObject, handling both old double format and new string format.
   *
   * @param obj        the JSON object
   * @param key        the key to read
   * @param defaultVal default value if key is missing
   * @return the BigDecimal value
   */
  @NotNull
  private BigDecimal getBigDecimal(@NotNull JsonObject obj, @NotNull String key, @NotNull BigDecimal defaultVal) {
    if (!obj.has(key) || obj.get(key).isJsonNull()) {
      return defaultVal;
    }
    try {
      return obj.get(key).getAsBigDecimal();
    } catch (Exception e) {
      return defaultVal;
    }
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
  @NotNull
  public BigDecimal getStartingBalance() {
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

  @NotNull public BigDecimal getDefaultMaxWithdrawAmount() {
    return defaultMaxWithdrawAmount;
  }

  /** Returns the default max withdraw per period. */
  @NotNull public BigDecimal getDefaultMaxWithdrawPerPeriod() {
    return defaultMaxWithdrawPerPeriod;
  }

  /** Returns the default max transfer amount. */
  @NotNull public BigDecimal getDefaultMaxTransferAmount() {
    return defaultMaxTransferAmount;
  }

  @NotNull public BigDecimal getDefaultMaxTransferPerPeriod() {
    return defaultMaxTransferPerPeriod;
  }

  public int getDefaultLimitPeriodHours() {
    return defaultLimitPeriodHours;
  }

  // Fee getters
  /** Returns the deposit fee percent. */
  @NotNull public BigDecimal getDepositFeePercent() {
    return depositFeePercent;
  }

  /** Returns the withdraw fee percent. */
  @NotNull public BigDecimal getWithdrawFeePercent() {
    return withdrawFeePercent;
  }

  /** Returns the transfer fee percent. */
  @NotNull public BigDecimal getTransferFeePercent() {
    return transferFeePercent;
  }

  // Upkeep getters
  public boolean isUpkeepEnabled() {
    return upkeepEnabled;
  }

  /** Returns the upkeep cost per chunk. */
  @NotNull public BigDecimal getUpkeepCostPerChunk() {
    return upkeepCostPerChunk;
  }

  /** Returns the upkeep interval hours. */
  public int getUpkeepIntervalHours() {
    return upkeepIntervalHours;
  }

  /** Returns the upkeep grace period hours. */
  public int getUpkeepGracePeriodHours() {
    return upkeepGracePeriodHours;
  }

  /** Checks if upkeep auto pay default. */
  public boolean isUpkeepAutoPayDefault() {
    return upkeepAutoPayDefault;
  }

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
  public String format(@NotNull BigDecimal amount) {
    return currencySymbol + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  /**
   * Formats an amount with the currency name.
   *
   * @param amount the amount to format
   * @return formatted string (e.g., "100.00 dollars")
   */
  @NotNull
  public String formatWithName(@NotNull BigDecimal amount) {
    String name = amount.compareTo(BigDecimal.ONE) == 0 ? currencyName : currencyNamePlural;
    return amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " " + name;
  }

  // === Validation ===

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // Starting balance must be >= 0
    if (startingBalance.compareTo(BigDecimal.ZERO) < 0) {
      result.addWarning(getConfigName(), "startingBalance",
          "Value must be at least 0", startingBalance, BigDecimal.ZERO);
      startingBalance = BigDecimal.ZERO;
      needsSave = true;
    }

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
    depositFeePercent = validateBigDecimalRange(result, "depositFeePercent", depositFeePercent,
        BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO);
    withdrawFeePercent = validateBigDecimalRange(result, "withdrawFeePercent", withdrawFeePercent,
        BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO);
    transferFeePercent = validateBigDecimalRange(result, "transferFeePercent", transferFeePercent,
        BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO);

    // Upkeep settings
    if (upkeepCostPerChunk.compareTo(BigDecimal.ZERO) < 0) {
      result.addWarning(getConfigName(), "upkeepCostPerChunk",
          "Value must be at least 0", upkeepCostPerChunk, new BigDecimal("10.0"));
      upkeepCostPerChunk = new BigDecimal("10.0");
      needsSave = true;
    }
    upkeepIntervalHours = validateMin(result, "upkeepIntervalHours", upkeepIntervalHours, 1, 24);
    upkeepGracePeriodHours = validateMin(result, "upkeepGracePeriodHours", upkeepGracePeriodHours, 0, 48);

    return result;
  }

  /**
   * Validates a BigDecimal is within a range, auto-correcting if not.
   */
  @NotNull
  private BigDecimal validateBigDecimalRange(@NotNull ValidationResult result, @NotNull String field,
                        @NotNull BigDecimal value, @NotNull BigDecimal min,
                        @NotNull BigDecimal max, @NotNull BigDecimal defaultVal) {
    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
      result.addWarning(getConfigName(), field,
          String.format("Value must be between %s and %s", min.toPlainString(), max.toPlainString()),
          value, defaultVal);
      needsSave = true;
      return defaultVal;
    }
    return value;
  }
}
