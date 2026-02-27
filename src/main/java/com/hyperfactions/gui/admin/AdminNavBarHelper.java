package com.hyperfactions.gui.admin;

import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.admin.data.AdminNavAwareData;
import com.hyperfactions.gui.shared.NavBarUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Helper class for building and handling the admin navigation bar component.
 * Follows the NavBarHelper pattern for admin-specific navigation.
 */
public final class AdminNavBarHelper {

  private AdminNavBarHelper() {
    // Static utility class
  }

  /**
   * Sets up the admin navigation bar in a page.
   * Follows AdminUI pattern exactly with indexed selectors.
   *
   * @param playerRef   The player viewing the page
   * @param currentPage The ID of the current page (to highlight it)
   * @param cmd         The UI command builder
   * @param events      The UI event builder
   */
  public static void setupBar(
      @NotNull PlayerRef playerRef,
      @NotNull String currentPage,
      @NotNull UICommandBuilder cmd,
      @NotNull UIEventBuilder events
  ) {
    // Get accessible admin nav bar entries
    List<AdminPageRegistry.Entry> entries = AdminPageRegistry.getInstance()
        .getAccessibleNavBarEntries(playerRef);

    if (entries.isEmpty()) {
      return;
    }

    // Nav bar is included in UI templates via $Nav.@HyperFactionsAdminNavBar
    // We just set up the dynamic content here

    // Create admin nav cards container and build buttons using shared utility
    cmd.appendInline("#HyperFactionsAdminNavBar #AdminNavBarButtons", "Group #AdminNavCards { LayoutMode: Left; }");
    NavBarUtil.buildButtons(entries, "#AdminNavCards", UIPaths.ADMIN_NAV_BUTTON, "#AdminNavActionButton",
        "AdminNav", "AdminNavBar", cmd, events);
  }

  /**
   * Handles admin navigation events from the nav bar.
   * Works with any data class implementing AdminNavAwareData.
   *
   * @param data       The event data (must implement AdminNavAwareData)
   * @param player     The player entity
   * @param ref        Entity reference
   * @param store      Entity store
   * @param playerRef  Player reference
   * @param guiManager The GUI manager
   * @return true if the event was handled, false otherwise
   */
  public static boolean handleNavEvent(
      @NotNull AdminNavAwareData data,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    // Check if we have adminNavBar data
    String targetId = data.getAdminNavBar();
    if (targetId == null || targetId.isEmpty()) {
      return false;
    }

    // Get the target entry
    AdminPageRegistry.Entry entry = AdminPageRegistry.getInstance().getEntry(targetId);
    if (entry == null) {
      return true; // Consumed but invalid target
    }

    // Check permission
    if (!NavBarUtil.hasPermission(playerRef.getUuid(), entry)) {
      return true; // Consumed but no permission
    }

    // Create and open the target page
    InteractiveCustomUIPage<?> page = entry.guiSupplier().create(
        player, ref, store, playerRef, guiManager
    );

    if (page != null) {
      player.getPageManager().openCustomPage(ref, store, page);
    }

    return true;
  }
}
