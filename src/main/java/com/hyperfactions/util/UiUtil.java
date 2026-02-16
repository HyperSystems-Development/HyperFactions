package com.hyperfactions.util;

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
        if (text == null) return "";
        return text.replace("\"", "'").replace("$", "").replace(";", ",")
                .replace("{", "(").replace("}", ")");
    }

    /**
     * Parses a string amount from UI input into a double.
     * Returns -1 if the input is null, blank, or not a valid number.
     *
     * @param value the string value from a TextField
     * @return the parsed amount, or -1 if invalid
     */
    public static double parseAmount(@Nullable String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
