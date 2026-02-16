package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the debug logging system.
 * Controls debug output by category, with integration into the Logger utility.
 */
public class DebugConfig extends ModuleConfig {

    // Global debug settings
    private boolean enabledByDefault = false;
    private boolean logToConsole = true;

    // Per-category settings
    private boolean power = false;
    private boolean claim = false;
    private boolean combat = false;
    private boolean protection = false;
    private boolean relation = false;
    private boolean territory = false;
    private boolean worldmap = false;
    private boolean interaction = false;
    private boolean mixin = false;
    private boolean spawning = false;
    private boolean integration = false;
    private boolean economy = false;

    /**
     * Creates a new debug config.
     *
     * @param filePath path to config/debug.json
     */
    public DebugConfig(@NotNull Path filePath) {
        super(filePath);
    }

    @Override
    @NotNull
    public String getModuleName() {
        return "debug";
    }

    @Override
    protected boolean getDefaultEnabled() {
        return false; // Debug disabled by default
    }

    @Override
    protected void createDefaults() {
        enabled = false;
        enabledByDefault = false;
        logToConsole = true;
        power = false;
        claim = false;
        combat = false;
        protection = false;
        relation = false;
        territory = false;
        worldmap = false;
        interaction = false;
        mixin = false;
        spawning = false;
        integration = false;
        economy = false;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        enabledByDefault = getBool(root, "enabledByDefault", enabledByDefault);
        logToConsole = getBool(root, "logToConsole", logToConsole);

        // Load categories
        if (hasSection(root, "categories")) {
            JsonObject categories = root.getAsJsonObject("categories");
            power = getBool(categories, "power", false);
            claim = getBool(categories, "claim", false);
            combat = getBool(categories, "combat", false);
            protection = getBool(categories, "protection", false);
            relation = getBool(categories, "relation", false);
            territory = getBool(categories, "territory", false);
            worldmap = getBool(categories, "worldmap", false);
            interaction = getBool(categories, "interaction", false);
            mixin = getBool(categories, "mixin", false);
            spawning = getBool(categories, "spawning", false);
            integration = getBool(categories, "integration", false);
            economy = getBool(categories, "economy", false);
        }

        // Apply settings to Logger
        applyToLogger();
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("enabledByDefault", enabledByDefault);
        root.addProperty("logToConsole", logToConsole);

        JsonObject categories = new JsonObject();
        categories.addProperty("power", power);
        categories.addProperty("claim", claim);
        categories.addProperty("combat", combat);
        categories.addProperty("protection", protection);
        categories.addProperty("relation", relation);
        categories.addProperty("territory", territory);
        categories.addProperty("worldmap", worldmap);
        categories.addProperty("interaction", interaction);
        categories.addProperty("mixin", mixin);
        categories.addProperty("spawning", spawning);
        categories.addProperty("integration", integration);
        categories.addProperty("economy", economy);
        root.add("categories", categories);
    }

    /**
     * Applies the debug settings to the Logger utility.
     * Individual category settings take precedence over enabledByDefault.
     */
    public void applyToLogger() {
        Logger.setLogToConsole(logToConsole);
        // Individual settings override enabledByDefault - if explicitly set to false, stay false
        Logger.setDebugEnabled(Logger.DebugCategory.POWER, power);
        Logger.setDebugEnabled(Logger.DebugCategory.CLAIM, claim);
        Logger.setDebugEnabled(Logger.DebugCategory.COMBAT, combat);
        Logger.setDebugEnabled(Logger.DebugCategory.PROTECTION, protection);
        Logger.setDebugEnabled(Logger.DebugCategory.RELATION, relation);
        Logger.setDebugEnabled(Logger.DebugCategory.TERRITORY, territory);
        Logger.setDebugEnabled(Logger.DebugCategory.WORLDMAP, worldmap);
        Logger.setDebugEnabled(Logger.DebugCategory.INTERACTION, interaction);
        Logger.setDebugEnabled(Logger.DebugCategory.MIXIN, mixin);
        Logger.setDebugEnabled(Logger.DebugCategory.SPAWNING, spawning);
        Logger.setDebugEnabled(Logger.DebugCategory.INTEGRATION, integration);
        Logger.setDebugEnabled(Logger.DebugCategory.ECONOMY, economy);
    }

