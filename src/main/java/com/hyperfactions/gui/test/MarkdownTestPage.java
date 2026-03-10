package com.hyperfactions.gui.test;

import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.help.HelpEntry;
import com.hyperfactions.gui.help.HelpEntry.EntryType;
import com.hyperfactions.gui.shared.data.PlaceholderData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual test page that renders every supported markdown entry type
 * using the real help templates. Serves as both a verification tool
 * and documentation for markdown authors.
 *
 * <p>Open via: /f admin test md
 */
public class MarkdownTestPage extends InteractiveCustomUIPage<PlaceholderData> {

  // Template paths
  private static final String TPL_LINE_TEXT = UIPaths.HELP_LINE_TEXT;
  private static final String TPL_LINE_COMMAND = UIPaths.HELP_LINE_COMMAND;
  private static final String TPL_LINE_HEADING = UIPaths.HELP_LINE_HEADING;
  private static final String TPL_SPACER = UIPaths.HELP_SPACER;
  private static final String TPL_LINE_BOLD = UIPaths.HELP_LINE_BOLD;
  private static final String TPL_LINE_ITALIC = UIPaths.HELP_LINE_ITALIC;
  private static final String TPL_LINE_LIST = UIPaths.HELP_LINE_LIST;
  private static final String TPL_SEPARATOR = UIPaths.HELP_SEPARATOR;
  private static final String TPL_LINE_CALLOUT = UIPaths.HELP_LINE_CALLOUT;

