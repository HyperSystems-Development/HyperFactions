package com.hyperfactions.util;

import com.hypixel.hytale.server.core.Message;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for formatting help messages in the HyperPerms standard style.
 */
public class HelpFormatter {

  // Standard colors matching HyperPerms
  private static final Color GOLD = new Color(255, 170, 0);

  private static final Color GREEN = new Color(85, 255, 85);

  private static final Color GRAY = Color.GRAY;

  private static final Color WHITE = Color.WHITE;

  private static final int WIDTH = 42;

  /**
   * Builds a formatted help message.
   *
   * @param title       the help title (e.g., "HyperFactions")
   * @param description optional plugin description
   * @param commands    list of command help entries
   * @param footer      optional footer message (e.g., "Use /f {@code <command>} --help for details")
   * @return formatted help message
   */
  public static Message buildHelp(
    @NotNull String title,
    @Nullable String description,
    @NotNull List<CommandHelp> commands,
    @Nullable String footer
  ) {
    List<Message> parts = new ArrayList<>();

    // Header with dashes
    int padding = WIDTH - title.length() - 2;
    int left = 3;
    int right = Math.max(3, padding - left);

    parts.add(Message.raw("-".repeat(left) + " ").color(GRAY));
    parts.add(Message.raw(title).color(GOLD));
    parts.add(Message.raw(" " + "-".repeat(right) + "\n").color(GRAY));

    // Description (if provided)
    if (description != null && !description.isEmpty()) {
      parts.add(Message.raw("  " + description + "\n\n").color(WHITE));
    }

    // Commands header
    parts.add(Message.raw("  Commands:\n").color(GOLD));

    // Sort commands and group by section
    List<CommandHelp> sorted = new ArrayList<>(commands);
    Collections.sort(sorted);

    String currentSection = null;
    for (CommandHelp cmd : sorted) {
      // Print section header if section changed
      if (cmd.section() != null && !cmd.section().equals(currentSection)) {
        if (currentSection != null) {
          parts.add(Message.raw("\n").color(WHITE)); // Blank line between sections
        }
        parts.add(Message.raw("  " + cmd.section() + ":\n").color(GOLD));
        currentSection = cmd.section();
      }

      // Print command
      parts.add(Message.raw("    " + cmd.command()).color(GREEN));
      parts.add(Message.raw(" - " + cmd.description() + "\n").color(WHITE));
    }

    // Footer (if provided)
    if (footer != null && !footer.isEmpty()) {
      parts.add(Message.raw("\n  " + footer + "\n").color(GRAY));
    }

    // Bottom border
    parts.add(Message.raw("-".repeat(WIDTH)).color(GRAY));

    return Message.join(parts.toArray(new Message[0]));
  }

  /**
   * Builds a simple help message without sections.
   *
   * @param title    the help title
   * @param commands list of command help entries
   * @return formatted help message
   */
  public static Message buildHelp(@NotNull String title, @NotNull List<CommandHelp> commands) {
    return buildHelp(title, null, commands, "Use /f <command> --help for details");
  }
}
