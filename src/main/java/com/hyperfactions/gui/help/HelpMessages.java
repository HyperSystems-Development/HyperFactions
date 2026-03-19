package com.hyperfactions.gui.help;

import com.hyperfactions.util.HFMessages;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Key-based string store for all help content.
 * Delegates to {@link HFMessages} for i18n resolution via Hytale's I18nModule.
 *
 * <p>Help content keys are prefixed {@code hyperfactions_help.} (auto-prefixed by
 * I18nModule from the {@code hyperfactions_help.lang} filename).
 *
 * <p>The .lang file is build-generated from markdown sources in {@code src/main/help/}.
 */
public final class HelpMessages {

  private HelpMessages() {}

  /**
   * Gets the localized string for a help message key.
   * Uses server default language.
   *
   * @param key The full message key (e.g. "hyperfactions_help.welcome.getting_started.title")
   * @return The localized string, or the key itself if not found
   */
  @NotNull
  public static String get(@NotNull String key) {
    return HFMessages.get((PlayerRef) null, key);
  }

  /**
   * Gets the localized string for a help message key, resolved for a specific player's language.
   *
   * @param player The player (null for server default)
   * @param key    The full message key
   * @return The localized string, or the key itself if not found
   */
  @NotNull
  public static String get(@Nullable PlayerRef player, @NotNull String key) {
    return HFMessages.get(player, key);
  }
}
