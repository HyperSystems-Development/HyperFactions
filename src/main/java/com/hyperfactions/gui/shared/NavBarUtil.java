package com.hyperfactions.gui.shared;

import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.HFMessages;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared utility for building navigation bar buttons.
 * Extracts the common loop pattern used by all three NavBarHelper classes.
 */
public final class NavBarUtil {

  private NavBarUtil() {}

  /**
   * Builds navigation buttons inside a cards container.
   * The entry's {@code displayName()} is treated as an i18n key and resolved
   * via {@link HFMessages} for the given player.
   *
   * @param entries       The nav entries to render
   * @param cardsId       The cards container selector (e.g., "#NavCards")
   * @param templatePath  The UI template path for each button
   * @param buttonId      The button element ID within the template (e.g., "#NavActionButton")
   * @param eventType     The event type value (e.g., "Nav" or "AdminNav")
   * @param eventKey      The event data key (e.g., "NavBar" or "AdminNavBar")
   * @param playerRef     The player viewing the page (for i18n resolution)
   * @param cmd           The UI command builder
   * @param events        The UI event builder
   */
  public static void buildButtons(
      @NotNull List<? extends NavEntry> entries,
      @NotNull String cardsId,
      @NotNull String templatePath,
      @NotNull String buttonId,
      @NotNull String eventType,
      @NotNull String eventKey,
      @NotNull PlayerRef playerRef,
      @NotNull UICommandBuilder cmd,
      @NotNull UIEventBuilder events
  ) {
    int index = 0;
    for (NavEntry entry : entries) {
      cmd.append(cardsId, templatePath);
      cmd.set(cardsId + "[" + index + "] " + buttonId + ".Text",
          HFMessages.get(playerRef, entry.displayName()));
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          cardsId + "[" + index + "] " + buttonId,
          EventData.of("Button", eventType).append(eventKey, entry.id()),
          false
      );
      index++;
    }
  }

  /**
   * Checks permission for a nav entry.
   *
   * @param playerUuid The player UUID
   * @param entry      The nav entry to check
   * @return true if the player has permission (or entry has no permission requirement)
   */
  public static boolean hasPermission(@NotNull UUID playerUuid, @Nullable NavEntry entry) {
    return entry == null
        || entry.permission() == null
        || PermissionManager.get().hasPermission(playerUuid, entry.permission());
  }
}
