package com.hyperfactions.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for configuration files.
 *
 * <p>
 * Provides common functionality for loading and saving JSON config files,
 * with helper methods for reading typed values with defaults, and validation
 * support for detecting and auto-correcting invalid values.
 */
public abstract class ConfigFile {

  protected static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  protected final Path filePath;

  protected boolean needsSave = false;

  protected ValidationResult lastValidationResult = null;

  /**
   * Creates a new config file handler.
   *
   * @param filePath path to the config file
   */
  protected ConfigFile(@NotNull Path filePath) {
    this.filePath = filePath;
  }

  /**
   * Gets the path to this config file.
   *
   * @return file path
   */
  @NotNull
  public Path getFilePath() {
    return filePath;
  }

  /**
   * Loads the configuration from file.
   *
   * <p>
   * If the file doesn't exist, creates a new one with defaults.
   * If any keys are missing, they will be added with defaults and
   * the file will be re-saved.
   */
  public void load() {
    if (!Files.exists(filePath)) {
      Logger.info("[Config] Creating new config file: %s", filePath.getFileName());
      createDefaults();
      save();
      return;
    }

    needsSave = false;

    try {
      String json = Files.readString(filePath);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();
      loadFromJson(root);

      // Detect new keys that the current code would write but aren't in the file yet.
      // This handles config modules that bypass the base getInt/getBool helpers (e.g.
      // FactionPermissionsConfig) and any future module additions — no manual needsSave
      // bookkeeping required per module.
      if (!needsSave) {
        JsonObject fresh = toJson();
        if (hasNewKeys(root, fresh)) {
          needsSave = true;
        }
      }

      if (needsSave) {
        Logger.info("[Config] Adding missing keys to: %s", filePath.getFileName());
        save();
      }
    } catch (Exception e) {
      ErrorHandler.report(String.format("[Config] Failed to load %s", filePath.getFileName()), e);
      createDefaults();
    }
  }

  /**
   * Saves the configuration to file.
   */
  public void save() {
    try {
      Files.createDirectories(filePath.getParent());
      JsonObject json = toJson();
      Files.writeString(filePath, GSON.toJson(json));
      needsSave = false;
      Logger.debug("[Config] Saved: %s", filePath.getFileName());
    } catch (IOException e) {
      ErrorHandler.report(String.format("[Config] Failed to save %s", filePath.getFileName()), e);
    }
  }

  /**
   * Reloads the configuration from file.
   */
  public void reload() {
    load();
  }

  /**
   * Resets to factory defaults and saves.
   */
  public void resetDefaults() {
    createDefaults();
    save();
  }

  /**
   * Loads configuration values from the parsed JSON object.
   *
   * @param root the root JSON object
   */
  protected abstract void loadFromJson(@NotNull JsonObject root);

  /**
   * Converts the current configuration to a JSON object.
   *
   * @return JSON representation
   */
  @NotNull
  protected abstract JsonObject toJson();

  /**
   * Creates default configuration values.
   * Called when config file doesn't exist.
   */
  protected abstract void createDefaults();

  /**
   * Gets the name of this config file for logging.
   *
   * @return config name (e.g., "config.json", "backup.json")
   */
  @NotNull
  public String getConfigName() {
    return filePath.getFileName().toString();
  }

  // === Validation ===

  /**
   * Validates the current configuration values.
   *
   * <p>
   * Subclasses should override this to add specific validation rules.
   * Invalid values should be logged as warnings and optionally corrected.
   *
   * @return validation result with any issues found
   */
  @NotNull
  public ValidationResult validate() {
    // Default implementation: no validation
    return new ValidationResult();
  }

  /**
   * Gets the result of the last validation run.
   *
   * @return last validation result, or null if never validated
   */
  public ValidationResult getLastValidationResult() {
    return lastValidationResult;
  }

  /**
   * Runs validation and logs any issues found.
   *
   * <p>
   * If corrections were made, the config will be re-saved.
   */
  public void validateAndLog() {
    lastValidationResult = validate();

    if (lastValidationResult.hasIssues()) {
      for (ValidationResult.Issue issue : lastValidationResult.getIssues()) {
        if (issue.severity() == ValidationResult.Severity.ERROR) {
          Logger.warn("[Config] %s", issue);
        } else {
          Logger.warn("[Config] %s", issue);
        }
      }

      if (lastValidationResult.needsSave()) {
        Logger.info("[Config] Saving corrected values to: %s", getConfigName());
        save();
      }
    }
  }

