package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for Sentry error tracking integration.
 * Controls whether errors are reported to Sentry, the DSN, environment, and debug settings.
 */
public class SentryConfig extends ModuleConfig {

  private static final String DEFAULT_DSN =
      "https://cc41f97749e8b8b1562defea6ba3de9c@o4510966614589440.ingest.us.sentry.io/4510966616162304";

  private String dsn = DEFAULT_DSN;

  private String environment = "production";

  private boolean debug = false;

  private double tracesSampleRate = 0.0;

  /**
   * Creates a new sentry config.
   *
   * @param filePath path to config/sentry.json
   */
  public SentryConfig(@NotNull Path filePath) {
    super(filePath);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return "sentry";
  }

  @Override
  protected boolean getDefaultEnabled() {
    return true;
  }

  @Override
  protected void createDefaults() {
    enabled = true;
    dsn = DEFAULT_DSN;
    environment = "production";
    debug = false;
    tracesSampleRate = 0.0;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    dsn = getString(root, "dsn", dsn);
    environment = getString(root, "environment", environment);
    debug = getBool(root, "debug", debug);
    tracesSampleRate = getDouble(root, "tracesSampleRate", tracesSampleRate);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("dsn", dsn);
    root.addProperty("environment", environment);
    root.addProperty("debug", debug);
    root.addProperty("tracesSampleRate", tracesSampleRate);
  }

  /**
   * Gets the Sentry DSN (Data Source Name) URL.
   *
   * @return DSN string, empty if not configured
   */
  @NotNull
  public String getDsn() {
    return dsn;
  }

  /**
   * Gets the environment name sent to Sentry (e.g., "production", "development").
   *
   * @return environment name
   */
  @NotNull
  public String getEnvironment() {
    return environment;
  }

  /**
   * Checks if Sentry debug logging is enabled.
   *
   * @return true if debug mode is on
   */
  public boolean isDebug() {
    return debug;
  }

  /**
   * Gets the traces sample rate for performance monitoring.
   * 0.0 = no performance traces, 1.0 = capture all.
   *
   * @return sample rate between 0.0 and 1.0
   */
  public double getTracesSampleRate() {
    return tracesSampleRate;
  }
}