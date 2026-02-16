package com.hyperfactions.gui.help.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.help.*;
import com.hyperfactions.gui.help.data.HelpPageData;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.FactionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main Help page with left category menu and scrollable content area.
 * Supports deep-linking from commands and can be used by both new players
 * and faction members.
 */
public class HelpMainPage extends InteractiveCustomUIPage<HelpPageData> {

    private static final String PAGE_ID = "help";

    // Template paths
    private static final String TPL_CATEGORY_HEADER = "HyperFactions/help/help_category_header.ui";
    private static final String TPL_TOPIC_HEADER = "HyperFactions/help/help_topic_header.ui";
    private static final String TPL_LINE_DEFAULT = "HyperFactions/help/help_line_default.ui";
    private static final String TPL_LINE_COMMAND = "HyperFactions/help/help_line_command.ui";
    private static final String TPL_LINE_BULLET = "HyperFactions/help/help_line_bullet.ui";
    private static final String TPL_LINE_DESC = "HyperFactions/help/help_line_desc.ui";
    private static final String TPL_SPACER_SMALL = "HyperFactions/help/help_spacer_small.ui";
    private static final String TPL_SPACER_LARGE = "HyperFactions/help/help_spacer_large.ui";

    private final PlayerRef playerRef;
    private final GuiManager guiManager;
    private final FactionManager factionManager;
    private final HelpCategory selectedCategory;
    private final Faction faction;

    /**
     * Creates a help page with the default category (GETTING_STARTED).
     */
    public HelpMainPage(@NotNull PlayerRef playerRef,
                        @NotNull GuiManager guiManager,
                        @NotNull FactionManager factionManager) {
        this(playerRef, guiManager, factionManager, HelpCategory.GETTING_STARTED);
    }

    /**
     * Creates a help page with a specific initial category.
     * Used for deep-linking from /f <command> help.
     */
    public HelpMainPage(@NotNull PlayerRef playerRef,
                        @NotNull GuiManager guiManager,
                        @NotNull FactionManager factionManager,
                        @NotNull HelpCategory initialCategory) {
        super(playerRef, CustomPageLifetime.CanDismiss, HelpPageData.CODEC);
        this.playerRef = playerRef;
        this.guiManager = guiManager;
        this.factionManager = factionManager;
        this.selectedCategory = initialCategory;
        this.faction = factionManager.getPlayerFaction(playerRef.getUuid());
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
                      UIEventBuilder events, Store<EntityStore> store) {

        // Load the main help template
        cmd.append("HyperFactions/help/help_main.ui");

        // Setup navigation bar based on player's faction status
        if (faction != null) {
            NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
        } else {
            NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
        }

        // Setup category buttons (disable selected, bind events to others)
        setupCategoryButtons(cmd, events);

        // Build content for selected category
        buildContent(cmd);
    }

    /**
     * Sets up the category buttons - disabling the selected one
     * and binding click events to the others.
     */
    private void setupCategoryButtons(UICommandBuilder cmd, UIEventBuilder events) {
        for (HelpCategory category : HelpCategory.values()) {
            int idx = category.ordinal();
            String buttonId = "#Cat" + idx;
            boolean isSelected = category == selectedCategory;

            if (isSelected) {
                // Disable the selected category button
                cmd.set(buttonId + ".Disabled", true);
            } else {
                // Bind click event for non-selected categories
                events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonId,
                    EventData.of("Button", "SelectCategory")
                        .append("Category", category.id())
                );
            }
        }
    }

    /**
     * Builds the content area for the selected category.
     * Uses separate template variants for different line styles.
     * IMPORTANT: Only .Text can be set dynamically - not .Style or .Anchor!
     */
    private void buildContent(UICommandBuilder cmd) {
        List<HelpTopic> topics = HelpRegistry.getInstance().getTopics(selectedCategory);

        int lineIndex = 0;

        // Category header (large cyan text)
        cmd.append("#ContentList", TPL_CATEGORY_HEADER);
        cmd.set("#ContentList[" + lineIndex + "] #Text.Text", selectedCategory.displayName().toUpperCase());
        lineIndex++;

        // Spacer after category header
        cmd.append("#ContentList", TPL_SPACER_LARGE);
        lineIndex++;

        // Build each topic
        for (HelpTopic topic : topics) {
            // Topic title (teal text)
            cmd.append("#ContentList", TPL_TOPIC_HEADER);
            cmd.set("#ContentList[" + lineIndex + "] #Text.Text", topic.title());
            lineIndex++;

            // Content lines
            for (String line : topic.lines()) {
                if (line.isEmpty()) {
                    // Empty line = small spacer
                    cmd.append("#ContentList", TPL_SPACER_SMALL);
                } else {
                    // Choose template based on line content
                    String template = getTemplateForLine(line);
                    cmd.append("#ContentList", template);
                    cmd.set("#ContentList[" + lineIndex + "] #Text.Text", line);
                }
                lineIndex++;
            }

            // Spacer after topic
            cmd.append("#ContentList", TPL_SPACER_LARGE);
            lineIndex++;
        }
    }

    /**
     * Returns the appropriate template path based on line content.
     */
    private String getTemplateForLine(String line) {
        if (line.startsWith("/f ")) {
            return TPL_LINE_COMMAND; // Yellow for commands
        } else if (line.startsWith("  ")) {
            return TPL_LINE_DESC; // Cyan-gray, indented for descriptions
        } else if (line.startsWith("- ")) {
            return TPL_LINE_BULLET; // Gray for bullet points
        }
        return TPL_LINE_DEFAULT; // Default gray
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                                HelpPageData data) {
        super.handleDataEvent(ref, store, data);

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            sendUpdate();
            return;
        }

        // Handle navigation bar events
        if (data.navBar != null && !data.navBar.isEmpty()) {
            if (faction != null) {
                if (NavBarHelper.handleNavEvent(data, player, ref, store, playerRef, faction, guiManager)) {
                    return;
                }
            } else {
                if (NewPlayerNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
                    return;
                }
            }
        }

        // Handle category selection
        if ("SelectCategory".equals(data.button) && data.category != null) {
            HelpCategory newCategory = HelpCategory.fromId(data.category);
            openWithCategory(player, ref, store, playerRef, newCategory);
            return;
        }

        // Default - just refresh
        sendUpdate();
    }

    /**
     * Opens the help page with a specific category selected.
     */
    private void openWithCategory(Player player, Ref<EntityStore> ref, Store<EntityStore> store,
                                  PlayerRef playerRef, HelpCategory category) {
        HelpMainPage newPage = new HelpMainPage(playerRef, guiManager, factionManager, category);
        player.getPageManager().openCustomPage(ref, store, newPage);
    }
}
