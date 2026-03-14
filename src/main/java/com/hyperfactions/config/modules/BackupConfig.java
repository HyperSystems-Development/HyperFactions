package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the backup system.
 *
 * <p>
 * Uses GFS (Grandfather-Father-Son) rotation scheme with configurable
 * retention periods for hourly, daily, weekly, and manual backups.
 */
public class BackupConfig extends ModuleConfig {

  // Retention settings
  private int hourlyRetention = 24;   // Keep last 24 hourly backups
  private int dailyRetention = 7;     // Keep last 7 daily backups
  private int weeklyRetention = 4;    // Keep last 4 weekly backups
  private int manualRetention = 10;   // Keep last 10 manual backups (0 = keep all)
  private boolean onShutdown = true;  // Create backup on server shutdown
  private int shutdownRetention = 5;  // Keep last 5 shutdown backups

  /**
   * Creates a new backup config.
   *
   * @param filePath path to config/backup.json
   */
  public BackupConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "backup";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    hourlyRetention = 24;
    dailyRetention = 7;
    weeklyRetention = 4;
    manualRetention = 10;
    onShutdown = true;
    shutdownRetention = 5;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    hourlyRetention = getInt(root, "hourlyRetention", hourlyRetention);
    dailyRetention = getInt(root, "dailyRetention", dailyRetention);
    weeklyRetention = getInt(root, "weeklyRetention", weeklyRetention);
    manualRetention = getInt(root, "manualRetention", manualRetention);
    onShutdown = getBool(root, "onShutdown", onShutdown);
    shutdownRetention = getInt(root, "shutdownRetention", shutdownRetention);
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("hourlyRetention", hourlyRetention);
    root.addProperty("dailyRetention", dailyRetention);
    root.addProperty("weeklyRetention", weeklyRetention);
    root.addProperty("manualRetention", manualRetention);
    root.addProperty("onShutdown", onShutdown);
    root.addProperty("shutdownRetention", shutdownRetention);
  }

  // === Getters ===

  /**
   * Gets the number of hourly backups to retain.
   *
   * @return hourly retention count
   */
  public int getHourlyRetention() {
    return hourlyRetention;
  }

  /**
   * Gets the number of daily backups to retain.
   *
   * @return daily retention count
   */
  public int getDailyRetention() {
    return dailyRetention;
  }

  /**
   * Gets the number of weekly backups to retain.
   *
   * @return weekly retention count
   */
  public int getWeeklyRetention() {
    return weeklyRetention;
  }

  /**
   * Gets the number of manual backups to retain.
   * 0 means keep all manual backups.
   *
   * @return manual retention count
   */
  public int getManualRetention() {
    return manualRetention;
  }

  /**
   * Checks if a backup should be created on shutdown.
   *
   * @return true if shutdown backup is enabled
   */
  public boolean isOnShutdown() {
    return onShutdown;
  }

  /**
   * Gets the number of shutdown backups to retain.
   * 0 means keep all shutdown backups.
   *
   * @return shutdown retention count
   */
  public int getShutdownRetention() {
    return shutdownRetention;
  }

  // === Setters (for admin config editor) ===

  /** Sets hourly retention. */
  public void setHourlyRetention(int value) { this.hourlyRetention = value; }

  /** Sets daily retention. */
  public void setDailyRetention(int value) { this.dailyRetention = value; }

  /** Sets weekly retention. */
  public void setWeeklyRetention(int value) { this.weeklyRetention = value; }

  /** Sets manual retention. */
  public void setManualRetention(int value) { this.manualRetention = value; }

  /** Sets on shutdown. */
  public void setOnShutdown(boolean value) { this.onShutdown = value; }

  /** Sets shutdown retention. */
  public void setShutdownRetention(int value) { this.shutdownRetention = value; }

  // === Validation ===

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // All retention values must be >= 0
    hourlyRetention = validateMin(result, "hourlyRetention", hourlyRetention, 0, 24);
    dailyRetention = validateMin(result, "dailyRetention", dailyRetention, 0, 7);
    weeklyRetention = validateMin(result, "weeklyRetention", weeklyRetention, 0, 4);
    manualRetention = validateMin(result, "manualRetention", manualRetention, 0, 10);
    shutdownRetention = validateMin(result, "shutdownRetention", shutdownRetention, 0, 5);

    return result;
  }
}