  /** Creates a new MarkdownTestPage. */
  public MarkdownTestPage(PlayerRef playerRef) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlaceholderData.CODEC);
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.MARKDOWN_TEST);

    List<TestEntry> entries = buildTestEntries();
    int index = 0;

    for (TestEntry entry : entries) {
      if (entry.isSyntaxLabel) {
        // Syntax label — rendered as muted gray text
        cmd.append("#ContentList", TPL_LINE_TEXT);
        String selector = "#ContentList[" + index + "]";
        cmd.set(selector + " #Text.Text", entry.text);
        cmd.set(selector + " #Text.Style.TextColor", "#666666");
        cmd.set(selector + " #Text.Style.FontSize", 10);
        index++;
        continue;
      }

      // Real rendered entry using the appropriate template
      String template = getTemplateForType(entry.type);
      cmd.append("#ContentList", template);
      String selector = "#ContentList[" + index + "]";

      if (entry.type != EntryType.SPACER && entry.type != EntryType.SEPARATOR) {
        String text = entry.text;

        // Add bullet prefix for unordered list items
        if (entry.type == EntryType.LIST && !text.matches("^\\d+\\.\\s.*")) {
          text = "\u2022 " + text;
        }

        cmd.set(selector + " #Text.Text", text);

        // Apply color override
        if (entry.color != null) {
          cmd.set(selector + " #Text.Style.TextColor", entry.color);

          if (entry.type == EntryType.CALLOUT) {
            cmd.set(selector + " #AccentBar.Background.Color", entry.color);
          }
        }
      }
      index++;
    }
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                PlaceholderData data) {
    sendUpdate();
  }

  private String getTemplateForType(EntryType type) {
    return switch (type) {
      case TEXT -> TPL_LINE_TEXT;
      case COMMAND -> TPL_LINE_COMMAND;
      case HEADING -> TPL_LINE_HEADING;
      case SPACER -> TPL_SPACER;
      case BOLD -> TPL_LINE_BOLD;
      case ITALIC -> TPL_LINE_ITALIC;
      case LIST -> TPL_LINE_LIST;
      case SEPARATOR -> TPL_SEPARATOR;
      case CALLOUT -> TPL_LINE_CALLOUT;
    };
  }

  /**
   * Builds the comprehensive list of test entries.
   * Each section: gray syntax label, then the rendered result.
   */
  private List<TestEntry> buildTestEntries() {
    List<TestEntry> entries = new ArrayList<>();

    // ── Section: Basic Entry Types ──
    section(entries, "BASIC ENTRY TYPES");

    syntax(entries, "Plain text");
    entry(entries, EntryType.TEXT, "This is a plain text line.");

    syntax(entries, "Plain text (second line)");
    entry(entries, EntryType.TEXT, "Another text line to verify stacking.");

    syntax(entries, "(blank line)");
    entry(entries, EntryType.SPACER, "");

    syntax(entries, "## Sub-Heading");
    entry(entries, EntryType.HEADING, "Sub-Heading");

    syntax(entries, "`/f create <name>`");
    entry(entries, EntryType.COMMAND, "/f create <name>");

    syntax(entries, "`/f claim`");
    entry(entries, EntryType.COMMAND, "/f claim");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Text Formatting ──
    section(entries, "TEXT FORMATTING");

    syntax(entries, "**This text is bold**");
    entry(entries, EntryType.BOLD, "This text is bold");

    syntax(entries, "*This text is italicized*");
    entry(entries, EntryType.ITALIC, "This text is italicized");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Lists ──
    section(entries, "LISTS");

    syntax(entries, "- First bullet item");
    entry(entries, EntryType.LIST, "First bullet item");

    syntax(entries, "- Second bullet item");
    entry(entries, EntryType.LIST, "Second bullet item");

    syntax(entries, "- Third bullet item");
    entry(entries, EntryType.LIST, "Third bullet item");

    entry(entries, EntryType.SPACER, "");

    syntax(entries, "1. First numbered item");
    entry(entries, EntryType.LIST, "1. First numbered item");

    syntax(entries, "2. Second numbered item");
    entry(entries, EntryType.LIST, "2. Second numbered item");

    syntax(entries, "3. Third numbered item");
    entry(entries, EntryType.LIST, "3. Third numbered item");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Separators ──
    section(entries, "SEPARATORS");

    syntax(entries, "---");
    entry(entries, EntryType.SEPARATOR, "");

    syntax(entries, "Text after separator");
    entry(entries, EntryType.TEXT, "Content continues after the horizontal rule.");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Inline Hex Colors ──
    section(entries, "INLINE HEX COLORS");

    syntax(entries, "[#FF5555] Red text");
    colored(entries, "Red colored text", "#FF5555");

    syntax(entries, "[#55AAFF] Blue text");
    colored(entries, "Blue colored text", "#55AAFF");

    syntax(entries, "[#FFAA55] Orange text");
    colored(entries, "Orange colored text", "#FFAA55");

    syntax(entries, "[#AA55FF] Purple text");
    colored(entries, "Purple colored text", "#AA55FF");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Named Color Shortcuts ──
    section(entries, "NAMED COLOR SHORTCUTS");

    syntax(entries, "!warning This is a warning");
    colored(entries, "This is a warning", "#FF5555");

    syntax(entries, "!success This is a success message");
    colored(entries, "This is a success message", "#55FF55");

    syntax(entries, "!note This is a note");
    colored(entries, "This is a note", "#55AAFF");

    syntax(entries, "!muted This is muted/dimmed text");
    colored(entries, "This is muted/dimmed text", "#888888");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Callout Boxes ──
    section(entries, "CALLOUT BOXES");

    syntax(entries, "> This is a tip (shorthand)");
    callout(entries, "This is a tip", "#55FF55");

    syntax(entries, ">[!TIP] This is an explicit tip");
    callout(entries, "This is an explicit tip", "#55FF55");

    syntax(entries, ">[!WARNING] Don't log out while combat tagged!");
    callout(entries, "Don't log out while combat tagged!", "#FF5555");

    syntax(entries, ">[!INFO] Allies can access your chests");
    callout(entries, "Allies can access your chests", "#55AAFF");

    syntax(entries, ">[!NOTE] Officers can invite new members");
    callout(entries, "Officers can invite new members", "#FFAA55");

    syntax(entries, ">[!SUCCESS] Territory claimed successfully");
    callout(entries, "Territory claimed successfully", "#55FF55");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Edge Cases ──
    section(entries, "EDGE CASES");

    syntax(entries, "Long text line (wrapping test)");
    entry(entries, EntryType.TEXT,
        "This is a very long text line intended to test whether the help system properly handles text that extends beyond the visible width of the content container, requiring wrapping or truncation.");

    syntax(entries, "Long command (wrapping test)");
    entry(entries, EntryType.COMMAND,
        "/f admin economy set <faction> <amount> --confirm --force --reason \"testing\"");

    syntax(entries, "Long list item (wrapping test)");
    entry(entries, EntryType.LIST,
        "This is a long bullet point that tests how list items with significant amounts of text wrap within the indented list template.");

    syntax(entries, "Long callout (wrapping test)");
    callout(entries, "This is a very long callout box to verify that the text inside properly wraps within the callout container with its accent bar and padding.", "#55AAFF");

    entry(entries, EntryType.SPACER, "");

    // ── Section: Mixed Content Flow ──
    section(entries, "MIXED CONTENT FLOW");

    entry(entries, EntryType.TEXT, "Create a faction to get started with territory control.");
    entry(entries, EntryType.COMMAND, "/f create <name>");
    entry(entries, EntryType.TEXT, "Then claim your first chunk of land:");
    callout(entries, "Stand in the chunk you want to claim before running the command.", "#55FF55");

    entry(entries, EntryType.SPACER, "");
    entry(entries, EntryType.SPACER, "");

    syntax(entries, "Double spacer above, then heading after separator:");
    entry(entries, EntryType.SEPARATOR, "");
    entry(entries, EntryType.HEADING, "New Section After Rule");
    entry(entries, EntryType.TEXT, "Content in the new section.");

    return entries;
  }

  // ── Helper methods ──

  private void section(List<TestEntry> entries, String title) {
    entries.add(new TestEntry(EntryType.HEADING, title, null, false));
    entries.add(new TestEntry(EntryType.SEPARATOR, "", null, false));
  }

  private void syntax(List<TestEntry> entries, String markdown) {
    entries.add(new TestEntry(null, markdown, null, true));
  }

  private void entry(List<TestEntry> entries, EntryType type, String text) {
    entries.add(new TestEntry(type, text, null, false));
  }

  private void colored(List<TestEntry> entries, String text, String color) {
    entries.add(new TestEntry(EntryType.TEXT, text, color, false));
  }

  private void callout(List<TestEntry> entries, String text, String color) {
    entries.add(new TestEntry(EntryType.CALLOUT, text, color, false));
  }

  /**
   * A test entry that can either be a syntax label or a real rendered entry.
   */
  private record TestEntry(EntryType type, String text, String color, boolean isSyntaxLabel) {}
}
