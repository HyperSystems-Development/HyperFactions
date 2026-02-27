package com.hyperfactions.util;

import com.hypixel.hytale.server.core.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Parses legacy Minecraft color codes and hex colors into Hytale's Message API.
 *
 * <p>Supported formats:
 * - &0-9, &a-f - Standard Minecraft color codes
 * - &k, &l, &m, &n, &o, &r - Format codes
 * - Section symbol variants (0-9, a-f, k-r)
 * - &#RRGGBB, &#RRGGBB - Hex color codes
 * - Spigot/Paper hex format
 */
public final class LegacyColorParser {

  private LegacyColorParser() {}

  /** Legacy color code to hex color mapping. */
  public static final Map<Character, String> LEGACY_COLORS = Map.ofEntries(
    Map.entry('0', "#000000"),  // Black
    Map.entry('1', "#0000AA"),  // Dark Blue
    Map.entry('2', "#00AA00"),  // Dark Green
    Map.entry('3', "#00AAAA"),  // Dark Aqua
    Map.entry('4', "#AA0000"),  // Dark Red
    Map.entry('5', "#AA00AA"),  // Dark Purple
    Map.entry('6', "#FFAA00"),  // Gold
    Map.entry('7', "#AAAAAA"),  // Gray
    Map.entry('8', "#555555"),  // Dark Gray
    Map.entry('9', "#5555FF"),  // Blue
    Map.entry('a', "#55FF55"),  // Green
    Map.entry('b', "#55FFFF"),  // Aqua
    Map.entry('c', "#FF5555"),  // Red
    Map.entry('d', "#FF55FF"),  // Light Purple
    Map.entry('e', "#FFFF55"),  // Yellow
    Map.entry('f', "#FFFFFF")   // White
  );

  /**
   * Converts a single-character legacy color code to a hex string.
   *
   * @param code the color code character (0-9, a-f)
   * @return hex string like "#55FFFF", defaults to "#FFFFFF" if unknown
   */
  @NotNull
  public static String codeToHex(char code) {
    return LEGACY_COLORS.getOrDefault(Character.toLowerCase(code), "#FFFFFF");
  }

  /**
   * Converts a hex color string (#RRGGBB) to the nearest legacy color code character.
   * Tries exact match first, then falls back to Euclidean RGB distance.
   *
   * @param hex the hex string (e.g., "#55FFFF")
   * @return the legacy code character (0-9, a-f), defaults to 'f' (white) if invalid
   */
  public static char hexToNearestLegacyCode(@NotNull String hex) {
    String normalized = hex.startsWith("#") ? hex.toUpperCase() : "#" + hex.toUpperCase();

    // Exact match first
    for (var entry : LEGACY_COLORS.entrySet()) {
      if (entry.getValue().equalsIgnoreCase(normalized)) {
        return entry.getKey();
      }
    }

    // Euclidean RGB distance
    int rgb = hexToRgbInt(normalized);
    int r = (rgb >> 16) & 0xFF;
    int g = (rgb >> 8) & 0xFF;
    int b = rgb & 0xFF;

    char nearest = 'f';
    int minDist = Integer.MAX_VALUE;
    for (var entry : LEGACY_COLORS.entrySet()) {
      int legacyRgb = hexToRgbInt(entry.getValue());
      int lr = (legacyRgb >> 16) & 0xFF;
      int lg = (legacyRgb >> 8) & 0xFF;
      int lb = legacyRgb & 0xFF;
      int dist = (r - lr) * (r - lr) + (g - lg) * (g - lg) + (b - lb) * (b - lb);
      if (dist < minDist) {
        minDist = dist;
        nearest = entry.getKey();
      }
    }
    return nearest;
  }

  /**
   * Converts a hex color string (#RRGGBB) to an RGB int.
   *
   * @param hex the hex string (e.g., "#55FFFF")
   * @return the RGB int value, defaults to 0xFFFFFF if invalid
   */
  public static int hexToRgbInt(@NotNull String hex) {
    try {
      if (hex.startsWith("#") && hex.length() >= 7) {
        return Integer.parseInt(hex.substring(1, 7), 16);
      }
    } catch (NumberFormatException ignored) {}
    return 0xFFFFFF;
  }

  /** Pattern for legacy color codes: & or section symbol followed by 0-9, a-f, or format codes. */
  private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("[&\u00A7]([0-9a-fA-FklmnorKLMNOR])");

  /** Pattern for hex colors: &#RRGGBB or &x&R&R&G&G&B&B (Spigot format). */
  private static final Pattern HEX_PATTERN = Pattern.compile("[&\u00A7]#([0-9a-fA-F]{6})");

  /** Pattern for Spigot-style hex: &x&R&R&G&G&B&B. */
  private static final Pattern SPIGOT_HEX_PATTERN = Pattern.compile("[&\u00A7]x([&\u00A7][0-9a-fA-F]){6}");

