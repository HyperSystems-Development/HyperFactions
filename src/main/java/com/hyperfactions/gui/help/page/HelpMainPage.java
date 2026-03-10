package com.hyperfactions.gui.help.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.help.*;
import com.hyperfactions.gui.help.data.HelpPageData;
import com.hyperfactions.gui.newplayer.NewPlayerNavBarHelper;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main Help page with colored sidebar navigation and card-based content area.
 * Supports deep-linking from commands and works for both new players and faction members.
 */
public class HelpMainPage extends InteractiveCustomUIPage<HelpPageData> {

  private static final String PAGE_ID = "help";

  // Template paths
  private static final String TPL_TOPIC_CARD = UIPaths.HELP_TOPIC_CARD;

  private static final String TPL_LINE_TEXT = UIPaths.HELP_LINE_TEXT;

  private static final String TPL_LINE_COMMAND = UIPaths.HELP_LINE_COMMAND;

  private static final String TPL_LINE_HEADING = UIPaths.HELP_LINE_HEADING;

  private static final String TPL_SPACER = UIPaths.HELP_SPACER;

  private static final String TPL_LINE_BOLD = UIPaths.HELP_LINE_BOLD;

  private static final String TPL_LINE_ITALIC = UIPaths.HELP_LINE_ITALIC;

  private static final String TPL_LINE_LIST = UIPaths.HELP_LINE_LIST;

  private static final String TPL_SEPARATOR = UIPaths.HELP_SEPARATOR;

  private static final String TPL_LINE_CALLOUT = UIPaths.HELP_LINE_CALLOUT;

  private static final String TPL_TABLE_HEADER = UIPaths.HELP_TABLE_HEADER;

  private static final String TPL_TABLE_ROW = UIPaths.HELP_TABLE_ROW;

  private static final String TPL_TABLE_HEADER_CELL = UIPaths.HELP_TABLE_HEADER_CELL;

  private static final String TPL_TABLE_CELL = UIPaths.HELP_TABLE_CELL;

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  private final FactionManager factionManager;

  private final HelpCategory selectedCategory;

  private final Faction faction;

  /**
   * Creates a help page with the default category (WELCOME).
   */
  public HelpMainPage(@NotNull PlayerRef playerRef,
            @NotNull GuiManager guiManager,
            @NotNull FactionManager factionManager) {
    this(playerRef, guiManager, factionManager, HelpCategory.WELCOME);
  }