    // === Getters ===

    /**
     * Checks if debug is enabled by default for all categories.
     *
     * @return true if enabled by default
     */
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    /**
     * Checks if debug output should go to console.
     *
     * @return true if logging to console
     */
    public boolean isLogToConsole() {
        return logToConsole;
    }

    /**
     * Checks if power debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isPower() {
        return power;
    }

    /**
     * Checks if claim debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isClaim() {
        return claim;
    }

    /**
     * Checks if combat debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isCombat() {
        return combat;
    }

    /**
     * Checks if protection debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isProtection() {
        return protection;
    }

    /**
     * Checks if relation debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isRelation() {
        return relation;
    }

    /**
     * Checks if territory debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isTerritory() {
        return territory;
    }

    /**
     * Checks if world map debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isWorldmap() {
        return worldmap;
    }

    /**
     * Checks if interaction debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isInteraction() {
        return interaction;
    }

    /**
     * Checks if mixin debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isMixin() {
        return mixin;
    }

    /**
     * Checks if spawning debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isSpawning() {
        return spawning;
    }

    /**
     * Checks if integration debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isIntegration() {
        return integration;
    }

    /**
     * Checks if economy debug is enabled.
     *
     * @return true if enabled
     */
    public boolean isEconomy() {
        return economy;
    }

    // === Setters (for runtime toggle) ===

    /**
     * Sets power debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setPower(boolean enabled) {
        this.power = enabled;
        applyToLogger();
    }

    /**
     * Sets claim debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setClaim(boolean enabled) {
        this.claim = enabled;
        applyToLogger();
    }

    /**
     * Sets combat debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setCombat(boolean enabled) {
        this.combat = enabled;
        applyToLogger();
    }

    /**
     * Sets protection debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setProtection(boolean enabled) {
        this.protection = enabled;
        applyToLogger();
    }

    /**
     * Sets relation debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setRelation(boolean enabled) {
        this.relation = enabled;
        applyToLogger();
    }

    /**
     * Sets territory debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setTerritory(boolean enabled) {
        this.territory = enabled;
        applyToLogger();
    }

    /**
     * Sets world map debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setWorldmap(boolean enabled) {
        this.worldmap = enabled;
        applyToLogger();
    }

    /**
     * Sets interaction debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setInteraction(boolean enabled) {
        this.interaction = enabled;
        applyToLogger();
    }

    /**
     * Sets mixin debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setMixin(boolean enabled) {
        this.mixin = enabled;
        applyToLogger();
    }

    /**
     * Sets spawning debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setSpawning(boolean enabled) {
        this.spawning = enabled;
        applyToLogger();
    }

    /**
     * Sets integration debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setIntegration(boolean enabled) {
        this.integration = enabled;
        applyToLogger();
    }

    /**
     * Sets economy debug state and applies to Logger.
     *
     * @param enabled true to enable
     */
    public void setEconomy(boolean enabled) {
        this.economy = enabled;
        applyToLogger();
    }

    /**
     * Enables all debug categories.
     */
    public void enableAll() {
        enabledByDefault = false; // Clear this so individual settings work correctly
        power = true;
        claim = true;
        combat = true;
        protection = true;
        relation = true;
        territory = true;
        worldmap = true;
        interaction = true;
        mixin = true;
        spawning = true;
        integration = true;
        economy = true;
        applyToLogger();
    }

    /**
     * Disables all debug categories.
     */
    public void disableAll() {
        enabledByDefault = false; // Clear this so individual settings work correctly
        power = false;
        claim = false;
        combat = false;
        protection = false;
        relation = false;
        territory = false;
        worldmap = false;
        interaction = false;
        mixin = false;
        spawning = false;
        integration = false;
        economy = false;
        applyToLogger();
    }
}