  /**
   * Parses a string containing legacy color codes into a Hytale Message.
   *
   * @param text the text to parse
   * @return a Message with proper coloring applied
   */
  @NotNull
  public static Message parse(@NotNull String text) {
    if (text.isEmpty()) {
      return Message.empty();
    }

    // First, convert Spigot hex format to standard hex format
    text = convertSpigotHex(text);

    // Parse segments
    List<TextSegment> segments = parseSegments(text);

    if (segments.isEmpty()) {
      return Message.raw(text);
    }

    // Build message from segments using insert()
    // Hytale Message API supports: bold(), italic(), monospace(), color()
    // Unsupported: underline, strikethrough, obfuscated (not exposed in API)
    Message result = Message.empty();

    for (TextSegment segment : segments) {
      if (segment.text.isEmpty()) {
        continue;
      }

      Message msg = Message.raw(segment.text);

      // Apply color if present
      if (segment.color != null) {
        msg = msg.color(segment.color);
      }

      // Apply supported formatting
      if (segment.bold) {
        msg = msg.bold(true);
      }
      if (segment.italic) {
        msg = msg.italic(true);
      }

      // Note: underline, strikethrough, obfuscated not exposed in Hytale Message API

      result = result.insert(msg);
    }

    return result;
  }

  /**
   * Converts Spigot-style hex codes (&x&R&R&G&G&B&B) to standard format (&#RRGGBB).
   */
  @NotNull
  private static String convertSpigotHex(@NotNull String text) {
    Matcher matcher = SPIGOT_HEX_PATTERN.matcher(text);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String match = matcher.group();
      // Extract the 6 hex characters
      StringBuilder hex = new StringBuilder("#");
      for (int i = 0; i < match.length(); i++) {
        char c = match.charAt(i);
        if (Character.isLetterOrDigit(c) && c != 'x' && c != 'X') {
          hex.append(Character.toLowerCase(c));
        }
      }
      if (hex.length() == 7) { // #RRGGBB
        matcher.appendReplacement(result, "&" + hex);
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Parses text into colored/formatted segments.
   */
  @NotNull
  private static List<TextSegment> parseSegments(@NotNull String text) {
    List<TextSegment> segments = new ArrayList<>();

    // Current formatting state
    String currentColor = null;
    boolean bold = false;
    boolean italic = false;
    boolean underlined = false;
    boolean strikethrough = false;
    boolean obfuscated = false;

    StringBuilder currentText = new StringBuilder();
    int i = 0;

    while (i < text.length()) {
      char c = text.charAt(i);

      // Check for color/format codes
      if ((c == '&' || c == '\u00A7') && i + 1 < text.length()) {
        char next = text.charAt(i + 1);

        // Check for hex color (&#RRGGBB)
        if (next == '#' && i + 8 <= text.length()) {
          String hexPart = text.substring(i + 2, i + 8);
          if (hexPart.matches("[0-9a-fA-F]{6}")) {
            // Save current segment
            if (currentText.length() > 0) {
              segments.add(new TextSegment(currentText.toString(), currentColor,
                bold, italic, underlined, strikethrough, obfuscated));
              currentText = new StringBuilder();
            }

            // Set new hex color
            currentColor = "#" + hexPart.toLowerCase();
            i += 8;
            continue;
          }
        }

        // Check for legacy color or format code
        char code = Character.toLowerCase(next);
        if (LEGACY_COLORS.containsKey(code)) {
          // Save current segment
          if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentColor,
              bold, italic, underlined, strikethrough, obfuscated));
            currentText = new StringBuilder();
          }

          // Set new color
          currentColor = LEGACY_COLORS.get(code);
          i += 2;
          continue;
        } else if (isFormatCode(code)) {
          // Save current segment (format codes don't reset color)
          if (currentText.length() > 0) {
            segments.add(new TextSegment(currentText.toString(), currentColor,
              bold, italic, underlined, strikethrough, obfuscated));
            currentText = new StringBuilder();
          }

          // Apply format
          switch (code) {
            case 'k' -> obfuscated = true;
            case 'l' -> bold = true;
            case 'm' -> strikethrough = true;
            case 'n' -> underlined = true;
            case 'o' -> italic = true;
            case 'r' -> {
              // Reset all formatting
              currentColor = null;
              bold = false;
              italic = false;
              underlined = false;
              strikethrough = false;
              obfuscated = false;
            }
            default -> throw new IllegalStateException("Unexpected value");
          }
          i += 2;
          continue;
        }
      }

      // Regular character
      currentText.append(c);
      i++;
    }

    // Add final segment
    if (currentText.length() > 0) {
      segments.add(new TextSegment(currentText.toString(), currentColor,
        bold, italic, underlined, strikethrough, obfuscated));
    }

    return segments;
  }

  /**
   * Checks if a character is a format code (not a color code).
   */
  private static boolean isFormatCode(char c) {
    return c == 'k' || c == 'l' || c == 'm' || c == 'n' || c == 'o' || c == 'r';
  }

  /**
   * Strips all color and format codes from a string.
   *
   * @param text the text to strip
   * @return the text without color codes
   */
  @NotNull
  public static String stripColors(@NotNull String text) {
    // Remove hex codes
    text = HEX_PATTERN.matcher(text).replaceAll("");
    text = SPIGOT_HEX_PATTERN.matcher(text).replaceAll("");
    // Remove legacy codes
    text = LEGACY_CODE_PATTERN.matcher(text).replaceAll("");
    return text;
  }

  /**
   * Represents a segment of text with associated formatting.
   */
  private record TextSegment(
    @NotNull String text,
    String color,
    boolean bold,
    boolean italic,
    boolean underlined,
    boolean strikethrough,
    boolean obfuscated
  ) {}
}