  // === Validation Helper Methods ===

  /**
   * Validates an integer is within a range, auto-correcting if not.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @param min      minimum allowed value
   * @param max      maximum allowed value
   * @param defaultVal default to use if out of range
   * @return the valid value (original or corrected)
   */
  protected int validateRange(@NotNull ValidationResult result, @NotNull String field,
                int value, int min, int max, int defaultVal) {
    if (value < min || value > max) {
      result.addWarning(getConfigName(), field,
          String.format("Value must be between %d and %d", min, max),
          value, defaultVal);
      needsSave = true;
      return defaultVal;
    }
    return value;
  }

  /**
   * Validates an integer has a minimum value, auto-correcting if not.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @param min      minimum allowed value
   * @param defaultVal default to use if below minimum
   * @return the valid value (original or corrected)
   */
  protected int validateMin(@NotNull ValidationResult result, @NotNull String field,
               int value, int min, int defaultVal) {
    if (value < min) {
      result.addWarning(getConfigName(), field,
          String.format("Value must be at least %d", min),
          value, defaultVal);
      needsSave = true;
      return defaultVal;
    }
    return value;
  }

  /**
   * Validates a double is within a range, auto-correcting if not.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @param min      minimum allowed value
   * @param max      maximum allowed value
   * @param defaultVal default to use if out of range
   * @return the valid value (original or corrected)
   */
  protected double validateRange(@NotNull ValidationResult result, @NotNull String field,
                 double value, double min, double max, double defaultVal) {
    if (value < min || value > max) {
      result.addWarning(getConfigName(), field,
          String.format("Value must be between %.2f and %.2f", min, max),
          value, defaultVal);
      needsSave = true;
      return defaultVal;
    }
    return value;
  }

  /**
   * Validates a double has a minimum value, auto-correcting if not.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @param min      minimum allowed value
   * @param defaultVal default to use if below minimum
   * @return the valid value (original or corrected)
   */
  protected double validateMin(@NotNull ValidationResult result, @NotNull String field,
                double value, double min, double defaultVal) {
    if (value < min) {
      result.addWarning(getConfigName(), field,
          String.format("Value must be at least %.2f", min),
          value, defaultVal);
      needsSave = true;
      return defaultVal;
    }
    return value;
  }

  /**
   * Validates a string is one of the allowed values, auto-correcting if not.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @param allowed  array of allowed values
   * @param defaultVal default to use if not in allowed list
   * @return the valid value (original or corrected)
   */
  @NotNull
  protected String validateEnum(@NotNull ValidationResult result, @NotNull String field,
                 @NotNull String value, @NotNull String[] allowed, @NotNull String defaultVal) {
    for (String a : allowed) {
      if (a.equalsIgnoreCase(value)) {
        return value;
      }
    }
    result.addWarning(getConfigName(), field,
        String.format("Value must be one of: %s", String.join(", ", allowed)),
        value, defaultVal);
    needsSave = true;
    return defaultVal;
  }

  /**
   * Validates a hex color format, logging warning if invalid but not correcting.
   *
   * @param result   validation result to add issues to
   * @param field    field name
   * @param value    current value
   * @return true if valid
   */
  protected boolean validateHexColor(@NotNull ValidationResult result, @NotNull String field,
                   @NotNull String value) {
    if (!value.matches("^#[0-9A-Fa-f]{6}$")) {
      result.addWarning(getConfigName(), field,
          "Should be a hex color in format #RRGGBB", value);
      return false;
    }
    return true;
  }

  // === Helper methods for reading JSON values ===

  /**
   * Gets an integer value, marking config as needing save if missing.
   *
   * @param obj         JSON object
   * @param key         property key
   * @param defaultVal  default value
   * @return the value or default
   */
  protected int getInt(@NotNull JsonObject obj, @NotNull String key, int defaultVal) {
    if (obj.has(key)) {
      return obj.get(key).getAsInt();
    }
    needsSave = true;
    return defaultVal;
  }

