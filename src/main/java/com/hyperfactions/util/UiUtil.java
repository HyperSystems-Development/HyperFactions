package com.hyperfactions.util;

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for CustomUI text handling and input parsing.
 */
public final class UiUtil {

  private UiUtil() {}

  /**
   * Sanitizes text for safe use in inline CustomUI strings.
   * Removes/replaces characters that would break .ui parser syntax:
   * quotes, dollar signs (variable refs), semicolons, braces.
   *
   * @param text the text to sanitize (may be null)
   * @return sanitized text safe for appendInline, or empty string if null
   */
  @NotNull
  public static String sanitize(@Nullable String text) {
    if (text == null) {
      return "";
    }
    return text.replace("\"", "'").replace("$", "").replace(";", ",")
        .replace("{", "(").replace("}", ")");
  }

  /**
   * Parses a string amount from UI input into a BigDecimal.
   * Returns null if the input is null, blank, or not a valid number.
   *
   * @param value the string value from a TextField
   * @return the parsed amount, or null if invalid
   */
  @Nullable
  public static BigDecimal parseAmount(@Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
