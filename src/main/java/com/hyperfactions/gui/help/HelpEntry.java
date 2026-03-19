package com.hyperfactions.gui.help;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A typed content entry within a help topic.
 * Replaces raw string lines with explicit types so rendering
 * doesn't rely on fragile string-prefix detection.
 *
 * @param type       The visual type of this entry
 * @param messageKey The HelpMessages key for this entry's text (ignored for SPACER/SEPARATOR)
 * @param color      Optional color override (hex string like "#FF5555"), null for default
 */
public record HelpEntry(@NotNull EntryType type, @NotNull String messageKey, @Nullable String color) {

  /**
   * Visual types for help content lines.
   */
  public enum EntryType {
    /** Normal body text (#CCCCCC). */
    TEXT,
    /** Command callout (#FFFF55, bold). */
    COMMAND,
    /** Bold sub-heading within a card (#00AAAA). */
    HEADING,
    /** Visual separator (no text). */
    SPACER,
    /** Bold text (#CCCCCC, bold). */
    BOLD,
    /** Italic text (#CCCCCC, italic). */
    ITALIC,
    /** List item with indent (#CCCCCC). */
    LIST,
    /** Horizontal rule separator (no text). */
    SEPARATOR,
    /** Boxed callout with colored accent bar. */
    CALLOUT,
    /** Table header row (bold column labels). Column keys pipe-separated in messageKey. */
    TABLE_HEADER,
    /** Table data row. Column keys pipe-separated in messageKey. */
    TABLE_ROW
  }

  /**
   * Gets the resolved display text for this entry (server default language).
   *
   * @return The localized text, or empty string for spacers/separators/tables
   */
  @NotNull
  public String text() {
    return switch (type) {
      case SPACER, SEPARATOR, TABLE_HEADER, TABLE_ROW -> "";
      default -> HelpMessages.get(messageKey);
    };
  }

  /**
   * Gets the resolved display text for a specific player's language.
   */
  @NotNull
  public String text(@Nullable PlayerRef playerRef) {
    return switch (type) {
      case SPACER, SEPARATOR, TABLE_HEADER, TABLE_ROW -> "";
      default -> HelpMessages.get(playerRef, messageKey);
    };
  }

  /**
   * Gets the individual column keys for table entries.
   * For non-table entries, returns an empty array.
   */
  @NotNull
  public String[] columnKeys() {
    return type == EntryType.TABLE_HEADER || type == EntryType.TABLE_ROW
        ? messageKey.split("\\|") : new String[0];
  }

  /** Creates a TEXT entry. */
  public static HelpEntry text(@NotNull String messageKey) {
    return new HelpEntry(EntryType.TEXT, messageKey, null);
  }

  /** Creates a COMMAND entry. */
  public static HelpEntry command(@NotNull String messageKey) {
    return new HelpEntry(EntryType.COMMAND, messageKey, null);
  }

  /** Creates a HEADING entry. */
  public static HelpEntry heading(@NotNull String messageKey) {
    return new HelpEntry(EntryType.HEADING, messageKey, null);
  }

  /** Creates a SPACER entry. */
  public static HelpEntry spacer() {
    return new HelpEntry(EntryType.SPACER, "", null);
  }

  /** Creates a BOLD entry. */
  public static HelpEntry bold(@NotNull String messageKey) {
    return new HelpEntry(EntryType.BOLD, messageKey, null);
  }

  /** Creates an ITALIC entry. */
  public static HelpEntry italic(@NotNull String messageKey) {
    return new HelpEntry(EntryType.ITALIC, messageKey, null);
  }

  /** Creates a LIST entry. */
  public static HelpEntry list(@NotNull String messageKey) {
    return new HelpEntry(EntryType.LIST, messageKey, null);
  }

  /** Creates a SEPARATOR entry. */
  public static HelpEntry separator() {
    return new HelpEntry(EntryType.SEPARATOR, "", null);
  }

  /** Creates a CALLOUT entry with a color. */
  public static HelpEntry callout(@NotNull String messageKey, @Nullable String color) {
    return new HelpEntry(EntryType.CALLOUT, messageKey, color);
  }

  /** Creates a TEXT entry with a custom color. */
  public static HelpEntry colored(@NotNull String messageKey, @NotNull String color) {
    return new HelpEntry(EntryType.TEXT, messageKey, color);
  }

  /** Creates a TABLE_HEADER entry with pipe-separated column keys. */
  public static HelpEntry tableHeader(@NotNull String columnKeys) {
    return new HelpEntry(EntryType.TABLE_HEADER, columnKeys, null);
  }

  /** Creates a TABLE_ROW entry with pipe-separated column keys. */
  public static HelpEntry tableRow(@NotNull String columnKeys) {
    return new HelpEntry(EntryType.TABLE_ROW, columnKeys, null);
  }
}
