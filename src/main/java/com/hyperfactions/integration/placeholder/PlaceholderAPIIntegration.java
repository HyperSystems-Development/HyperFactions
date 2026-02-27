package com.hyperfactions.integration.placeholder;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Soft dependency integration with PlaceholderAPI.
 *
 * <p>
 * Detects PlaceholderAPI at runtime and registers the HyperFactions
 * expansion to expose faction data as placeholders.
 *
 * <p>
 * Internal expansions must be manually registered since they are not
 * separate JARs in the expansions folder.
 */
public final class PlaceholderAPIIntegration {

  private static boolean available = false;

  @Nullable
  private static HyperFactionsExpansion expansion;

  private PlaceholderAPIIntegration() {}

  /**
   * Initializes PlaceholderAPI integration.
   * Detects PAPI at runtime and registers the expansion.
   * Must be called after all managers are initialized.
   *
   * @param plugin the HyperFactions instance
   */
  public static void init(HyperFactions plugin) {
    try {
      Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] PlaceholderAPI not found - placeholders disabled");
      return;
    }

    try {
      expansion = new HyperFactionsExpansion(plugin);
      // Internal expansions must be manually registered
      boolean registered = expansion.register();
      if (registered) {
        available = true;
        Logger.info("[Integration] PlaceholderAPI expansion registered (%%factions_*%%)");
      } else {
        Logger.warn("PlaceholderAPI expansion registration failed");
        expansion = null;
      }
    } catch (Exception e) {
      Logger.warn("Failed to register PlaceholderAPI expansion: %s", e.getMessage());
      expansion = null;
    }
  }

  /**
   * Shuts down PlaceholderAPI integration.
   * Unregisters the expansion.
   */
  public static void shutdown() {
    if (expansion != null) {
      try {
        expansion.unregister();
      } catch (Exception e) {
        Logger.debug("Failed to unregister PlaceholderAPI expansion: %s", e.getMessage());
      }
      expansion = null;
    }
    available = false;
  }

  /**
   * Checks if PlaceholderAPI is available and the expansion is registered.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    return available;
  }
}
