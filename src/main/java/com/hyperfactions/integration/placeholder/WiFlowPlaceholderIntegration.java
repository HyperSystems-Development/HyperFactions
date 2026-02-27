package com.hyperfactions.integration.placeholder;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Method;
import org.jetbrains.annotations.Nullable;

/**
 * Soft dependency integration with WiFlow PlaceholderAPI.
 *
 * <p>
 * All WiFlow classes are accessed via reflection so HyperFactions compiles
 * and runs without the WiFlowPlaceholderAPI JAR present.
 *
 * <p>
 * The actual expansion implementation lives in {@code WiFlowExpansion} which
 * extends PlaceholderExpansion directly — that class is conditionally compiled
 * only when the WiFlow API is available on the classpath.
 */
public final class WiFlowPlaceholderIntegration {

  private static boolean available = false;

  @Nullable
  private static Object expansion;

  // Cached reflection handles for shutdown
  private static Method unregisterMethod;

  private static Class<?> baseExpansionClass;

  private WiFlowPlaceholderIntegration() {}

  /**
   * Initializes WiFlow PlaceholderAPI integration.
   * Detects WiFlow at runtime and registers the expansion.
   * Must be called after all managers are initialized.
   *
   * @param plugin the HyperFactions instance
   */
  public static void init(HyperFactions plugin) {
    try {
      Class.forName("com.wiflow.placeholderapi.WiFlowPlaceholderAPI");
    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] WiFlow PlaceholderAPI not found - WiFlow placeholders disabled");
      return;
    }

    try {
      // WiFlowExpansion extends PlaceholderExpansion — only compiled when WiFlow is available.
      // Load it reflectively to avoid a compile-time dependency in this class.
      Class<?> expansionClass = Class.forName(
          "com.hyperfactions.integration.placeholder.WiFlowExpansion");
      expansion = expansionClass.getConstructor(HyperFactions.class).newInstance(plugin);

      // WiFlowPlaceholderAPI.registerExpansion(PlaceholderExpansion)
      Class<?> apiClass = Class.forName("com.wiflow.placeholderapi.WiFlowPlaceholderAPI");
      baseExpansionClass = Class.forName("com.wiflow.placeholderapi.expansion.PlaceholderExpansion");
      Method registerMethod = apiClass.getMethod("registerExpansion", baseExpansionClass);
      boolean registered = (boolean) registerMethod.invoke(null, expansion);

      if (registered) {
        // Cache unregister method for shutdown
        unregisterMethod = apiClass.getMethod("unregisterExpansion", baseExpansionClass);
        available = true;
        Logger.info("[Integration] WiFlow PlaceholderAPI expansion registered ({factions_*})");
      } else {
        Logger.warn("WiFlow PlaceholderAPI expansion registration failed");
        expansion = null;
      }
    } catch (Exception e) {
      Logger.warn("Failed to register WiFlow PlaceholderAPI expansion: %s", e.getMessage());
      expansion = null;
    }
  }

  /**
   * Shuts down WiFlow PlaceholderAPI integration.
   * Unregisters the expansion.
   */
  public static void shutdown() {
    if (expansion != null) {
      try {
        if (unregisterMethod != null) {
          unregisterMethod.invoke(null, expansion);
        }
      } catch (Exception e) {
        Logger.debug("Failed to unregister WiFlow PlaceholderAPI expansion: %s", e.getMessage());
      }
      expansion = null;
      unregisterMethod = null;
      baseExpansionClass = null;
    }
    available = false;
  }

  /**
   * Checks if WiFlow PlaceholderAPI is available and the expansion is registered.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    return available;
  }
}
