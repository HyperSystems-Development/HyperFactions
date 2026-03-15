package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for the world map integration system.
 *
 * <p>
 * Controls how claim overlays are rendered on the in-game world map,
 * with multiple refresh modes to balance performance vs. responsiveness.
 *
 * <p>
 * Config file: config/worldmap.json
 */
public class WorldMapConfig extends ModuleConfig {

  /**
   * Refresh mode determines how map updates are triggered when claims change.
   */
  public enum RefreshMode {
    /**
     * Only refresh for players within range of claim changes.
     * Most performant option - recommended for busy servers.
     */
    PROXIMITY("proximity"),

    /**
     * Refresh specific chunks for all players.
     * Good balance of performance and consistency.
     */
    INCREMENTAL("incremental"),

    /**
     * Full map refresh after a quiet period with no changes.
     * Use if incremental/proximity causes issues.
     */
    DEBOUNCED("debounced"),

    /**
     * Full map refresh on every claim change.
     * Original behavior - not recommended for busy servers.
     */
    IMMEDIATE("immediate"),

    /**
     * No automatic refresh. Use /f admin map refresh manually.
     */
    MANUAL("manual");

    private final String configName;

    RefreshMode(String configName) {
      this.configName = configName;
    }

    /** Returns the config name. */
    public String getConfigName() {
      return configName;
    }

    /** Creates from string. */
    public static RefreshMode fromString(String name) {
      for (RefreshMode mode : values()) {
        if (mode.configName.equalsIgnoreCase(name)) {
          return mode;
        }
      }
      return INCREMENTAL; // Default
    }
  }

  // Current refresh mode
  private RefreshMode refreshMode = RefreshMode.INCREMENTAL;

  // Proximity mode settings
  private int proximityChunkRadius = 32;

  private int proximityBatchIntervalTicks = 30;  // 30 ticks = 1 second at 30 TPS
  private int proximityMaxChunksPerBatch = 50;

  // Incremental mode settings
  private int incrementalBatchIntervalTicks = 30;  // 30 ticks = 1 second at 30 TPS
  private int incrementalMaxChunksPerBatch = 50;

  // Debounced mode settings
  private int debouncedDelaySeconds = 5;

  // Fallback behavior
  private boolean autoFallbackOnError = true;

  // Display settings
  private boolean showFactionTags = true;

  // Player visibility filtering (disabled = vanilla behavior, all players visible)
  private boolean playerVisibilityEnabled = true;

  private boolean showOwnFaction = true;

  private boolean showAllies = true;

  private boolean showNeutrals = false;

  private boolean showEnemies = false;

  private boolean showFactionlessPlayers = false;

  private boolean showFactionlessToFactionless = true;

  // World config respect (Issue #96)
  private boolean respectWorldConfig = true;

  // BetterMap compatibility mode: "auto", "always", "never" (Issue #97)
  private String betterMapCompat = "auto";

  // Optional settings overrides when respectWorldConfig is true (null = inherit from world)
  private Float overrideDefaultScale = null;
  private Float overrideMinScale = null;
  private Float overrideMaxScale = null;
  private Float overrideImageScale = null;
  private Boolean overrideAllowTeleportToCoordinates = null;
  private Boolean overrideAllowTeleportToMarkers = null;
  private Boolean overrideAllowCreatingMapMarkers = null;

  // Performance settings
  private int factionWideRefreshThreshold = 200;  // Above this, use full refresh instead of queuing

