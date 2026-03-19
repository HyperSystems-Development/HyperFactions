package com.hyperfactions.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.UuidUtil;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Manages player preferences for update notifications.
 * Stores a simple UUID -> boolean mapping in a JSON file to persist
 * notification preferences across server restarts.
 */
public final class UpdateNotificationPreferences {

  private static final String FILE_NAME = "notification-preferences.json";

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final Type MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();

  private final Path filePath;

  private final Map<UUID, Boolean> preferences = new ConcurrentHashMap<>();

  /**
   * Creates a new preferences manager.
   *
   * @param dataDirectory the plugin's data directory
   */
  public UpdateNotificationPreferences(@NotNull Path dataDirectory) {
    this.filePath = dataDirectory.resolve(FILE_NAME);
  }

  /**
   * Loads preferences from disk.
   */
  public void load() {
    if (!Files.exists(filePath)) {
      Logger.debug("[UpdatePrefs] No preferences file found, using defaults");
      return;
    }

    try {
      String json = Files.readString(filePath);
      Map<String, Boolean> loaded = GSON.fromJson(json, MAP_TYPE);
      if (loaded != null) {
        loaded.forEach((key, value) -> {
          UUID uuid = UuidUtil.parseOrNull(key);
          if (uuid != null) {
            preferences.put(uuid, value);
          } else {
            Logger.warn("[UpdatePrefs] Invalid UUID in preferences: %s", key);
          }
        });
      }
      Logger.debug("[UpdatePrefs] Loaded %d preferences", preferences.size());
    } catch (IOException e) {
      ErrorHandler.report("[UpdatePrefs] Failed to load preferences", e);
    }
  }

  /**
   * Saves preferences to disk.
   */
  public void save() {
    try {
      Map<String, Boolean> toSave = new ConcurrentHashMap<>();
      preferences.forEach((uuid, enabled) -> toSave.put(uuid.toString(), enabled));
      String json = GSON.toJson(toSave);
      Files.writeString(filePath, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      Logger.debug("[UpdatePrefs] Saved %d preferences", preferences.size());
    } catch (IOException e) {
      ErrorHandler.report("[UpdatePrefs] Failed to save preferences", e);
    }
  }

  /**
   * Checks if notifications are enabled for a player.
   * Returns true by default if no preference is set (opt-out model).
   *
   * @param uuid the player's UUID
   * @return true if notifications are enabled
   */
  public boolean isEnabled(@NotNull UUID uuid) {
    return preferences.getOrDefault(uuid, true);
  }

  /**
   * Sets whether notifications are enabled for a player.
   *
   * @param uuid    the player's UUID
   * @param enabled true to enable, false to disable
   */
  public void setEnabled(@NotNull UUID uuid, boolean enabled) {
    if (enabled) {
      // Remove from map if enabled (default is true, so we only store disabled)
      preferences.remove(uuid);
    } else {
      preferences.put(uuid, false);
    }

    // Save asynchronously to not block the main thread
    save();
  }

  /**
   * Checks if a preference has been explicitly set for a player.
   *
   * @param uuid the player's UUID
   * @return true if preference was explicitly set
   */
  public boolean hasPreference(@NotNull UUID uuid) {
    return preferences.containsKey(uuid);
  }
}
