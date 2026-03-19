package com.hyperfactions.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility for formatting help messages in the HyperPerms standard style.
 *
 * <p>Resolves i18n keys through {@link HFMessages} when a {@link PlayerRef} is provided.
 */
public class HelpFormatter {

  // Standard colors matching HyperPerms
  private static final Color GOLD = new Color(255, 170, 0);

  private static final Color GREEN = new Color(85, 255, 85);

  private static final Color GRAY = Color.GRAY;

  private static final Color WHITE = Color.WHITE;

  private static final int WIDTH = 42;

  /**
   * Resolves a string through HFMessages if a player is provided.
   * Returns the raw string if player is null (server default language).
   */
  private static String resolve(@Nullable PlayerRef player, @NotNull String key) {
    return HFMessages.get(player, key);
  }

  /**
   * Builds a formatted help message with i18n support.
   *
   * @param titleKey       i18n key for the help title
   * @param descriptionKey optional i18n key for the description
   * @param commands       list of command help entries (descriptionKey/sectionKey are resolved)
   * @param footerKey      optional i18n key for the footer
   * @param player         the player (for language resolution, null for server default)
   * @return formatted help message
   */
  public static Message buildHelp(
    @NotNull String titleKey,
    @Nullable String descriptionKey,
    @NotNull List<CommandHelp> commands,
    @Nullable String footerKey,
    @Nullable PlayerRef player
  ) {
    List<Message> parts = new ArrayList<>();

    String title = resolve(player, titleKey);

    // Header with dashes
    int padding = WIDTH - title.length() - 2;
    int left = 3;
    int right = Math.max(3, padding - left);

    parts.add(Message.raw("-".repeat(left) + " ").color(GRAY));
    parts.add(Message.raw(title).color(GOLD));
    parts.add(Message.raw(" " + "-".repeat(right) + "\n").color(GRAY));

    // Description (if provided)
    if (descriptionKey != null && !descriptionKey.isEmpty()) {
      String description = resolve(player, descriptionKey);
      parts.add(Message.raw("  " + description + "\n\n").color(WHITE));
    }

    // Commands header
    String commandsLabel = resolve(player, HelpKeys.Help.COMMANDS_LABEL);
    parts.add(Message.raw("  " + commandsLabel + "\n").color(GOLD));

    // Sort commands and group by section
    List<CommandHelp> sorted = new ArrayList<>(commands);
    Collections.sort(sorted);

    String currentSectionKey = null;
    for (CommandHelp cmd : sorted) {
      // Print section header if section changed
      if (cmd.sectionKey() != null && !cmd.sectionKey().equals(currentSectionKey)) {
        if (currentSectionKey != null) {
          parts.add(Message.raw("\n").color(WHITE)); // Blank line between sections
        }
        String sectionName = resolve(player, cmd.sectionKey());
        parts.add(Message.raw("  " + sectionName + ":\n").color(GOLD));
        currentSectionKey = cmd.sectionKey();
      }

      // Print command
      parts.add(Message.raw("    " + cmd.command()).color(GREEN));
      String desc = resolve(player, cmd.descriptionKey());
      if (!desc.isEmpty()) {
        parts.add(Message.raw(" - " + desc + "\n").color(WHITE));
      } else {
        parts.add(Message.raw("\n").color(WHITE));
      }
    }

    // Footer (if provided)
    if (footerKey != null && !footerKey.isEmpty()) {
      String footer = resolve(player, footerKey);
      parts.add(Message.raw("\n  " + footer + "\n").color(GRAY));
    }

    // Bottom border
    parts.add(Message.raw("-".repeat(WIDTH)).color(GRAY));

    return Message.join(parts.toArray(new Message[0]));
  }

  /**
   * Builds a formatted help message (server default language).
   *
   * @param titleKey       i18n key for the title
   * @param descriptionKey optional i18n key for the description
   * @param commands       list of command help entries
   * @param footerKey      optional i18n key for the footer
   * @return formatted help message
   */
  public static Message buildHelp(
    @NotNull String titleKey,
    @Nullable String descriptionKey,
    @NotNull List<CommandHelp> commands,
    @Nullable String footerKey
  ) {
    return buildHelp(titleKey, descriptionKey, commands, footerKey, null);
  }

  /**
   * Builds a simple help message without description or footer (server default language).
   *
   * @param titleKey the i18n key for the title
   * @param commands list of command help entries
   * @return formatted help message
   */
  public static Message buildHelp(@NotNull String titleKey, @NotNull List<CommandHelp> commands) {
    return buildHelp(titleKey, null, commands, HelpKeys.Help.DEFAULT_FOOTER, null);
  }
}
