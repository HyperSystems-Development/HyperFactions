package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;

/**
 * A typed content entry within a help topic.
 * Replaces raw string lines with explicit types so rendering
 * doesn't rely on fragile string-prefix detection.
 *
 * @param type       The visual type of this entry
 * @param messageKey The HelpMessages key for this entry's text (ignored for SPACER)
 */
public record HelpEntry(@NotNull EntryType type, @NotNull String messageKey) {

    /**
     * Visual types for help content lines.
     */
    public enum EntryType {
        /** Normal body text (#CCCCCC) */
        TEXT,
        /** Command callout (#FFFF55, bold) */
        COMMAND,
        /** Green tip/advice text (#55FF55) */
        TIP,
        /** Bold sub-heading within a card (#00AAAA) */
        HEADING,
        /** Visual separator (no text) */
        SPACER
    }

    /**
     * Gets the resolved display text for this entry.
     *
     * @return The localized text, or empty string for spacers
     */
    @NotNull
    public String text() {
        return type == EntryType.SPACER ? "" : HelpMessages.get(messageKey);
    }

    /** Creates a TEXT entry. */
    public static HelpEntry text(@NotNull String messageKey) {
        return new HelpEntry(EntryType.TEXT, messageKey);
    }

    /** Creates a COMMAND entry. */
    public static HelpEntry command(@NotNull String messageKey) {
        return new HelpEntry(EntryType.COMMAND, messageKey);
    }

    /** Creates a TIP entry. */
    public static HelpEntry tip(@NotNull String messageKey) {
        return new HelpEntry(EntryType.TIP, messageKey);
    }

    /** Creates a HEADING entry. */
    public static HelpEntry heading(@NotNull String messageKey) {
        return new HelpEntry(EntryType.HEADING, messageKey);
    }

    /** Creates a SPACER entry. */
    public static HelpEntry spacer() {
        return new HelpEntry(EntryType.SPACER, "");
    }
}
