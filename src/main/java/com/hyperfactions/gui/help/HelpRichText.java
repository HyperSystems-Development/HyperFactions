package com.hyperfactions.gui.help;

import com.hypixel.hytale.server.core.Message;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parses inline markdown markers within help text and builds a {@link Message}
 * with proper formatting (bold, italic, colored command references).
 *
 * <p>Supported inline markers:
 * <ul>
 *   <li>{@code **bold text**} → bold</li>
 *   <li>{@code `command`} → yellow bold (command style)</li>
 *   <li>{@code *italic text*} → italic</li>
 *   <li>{@code --} → em-dash (—)</li>
 * </ul>
 *
 * <p>Used by both {@code HelpMainPage} and {@code AdminHelpPage} to render
 * rich text within Labels via the {@code TextSpans} property.
 */
public final class HelpRichText {

  /** Command color: yellow (#FFFF55) matching the COMMAND entry style. */
  private static final Color CMD_COLOR = new Color(0xFF, 0xFF, 0x55);

  /**
   * Tokenizer pattern that matches inline markers in order of priority:
   * <ol>
   *   <li>{@code **...** } bold (non-greedy)</li>
   *   <li>{@code `...`} code/command (non-greedy)</li>
   *   <li>{@code *...*} italic (not preceded/followed by *)</li>
   * </ol>
   */
  private static final Pattern INLINE_PATTERN = Pattern.compile(
      "\\*\\*(.+?)\\*\\*"       // Group 1: bold
      + "|`(.+?)`"              // Group 2: code
      + "|(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)" // Group 3: italic
  );

  private HelpRichText() {}

  /**
   * Parses inline markdown in the given text and returns a formatted {@link Message}.
   *
   * @param text      the raw text with inline markers
   * @param baseColor the default text color (null for default label color)
   * @return a composed Message with inline formatting applied
   */
  public static @NotNull Message parse(@NotNull String text, @Nullable Color baseColor) {
    // Replace em-dashes first
    text = text.replace(" -- ", " \u2014 ");

    Matcher matcher = INLINE_PATTERN.matcher(text);
    List<Message> parts = new ArrayList<>();
    int lastEnd = 0;

    while (matcher.find()) {
      // Add any plain text before this match
      if (matcher.start() > lastEnd) {
        String plain = text.substring(lastEnd, matcher.start());
        Message plainMsg = Message.raw(plain);
        if (baseColor != null) plainMsg = plainMsg.color(baseColor);
        parts.add(plainMsg);
      }

      if (matcher.group(1) != null) {
        // Bold: **text**
        Message boldMsg = Message.raw(matcher.group(1)).bold(true);
        if (baseColor != null) boldMsg = boldMsg.color(baseColor);
        parts.add(boldMsg);
      } else if (matcher.group(2) != null) {
        // Code/Command: `text` → yellow bold
        parts.add(Message.raw(matcher.group(2)).color(CMD_COLOR).bold(true));
      } else if (matcher.group(3) != null) {
        // Italic: *text*
        Message italicMsg = Message.raw(matcher.group(3)).italic(true);
        if (baseColor != null) italicMsg = italicMsg.color(baseColor);
        parts.add(italicMsg);
      }

      lastEnd = matcher.end();
    }

    // Add remaining plain text after last match
    if (lastEnd < text.length()) {
      String remaining = text.substring(lastEnd);
      Message remainMsg = Message.raw(remaining);
      if (baseColor != null) remainMsg = remainMsg.color(baseColor);
      parts.add(remainMsg);
    }

    // If no matches found, return plain text
    if (parts.isEmpty()) {
      Message plainMsg = Message.raw(text);
      if (baseColor != null) plainMsg = plainMsg.color(baseColor);
      return plainMsg;
    }

    return Message.join(parts.toArray(new Message[0]));
  }

  /**
   * Convenience overload using default label color.
   */
  public static @NotNull Message parse(@NotNull String text) {
    return parse(text, null);
  }
}
