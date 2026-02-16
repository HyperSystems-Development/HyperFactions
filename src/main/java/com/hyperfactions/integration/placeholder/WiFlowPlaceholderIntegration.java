package com.hyperfactions.integration.placeholder;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.Logger;
import com.wiflow.placeholderapi.WiFlowPlaceholderAPI;
import org.jetbrains.annotations.Nullable;

/**
 * Soft dependency integration with WiFlow PlaceholderAPI.
 * <p>
 * Detects WiFlow PlaceholderAPI at runtime and registers the HyperFactions
 * expansion to expose faction data as placeholders using {@code {factions_xxx}} syntax.
 */
public final class WiFlowPlaceholderIntegration {

    private static boolean available = false;
    @Nullable
    private static WiFlowExpansion expansion;

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
            Logger.info("WiFlow PlaceholderAPI not found - WiFlow placeholders disabled");
            return;
        }

        try {
            expansion = new WiFlowExpansion(plugin);
            boolean registered = WiFlowPlaceholderAPI.registerExpansion(expansion);
            if (registered) {
                available = true;
                Logger.info("WiFlow PlaceholderAPI expansion registered ({factions_*})");
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
                WiFlowPlaceholderAPI.unregisterExpansion(expansion);
            } catch (Exception e) {
                Logger.debug("Failed to unregister WiFlow PlaceholderAPI expansion: %s", e.getMessage());
            }
            expansion = null;
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
