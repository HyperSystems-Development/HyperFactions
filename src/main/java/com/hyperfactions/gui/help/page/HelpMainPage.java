package com.hyperfactions.gui.help.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.help.*;
import com.hyperfactions.gui.help.data.HelpPageData;
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
import java.util.List;
import org.jetbrains.annotations.NotNull;

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

  private static final String TPL_LINE_TIP = UIPaths.HELP_LINE_TIP;

  private static final String TPL_LINE_HEADING = UIPaths.HELP_LINE_HEADING;

  private static final String TPL_SPACER = UIPaths.HELP_SPACER;

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

    // Setup category buttons (disable selected, bind events to others)
    setupCategoryButtons(cmd, events);

    // Set the category title header text and color
    cmd.set("#CategoryTitle.Text", selectedCategory.displayName().toUpperCase());
    cmd.set("#CategoryTitle.Style.TextColor", selectedCategory.color());

    // Build topic cards for selected category
    buildTopicCards(cmd);
  }

  /**
   * Sets up the 7 category buttons - disabling the selected one
   * and binding click events to the others.
   */
  private void setupCategoryButtons(UICommandBuilder cmd, UIEventBuilder events) {
    for (HelpCategory category : HelpCategory.values()) {
      int idx = category.ordinal();
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
      cmd.set(cardPrefix + " #Title.Text", topic.title());

      // Append lines into card's #Lines container
      int lineIndex = 0;
      for (HelpEntry entry : topic.entries()) {
        String linesContainer = cardPrefix + " #Lines";
        String template = getTemplateForType(entry.type());
        cmd.append(linesContainer, template);

        if (entry.type() != HelpEntry.EntryType.SPACER) {
          String text = entry.text();
          // Prefix tips with >> for visual distinction
          if (entry.type() == HelpEntry.EntryType.TIP) {
            text = ">> " + text;
          }
          cmd.set(linesContainer + "[" + lineIndex + "] #Text.Text", text);
        }
        lineIndex++;
      }
      cardIndex++;
    }
  }

  /**
   * Returns the appropriate template path for an entry type.
   */
  private String getTemplateForType(HelpEntry.EntryType type) {
    return switch (type) {
      case TEXT -> TPL_LINE_TEXT;
      case COMMAND -> TPL_LINE_COMMAND;
      case TIP -> TPL_LINE_TIP;
      case HEADING -> TPL_LINE_HEADING;
      case SPACER -> TPL_SPACER;
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
