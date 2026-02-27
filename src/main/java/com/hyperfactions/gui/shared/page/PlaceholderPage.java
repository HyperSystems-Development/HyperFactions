package com.hyperfactions.gui.shared.page;

import com.hyperfactions.data.Faction;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.shared.data.PlaceholderData;
import com.hyperfactions.manager.FactionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic placeholder page for features under development.
 * Displays a "Coming Soon" message with navigation bar support.
 */
public class PlaceholderPage extends InteractiveCustomUIPage<PlaceholderData> {

  private final String pageId;

  private final String title;

  private final String description;

  private final String icon;

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final GuiManager guiManager;

  private final Faction faction;

  /**
   * Creates a new placeholder page.
   *
   * @param pageId        The page ID for nav bar highlighting
   * @param title         The page title
   * @param description   The description shown below "Coming Soon"
   * @param icon          The icon character (e.g., "?", "!", etc.)
   * @param playerRef     The player reference
   * @param factionManager The faction manager
   * @param guiManager    The GUI manager
   * @param faction       The player's faction (null if not in faction)
   */
  public PlaceholderPage(
      @NotNull String pageId,
      @NotNull String title,
      @NotNull String description,
      @NotNull String icon,
      @NotNull PlayerRef playerRef,
      @NotNull FactionManager factionManager,
      @NotNull GuiManager guiManager,
      @Nullable Faction faction
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlaceholderData.CODEC);
    this.pageId = pageId;
    this.title = title;
    this.description = description;
    this.icon = icon;
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    // Load the placeholder template
    cmd.append(UIPaths.PLACEHOLDER_PAGE);

    // Setup navigation bar
    NavBarHelper.setupBar(playerRef, faction, pageId, cmd, events);

    // Set page content
    cmd.set("#Title @Text", title.toUpperCase());
    cmd.set("#PageIcon.Text", icon);
    cmd.set("#DescriptionLabel.Text", description);
  }

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                PlaceholderData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef currentPlayerRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || currentPlayerRef == null || data.button == null) {
      sendUpdate();
      return;
    }

    // Handle navigation
    if ("Nav".equals(data.button)) {
      Faction faction = factionManager.getPlayerFaction(currentPlayerRef.getUuid());
      if (NavBarHelper.handleNavEvent(data, player, ref, store, currentPlayerRef, faction, guiManager)) {
        return;
      }
    }

    sendUpdate();
  }
}
