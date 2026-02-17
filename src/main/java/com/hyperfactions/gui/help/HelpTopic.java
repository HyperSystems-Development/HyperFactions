package com.hyperfactions.gui.help;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents an individual help topic within a category.
 *
 * @param id        Unique identifier for this topic
 * @param titleKey  HelpMessages key for the display title
 * @param entries   Typed content entries to display
 * @param commands  Associated command names (for deep-linking from /f <command> help)
 * @param category  Parent category
 */
public record HelpTopic(
        @NotNull String id,
        @NotNull String titleKey,
        @NotNull List<HelpEntry> entries,
        @NotNull List<String> commands,
        @NotNull HelpCategory category
) {
    /**
     * Gets the resolved display title.
     */
    @NotNull
    public String title() {
        return HelpMessages.get(titleKey);
    }

    /**
     * Creates a topic with entries but no associated commands.
     */
    public static HelpTopic of(@NotNull String id, @NotNull String titleKey,
                               @NotNull List<HelpEntry> entries,
                               @NotNull HelpCategory category) {
        return new HelpTopic(id, titleKey, entries, List.of(), category);
    }

    /**
     * Creates a topic with both entries and associated commands.
     */
    public static HelpTopic withCommands(@NotNull String id, @NotNull String titleKey,
                                         @NotNull List<HelpEntry> entries,
                                         @NotNull List<String> commands,
                                         @NotNull HelpCategory category) {
        return new HelpTopic(id, titleKey, entries, commands, category);
    }
}
