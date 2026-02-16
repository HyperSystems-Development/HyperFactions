package com.hyperfactions.util;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.logging.Level;

/**
 * Wrapped logger with HyperFactions prefix, formatting, and category-based debug logging.
 */
public final class Logger {

    private static final String PREFIX = "[HyperFactions] ";
    private static java.util.logging.Logger logger;

    /**
     * Debug categories for category-based debug logging.
     */
    public enum DebugCategory {
        POWER("power"),
        CLAIM("claim"),
        COMBAT("combat"),
        PROTECTION("protection"),
        RELATION("relation"),
        TERRITORY("territory"),
        WORLDMAP("worldmap"),
        INTERACTION("interaction"),
        MIXIN("mixin"),
        SPAWNING("spawning"),
        INTEGRATION("integration"),
        ECONOMY("economy");

        private final String configKey;

        DebugCategory(String configKey) {
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    // Volatile flags for thread-safety
    private static volatile EnumSet<DebugCategory> enabledCategories = EnumSet.noneOf(DebugCategory.class);
    private static volatile boolean verboseMode = false;
    private static volatile boolean logToConsole = true;

    private Logger() {}

    /**
     * Initializes the logger.
     *
     * @param parentLogger the parent logger from the plugin
     */
    public static void init(@NotNull java.util.logging.Logger parentLogger) {
        logger = parentLogger;
    }

    // === Standard Logging ===

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public static void info(@NotNull String message) {
        if (logger != null) {
            logger.info(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[INFO] " + message);
        }
    }

    /**
     * Logs an info message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void info(@NotNull String message, Object... args) {
        info(String.format(message, args));
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public static void warn(@NotNull String message) {
        if (logger != null) {
            logger.warning(PREFIX + message);
        } else {
            System.out.println(PREFIX + "[WARN] " + message);
        }
    }

    /**
     * Logs a warning message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void warn(@NotNull String message, Object... args) {
        warn(String.format(message, args));
    }

    /**
     * Logs a severe error message.
     *
     * @param message the message
     */
    public static void severe(@NotNull String message) {
        if (logger != null) {
            logger.severe(PREFIX + message);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + message);
        }
    }

    /**
     * Logs a severe error message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void severe(@NotNull String message, Object... args) {
        severe(String.format(message, args));
    }

    /**
     * Logs a severe error with exception.
     *
     * @param message   the message format
     * @param throwable the exception
     * @param args      format arguments
     */
    public static void severe(@NotNull String message, @NotNull Throwable throwable, Object... args) {
        String formatted = String.format(message, args);
        if (logger != null) {
            logger.log(Level.SEVERE, PREFIX + formatted, throwable);
        } else {
            System.err.println(PREFIX + "[SEVERE] " + formatted);
            throwable.printStackTrace();
        }
    }

    /**
     * Logs a debug message (only if debug is enabled).
     *
     * @param message the message
     */
    public static void debug(@NotNull String message) {
        if (logger != null) {
            logger.fine(PREFIX + "[DEBUG] " + message);
        }
    }

    /**
     * Logs a debug message with formatting.
     *
     * @param message the message format
     * @param args    the format arguments
     */
    public static void debug(@NotNull String message, Object... args) {
        debug(String.format(message, args));
    }

    // === Category-Based Debug Logging ===

    /**
     * Enables or disables a specific debug category.
     *
     * @param category the category to toggle
     * @param enabled  true to enable, false to disable
     */
    public static void setDebugEnabled(@NotNull DebugCategory category, boolean enabled) {
        EnumSet<DebugCategory> newSet = EnumSet.copyOf(enabledCategories);
        if (enabled) {
            newSet.add(category);
        } else {
            newSet.remove(category);
        }
        enabledCategories = newSet;
    }

    /**
     * Checks if a debug category is enabled.
     *
     * @param category the category to check
     * @return true if enabled
     */
    public static boolean isDebugEnabled(@NotNull DebugCategory category) {
        return enabledCategories.contains(category);
    }

    /**
     * Enables all debug categories.
     */
    public static void enableAll() {
        enabledCategories = EnumSet.allOf(DebugCategory.class);
        info("[Debug] All debug categories enabled");
    }

    /**
     * Disables all debug categories.
     */
    public static void disableAll() {
        enabledCategories = EnumSet.noneOf(DebugCategory.class);
        info("[Debug] All debug categories disabled");
    }

    /**
     * Gets the currently enabled categories.
     *
     * @return unmodifiable set of enabled categories
     */
    public static EnumSet<DebugCategory> getEnabledCategories() {
        return EnumSet.copyOf(enabledCategories);
    }

    /**
     * Sets verbose mode for extra detailed output.
     *
     * @param enabled true to enable verbose mode
     */
    public static void setVerboseMode(boolean enabled) {
        verboseMode = enabled;
    }

    /**
     * Checks if verbose mode is enabled.
     *
     * @return true if verbose mode is enabled
     */
    public static boolean isVerboseMode() {
        return verboseMode;
    }

    /**
     * Sets whether debug output goes to console.
     *
     * @param enabled true to log to console
     */
    public static void setLogToConsole(boolean enabled) {
        logToConsole = enabled;
    }

    // === Category-Specific Debug Methods ===

    /**
     * Logs a power-related debug message.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugPower(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.POWER)) {
            logDebug("POWER", message, args);
        }
    }

    /**
     * Logs a claim-related debug message.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugClaim(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.CLAIM)) {
            logDebug("CLAIM", message, args);
        }
    }

    /**
     * Logs a combat-related debug message.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugCombat(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.COMBAT)) {
            logDebug("COMBAT", message, args);
        }
    }

    /**
     * Logs a protection-related debug message.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugProtection(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.PROTECTION)) {
            logDebug("PROTECTION", message, args);
        }
    }

    /**
     * Logs a relation-related debug message.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugRelation(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.RELATION)) {
            logDebug("RELATION", message, args);
        }
    }

    /**
     * Logs a territory-related debug message (notifications, chunk entry/exit).
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugTerritory(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.TERRITORY)) {
            logDebug("TERRITORY", message, args);
        }
    }

    /**
     * Logs a world map-related debug message (map generation, tile updates).
     * Separated from territory to reduce noise from frequent map updates.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugWorldMap(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.WORLDMAP)) {
            logDebug("WORLDMAP", message, args);
        }
    }

    /**
     * Logs an interaction-related debug message (F-key pickups, entity interactions).
     * Used for debugging player interactions with world entities.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugInteraction(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.INTERACTION)) {
            logDebug("INTERACTION", message, args);
        }
    }

    /**
     * Logs a mixin-related debug message (OrbisGuard-Mixins integration, hook registration).
     * Used for debugging mixin-dependent features like F-key pickup, keep inventory, etc.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugMixin(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.MIXIN)) {
            logDebug("MIXIN", message, args);
        }
    }

    /**
     * Logs a spawning-related debug message (mob spawn suppression, NPC groups).
     * Used for debugging spawn suppression controller integration and zone-based spawning rules.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugSpawning(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.SPAWNING)) {
            logDebug("SPAWNING", message, args);
        }
    }

    /**
     * Logs an economy-related debug message (treasury transactions, balance changes, admin adjustments).
     * Used for debugging the faction economy system.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugEconomy(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.ECONOMY)) {
            logDebug("ECONOMY", message, args);
        }
    }

    /**
     * Logs an integration-related debug message (cross-plugin integrations, access checkers).
     * Used for debugging GravestonePlugin integration, future integrations.
     *
     * @param message the message format
     * @param args    format arguments
     */
    public static void debugIntegration(@NotNull String message, Object... args) {
        if (isDebugEnabled(DebugCategory.INTEGRATION)) {
            logDebug("INTEGRATION", message, args);
        }
    }

    /**
     * Internal method to log a categorized debug message.
     */
    private static void logDebug(@NotNull String category, @NotNull String message, Object... args) {
        String formatted = args.length > 0 ? String.format(message, args) : message;
        String logMessage = PREFIX + "[DEBUG:" + category + "] " + formatted;

        if (logToConsole) {
            if (logger != null) {
                logger.info(logMessage); // Use INFO level for visibility
            } else {
                System.out.println(logMessage);
            }
        } else if (logger != null) {
            logger.fine(logMessage);
        }
    }

    /**
     * Logs a debug message with verbose details if verbose mode is enabled.
     *
     * @param category       the debug category
     * @param message        the main message format
     * @param verboseDetails the verbose details format
     * @param args           format arguments (used for both messages)
     */
    public static void debugVerbose(@NotNull DebugCategory category, @NotNull String message,
                                     @NotNull String verboseDetails, Object... args) {
        if (!isDebugEnabled(category)) {
            return;
        }

        String formatted = args.length > 0 ? String.format(message, args) : message;
        logDebug(category.name(), formatted);

        if (verboseMode) {
            String verboseFormatted = args.length > 0 ? String.format(verboseDetails, args) : verboseDetails;
            logDebug(category.name() + ":VERBOSE", verboseFormatted);
        }
    }
}
