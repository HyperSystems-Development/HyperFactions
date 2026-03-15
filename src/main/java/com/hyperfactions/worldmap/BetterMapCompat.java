package com.hyperfactions.worldmap;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.util.Logger;

/**
 * Handles BetterMap mod compatibility via reflection-based detection.
 * BetterMap is an optional dependency — HyperFactions functions fully without it.
 *
 * <p>BetterMap hooks into the existing WorldMapTracker via reflection and modifies
 * WorldMapSettings fields. When detected, HyperFactions adapts by:
 * <ul>
 *   <li>Inheriting world settings instead of hardcoding (BetterMap can then modify)</li>
 *   <li>Deferring imageScale to BetterMap's quality system</li>
 *   <li>Not overriding scale/marker flags that BetterMap manages</li>
 * </ul>
 *
 * <p>Config-driven via {@code betterMapCompat} setting: "auto" (detect), "always", "never".
 *
 * <p><b>Thread Safety:</b> Detection runs once at startup. {@link #isActive()} and
 * {@link #isDetected()} are safe to call from any thread after initialization.
 */
public final class BetterMapCompat {

  private static final String BETTERMAP_CLASS = "dev.ninesliced.BetterMap";

  private static volatile boolean detected = false;
  private static volatile boolean initialized = false;

  private BetterMapCompat() {
    // Utility class
  }

  /**
   * Initializes BetterMap detection. Call once at plugin startup, before WorldMapService.
   * Safe to call multiple times (idempotent).
   */
  public static void initialize() {
    if (initialized) {
      return;
    }
    initialized = true;

    try {
      Class.forName(BETTERMAP_CLASS);
      detected = true;
      Logger.info("[BetterMap] BetterMap detected — compatibility mode available");
    } catch (ClassNotFoundException e) {
      detected = false;
      Logger.debug("[BetterMap] BetterMap not detected");
    }
  }

  /**
   * Checks if BetterMap compatibility mode is currently active.
   * Depends on both detection result and the {@code betterMapCompat} config setting.
   *
   * @return true if BetterMap compat is active
   */
  public static boolean isActive() {
    String mode = ConfigManager.get().worldMap().getBetterMapCompat();
    return switch (mode) {
      case "always" -> true;
      case "never" -> false;
      default -> detected; // "auto"
    };
  }

  /**
   * Checks if BetterMap was detected on the classpath.
   * This is independent of the config setting.
   *
   * @return true if BetterMap is present
   */
  public static boolean isDetected() {
    return detected;
  }
}