  /**
   * Gets a double value, marking config as needing save if missing.
   *
   * @param obj         JSON object
   * @param key         property key
   * @param defaultVal  default value
   * @return the value or default
   */
  protected double getDouble(@NotNull JsonObject obj, @NotNull String key, double defaultVal) {
    if (obj.has(key)) {
      return obj.get(key).getAsDouble();
    }
    needsSave = true;
    return defaultVal;
  }

  /**
   * Gets a boolean value, marking config as needing save if missing.
   *
   * @param obj         JSON object
   * @param key         property key
   * @param defaultVal  default value
   * @return the value or default
   */
  protected boolean getBool(@NotNull JsonObject obj, @NotNull String key, boolean defaultVal) {
    if (obj.has(key)) {
      return obj.get(key).getAsBoolean();
    }
    needsSave = true;
    return defaultVal;
  }

  /**
   * Gets a string value, marking config as needing save if missing.
   *
   * @param obj         JSON object
   * @param key         property key
   * @param defaultVal  default value
   * @return the value or default
   */
  @NotNull
  protected String getString(@NotNull JsonObject obj, @NotNull String key, @NotNull String defaultVal) {
    if (obj.has(key)) {
      return obj.get(key).getAsString();
    }
    needsSave = true;
    return defaultVal;
  }

  /**
   * Gets a string list value.
   *
   * @param obj JSON object
   * @param key property key
   * @return the list or empty list
   */
  @NotNull
  protected List<String> getStringList(@NotNull JsonObject obj, @NotNull String key) {
    List<String> list = new ArrayList<>();
    if (obj.has(key) && obj.get(key).isJsonArray()) {
      obj.getAsJsonArray(key).forEach(el -> list.add(el.getAsString()));
    } else if (!obj.has(key)) {
      needsSave = true;
    }
    return list;
  }

  /**
   * Checks if a section exists and is a JSON object.
   *
   * @param obj         parent object
   * @param sectionName section key
   * @return true if section exists
   */
  protected boolean hasSection(@NotNull JsonObject obj, @NotNull String sectionName) {
    if (obj.has(sectionName) && obj.get(sectionName).isJsonObject()) {
      return true;
    }
    needsSave = true;
    return false;
  }

  /**
   * Gets a section as JsonObject, or creates an empty one.
   *
   * @param obj         parent object
   * @param sectionName section key
   * @return the section or empty object
   */
  @NotNull
  protected JsonObject getSection(@NotNull JsonObject obj, @NotNull String sectionName) {
    if (obj.has(sectionName) && obj.get(sectionName).isJsonObject()) {
      return obj.getAsJsonObject(sectionName);
    }
    needsSave = true;
    return new JsonObject();
  }

  /**
   * Creates a JsonArray from a list of strings.
   *
   * @param list the string list
   * @return JSON array
   */
  @NotNull
  protected JsonArray toJsonArray(@NotNull List<String> list) {
    JsonArray array = new JsonArray();
    list.forEach(array::add);
    return array;
  }

  /**
   * Recursively checks if {@code fresh} contains any keys not present in {@code original}.
   *
   * <p>
   * Used to detect when the code's serialization would produce additional keys
   * (e.g. a new config flag was added in a code update). Only checks key presence,
   * not value equality — value differences from user customization are intentional.
   *
   * @param original the JSON loaded from disk
   * @param fresh    the JSON produced by serializing the current in-memory state
   * @return true if fresh has at least one key (at any depth) not in original
   */
  private static boolean hasNewKeys(@NotNull JsonObject original, @NotNull JsonObject fresh) {
    for (String key : fresh.keySet()) {
      if (!original.has(key)) {
        return true;
      }
      JsonElement origVal = original.get(key);
      JsonElement freshVal = fresh.get(key);
      if (freshVal.isJsonObject() && origVal.isJsonObject()) {
        if (hasNewKeys(origVal.getAsJsonObject(), freshVal.getAsJsonObject())) {
          return true;
        }
      }
    }
    return false;
  }
}
