package com.hyperfactions.util;

import com.hyperfactions.config.ConfigManager;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
 * Per-player saved language preferences (from PlayerData) will be added
 * when the Player Settings GUI is implemented.
 *
 * <p>Usage:
 * <pre>
 *   HFMessages.get(playerRef, MessageKeys.Common.NO_PERMISSION);
 *   HFMessages.get(playerRef, MessageKeys.Create.SUCCESS, factionName);
 *   HFMessages.get(MessageKeys.Common.LOADING); // server language
 * </pre>
 */
public final class HFMessages {

  private HFMessages() {}

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

    // Use client language if enabled
    if (config.isUsePlayerLanguage()) {
      return player.getLanguage();
    }

    return serverDefault;
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