  /**
   * Creates a new world map config.
   *
   * @param filePath path to config/worldmap.json
   */
  public WorldMapConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "worldmap";
  }

  /** Returns the default enabled. */
  @Override
  protected boolean getDefaultEnabled() {
    return true; // World map integration enabled by default
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    refreshMode = RefreshMode.INCREMENTAL;

    // Proximity defaults (30 TPS: 30 ticks = 1 second)
    proximityChunkRadius = 32;
    proximityBatchIntervalTicks = 30;
    proximityMaxChunksPerBatch = 50;

    // Incremental defaults (30 TPS: 30 ticks = 1 second)
    incrementalBatchIntervalTicks = 30;
    incrementalMaxChunksPerBatch = 50;

    // Debounced defaults
    debouncedDelaySeconds = 5;

    // Fallback
    autoFallbackOnError = true;

    // Display settings
    showFactionTags = true;

    // Player visibility filtering (enabled by default)
    playerVisibilityEnabled = true;
    showOwnFaction = true;
    showAllies = true;
    showNeutrals = false;
    showEnemies = false;
    showFactionlessPlayers = false;
    showFactionlessToFactionless = true;

    // World config respect
    respectWorldConfig = true;
    betterMapCompat = "auto";

    // Settings overrides (null = inherit from world)
    overrideDefaultScale = null;
    overrideMinScale = null;
    overrideMaxScale = null;
    overrideImageScale = null;
    overrideAllowTeleportToCoordinates = null;
    overrideAllowTeleportToMarkers = null;
    overrideAllowCreatingMapMarkers = null;

    // Performance settings
    factionWideRefreshThreshold = 200;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    // Load refresh mode
    String modeStr = getString(root, "refreshMode", refreshMode.getConfigName());
    refreshMode = RefreshMode.fromString(modeStr);

    // Load auto-fallback setting
    autoFallbackOnError = getBool(root, "autoFallbackOnError", autoFallbackOnError);

    // Load display settings
    showFactionTags = getBool(root, "showFactionTags", showFactionTags);

    // Load performance settings
    factionWideRefreshThreshold = getInt(root, "factionWideRefreshThreshold", factionWideRefreshThreshold);

    // Load proximity settings
    if (hasSection(root, "proximity")) {
      JsonObject proximity = root.getAsJsonObject("proximity");
      proximityChunkRadius = getInt(proximity, "chunkRadius", proximityChunkRadius);
      proximityBatchIntervalTicks = getInt(proximity, "batchIntervalTicks", proximityBatchIntervalTicks);
      proximityMaxChunksPerBatch = getInt(proximity, "maxChunksPerBatch", proximityMaxChunksPerBatch);
    }

    // Load incremental settings
    if (hasSection(root, "incremental")) {
      JsonObject incremental = root.getAsJsonObject("incremental");
      incrementalBatchIntervalTicks = getInt(incremental, "batchIntervalTicks", incrementalBatchIntervalTicks);
      incrementalMaxChunksPerBatch = getInt(incremental, "maxChunksPerBatch", incrementalMaxChunksPerBatch);
    }

    // Load player visibility settings
    if (hasSection(root, "playerVisibility")) {
      JsonObject pv = root.getAsJsonObject("playerVisibility");
      playerVisibilityEnabled = getBool(pv, "enabled", playerVisibilityEnabled);
      showOwnFaction = getBool(pv, "showOwnFaction", showOwnFaction);
      showAllies = getBool(pv, "showAllies", showAllies);
      showNeutrals = getBool(pv, "showNeutrals", showNeutrals);
      showEnemies = getBool(pv, "showEnemies", showEnemies);
      showFactionlessPlayers = getBool(pv, "showFactionlessPlayers", showFactionlessPlayers);
      showFactionlessToFactionless = getBool(pv, "showFactionlessToFactionless", showFactionlessToFactionless);
    }

    // Load debounced settings
    if (hasSection(root, "debounced")) {
      JsonObject debounced = root.getAsJsonObject("debounced");
      debouncedDelaySeconds = getInt(debounced, "delaySeconds", debouncedDelaySeconds);
    }

    // Load world config respect settings
    respectWorldConfig = getBool(root, "respectWorldConfig", respectWorldConfig);
    betterMapCompat = getString(root, "betterMapCompat", betterMapCompat);

    // Load optional settings overrides
    if (hasSection(root, "settingsOverrides")) {
      JsonObject overrides = root.getAsJsonObject("settingsOverrides");
      overrideDefaultScale = getOptionalFloat(overrides, "defaultScale");
      overrideMinScale = getOptionalFloat(overrides, "minScale");
      overrideMaxScale = getOptionalFloat(overrides, "maxScale");
      overrideImageScale = getOptionalFloat(overrides, "imageScale");
      overrideAllowTeleportToCoordinates = getOptionalBool(overrides, "allowTeleportToCoordinates");
      overrideAllowTeleportToMarkers = getOptionalBool(overrides, "allowTeleportToMarkers");
      overrideAllowCreatingMapMarkers = getOptionalBool(overrides, "allowCreatingMapMarkers");
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("refreshMode", refreshMode.getConfigName());
    root.addProperty("autoFallbackOnError", autoFallbackOnError);
    root.addProperty("showFactionTags", showFactionTags);
    root.addProperty("factionWideRefreshThreshold", factionWideRefreshThreshold);
    root.addProperty("_thresholdNote", "If faction has more claims than threshold, use full refresh instead of queuing each chunk");

    // Player visibility section
    JsonObject playerVisibility = new JsonObject();
    playerVisibility.addProperty("_description", "Controls which players are visible on the world map and compass based on faction relations.");
    playerVisibility.addProperty("enabled", playerVisibilityEnabled);
    playerVisibility.addProperty("showOwnFaction", showOwnFaction);
    playerVisibility.addProperty("showAllies", showAllies);
    playerVisibility.addProperty("showNeutrals", showNeutrals);
    playerVisibility.addProperty("showEnemies", showEnemies);
    playerVisibility.addProperty("showFactionlessPlayers", showFactionlessPlayers);
    playerVisibility.addProperty("showFactionlessToFactionless", showFactionlessToFactionless);
    root.add("playerVisibility", playerVisibility);

    // Proximity section
    JsonObject proximity = new JsonObject();
    proximity.addProperty("_description", "Only refresh for players within range of claim changes. Most performant option.");
    proximity.addProperty("_tickNote", "Hytale runs at 30 TPS: 30 ticks = 1 second, 60 ticks = 2 seconds");
    proximity.addProperty("chunkRadius", proximityChunkRadius);
    proximity.addProperty("batchIntervalTicks", proximityBatchIntervalTicks);
    proximity.addProperty("maxChunksPerBatch", proximityMaxChunksPerBatch);
    root.add("proximity", proximity);

    // Incremental section
    JsonObject incremental = new JsonObject();
    incremental.addProperty("_description", "Refresh specific chunks for all players. Good balance of performance and consistency.");
    incremental.addProperty("_tickNote", "Hytale runs at 30 TPS: 30 ticks = 1 second, 60 ticks = 2 seconds");
    incremental.addProperty("batchIntervalTicks", incrementalBatchIntervalTicks);
    incremental.addProperty("maxChunksPerBatch", incrementalMaxChunksPerBatch);
    root.add("incremental", incremental);

    // Debounced section
    JsonObject debounced = new JsonObject();
    debounced.addProperty("_description", "Full map refresh after a quiet period. Use if incremental causes issues.");
    debounced.addProperty("delaySeconds", debouncedDelaySeconds);
    root.add("debounced", debounced);

    // Immediate section (no settings, just description)
    JsonObject immediate = new JsonObject();
    immediate.addProperty("_description", "Full map refresh on every claim change. Original behavior, not recommended for busy servers.");
    root.add("immediate", immediate);

    // Manual section (no settings, just description)
    JsonObject manual = new JsonObject();
    manual.addProperty("_description", "No automatic refresh. Use /f admin map refresh to update manually.");
    root.add("manual", manual);

    // World config respect
    root.addProperty("respectWorldConfig", respectWorldConfig);
    root.addProperty("_respectNote", "When true, inherits map settings from world config. Disabled worlds are skipped.");
    root.addProperty("betterMapCompat", betterMapCompat);
    root.addProperty("_betterMapNote", "BetterMap compatibility: auto (detect), always (force on), never (force off)");

    // Settings overrides section
    JsonObject settingsOverrides = new JsonObject();
    settingsOverrides.addProperty("_description", "Override inherited world settings. Remove a key or set to null to inherit from the world config.");
    if (overrideDefaultScale != null) settingsOverrides.addProperty("defaultScale", overrideDefaultScale);
    if (overrideMinScale != null) settingsOverrides.addProperty("minScale", overrideMinScale);
    if (overrideMaxScale != null) settingsOverrides.addProperty("maxScale", overrideMaxScale);
    if (overrideImageScale != null) settingsOverrides.addProperty("imageScale", overrideImageScale);
    if (overrideAllowTeleportToCoordinates != null) settingsOverrides.addProperty("allowTeleportToCoordinates", overrideAllowTeleportToCoordinates);
    if (overrideAllowTeleportToMarkers != null) settingsOverrides.addProperty("allowTeleportToMarkers", overrideAllowTeleportToMarkers);
    if (overrideAllowCreatingMapMarkers != null) settingsOverrides.addProperty("allowCreatingMapMarkers", overrideAllowCreatingMapMarkers);
    root.add("settingsOverrides", settingsOverrides);
  }

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // Validate proximity settings
    proximityChunkRadius = validateRange(result, "proximity.chunkRadius",
        proximityChunkRadius, 1, 128, 32);
    proximityBatchIntervalTicks = validateRange(result, "proximity.batchIntervalTicks",
        proximityBatchIntervalTicks, 1, 200, 20);
    proximityMaxChunksPerBatch = validateRange(result, "proximity.maxChunksPerBatch",
        proximityMaxChunksPerBatch, 1, 500, 50);

    // Validate incremental settings
    incrementalBatchIntervalTicks = validateRange(result, "incremental.batchIntervalTicks",
        incrementalBatchIntervalTicks, 1, 200, 20);
    incrementalMaxChunksPerBatch = validateRange(result, "incremental.maxChunksPerBatch",
        incrementalMaxChunksPerBatch, 1, 500, 50);

    // Validate debounced settings
    debouncedDelaySeconds = validateRange(result, "debounced.delaySeconds",
        debouncedDelaySeconds, 1, 60, 5);

    // Validate betterMapCompat value
    String originalCompat = betterMapCompat;
    betterMapCompat = betterMapCompat.toLowerCase(java.util.Locale.ROOT);
    if (!java.util.Set.of("auto", "always", "never").contains(betterMapCompat)) {
      result.addWarning(getModuleName(), "betterMapCompat",
          "Invalid value, defaulting to 'auto'", originalCompat, "auto");
      betterMapCompat = "auto";
    }

    return result;
  }

  // === Getters ===

  /**
   * Gets the current refresh mode.
   *
   * @return refresh mode
   */
  @NotNull
  public RefreshMode getRefreshMode() {
    return refreshMode;
  }

  /**
   * Sets the refresh mode.
   *
   * @param mode the new mode
   */
  public void setRefreshMode(@NotNull RefreshMode mode) {
    this.refreshMode = mode;
  }

  /**
   * Gets the chunk radius for proximity refresh mode.
   * Players within this many chunks of a claim change will have their map refreshed.
   *
   * @return chunk radius
   */
  public int getProximityChunkRadius() {
    return proximityChunkRadius;
  }

  /**
   * Gets the batch interval for proximity mode in ticks.
   *
   * @return ticks between batch processing
   */
  public int getProximityBatchIntervalTicks() {
    return proximityBatchIntervalTicks;
  }

  /**
   * Gets the maximum chunks to process per batch in proximity mode.
   *
   * @return max chunks per batch
   */
  public int getProximityMaxChunksPerBatch() {
    return proximityMaxChunksPerBatch;
  }

  /**
   * Gets the batch interval for incremental mode in ticks.
   *
   * @return ticks between batch processing
   */
  public int getIncrementalBatchIntervalTicks() {
    return incrementalBatchIntervalTicks;
  }

  /**
   * Gets the maximum chunks to process per batch in incremental mode.
   *
   * @return max chunks per batch
   */
  public int getIncrementalMaxChunksPerBatch() {
    return incrementalMaxChunksPerBatch;
  }

  /**
   * Gets the delay in seconds for debounced mode.
   *
   * @return delay seconds
   */
  public int getDebouncedDelaySeconds() {
    return debouncedDelaySeconds;
  }

  /**
   * Checks if auto-fallback on error is enabled.
   * When enabled, if reflection fails to access the image cache,
   * the system will automatically fall back to debounced mode.
   *
   * @return true if auto-fallback is enabled
   */
  public boolean isAutoFallbackOnError() {
    return autoFallbackOnError;
  }

  // === Setters (for admin config editor) ===

  /** Sets show faction tags. */
  public void setShowFactionTags(boolean value) { this.showFactionTags = value; }

  /** Sets player visibility enabled. */
  public void setPlayerVisibilityEnabled(boolean value) { this.playerVisibilityEnabled = value; }

  /** Sets show own faction. */
  public void setShowOwnFaction(boolean value) { this.showOwnFaction = value; }

  /** Sets show allies. */
  public void setShowAllies(boolean value) { this.showAllies = value; }

  /** Sets show neutrals. */
  public void setShowNeutrals(boolean value) { this.showNeutrals = value; }

  /** Sets show enemies. */
  public void setShowEnemies(boolean value) { this.showEnemies = value; }

  /** Sets show factionless players. */
  public void setShowFactionlessPlayers(boolean value) { this.showFactionlessPlayers = value; }

  /** Sets proximity chunk radius. */
  public void setProximityChunkRadius(int value) { this.proximityChunkRadius = value; }

  /** Sets proximity batch interval ticks. */
  public void setProximityBatchIntervalTicks(int value) { this.proximityBatchIntervalTicks = value; }

  /** Sets proximity max chunks per batch. */
  public void setProximityMaxChunksPerBatch(int value) { this.proximityMaxChunksPerBatch = value; }

  /** Sets incremental batch interval ticks. */
  public void setIncrementalBatchIntervalTicks(int value) { this.incrementalBatchIntervalTicks = value; }

  /** Sets incremental max chunks per batch. */
  public void setIncrementalMaxChunksPerBatch(int value) { this.incrementalMaxChunksPerBatch = value; }

  /** Sets debounced delay seconds. */
  public void setDebouncedDelaySeconds(int value) { this.debouncedDelaySeconds = value; }

  /** Sets auto fallback on error. */
  public void setAutoFallbackOnError(boolean value) { this.autoFallbackOnError = value; }

  /** Sets show factionless to factionless. */
  public void setShowFactionlessToFactionless(boolean value) { this.showFactionlessToFactionless = value; }

  /** Sets faction wide refresh threshold. */
  public void setFactionWideRefreshThreshold(int value) { this.factionWideRefreshThreshold = value; }

  // === World Config Respect ===

  /** Checks if world config settings should be respected. */
  public boolean isRespectWorldConfig() { return respectWorldConfig; }

  /** Sets whether to respect world config settings. */
  public void setRespectWorldConfig(boolean value) { this.respectWorldConfig = value; }

  /** Gets the BetterMap compatibility mode: "auto", "always", or "never". */
  @NotNull
  public String getBetterMapCompat() { return betterMapCompat; }

  /** Sets the BetterMap compatibility mode. */
  public void setBetterMapCompat(@NotNull String value) { this.betterMapCompat = value; }

  // === Settings Overrides (null = inherit from world) ===

  /** Gets the override for default map scale, or null to inherit. */
  @Nullable
  public Float getOverrideDefaultScale() { return overrideDefaultScale; }

  /** Gets the override for minimum map scale, or null to inherit. */
  @Nullable
  public Float getOverrideMinScale() { return overrideMinScale; }

  /** Gets the override for maximum map scale, or null to inherit. */
  @Nullable
  public Float getOverrideMaxScale() { return overrideMaxScale; }

  /** Gets the override for image scale, or null to inherit. */
  @Nullable
  public Float getOverrideImageScale() { return overrideImageScale; }

  /** Gets the override for allowing teleport to coordinates, or null to inherit. */
  @Nullable
  public Boolean getOverrideAllowTeleportToCoordinates() { return overrideAllowTeleportToCoordinates; }

  /** Gets the override for allowing teleport to markers, or null to inherit. */
  @Nullable
  public Boolean getOverrideAllowTeleportToMarkers() { return overrideAllowTeleportToMarkers; }

  /** Gets the override for allowing map marker creation, or null to inherit. */
  @Nullable
  public Boolean getOverrideAllowCreatingMapMarkers() { return overrideAllowCreatingMapMarkers; }

  // === Settings Override Setters (for admin GUI) ===
  // Scale overrides: 0 = inherit from world, positive = override value

  /** Sets the default scale override. 0 = inherit from world. */
  public void setOverrideDefaultScale(int value) { this.overrideDefaultScale = value > 0 ? (float) value : null; }

  /** Sets the min scale override. 0 = inherit from world. */
  public void setOverrideMinScale(int value) { this.overrideMinScale = value > 0 ? (float) value : null; }

  /** Sets the max scale override. 0 = inherit from world. */
  public void setOverrideMaxScale(int value) { this.overrideMaxScale = value > 0 ? (float) value : null; }

  /** Sets the image scale override. 0 = inherit from world. */
  public void setOverrideImageScale(int value) { this.overrideImageScale = value > 0 ? (float) value : null; }

  /** Gets the effective default scale for GUI display (0 = inherit). */
  public int getOverrideDefaultScaleInt() { return overrideDefaultScale != null ? overrideDefaultScale.intValue() : 0; }

  /** Gets the effective min scale for GUI display (0 = inherit). */
  public int getOverrideMinScaleInt() { return overrideMinScale != null ? overrideMinScale.intValue() : 0; }

  /** Gets the effective max scale for GUI display (0 = inherit). */
  public int getOverrideMaxScaleInt() { return overrideMaxScale != null ? overrideMaxScale.intValue() : 0; }

  /** Gets the effective image scale for GUI display (0 = inherit). */
  public int getOverrideImageScaleInt() { return overrideImageScale != null ? overrideImageScale.intValue() : 0; }

  // Boolean overrides: "inherit"/"enabled"/"disabled"

  /** Sets allow teleport to coordinates override from GUI string. */
  public void setOverrideAllowTeleportToCoordinates(String value) {
    this.overrideAllowTeleportToCoordinates = "enabled".equals(value) ? Boolean.TRUE : "disabled".equals(value) ? Boolean.FALSE : null;
  }

  /** Sets allow teleport to markers override from GUI string. */
  public void setOverrideAllowTeleportToMarkers(String value) {
    this.overrideAllowTeleportToMarkers = "enabled".equals(value) ? Boolean.TRUE : "disabled".equals(value) ? Boolean.FALSE : null;
  }

  /** Sets allow creating map markers override from GUI string. */
  public void setOverrideAllowCreatingMapMarkers(String value) {
    this.overrideAllowCreatingMapMarkers = "enabled".equals(value) ? Boolean.TRUE : "disabled".equals(value) ? Boolean.FALSE : null;
  }

  /** Gets allow teleport to coordinates as GUI string. */
  public String getOverrideAllowTeleportToCoordinatesStr() {
    return overrideAllowTeleportToCoordinates == null ? "inherit" : overrideAllowTeleportToCoordinates ? "enabled" : "disabled";
  }

  /** Gets allow teleport to markers as GUI string. */
  public String getOverrideAllowTeleportToMarkersStr() {
    return overrideAllowTeleportToMarkers == null ? "inherit" : overrideAllowTeleportToMarkers ? "enabled" : "disabled";
  }

  /** Gets allow creating map markers as GUI string. */
  public String getOverrideAllowCreatingMapMarkersStr() {
    return overrideAllowCreatingMapMarkers == null ? "inherit" : overrideAllowCreatingMapMarkers ? "enabled" : "disabled";
  }

  /**
   * Checks if faction tags should be shown on the world map.
   * When enabled, claimed chunks display faction tag text in the corner.
   * When disabled, only faction colors are shown (no text).
   *
   * @return true if faction tags should be displayed
   */
  public boolean isShowFactionTags() {
    return showFactionTags;
  }

  /**
   * Gets the threshold for faction-wide refresh operations.
   * If a faction has more claims than this threshold, a full refresh is used
   * instead of queuing each chunk individually.
   *
   * @return the threshold (default: 200)
   */
  public int getFactionWideRefreshThreshold() {
    return factionWideRefreshThreshold;
  }

  // === Player Visibility Getters ===

  /**
   * Checks if player visibility filtering is enabled.
   * When disabled, all players are visible on the map (vanilla behavior).
   *
   * @return true if filtering is enabled
   */
  public boolean isPlayerVisibilityEnabled() {
    return playerVisibilityEnabled;
  }

  /**
   * Checks if own faction members are shown on the map.
   *
   * @return true if own faction is visible
   */
  public boolean isShowOwnFaction() {
    return showOwnFaction;
  }

  /**
   * Checks if allied faction members are shown on the map.
   *
   * @return true if allies are visible
   */
  public boolean isShowAllies() {
    return showAllies;
  }

  /**
   * Checks if neutral faction members are shown on the map.
   *
   * @return true if neutrals are visible
   */
  public boolean isShowNeutrals() {
    return showNeutrals;
  }

  /**
   * Checks if enemy faction members are shown on the map.
   *
   * @return true if enemies are visible
   */
  public boolean isShowEnemies() {
    return showEnemies;
  }

  /**
   * Checks if factionless players are shown to faction members on the map.
   *
   * @return true if factionless players are visible
   */
  public boolean isShowFactionlessPlayers() {
    return showFactionlessPlayers;
  }

  /**
   * Checks if factionless players can see other factionless players on the map.
   *
   * @return true if factionless-to-factionless is visible
   */
  public boolean isShowFactionlessToFactionless() {
    return showFactionlessToFactionless;
  }
}
