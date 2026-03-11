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

  private static final Pattern CELL_HEX_COLOR = Pattern.compile("^\\[#([0-9A-Fa-f]{6})]\\s*(.+)$");

  /**
   * Builds topic cards in the content area for the selected category.
   */
  private void buildTopicCards(UICommandBuilder cmd) {
    List<HelpTopic> topics = HelpRegistry.getInstance().getTopics(selectedCategory);
    int cardIndex = 0;

    for (HelpTopic topic : topics) {
      cmd.append("#ContentList", TPL_TOPIC_CARD);
      String cardPrefix = "#ContentList[" + cardIndex + "]";
      cmd.set(cardPrefix + " #Title.Text", topic.title(playerRef));

      int lineIndex = 0;
      for (HelpEntry entry : topic.entries()) {
        String linesContainer = cardPrefix + " #Lines";

        // Table entries: inline rows with calculated height and variable columns
        if (entry.type() == HelpEntry.EntryType.TABLE_HEADER || entry.type() == HelpEntry.EntryType.TABLE_ROW) {
          boolean isHeader = entry.type() == HelpEntry.EntryType.TABLE_HEADER;
          String[] columnKeys = entry.columnKeys();
          int numCols = columnKeys.length;

          // Resolve all cell texts for height estimation
          String[] cellTexts = new String[numCols];
          for (int col = 0; col < numCols; col++) {
            cellTexts[col] = HelpMessages.get(playerRef, columnKeys[col]);
          }
          int rowHeight = estimateTableRowHeight(cellTexts, numCols);

          cmd.appendInline(linesContainer, buildTableRowInline(rowHeight, numCols, isHeader));
          String rowSelector = linesContainer + "[" + lineIndex + "]";

          for (int col = 0; col < numCols; col++) {
            applyCellText(cmd, rowSelector, col, cellTexts[col], entry.color());
          }
          lineIndex++;
          continue;
        }

        String template = getTemplateForType(entry.type());
        cmd.append(linesContainer, template);
        String selector = linesContainer + "[" + lineIndex + "]";

        if (entry.type() != HelpEntry.EntryType.SPACER && entry.type() != HelpEntry.EntryType.SEPARATOR) {
          String text = entry.text(playerRef);

          if (entry.type() == HelpEntry.EntryType.LIST && !text.matches("^\\d+\\.\\s.*")) {
            text = "\u2022 " + text;
          }

          java.awt.Color baseColor = entry.color() != null
              ? java.awt.Color.decode(entry.color()) : null;
          cmd.set(selector + " #Text.TextSpans", HelpRichText.parse(text, baseColor));

          if (entry.color() != null && entry.type() == HelpEntry.EntryType.CALLOUT) {
            cmd.set(selector + " #AccentBar.Background.Color", entry.color());
          }
        }
        lineIndex++;
      }
      cardIndex++;
    }
  }

  /**
   * Sets text on a table cell Label (#Col0 or #Col1), handling [#RRGGBB] color prefix.
   */
  private void applyCellText(UICommandBuilder cmd, String rowSelector,
                 int col, String text, @Nullable String rowColor) {
    String displayText = text;
    java.awt.Color cellColor = rowColor != null ? java.awt.Color.decode(rowColor) : null;

    Matcher hexMatcher = CELL_HEX_COLOR.matcher(displayText);
    if (hexMatcher.matches()) {
      cellColor = java.awt.Color.decode("#" + hexMatcher.group(1));
      displayText = hexMatcher.group(2);
    }

    cmd.set(rowSelector + " #Col" + col + ".TextSpans", HelpRichText.parse(displayText, cellColor));
  }

  /** Column pixel widths for height estimation (includes last column). */
  private static int[] getColumnPixelWidths(int numCols) {
    return switch (numCols) {
      case 3 -> new int[]{170, 170, 280};
      case 4 -> new int[]{140, 140, 140, 190};
      default -> new int[]{217, 400};
    };
  }

  /** Fixed widths for non-last columns (last column uses Right anchor). */
  private static int[] getColumnFixedWidths(int numCols) {
    return switch (numCols) {
      case 3 -> new int[]{170, 170};
      case 4 -> new int[]{140, 140, 140};
      default -> new int[]{217};
    };
  }

  private static int estimateTableRowHeight(String[] cellTexts, int numCols) {
    int[] pixelWidths = getColumnPixelWidths(numCols);
    int maxLines = 1;
    for (int col = 0; col < Math.min(cellTexts.length, numCols); col++) {
      int charsPerLine = Math.max(6, pixelWidths[col] / 6);
      int lines = Math.max(1, (int) Math.ceil((double) cellTexts[col].length() / charsPerLine));
      maxLines = Math.max(maxLines, lines);
    }
    return Math.max(20, 4 + (maxLines * 13));
  }

  private static String buildTableRowInline(int height, int numCols, boolean isHeader) {
    String bg = isHeader ? "#141a28" : "#0f1520";
    String tc = isHeader ? "#DDDDDD" : "#CCCCCC";
    String bd = isHeader ? ", RenderBold: true" : "";
    String bh = "2";
    int[] widths = getColumnFixedWidths(numCols);

    StringBuilder sb = new StringBuilder();
    sb.append("Group { Anchor: (Height: ").append(height).append("); Background: (Color: ").append(bg).append("); ");

    int pos = 2;
    for (int col = 0; col < numCols; col++) {
      boolean last = (col == numCols - 1);
      String style = "Style: (FontSize: 10, TextColor: " + tc + bd + ", Wrap: true, VerticalAlignment: Center)";

      if (last) {
        sb.append("Label #Col").append(col).append(" { Text: \"\"; ").append(style).append("; ");
        sb.append("Padding: (Left: 10, Right: 8); ");
        sb.append("Anchor: (Left: ").append(pos).append(", Right: 2, Top: 0, Bottom: 0); } ");
      } else {
        sb.append("Group { Anchor: (Left: ").append(pos).append(", Width: ").append(widths[col]);
        sb.append(", Top: 0, Bottom: 0); ");
        sb.append("Label #Col").append(col).append(" { Text: \"\"; ").append(style).append("; ");
        sb.append("Padding: (Left: 10, Right: 6); ");
        sb.append("Anchor: (Left: 0, Right: 0, Top: 0, Bottom: 0); } } ");

        int sepPos = pos + widths[col] + 1;
        sb.append("Group { Anchor: (Width: 1, Left: ").append(sepPos);
        sb.append(", Top: 0, Bottom: 0); Background: (Color: #2a3a4a); } ");
        pos = sepPos + 2;
      }
    }

    if (isHeader) {
      sb.append("Group { Anchor: (Height: 1, Top: 0, Left: 0, Right: 0); Background: (Color: #2a3a4a); } ");
    }
    sb.append("Group { Anchor: (Height: ").append(bh).append(", Bottom: 0, Left: 0, Right: 0); Background: (Color: #2a3a4a); } ");
    sb.append("Group { Anchor: (Width: 1, Left: 0, Top: 0, Bottom: 0); Background: (Color: #2a3a4a); } ");
    sb.append("Group { Anchor: (Width: 1, Right: 0, Top: 0, Bottom: 0); Background: (Color: #2a3a4a); } ");
    sb.append("}");
    return sb.toString();
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
      case TABLE_HEADER, TABLE_ROW -> TPL_LINE_TEXT; // fallback, not reached
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
