package com.hyperfactions.gui.admin.page;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.AdminNavBarHelper;
import com.hyperfactions.gui.admin.data.AdminHelpData;
import com.hyperfactions.gui.help.*;
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
 * Admin Help page with sidebar navigation and card-based content area.
 * Mirrors the player help layout but shows only admin categories.
 */
public class AdminHelpPage extends InteractiveCustomUIPage<AdminHelpData> {

  private static final Pattern CELL_HEX_COLOR = Pattern.compile("^\\[#([0-9A-Fa-f]{6})]\\s*(.+)$");

  private final PlayerRef playerRef;

  private final GuiManager guiManager;

  private final HelpCategory selectedCategory;

  /** Creates a new AdminHelpPage with default category. */
  public AdminHelpPage(PlayerRef playerRef, GuiManager guiManager) {
    this(playerRef, guiManager, HelpCategory.ADMIN_OVERVIEW);
  }

  /** Creates a new AdminHelpPage with a specific category. */
  public AdminHelpPage(PlayerRef playerRef, GuiManager guiManager,
             @NotNull HelpCategory initialCategory) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminHelpData.CODEC);
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.selectedCategory = initialCategory.isAdmin() ? initialCategory : HelpCategory.ADMIN_OVERVIEW;
  }

  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_HELP);

    // Setup admin nav bar
    AdminNavBarHelper.setupBar(playerRef, "help", cmd, events);

    // Page title
    cmd.set("#PageTitle.Text", HFMessages.get(playerRef, MessageKeys.AdminGui.GUI_TITLE_HELP));

    // Set localized sidebar button labels (admin categories only)
    int catIdx = 0;
    for (HelpCategory category : HelpCategory.values()) {
      if (!category.isAdmin()) continue;
      cmd.set("#Cat" + catIdx + ".Text", "  " + category.displayName(playerRef));
      catIdx++;
    }

    // Setup category buttons
    setupCategoryButtons(cmd, events);

    // Set the category title header text and color
    cmd.set("#CategoryTitle.Text", selectedCategory.displayName(playerRef).toUpperCase());
    cmd.set("#CategoryTitle.Style.TextColor", selectedCategory.color());

    // Build topic cards
    buildTopicCards(cmd);
  }

  private void setupCategoryButtons(UICommandBuilder cmd, UIEventBuilder events) {
    int idx = 0;
    for (HelpCategory category : HelpCategory.values()) {
      if (!category.isAdmin()) continue;
      String buttonId = "#Cat" + idx;
      boolean isSelected = category == selectedCategory;

      if (isSelected) {
        cmd.set(buttonId + ".Disabled", true);
      } else {
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

  private void buildTopicCards(UICommandBuilder cmd) {
    List<HelpTopic> topics = HelpRegistry.getInstance().getTopics(selectedCategory);
    int cardIndex = 0;

    for (HelpTopic topic : topics) {
      cmd.append("#ContentList", UIPaths.HELP_TOPIC_CARD);
      String cardPrefix = "#ContentList[" + cardIndex + "]";

      cmd.set(cardPrefix + " #Title.Text", topic.title(playerRef));

      int lineIndex = 0;
      for (HelpEntry entry : topic.entries()) {
        String linesContainer = cardPrefix + " #Lines";

        if (entry.type() == HelpEntry.EntryType.TABLE_HEADER || entry.type() == HelpEntry.EntryType.TABLE_ROW) {
          boolean isHeader = entry.type() == HelpEntry.EntryType.TABLE_HEADER;
          String rowTemplate = isHeader ? UIPaths.HELP_TABLE_HEADER : UIPaths.HELP_TABLE_ROW;
          String cellTemplate = isHeader ? UIPaths.HELP_TABLE_HEADER_CELL : UIPaths.HELP_TABLE_CELL;

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

          if (entry.type() == HelpEntry.EntryType.LIST && !text.matches("^\\d+\\.\\s.*")) {
            text = "\u2022 " + text;
          }

          cmd.set(selector + " #Text.Text", text);

          if (entry.color() != null) {
            cmd.set(selector + " #Text.Style.TextColor", entry.color());
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

  private void applyCellFormatting(UICommandBuilder cmd, String cellSelector,
                   String text, @Nullable String rowColor) {
    String displayText = text;
    String cellColor = rowColor;
    boolean bold = false;
    boolean italic = false;

    Matcher hexMatcher = CELL_HEX_COLOR.matcher(displayText);
    if (hexMatcher.matches()) {
      cellColor = "#" + hexMatcher.group(1);
      displayText = hexMatcher.group(2);
    }

    if (displayText.startsWith("**") && displayText.endsWith("**") && displayText.length() > 4) {
      displayText = displayText.substring(2, displayText.length() - 2);
      bold = true;
    } else if (displayText.startsWith("`") && displayText.endsWith("`") && displayText.length() > 2) {
      displayText = displayText.substring(1, displayText.length() - 1);
      bold = true;
      if (cellColor == null) cellColor = "#FFFF55";
    } else if (displayText.startsWith("*") && displayText.endsWith("*") && displayText.length() > 2) {
      displayText = displayText.substring(1, displayText.length() - 1);
      italic = true;
    }

    cmd.set(cellSelector + " #CellText.Text", displayText);
    if (bold) cmd.set(cellSelector + " #CellText.Style.RenderBold", true);
    if (italic) cmd.set(cellSelector + " #CellText.Style.RenderItalics", true);
    if (cellColor != null) cmd.set(cellSelector + " #CellText.Style.TextColor", cellColor);
  }

  private String getTemplateForType(HelpEntry.EntryType type) {
    return switch (type) {
      case TEXT -> UIPaths.HELP_LINE_TEXT;
      case COMMAND -> UIPaths.HELP_LINE_COMMAND;
      case HEADING -> UIPaths.HELP_LINE_HEADING;
      case SPACER -> UIPaths.HELP_SPACER;
      case BOLD -> UIPaths.HELP_LINE_BOLD;
      case ITALIC -> UIPaths.HELP_LINE_ITALIC;
      case LIST -> UIPaths.HELP_LINE_LIST;
      case SEPARATOR -> UIPaths.HELP_SEPARATOR;
      case CALLOUT -> UIPaths.HELP_LINE_CALLOUT;
      case TABLE_HEADER -> UIPaths.HELP_TABLE_HEADER;
      case TABLE_ROW -> UIPaths.HELP_TABLE_ROW;
    };
  }

  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                AdminHelpData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || playerRef == null) {
      sendUpdate();
      return;
    }

    // Handle admin nav bar navigation
    if (AdminNavBarHelper.handleNavEvent(data, player, ref, store, playerRef, guiManager)) {
      return;
    }

    // Handle category selection
    if ("SelectCategory".equals(data.button) && data.category != null) {
      HelpCategory newCategory = HelpCategory.fromId(data.category);
      AdminHelpPage newPage = new AdminHelpPage(playerRef, guiManager, newCategory);
      player.getPageManager().openCustomPage(ref, store, newPage);
      return;
    }

    // Handle back button
    if (data.button != null && "Back".equals(data.button)) {
      guiManager.closePage(player, ref, store);
      return;
    }

    sendUpdate();
  }
}
