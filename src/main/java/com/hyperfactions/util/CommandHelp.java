package com.hyperfactions.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a command help entry for display in help messages.
 *
 * <p>The {@code descriptionKey} and {@code sectionKey} fields store i18n message keys
 * that are resolved at display time by {@link HelpFormatter} via {@link HFMessages}.
 *
 * @param command        the command syntax (e.g., "/f create {@code <name>}")
 * @param descriptionKey the i18n key for the command description
 * @param sectionKey     optional i18n key for the section name (null for no section)
 * @param sortOrder      controls display ordering (lower values first)
 */
public record CommandHelp(
  @NotNull String command,
  @NotNull String descriptionKey,
  @Nullable String sectionKey,
  int sortOrder
) implements Comparable<CommandHelp> {

  /**
   * Creates a command help entry without a section (sortOrder 0).
   */
  public CommandHelp(@NotNull String command, @NotNull String descriptionKey) {
    this(command, descriptionKey, null, 0);
  }

  /**
   * Creates a command help entry with a section (sortOrder 0).
   */
  public CommandHelp(@NotNull String command, @NotNull String descriptionKey, @Nullable String sectionKey) {
    this(command, descriptionKey, sectionKey, 0);
  }

  /**
   * Compares by sortOrder first, then by command name within same order.
   */
  @Override
  public int compareTo(@NotNull CommandHelp other) {
    int orderCmp = Integer.compare(this.sortOrder, other.sortOrder);
    if (orderCmp != 0) {
      return orderCmp;
    }
    return this.command.compareTo(other.command);
  }
}