  /**
   * Creates a help page with a specific initial category.
   * Used for deep-linking from /f {@code <command>} help.
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

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the main help template
    cmd.append(UIPaths.HELP_MAIN);

    // Setup navigation bar based on player's faction status
    if (faction != null) {
      NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);
    } else {
      NewPlayerNavBarHelper.setupBar(playerRef, PAGE_ID, cmd, events);
    }

    // Page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.HelpGui.HELP_CENTER_TITLE));

    // Set localized sidebar button labels (player categories only)
    int catIdx = 0;
    for (HelpCategory category : HelpCategory.values()) {
      if (category.isAdmin()) continue;
      cmd.set("#Cat" + catIdx + ".Text", "  " + category.displayName(playerRef));
      catIdx++;
    }

    // Setup category buttons (disable selected, bind events to others)
    setupCategoryButtons(cmd, events);

    // Set the category title header text and color
    cmd.set("#CategoryTitle.Text", selectedCategory.displayName(playerRef).toUpperCase());
    cmd.set("#CategoryTitle.Style.TextColor", selectedCategory.color());

    // Build topic cards for selected category
    buildTopicCards(cmd);
  }

  /**
   * Sets up the 7 category buttons - disabling the selected one
   * and binding click events to the others.
   */
  private void setupCategoryButtons(UICommandBuilder cmd, UIEventBuilder events) {
    int idx = 0;
    for (HelpCategory category : HelpCategory.values()) {
      if (category.isAdmin()) continue;
      String buttonId = "#Cat" + idx;
      boolean isSelected = category == selectedCategory;

      if (isSelected) {
        // Disable the selected category button (shows accent color via style)
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
      idx++;
    }
  }

  /**
   * Builds topic cards in the content area for the selected category.
   */
  private void buildTopicCards(UICommandBuilder cmd) {
    List<HelpTopic> topics = HelpRegistry.getInstance().getTopics(selectedCategory);
    int cardIndex = 0;

    for (HelpTopic topic : topics) {
      // Append card template
      cmd.append("#ContentList", TPL_TOPIC_CARD);
      String cardPrefix = "#ContentList[" + cardIndex + "]";

      // Set card title
      cmd.set(cardPrefix + " #Title.Text", topic.title(playerRef));

      // Append lines into card's #Lines container
      int lineIndex = 0;
      for (HelpEntry entry : topic.entries()) {
        String linesContainer = cardPrefix + " #Lines";

        // Table entries need special rendering
        if (entry.type() == HelpEntry.EntryType.TABLE_HEADER || entry.type() == HelpEntry.EntryType.TABLE_ROW) {
          boolean isHeader = entry.type() == HelpEntry.EntryType.TABLE_HEADER;
          String rowTemplate = isHeader ? TPL_TABLE_HEADER : TPL_TABLE_ROW;
          String cellTemplate = isHeader ? TPL_TABLE_HEADER_CELL : TPL_TABLE_CELL;

          cmd.append(linesContainer, rowTemplate);
          String rowSelector = linesContainer + "[" + lineIndex + "]";
          String colsContainer = rowSelector + " #Cols";

          String[] columnKeys = entry.columnKeys();
          for (int col = 0; col < columnKeys.length; col++) {
            cmd.append(colsContainer, cellTemplate);
            String cellSelector = colsContainer + "[" + col + "]";
            String cellText = HelpMessages.get(playerRef, columnKeys[col]);
            applyCellFormatting(cmd, cellSelector, cellText, entry.color());
          }

          lineIndex++;
          continue;
        }

        String template = getTemplateForType(entry.type());
        cmd.append(linesContainer, template);

        String selector = linesContainer + "[" + lineIndex + "]";

        if (entry.type() != HelpEntry.EntryType.SPACER && entry.type() != HelpEntry.EntryType.SEPARATOR) {
          String text = entry.text(playerRef);

          // Add bullet prefix for unordered list items
          if (entry.type() == HelpEntry.EntryType.LIST && !text.matches("^\\d+\\.\\s.*")) {
            text = "\u2022 " + text;
          }

          cmd.set(selector + " #Text.Text", text);

          // Apply color override if present
          if (entry.color() != null) {
            cmd.set(selector + " #Text.Style.TextColor", entry.color());

            // For callouts, also color the accent bar
            if (entry.type() == HelpEntry.EntryType.CALLOUT) {
              cmd.set(selector + " #AccentBar.Background.Color", entry.color());
            }
          }
        }
        lineIndex++;
      }
      cardIndex++;
    }
  }

  /**
   * Returns the appropriate template path for an entry type.
   */
  private static final Pattern CELL_HEX_COLOR = Pattern.compile("^\\[#([0-9A-Fa-f]{6})]\\s*(.+)$");

  /**
   * Applies inline formatting to a table cell.
   * Supports: **bold**, *italic*, `command`, [#RRGGBB] color prefix.
   */
  private void applyCellFormatting(UICommandBuilder cmd, String cellSelector,
                   String text, @Nullable String rowColor) {
    String displayText = text;
    String cellColor = rowColor;
    boolean bold = false;
    boolean italic = false;

    // Check for inline hex color: [#RRGGBB] text
    Matcher hexMatcher = CELL_HEX_COLOR.matcher(displayText);
    if (hexMatcher.matches()) {
      cellColor = "#" + hexMatcher.group(1);
      displayText = hexMatcher.group(2);
    }

    // Check for bold: **text**
    if (displayText.startsWith("**") && displayText.endsWith("**") && displayText.length() > 4) {
      displayText = displayText.substring(2, displayText.length() - 2);
      bold = true;
    }
    // Check for command: `text`
    else if (displayText.startsWith("`") && displayText.endsWith("`") && displayText.length() > 2) {
      displayText = displayText.substring(1, displayText.length() - 1);
      bold = true;
      if (cellColor == null) {
        cellColor = "#FFFF55";
      }
    }
    // Check for italic: *text*
    else if (displayText.startsWith("*") && displayText.endsWith("*") && displayText.length() > 2) {
      displayText = displayText.substring(1, displayText.length() - 1);
      italic = true;
    }

    cmd.set(cellSelector + " #CellText.Text", displayText);
    if (bold) {
      cmd.set(cellSelector + " #CellText.Style.RenderBold", true);
    }
    if (italic) {
      cmd.set(cellSelector + " #CellText.Style.RenderItalics", true);
    }
    if (cellColor != null) {
      cmd.set(cellSelector + " #CellText.Style.TextColor", cellColor);
    }
  }

  private String getTemplateForType(HelpEntry.EntryType type) {
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
      case TABLE_HEADER -> TPL_TABLE_HEADER;
      case TABLE_ROW -> TPL_TABLE_ROW;
    };
  }

  /** Handles data event. */
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
