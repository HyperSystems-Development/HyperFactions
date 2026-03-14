package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.util.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the debug logging system.
 * Controls debug output by category, with integration into the Logger utility.
 */
public class DebugConfig extends ModuleConfig {

  // Global debug settings
  private boolean enabledByDefault = false;

  private boolean logToConsole = true;

  // Per-category settings
  private boolean power = false;

  private boolean claim = false;

  private boolean combat = false;

  private boolean protection = false;

  private boolean relation = false;

  private boolean territory = false;

  private boolean worldmap = false;

  private boolean interaction = false;

  private boolean mixin = false;

  private boolean spawning = false;

  private boolean integration = false;

  private boolean economy = false;

  // Sentry error tracking settings
  private static final String DEFAULT_SENTRY_DSN =
      "https://cc41f97749e8b8b1562defea6ba3de9c@o4510966614589440.ingest.us.sentry.io/4510966616162304";

  private boolean sentryEnabled = true;

  private String sentryDsn = DEFAULT_SENTRY_DSN;

  private String sentryEnvironment = "production";

  private boolean sentryDebug = false;

  private double sentryTracesSampleRate = 0.0;

  /**
   * Creates a new debug config.
   *
   * @param filePath path to config/debug.json
   */
  public DebugConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "debug";
  }

  /** Returns the default enabled. */
  @Override
  protected boolean getDefaultEnabled() {
    return false; // Debug disabled by default
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = false;
    enabledByDefault = false;
    logToConsole = true;
    power = false;
    claim = false;
    combat = false;
    protection = false;
    relation = false;
    territory = false;
    worldmap = false;
    interaction = false;
    mixin = false;
    spawning = false;
    integration = false;
    economy = false;
    sentryEnabled = true;
    sentryDsn = DEFAULT_SENTRY_DSN;
    sentryEnvironment = "production";
    sentryDebug = false;
    sentryTracesSampleRate = 0.0;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    enabledByDefault = getBool(root, "enabledByDefault", enabledByDefault);
    logToConsole = getBool(root, "logToConsole", logToConsole);

    // Load categories
    if (hasSection(root, "categories")) {
      JsonObject categories = root.getAsJsonObject("categories");
      power = getBool(categories, "power", false);
      claim = getBool(categories, "claim", false);
      combat = getBool(categories, "combat", false);
      protection = getBool(categories, "protection", false);
      relation = getBool(categories, "relation", false);
      territory = getBool(categories, "territory", false);
      worldmap = getBool(categories, "worldmap", false);
      interaction = getBool(categories, "interaction", false);
      mixin = getBool(categories, "mixin", false);
      spawning = getBool(categories, "spawning", false);
      integration = getBool(categories, "integration", false);
      economy = getBool(categories, "economy", false);
    }

    // Load sentry settings (nested object)
    if (hasSection(root, "sentry")) {
      JsonObject sentry = root.getAsJsonObject("sentry");
      sentryEnabled = getBool(sentry, "enabled", sentryEnabled);
      sentryDsn = getString(sentry, "dsn", sentryDsn);
      sentryEnvironment = getString(sentry, "environment", sentryEnvironment);
      sentryDebug = getBool(sentry, "debug", sentryDebug);
      sentryTracesSampleRate = getDouble(sentry, "tracesSampleRate", sentryTracesSampleRate);
    } else {
      // Auto-migrate from old config/sentry.json if it exists
      migrateLegacySentryConfig();
    }

    // Apply settings to Logger
    applyToLogger();
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("enabledByDefault", enabledByDefault);
    root.addProperty("logToConsole", logToConsole);

    JsonObject categories = new JsonObject();
    categories.addProperty("power", power);
    categories.addProperty("claim", claim);
    categories.addProperty("combat", combat);
    categories.addProperty("protection", protection);
    categories.addProperty("relation", relation);
    categories.addProperty("territory", territory);
    categories.addProperty("worldmap", worldmap);
    categories.addProperty("interaction", interaction);
    categories.addProperty("mixin", mixin);
    categories.addProperty("spawning", spawning);
    categories.addProperty("integration", integration);
    categories.addProperty("economy", economy);
    root.add("categories", categories);

    JsonObject sentry = new JsonObject();
    sentry.addProperty("enabled", sentryEnabled);
    sentry.addProperty("dsn", sentryDsn);
    sentry.addProperty("environment", sentryEnvironment);
    sentry.addProperty("debug", sentryDebug);
    sentry.addProperty("tracesSampleRate", sentryTracesSampleRate);
    root.add("sentry", sentry);
  }

  /**
   * Applies the debug settings to the Logger utility.
   * Individual category settings take precedence over enabledByDefault.
   */
  public void applyToLogger() {
    Logger.setLogToConsole(logToConsole);
    // Individual settings override enabledByDefault - if explicitly set to false, stay false
    Logger.setDebugEnabled(Logger.DebugCategory.POWER, power);
    Logger.setDebugEnabled(Logger.DebugCategory.CLAIM, claim);
    Logger.setDebugEnabled(Logger.DebugCategory.COMBAT, combat);
    Logger.setDebugEnabled(Logger.DebugCategory.PROTECTION, protection);
    Logger.setDebugEnabled(Logger.DebugCategory.RELATION, relation);
    Logger.setDebugEnabled(Logger.DebugCategory.TERRITORY, territory);
    Logger.setDebugEnabled(Logger.DebugCategory.WORLDMAP, worldmap);
    Logger.setDebugEnabled(Logger.DebugCategory.INTERACTION, interaction);
    Logger.setDebugEnabled(Logger.DebugCategory.MIXIN, mixin);
    Logger.setDebugEnabled(Logger.DebugCategory.SPAWNING, spawning);
    Logger.setDebugEnabled(Logger.DebugCategory.INTEGRATION, integration);
    Logger.setDebugEnabled(Logger.DebugCategory.ECONOMY, economy);
  }

  // === Getters ===

  /**
   * Checks if debug is enabled by default for all categories.
   *
   * @return true if enabled by default
   */
  public boolean isEnabledByDefault() {
    return enabledByDefault;
  }

  /**
   * Checks if debug output should go to console.
   *
   * @return true if logging to console
   */
  public boolean isLogToConsole() {
    return logToConsole;
  }

  /**
   * Checks if power debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isPower() {
    return power;
  }

  /**
   * Checks if claim debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isClaim() {
    return claim;
  }

  /**
   * Checks if combat debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isCombat() {
    return combat;
  }

  /**
   * Checks if protection debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isProtection() {
    return protection;
  }

  /**
   * Checks if relation debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isRelation() {
    return relation;
  }

  /**
   * Checks if territory debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isTerritory() {
    return territory;
  }

  /**
   * Checks if world map debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isWorldmap() {
    return worldmap;
  }

  /**
   * Checks if interaction debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isInteraction() {
    return interaction;
  }

  /**
   * Checks if mixin debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isMixin() {
    return mixin;
  }

  /**
   * Checks if spawning debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isSpawning() {
    return spawning;
  }

  /**
   * Checks if integration debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isIntegration() {
    return integration;
  }

  /**
   * Checks if economy debug is enabled.
   *
   * @return true if enabled
   */
  public boolean isEconomy() {
    return economy;
  }

  // === Sentry Getters ===

  /**
   * Checks if Sentry error tracking is enabled.
   *
   * @return true if Sentry is enabled
   */
  public boolean isSentryEnabled() {
    return sentryEnabled;
  }

  /**
   * Gets the Sentry DSN (Data Source Name) URL.
   *
   * @return DSN string
   */
  @NotNull
  public String getSentryDsn() {
    return sentryDsn;
  }

  /**
   * Gets the environment name sent to Sentry (e.g., "production", "development").
   *
   * @return environment name
   */
  @NotNull
  public String getSentryEnvironment() {
    return sentryEnvironment;
  }

  /**
   * Checks if Sentry debug logging is enabled.
   *
   * @return true if Sentry debug mode is on
   */
  public boolean isSentryDebug() {
    return sentryDebug;
  }

  /**
   * Gets the traces sample rate for Sentry performance monitoring.
   * 0.0 = no performance traces, 1.0 = capture all.
   *
   * @return sample rate between 0.0 and 1.0
   */
  public double getSentryTracesSampleRate() {
    return sentryTracesSampleRate;
  }

  /**
   * Sets whether Sentry error tracking is enabled.
   *
   * @param enabled true to enable
   */
  public void setSentryEnabled(boolean enabled) {
    this.sentryEnabled = enabled;
  }

  /** Sets enabled by default. */
  public void setEnabledByDefault(boolean value) { this.enabledByDefault = value; }

  /** Sets log to console. */
  public void setLogToConsole(boolean value) { this.logToConsole = value; applyToLogger(); }

  /** Sets sentry debug mode. */
  public void setSentryDebug(boolean value) { this.sentryDebug = value; }

  /** Sets sentry traces sample rate. */
  public void setSentryTracesSampleRate(double value) { this.sentryTracesSampleRate = value; }

  // === Setters (for runtime toggle) ===

  /**
   * Sets power debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setPower(boolean enabled) {
    this.power = enabled;
    applyToLogger();
  }

  /**
   * Sets claim debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setClaim(boolean enabled) {
    this.claim = enabled;
    applyToLogger();
  }

  /**
   * Sets combat debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setCombat(boolean enabled) {
    this.combat = enabled;
    applyToLogger();
  }

  /**
   * Sets protection debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setProtection(boolean enabled) {
    this.protection = enabled;
    applyToLogger();
  }

  /**
   * Sets relation debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setRelation(boolean enabled) {
    this.relation = enabled;
    applyToLogger();
  }

  /**
   * Sets territory debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setTerritory(boolean enabled) {
    this.territory = enabled;
    applyToLogger();
  }

  /**
   * Sets world map debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setWorldmap(boolean enabled) {
    this.worldmap = enabled;
    applyToLogger();
  }

  /**
   * Sets interaction debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setInteraction(boolean enabled) {
    this.interaction = enabled;
    applyToLogger();
  }

  /**
   * Sets mixin debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setMixin(boolean enabled) {
    this.mixin = enabled;
    applyToLogger();
  }

  /**
   * Sets spawning debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setSpawning(boolean enabled) {
    this.spawning = enabled;
    applyToLogger();
  }

  /**
   * Sets integration debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setIntegration(boolean enabled) {
    this.integration = enabled;
    applyToLogger();
  }

  /**
   * Sets economy debug state and applies to Logger.
   *
   * @param enabled true to enable
   */
  public void setEconomy(boolean enabled) {
    this.economy = enabled;
    applyToLogger();
  }

  /**
   * Enables all debug categories.
   */
  public void enableAll() {
    enabledByDefault = false; // Clear this so individual settings work correctly
    power = true;
    claim = true;
    combat = true;
    protection = true;
    relation = true;
    territory = true;
    worldmap = true;
    interaction = true;
    mixin = true;
    spawning = true;
    integration = true;
    economy = true;
    applyToLogger();
  }

  /**
   * Disables all debug categories.
   */
  public void disableAll() {
    enabledByDefault = false; // Clear this so individual settings work correctly
    power = false;
    claim = false;
    combat = false;
    protection = false;
    relation = false;
    territory = false;
    worldmap = false;
    interaction = false;
    mixin = false;
    spawning = false;
    integration = false;
    economy = false;
    applyToLogger();
  }

  /**
   * Migrates sentry settings from the old config/sentry.json file into this config.
   * Called when debug.json has no "sentry" section and we need to check for legacy data.
   * Deletes the old file after successful migration.
   */
  private void migrateLegacySentryConfig() {
    Path sentryFile = filePath.getParent().resolve("sentry.json");
    if (!Files.exists(sentryFile)) {
      return;
    }

    try {
      String json = Files.readString(sentryFile);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      // Read sentry values from the old file
      if (root.has("enabled")) {
        sentryEnabled = root.get("enabled").getAsBoolean();
      }
      if (root.has("dsn")) {
        sentryDsn = root.get("dsn").getAsString();
      }
      if (root.has("environment")) {
        sentryEnvironment = root.get("environment").getAsString();
      }
      if (root.has("debug")) {
        sentryDebug = root.get("debug").getAsBoolean();
      }
      if (root.has("tracesSampleRate")) {
        sentryTracesSampleRate = root.get("tracesSampleRate").getAsDouble();
      }

      needsSave = true;
      Logger.info("[Config] Migrated sentry config from sentry.json into debug.json");

      // Delete the old file
      Files.delete(sentryFile);
      Logger.info("[Config] Deleted old config/sentry.json");
    } catch (Exception e) {
      Logger.warn("[Config] Failed to migrate sentry.json: %s", e.getMessage());
    }
  }
}
