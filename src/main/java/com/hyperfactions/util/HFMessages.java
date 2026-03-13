package com.hyperfactions.util;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.FactionLog;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized i18n message resolution for HyperFactions.
 *
 * <p>
 * Uses Hytale's native {@link I18nModule} for translations.
 * Supports server-wide language and per-player client language.
 *
 * <p>
 * Language resolution order:
 * <ol>
 *   <li>Player's client language via {@link PlayerRef#getLanguage()} (if {@code usePlayerLanguage=true})</li>
 *   <li>Server default language from config</li>
 * </ol>
 *
 * <p>
 * Per-player saved language preferences are cached via
 * {@link #setLanguageOverride(UUID, String)} when loaded from PlayerData.
 *
 * <p>Usage:
 * <pre>
 *   HFMessages.get(playerRef, CommonKeys.Common.NO_PERMISSION);
 *   HFMessages.get(playerRef, CommandKeys.Create.SUCCESS, factionName);
 *   HFMessages.get(CommonKeys.Common.LOADING); // server language
 * </pre>
 */
public final class HFMessages {

  /** Per-player language overrides from PlayerData preferences. */
  private static final Map<UUID, String> languageOverrides = new ConcurrentHashMap<>();

  private HFMessages() {}

  /**
   * Sets a language override for a player.
   * Called when preferences are loaded from PlayerData on connect,
   * or when the player changes their language in settings.
   *
   * @param uuid     The player's UUID
   * @param language The language code, or null to clear the override (auto-detect)
   */
  public static void setLanguageOverride(@NotNull UUID uuid, @Nullable String language) {
    if (language == null) {
      languageOverrides.remove(uuid);
    } else {
      languageOverrides.put(uuid, language);
    }
  }

  /**
   * Clears the language override for a player.
   * Called on player disconnect.
   *
   * @param uuid The player's UUID
   */
  public static void clearLanguageOverride(@NotNull UUID uuid) {
    languageOverrides.remove(uuid);
  }

  /**
   * Gets a translated message for a specific player.
   * Uses the player's resolved language (preference → client → server default).
   *
   * @param player The player (null falls back to server language)
   * @param key    The full message key (e.g. "hyperfactions.common.no_permission")
   * @param args   Replacement arguments for {0}, {1}, etc.
   * @return Translated and formatted message, or the key itself if not found
   */
  @NotNull
  public static String get(@Nullable PlayerRef player, @NotNull String key, Object... args) {
    String lang = getLanguageFor(player);
    return getForLanguage(lang, key, args);
  }

  /**
   * Gets a translated message using the server default language.
   *
   * @param key  The full message key
   * @param args Replacement arguments for {0}, {1}, etc.
   * @return Translated and formatted message
   */
  @NotNull
  public static String get(@NotNull String key, Object... args) {
    return get((PlayerRef) null, key, args);
  }

  /**
   * Gets a translated message for a specific language code.
   *
   * @param language The language code (e.g. "en-US", "es-ES")
   * @param key      The full message key
   * @param args     Replacement arguments
   * @return Translated and formatted message
   */
  @NotNull
  public static String getForLanguage(@NotNull String language, @NotNull String key, Object... args) {
    I18nModule i18n = I18nModule.get();
    if (i18n == null) {
      return formatFallback(key, args);
    }

    String message = i18n.getMessage(language, key);
    if (message == null) {
      // Try fallback to en-US
      message = i18n.getMessage("en-US", key);
    }
    if (message == null) {
      // Key not found — return key itself for debugging
      return key;
    }

    return format(message, args);
  }

  /**
   * Determines the language to use for a player.
   *
   * <p>Resolution order:
   * <ol>
   *   <li>Player's saved language preference (from PlayerData, cached in memory)</li>
   *   <li>Player's client language (if {@code usePlayerLanguage} enabled in config)</li>
   *   <li>Server default language</li>
   * </ol>
   *
   * @param player The player (null returns server default)
   * @return The resolved language code
   */
  @NotNull
  public static String getLanguageFor(@Nullable PlayerRef player) {
    ConfigManager config = ConfigManager.get();
    String serverDefault = config.getDefaultLanguage();

    if (player == null) {
      return serverDefault;
    }

    // Check saved language preference first
    String override = languageOverrides.get(player.getUuid());
    if (override != null) {
      return override;
    }

    // Use client language if enabled
    if (config.isUsePlayerLanguage()) {
      return player.getLanguage();
    }

    return serverDefault;
  }

  /**
   * Resolves a FactionLog's message for display, using the i18n key if available.
   * Falls back to the English message for legacy logs without a messageKey.
   *
   * @param player the player viewing the log (determines locale)
   * @param log    the faction log entry
   * @return the localized message, or the English fallback
   */
  @NotNull
  public static String resolveLogMessage(@Nullable PlayerRef player, @NotNull FactionLog log) {
    if (log.messageKey() != null) {
      Object[] args = log.messageArgs() != null ? log.messageArgs().toArray() : new Object[0];
      return get(player, log.messageKey(), args);
    }
    return log.message();
  }

  /**
   * Formats a message by replacing {0}, {1}, etc. with provided arguments.
   */
  @NotNull
  private static String format(@NotNull String message, Object... args) {
    if (args == null || args.length == 0) {
      return message;
    }

    String result = message;
    for (int i = 0; i < args.length; i++) {
      String placeholder = "{" + i + "}";
      String replacement = args[i] != null ? args[i].toString() : "";
      result = result.replace(placeholder, replacement);
    }
    return result;
  }

  /**
   * Fallback formatting when I18nModule is not available.
   */
  @NotNull
  private static String formatFallback(@NotNull String key, Object... args) {
    StringBuilder sb = new StringBuilder(key);
    if (args != null && args.length > 0) {
      sb.append(": ");
      for (Object arg : args) {
        sb.append(arg).append(" ");
      }
    }
    return sb.toString().trim();
  }
}
