package com.hyperfactions.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a command help entry for display in help messages.
 *
 * @param command     the command syntax (e.g., "/f create {@code <name>}")
 * @param description the command description
 * @param section     optional section name for grouping (null for no section)
 */
public record CommandHelp(
  @NotNull String command,
  @NotNull String description,
  @Nullable String section
) implements Comparable<CommandHelp> {

  /**
   * Creates a command help entry without a section.
   */
  public CommandHelp(@NotNull String command, @NotNull String description) {
    this(command, description, null);
  }

  /**
   * Compares by section (nulls first), then by command.
   */
  @Override
  public int compareTo(@NotNull CommandHelp other) {
    // Null sections first
    if (this.section == null && other.section != null) {
      return -1;
    }
    if (this.section != null && other.section == null) {
      return 1;
    }

    // Both null or both non-null: compare sections
    if (this.section != null && other.section != null) {
      int sectionCmp = this.section.compareTo(other.section);
      if (sectionCmp != 0) {
        return sectionCmp;
      }
    }

    // Same section: compare commands
    return this.command.compareTo(other.command);
  }
}
