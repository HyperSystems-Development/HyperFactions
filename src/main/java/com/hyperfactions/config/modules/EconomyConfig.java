package com.hyperfactions.config.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

  private String currencySymbolPosition = "left";

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
  private boolean upkeepEnabled = true;

  private BigDecimal upkeepCostPerChunk = new BigDecimal("2.0");

  private int upkeepIntervalHours = 24;

  private int upkeepGracePeriodHours = 48;

  private boolean upkeepAutoPayDefault = true;

  private int upkeepFreeChunks = 3;

  private int upkeepClaimLossPerCycle = 1;

  private int upkeepWarningHours = 6;

  private BigDecimal upkeepMaxCostCap = BigDecimal.ZERO;

  private String upkeepScalingMode = "flat";

  private List<ScalingTier> upkeepScalingTiers = List.of(
      new ScalingTier(10, new BigDecimal("2.00")),
      new ScalingTier(15, new BigDecimal("3.00")),
      new ScalingTier(0, new BigDecimal("5.00"))
  );

  /**
   * Defines a pricing tier for progressive upkeep scaling.
   *
   * @param chunkCount number of chunks covered by this tier (0 = all remaining)
   * @param costPerChunk cost per chunk in this tier
   */
  public record ScalingTier(int chunkCount, @NotNull BigDecimal costPerChunk) {}

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
    currencySymbolPosition = "left";
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
    upkeepEnabled = true;
    upkeepCostPerChunk = new BigDecimal("2.0");
    upkeepIntervalHours = 24;
    upkeepGracePeriodHours = 48;
    upkeepAutoPayDefault = true;
    upkeepFreeChunks = 3;
    upkeepClaimLossPerCycle = 1;
    upkeepWarningHours = 6;
    upkeepMaxCostCap = BigDecimal.ZERO;
    upkeepScalingMode = "flat";
    upkeepScalingTiers = List.of(
        new ScalingTier(10, new BigDecimal("2.00")),
        new ScalingTier(15, new BigDecimal("3.00")),
        new ScalingTier(0, new BigDecimal("5.00"))
    );
  }

  /** Loads module settings. Supports both nested (new) and flat (legacy) formats. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    // Currency section
    JsonObject currency = getSection(root, "currency");
    currencyName = getString(currency, "name", getString(root, "currencyName", currencyName));
    currencyNamePlural = getString(currency, "namePlural", getString(root, "currencyNamePlural", currencyNamePlural));
    currencySymbol = getString(currency, "symbol", getString(root, "currencySymbol", currencySymbol));
    currencySymbolPosition = getString(currency, "symbolPosition", getString(root, "currencySymbolPosition", currencySymbolPosition));

    // Treasury section
    JsonObject treasury = getSection(root, "treasury");
    startingBalance = getBigDecimal(treasury, "startingBalance", getBigDecimal(root, "startingBalance", startingBalance));
    disbandRefundToLeader = getBool(treasury, "disbandRefundToLeader", getBool(root, "disbandRefundToLeader", disbandRefundToLeader));

    // Limits subsection
    JsonObject limits = getSection(treasury, "limits");
    if (limits.isEmpty()) limits = root; // fall back to flat keys
    defaultMaxWithdrawAmount = getBigDecimal(limits, "maxWithdrawAmount",
        getBigDecimal(root, "defaultMaxWithdrawAmount", defaultMaxWithdrawAmount));
    defaultMaxWithdrawPerPeriod = getBigDecimal(limits, "maxWithdrawPerPeriod",
        getBigDecimal(root, "defaultMaxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod));
    defaultMaxTransferAmount = getBigDecimal(limits, "maxTransferAmount",
        getBigDecimal(root, "defaultMaxTransferAmount", defaultMaxTransferAmount));
    defaultMaxTransferPerPeriod = getBigDecimal(limits, "maxTransferPerPeriod",
        getBigDecimal(root, "defaultMaxTransferPerPeriod", defaultMaxTransferPerPeriod));
    defaultLimitPeriodHours = getInt(limits, "periodHours",
        getInt(root, "defaultLimitPeriodHours", defaultLimitPeriodHours));

    // Fees section
    JsonObject fees = getSection(root, "fees");
    depositFeePercent = getBigDecimal(fees, "depositPercent", getBigDecimal(root, "depositFeePercent", depositFeePercent));
    withdrawFeePercent = getBigDecimal(fees, "withdrawPercent", getBigDecimal(root, "withdrawFeePercent", withdrawFeePercent));
    transferFeePercent = getBigDecimal(fees, "transferPercent", getBigDecimal(root, "transferFeePercent", transferFeePercent));

    // Upkeep section
    JsonObject upkeep = getSection(root, "upkeep");
    upkeepEnabled = getBool(upkeep, "enabled", getBool(root, "upkeepEnabled", upkeepEnabled));
    upkeepCostPerChunk = getBigDecimal(upkeep, "costPerChunk", getBigDecimal(root, "upkeepCostPerChunk", upkeepCostPerChunk));
    upkeepIntervalHours = getInt(upkeep, "intervalHours", getInt(root, "upkeepIntervalHours", upkeepIntervalHours));
    upkeepGracePeriodHours = getInt(upkeep, "gracePeriodHours", getInt(root, "upkeepGracePeriodHours", upkeepGracePeriodHours));
    upkeepAutoPayDefault = getBool(upkeep, "autoPayDefault", getBool(root, "upkeepAutoPayDefault", upkeepAutoPayDefault));
    upkeepFreeChunks = getInt(upkeep, "freeChunks", getInt(root, "upkeepFreeChunks", upkeepFreeChunks));
    upkeepClaimLossPerCycle = getInt(upkeep, "claimLossPerCycle", getInt(root, "upkeepClaimLossPerCycle", upkeepClaimLossPerCycle));
    upkeepWarningHours = getInt(upkeep, "warningHours", getInt(root, "upkeepWarningHours", upkeepWarningHours));
    upkeepMaxCostCap = getBigDecimal(upkeep, "maxCostCap", getBigDecimal(root, "upkeepMaxCostCap", upkeepMaxCostCap));
    upkeepScalingMode = getString(upkeep, "scalingMode", getString(root, "upkeepScalingMode", upkeepScalingMode));

    // Scaling tiers (check nested first, then flat)
    JsonObject tiersSource = upkeep.has("scalingTiers") ? upkeep : root;
    String tiersKey = upkeep.has("scalingTiers") ? "scalingTiers" : "upkeepScalingTiers";
    if (tiersSource.has(tiersKey) && tiersSource.get(tiersKey).isJsonArray()) {
      upkeepScalingTiers = parseScalingTiers(tiersSource.getAsJsonArray(tiersKey));
    }
  }

  /** Write Module Settings in grouped/nested format. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    // Currency section
    JsonObject currency = new JsonObject();
    currency.addProperty("name", currencyName);
    currency.addProperty("namePlural", currencyNamePlural);
    currency.addProperty("symbol", currencySymbol);
    currency.addProperty("symbolPosition", currencySymbolPosition);
    root.add("currency", currency);

    // Treasury section
    JsonObject treasury = new JsonObject();
    treasury.addProperty("startingBalance", startingBalance.toPlainString());
    treasury.addProperty("disbandRefundToLeader", disbandRefundToLeader);

    JsonObject limits = new JsonObject();
    limits.addProperty("maxWithdrawAmount", defaultMaxWithdrawAmount.toPlainString());
    limits.addProperty("maxWithdrawPerPeriod", defaultMaxWithdrawPerPeriod.toPlainString());
    limits.addProperty("maxTransferAmount", defaultMaxTransferAmount.toPlainString());
    limits.addProperty("maxTransferPerPeriod", defaultMaxTransferPerPeriod.toPlainString());
    limits.addProperty("periodHours", defaultLimitPeriodHours);
    treasury.add("limits", limits);
    root.add("treasury", treasury);

    // Fees section
    JsonObject fees = new JsonObject();
    fees.addProperty("depositPercent", depositFeePercent.toPlainString());
    fees.addProperty("withdrawPercent", withdrawFeePercent.toPlainString());
    fees.addProperty("transferPercent", transferFeePercent.toPlainString());
    root.add("fees", fees);

    // Upkeep section
    JsonObject upkeep = new JsonObject();
    upkeep.addProperty("enabled", upkeepEnabled);
    upkeep.addProperty("costPerChunk", upkeepCostPerChunk.toPlainString());
    upkeep.addProperty("intervalHours", upkeepIntervalHours);
    upkeep.addProperty("gracePeriodHours", upkeepGracePeriodHours);
    upkeep.addProperty("autoPayDefault", upkeepAutoPayDefault);
    upkeep.addProperty("freeChunks", upkeepFreeChunks);
    upkeep.addProperty("claimLossPerCycle", upkeepClaimLossPerCycle);
    upkeep.addProperty("warningHours", upkeepWarningHours);
    upkeep.addProperty("maxCostCap", upkeepMaxCostCap.toPlainString());
    upkeep.addProperty("scalingMode", upkeepScalingMode);
    upkeep.add("scalingTiers", serializeScalingTiers(upkeepScalingTiers));
    root.add("upkeep", upkeep);
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
   * Gets the currency symbol position ("left" or "right").
   *
   * @return currency symbol position
   */
  @NotNull
  public String getCurrencySymbolPosition() {
    return currencySymbolPosition;
  }

  /**
   * Checks if the currency symbol should be placed on the right side.
   *
   * @return true if symbol position is "right"
   */
  public boolean isSymbolRight() {
    return "right".equalsIgnoreCase(currencySymbolPosition);
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

  /** Returns the number of chunks exempt from upkeep cost. */
  public int getUpkeepFreeChunks() {
    return upkeepFreeChunks;
  }

  /** Returns the number of claims lost per failed cycle after grace. */
  public int getUpkeepClaimLossPerCycle() {
    return upkeepClaimLossPerCycle;
  }

  /** Returns the hours before upkeep to warn online members. */
  public int getUpkeepWarningHours() {
    return upkeepWarningHours;
  }

  /** Returns the max cost cap per cycle (0 = unlimited). */
  @NotNull public BigDecimal getUpkeepMaxCostCap() {
    return upkeepMaxCostCap;
  }

  /** Returns the scaling mode ("flat" or "progressive"). */
  @NotNull public String getUpkeepScalingMode() {
    return upkeepScalingMode;
  }

  /** Returns the progressive scaling tiers. */
  @NotNull public List<ScalingTier> getUpkeepScalingTiers() {
    return upkeepScalingTiers;
  }

  // === Upkeep Setters (for admin GUI / commands) ===

  /** Sets upkeep enabled. */
  public void setUpkeepEnabled(boolean value) { upkeepEnabled = value; needsSave = true; }

  /** Sets upkeep cost per chunk. */
  public void setUpkeepCostPerChunk(@NotNull BigDecimal value) { upkeepCostPerChunk = value; needsSave = true; }

  /** Sets upkeep interval hours. */
  public void setUpkeepIntervalHours(int value) { upkeepIntervalHours = value; needsSave = true; }

  /** Sets upkeep grace period hours. */
  public void setUpkeepGracePeriodHours(int value) { upkeepGracePeriodHours = value; needsSave = true; }

  /** Sets upkeep auto pay default. */
  public void setUpkeepAutoPayDefault(boolean value) { upkeepAutoPayDefault = value; needsSave = true; }

  /** Sets upkeep free chunks. */
  public void setUpkeepFreeChunks(int value) { upkeepFreeChunks = value; needsSave = true; }

  /** Sets upkeep claim loss per cycle. */
  public void setUpkeepClaimLossPerCycle(int value) { upkeepClaimLossPerCycle = value; needsSave = true; }

  /** Sets upkeep warning hours. */
  public void setUpkeepWarningHours(int value) { upkeepWarningHours = value; needsSave = true; }

  /** Sets upkeep max cost cap. */
  public void setUpkeepMaxCostCap(@NotNull BigDecimal value) { upkeepMaxCostCap = value; needsSave = true; }

  /** Sets upkeep scaling mode. */
  public void setUpkeepScalingMode(@NotNull String value) { upkeepScalingMode = value; needsSave = true; }

  /** Sets upkeep scaling tiers. */
  public void setUpkeepScalingTiers(@NotNull List<ScalingTier> value) { upkeepScalingTiers = value; needsSave = true; }

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
    String formatted = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    return isSymbolRight() ? formatted + currencySymbol : currencySymbol + formatted;
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

    // Currency symbol position must be "left" or "right"
    if (!"left".equalsIgnoreCase(currencySymbolPosition) && !"right".equalsIgnoreCase(currencySymbolPosition)) {
      result.addWarning(getConfigName(), "currencySymbolPosition",
          "Must be 'left' or 'right'", currencySymbolPosition, "left");
      currencySymbolPosition = "left";
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
          "Value must be at least 0", upkeepCostPerChunk, new BigDecimal("2.0"));
      upkeepCostPerChunk = new BigDecimal("2.0");
      needsSave = true;
    }
    upkeepIntervalHours = validateMin(result, "upkeepIntervalHours", upkeepIntervalHours, 1, 24);
    upkeepGracePeriodHours = validateMin(result, "upkeepGracePeriodHours", upkeepGracePeriodHours, 0, 48);

    if (upkeepFreeChunks < 0) {
      result.addWarning(getConfigName(), "upkeepFreeChunks",
          "Value must be at least 0", upkeepFreeChunks, 3);
      upkeepFreeChunks = 3;
      needsSave = true;
    }
    upkeepClaimLossPerCycle = validateMin(result, "upkeepClaimLossPerCycle", upkeepClaimLossPerCycle, 1, 1);
    if (upkeepWarningHours < 0) {
      result.addWarning(getConfigName(), "upkeepWarningHours",
          "Value must be at least 0", upkeepWarningHours, 6);
      upkeepWarningHours = 6;
      needsSave = true;
    }
    if (upkeepMaxCostCap.compareTo(BigDecimal.ZERO) < 0) {
      result.addWarning(getConfigName(), "upkeepMaxCostCap",
          "Value must be at least 0", upkeepMaxCostCap, BigDecimal.ZERO);
      upkeepMaxCostCap = BigDecimal.ZERO;
      needsSave = true;
    }
    if (!"flat".equals(upkeepScalingMode) && !"progressive".equals(upkeepScalingMode)) {
      result.addWarning(getConfigName(), "upkeepScalingMode",
          "Must be 'flat' or 'progressive'", upkeepScalingMode, "flat");
      upkeepScalingMode = "flat";
      needsSave = true;
    }

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

  // === Scaling Tier Serialization ===

  @NotNull
  private List<ScalingTier> parseScalingTiers(@NotNull JsonArray array) {
    List<ScalingTier> tiers = new ArrayList<>();
    for (JsonElement elem : array) {
      if (elem.isJsonObject()) {
        JsonObject tierObj = elem.getAsJsonObject();
        int chunkCount = tierObj.has("chunkCount") ? tierObj.get("chunkCount").getAsInt() : 0;
        BigDecimal costPerChunk = getBigDecimal(tierObj, "costPerChunk", BigDecimal.ZERO);
        tiers.add(new ScalingTier(chunkCount, costPerChunk));
      }
    }
    return tiers;
  }

  @NotNull
  private JsonArray serializeScalingTiers(@NotNull List<ScalingTier> tiers) {
    JsonArray array = new JsonArray();
    for (ScalingTier tier : tiers) {
      JsonObject obj = new JsonObject();
      obj.addProperty("chunkCount", tier.chunkCount());
      obj.addProperty("costPerChunk", tier.costPerChunk().toPlainString());
      array.add(obj);
    }
    return array;
  }
}
