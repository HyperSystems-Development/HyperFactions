package com.hyperfactions.config;

import com.google.gson.JsonObject;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract base class for module configuration files.
 *
 * <p>
 * Module configs live in the config/ subdirectory and have an "enabled" field
 * that can completely disable the module's functionality.
 */
public abstract class ModuleConfig extends ConfigFile {

  protected boolean enabled = true;

  /**
   * Creates a new module config handler.
   *
   * @param filePath path to the module config file
   */
  protected ModuleConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /**
   * Gets the module name for logging.
   *
   * @return module name
   */
  @NotNull
  public abstract String getModuleName();

  /**
   * Checks if this module is enabled.
   * When disabled, the module's functionality should be completely skipped.
   *
   * @return true if enabled
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Sets whether this module is enabled.
   *
   * @param enabled true to enable
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Gets the default enabled state for this module.
   * Override to change the default (most modules default to true).
   *
   * @return default enabled state
   */
  protected boolean getDefaultEnabled() {
    return true;
  }

  /** Loads from json. */
  @Override
  protected void loadFromJson(@NotNull JsonObject root) {
    this.enabled = getBool(root, "enabled", getDefaultEnabled());
    loadModuleSettings(root);
  }

  /** Converts to json. */
  @Override
  @NotNull
  protected JsonObject toJson() {
    JsonObject root = new JsonObject();
    root.addProperty("enabled", enabled);
    writeModuleSettings(root);
    return root;
  }

  // === Optional value helpers (null = absent/inherit) ===

  /**
   * Gets an optional float value from a JSON object.
   * Returns null if the key is absent or null, allowing "inherit" semantics.
   *
   * @param obj JSON object
   * @param key property key
   * @return the float value, or null if absent/null
   */
  @Nullable
  protected Float getOptionalFloat(@NotNull JsonObject obj, @NotNull String key) {
    if (!obj.has(key) || obj.get(key).isJsonNull()) {
      return null;
    }
    return obj.get(key).getAsFloat();
  }

  /**
   * Gets an optional boolean value from a JSON object.
   * Returns null if the key is absent or null, allowing "inherit" semantics.
   *
   * @param obj JSON object
   * @param key property key
   * @return the boolean value, or null if absent/null
   */
  @Nullable
  protected Boolean getOptionalBool(@NotNull JsonObject obj, @NotNull String key) {
    if (!obj.has(key) || obj.get(key).isJsonNull()) {
      return null;
    }
    return obj.get(key).getAsBoolean();
  }

  /**
   * Loads module-specific settings from the JSON object.
   * The "enabled" field has already been loaded.
   *
   * @param root the root JSON object
   */
  protected abstract void loadModuleSettings(@NotNull JsonObject root);

  /**
   * Writes module-specific settings to the JSON object.
   * The "enabled" field has already been written.
   *
   * @param root the root JSON object
   */
  protected abstract void writeModuleSettings(@NotNull JsonObject root);
}
