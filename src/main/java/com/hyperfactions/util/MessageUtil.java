package com.hyperfactions.util;

import com.hyperfactions.config.ConfigManager;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

/**
 * Centralized message utilities for HyperFactions.
 *
 * <p>
 * Provides consistent message formatting across commands, GUI pages, and managers.
 * <ul>
 *   <li>Commands/broadcasts: {@link #error}, {@link #success}, {@link #info} (prefixed)</li>
 *   <li>GUI pages: {@link #errorText}, {@link #successText}, {@link #text} (unprefixed)</li>
 *   <li>Admin GUI pages: {@link #adminError}, {@link #adminSuccess}, {@link #adminInfo} (admin-prefixed)</li>
 * </ul>
 */
public final class MessageUtil {

  // === Color Constants ===
  public static final String COLOR_RED = "#FF5555";

  public static final String COLOR_GREEN = "#55FF55";

  public static final String COLOR_YELLOW = "#FFFF55";

  public static final String COLOR_GOLD = "#FFAA00";

  public static final String COLOR_CYAN = "#55FFFF";

  public static final String COLOR_BLUE = "#00AAFF";

  public static final String COLOR_WHITE = "#FFFFFF";

  public static final String COLOR_GRAY = "#AAAAAA";

  public static final String COLOR_DARK_GRAY = "#555555";

  private MessageUtil() {}

  // ==================== Prefix Builders ====================

  /**
   * Creates the standard HyperFactions message prefix using configured values.
   * Format: [PrefixText] with configurable colors for brackets and text.
   */
  @NotNull
  public static Message prefix() {
    ConfigManager config = ConfigManager.get();
    String text = config.getPrefixText();
    String textColor = config.getPrefixColor();
    String bracketColor = config.getPrefixBracketColor();

    return Message.raw("[").color(bracketColor)
      .insert(Message.raw(text).color(textColor))
      .insert(Message.raw("] ").color(bracketColor));
  }

  /**
   * Creates the admin message prefix: [Admin] using the configured bracket color.
   */
  @NotNull
  public static Message adminPrefix() {
    String bracketColor = ConfigManager.get().getPrefixBracketColor();

    return Message.raw("[").color(bracketColor)
      .insert(Message.raw("Admin").color(COLOR_GOLD))
      .insert(Message.raw("] ").color(bracketColor));
  }

  // ==================== i18n-aware (PlayerRef + key) ====================

  /**
   * Creates a prefixed red error message using i18n key resolution.
   *
   * @param player The player (for language resolution)
   * @param key    The message key
   * @param args   Replacement arguments for {0}, {1}, etc.
   */
  @NotNull
  public static Message error(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return prefix().insert(Message.raw(HFMessages.get(player, key, args)).color(COLOR_RED));
  }

  /**
   * Creates a prefixed green success message using i18n key resolution.
   */
  @NotNull
  public static Message success(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return prefix().insert(Message.raw(HFMessages.get(player, key, args)).color(COLOR_GREEN));
  }

  /**
   * Creates a prefixed info message with custom color using i18n key resolution.
   */
  @NotNull
  public static Message info(@NotNull PlayerRef player, @NotNull String key, @NotNull String color, Object... args) {
    return prefix().insert(Message.raw(HFMessages.get(player, key, args)).color(color));
  }

  /**
   * Creates a red error message (no prefix) using i18n key resolution.
   */
  @NotNull
  public static Message errorText(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return Message.raw(HFMessages.get(player, key, args)).color(COLOR_RED);
  }

  /**
   * Creates a green success message (no prefix) using i18n key resolution.
   */
  @NotNull
  public static Message successText(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return Message.raw(HFMessages.get(player, key, args)).color(COLOR_GREEN);
  }

  /**
   * Creates an admin-prefixed red error message using i18n key resolution.
   */
  @NotNull
  public static Message adminError(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return adminPrefix().insert(Message.raw(HFMessages.get(player, key, args)).color(COLOR_RED));
  }

  /**
   * Creates an admin-prefixed green success message using i18n key resolution.
   */
  @NotNull
  public static Message adminSuccess(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return adminPrefix().insert(Message.raw(HFMessages.get(player, key, args)).color(COLOR_GREEN));
  }

  /**
   * Creates an admin-prefixed gray info message using i18n key resolution.
   */
  @NotNull
  public static Message adminInfo(@NotNull PlayerRef player, @NotNull String key, Object... args) {
    return adminPrefix().insert(Message.raw(HFMessages.get(player, key, args)).color(COLOR_GRAY));
  }

  // ==================== Unprefixed (GUI pages) ====================

  /**
   * Creates a colored message with no prefix.
   * Use in GUI pages where context is obvious.
   */
  @NotNull
  public static Message text(@NotNull String text, @NotNull String color) {
    return Message.raw(text).color(color);
  }

  /**
   * Creates a red error message with no prefix.
   */
  @NotNull
  public static Message errorText(@NotNull String text) {
    return Message.raw(text).color(COLOR_RED);
  }

  /**
   * Creates a green success message with no prefix.
   */
  @NotNull
  public static Message successText(@NotNull String text) {
    return Message.raw(text).color(COLOR_GREEN);
  }

  // ==================== Prefixed (commands and broadcasts) ====================

  /**
   * Creates a prefixed red error message: [HyperFactions] Error text.
   */
  @NotNull
  public static Message error(@NotNull String text) {
    return prefix().insert(Message.raw(text).color(COLOR_RED));
  }

  /**
   * Creates a prefixed green success message: [HyperFactions] Success text.
   */
  @NotNull
  public static Message success(@NotNull String text) {
    return prefix().insert(Message.raw(text).color(COLOR_GREEN));
  }

  /**
   * Creates a prefixed message with custom color: [HyperFactions] Text.
   */
  @NotNull
  public static Message info(@NotNull String text, @NotNull String color) {
    return prefix().insert(Message.raw(text).color(color));
  }

  // ==================== Admin-prefixed (admin GUI pages) ====================

  /**
   * Creates an admin-prefixed red error message: [Admin] Error text.
   */
  @NotNull
  public static Message adminError(@NotNull String text) {
    return adminPrefix().insert(Message.raw(text).color(COLOR_RED));
  }

  /**
   * Creates an admin-prefixed green success message: [Admin] Success text.
   */
  @NotNull
  public static Message adminSuccess(@NotNull String text) {
    return adminPrefix().insert(Message.raw(text).color(COLOR_GREEN));
  }

  /**
   * Creates an admin-prefixed gray info message: [Admin] Info text.
   */
  @NotNull
  public static Message adminInfo(@NotNull String text) {
    return adminPrefix().insert(Message.raw(text).color(COLOR_GRAY));
  }
}
