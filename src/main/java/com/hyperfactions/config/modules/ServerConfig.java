package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Server behavior configuration (config/server.json).
 *
 * <p>
 * Contains server-level settings: teleport mechanics, auto-save, message formatting,
 * GUI, permissions, update checking, and HyperProtect-Mixin management.
 *
 * <p>
 * Also holds the {@code configVersion} field used for migration tracking.
 * Previously these settings lived in the monolithic config.json (CoreConfig).
 * Migration V5→V6 extracts them into this dedicated file.
 */
public class ServerConfig extends ModuleConfig {

  // Config version (for migration tracking — lives here after V5→V6)
  private int configVersion = 8;

  // Teleport settings
  private int warmupSeconds = 5;

  private int cooldownSeconds = 300;

  private boolean cancelOnMove = true;

  private boolean cancelOnDamage = true;

  // Auto-save settings
  private boolean autoSaveEnabled = true;

  private int autoSaveIntervalMinutes = 5;

  // Message settings (v3 format: structured prefix)
  private String prefixText = "HyperFactions";

  private String prefixColor = "#55FFFF";

  private String prefixBracketColor = "#AAAAAA";

  private String primaryColor = "#00FFFF";

  // GUI settings
  private String guiTitle = "HyperFactions";

  private boolean terrainMapEnabled = true;

  private int leaderboardKdRefreshSeconds = 300;

  // Permission settings
  private boolean adminRequiresOp = true;

  private boolean allowWithoutPermissionMod = false;

  // Update settings
  private boolean updateCheckEnabled = true;

  private static final String OLD_ZENITH_URL = "https://api.github.com/repos/ZenithDevHQ/HyperFactions/releases/latest";

  private static final String OLD_HYPERSYSTEMSDEV_URL = "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";

  private static final String OLD_HYPERSYSTEMSDEV_HP_URL = "https://api.github.com/repos/HyperSystemsDev/HyperProtect-Mixin/releases/latest";

  private String updateCheckUrl = "https://api.github.com/repos/HyperSystems-Development/HyperFactions/releases/latest";

  private String releaseChannel = "stable";

  // Mob clearing settings
  private boolean mobClearEnabled = true;

  private int mobClearIntervalSeconds = 10;

  // Language / i18n settings
  private String defaultLanguage = "en-US";

  private boolean usePlayerLanguage = true;

  // HyperProtect-Mixin management
  private boolean hyperProtectAutoDownload = false;

  private boolean hyperProtectAutoUpdate = true;

  private String hyperProtectUpdateUrl = "https://api.github.com/repos/HyperSystems-Development/HyperProtect-Mixin/releases/latest";

  /** Creates a new ServerConfig. */
  public ServerConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "server";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    configVersion = 8;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    configVersion = getInt(root, "configVersion", configVersion);

    // Teleport settings
    if (hasSection(root, "teleport")) {
      JsonObject teleport = root.getAsJsonObject("teleport");
      warmupSeconds = getInt(teleport, "warmupSeconds", warmupSeconds);
      cooldownSeconds = getInt(teleport, "cooldownSeconds", cooldownSeconds);
      cancelOnMove = getBool(teleport, "cancelOnMove", cancelOnMove);
      cancelOnDamage = getBool(teleport, "cancelOnDamage", cancelOnDamage);
    }

    // Auto-save settings
    if (hasSection(root, "autoSave")) {
      JsonObject autoSave = root.getAsJsonObject("autoSave");
      autoSaveEnabled = getBool(autoSave, "enabled", autoSaveEnabled);
      autoSaveIntervalMinutes = getInt(autoSave, "intervalMinutes", autoSaveIntervalMinutes);
    }

    // Message settings (supports both v2 string format and v3 structured format)
    if (hasSection(root, "messages")) {
      JsonObject messages = root.getAsJsonObject("messages");
      primaryColor = getString(messages, "primaryColor", primaryColor);

      if (messages.has("prefix")) {
        var prefixElement = messages.get("prefix");
        if (prefixElement.isJsonObject()) {
          JsonObject prefixObj = prefixElement.getAsJsonObject();
          prefixText = getString(prefixObj, "text", prefixText);
          prefixColor = getString(prefixObj, "color", prefixColor);
          prefixBracketColor = getString(prefixObj, "bracketColor", prefixBracketColor);
        } else {
          // Legacy v2 string format
          String oldPrefix = prefixElement.getAsString();
          prefixText = oldPrefix
              .replaceAll("\u00A7[0-9a-fk-or]", "")
              .replaceAll("&[0-9a-fk-or]", "")
              .replaceAll("[\\[\\]]", "")
              .trim();
          if (prefixText.isEmpty()) {
            prefixText = "HyperFactions";
          }
        }
      }
    }

    // GUI settings
    if (hasSection(root, "gui")) {
      JsonObject gui = root.getAsJsonObject("gui");
      guiTitle = getString(gui, "title", guiTitle);
      terrainMapEnabled = getBool(gui, "terrainMapEnabled", terrainMapEnabled);
      leaderboardKdRefreshSeconds = getInt(gui, "leaderboardKdRefreshSeconds", leaderboardKdRefreshSeconds);
    }

    // Permission settings
    if (hasSection(root, "permissions")) {
      JsonObject permissions = root.getAsJsonObject("permissions");
      adminRequiresOp = getBool(permissions, "adminRequiresOp", adminRequiresOp);
      allowWithoutPermissionMod = getBool(permissions, "allowWithoutPermissionMod", allowWithoutPermissionMod);
    }

    // Language / i18n settings
    if (hasSection(root, "language")) {
      JsonObject language = root.getAsJsonObject("language");
      defaultLanguage = getString(language, "default", defaultLanguage);
      usePlayerLanguage = getBool(language, "usePlayerLanguage", usePlayerLanguage);
    }

    // Mob clearing settings
    if (hasSection(root, "mobClearing")) {
      JsonObject mobClearing = root.getAsJsonObject("mobClearing");
      mobClearEnabled = getBool(mobClearing, "enabled", mobClearEnabled);
      mobClearIntervalSeconds = getInt(mobClearing, "intervalSeconds", mobClearIntervalSeconds);
    }

    // Update settings
    if (hasSection(root, "updates")) {
      JsonObject updates = root.getAsJsonObject("updates");
      updateCheckEnabled = getBool(updates, "enabled", updateCheckEnabled);
      updateCheckUrl = getString(updates, "url", updateCheckUrl);
      if (OLD_ZENITH_URL.equals(updateCheckUrl)) {
        updateCheckUrl = "https://api.github.com/repos/HyperSystems-Development/HyperFactions/releases/latest";
        needsSave = true;
      }
      if (OLD_HYPERSYSTEMSDEV_URL.equals(updateCheckUrl)) {
        updateCheckUrl = "https://api.github.com/repos/HyperSystems-Development/HyperFactions/releases/latest";
        needsSave = true;
      }
      releaseChannel = getString(updates, "releaseChannel", releaseChannel);
      if (!releaseChannel.equals("stable") && !releaseChannel.equals("prerelease")) {
        releaseChannel = "stable";
      }
      if (hasSection(updates, "hyperProtect")) {
        JsonObject hp = updates.getAsJsonObject("hyperProtect");
        hyperProtectAutoDownload = getBool(hp, "autoDownload", hyperProtectAutoDownload);
        hyperProtectAutoUpdate = getBool(hp, "autoUpdate", hyperProtectAutoUpdate);
        hyperProtectUpdateUrl = getString(hp, "url", hyperProtectUpdateUrl);
        if (OLD_HYPERSYSTEMSDEV_HP_URL.equals(hyperProtectUpdateUrl)) {
          hyperProtectUpdateUrl = "https://api.github.com/repos/HyperSystems-Development/HyperProtect-Mixin/releases/latest";
          needsSave = true;
        }
      }
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("configVersion", configVersion);

    // Teleport settings
    JsonObject teleport = new JsonObject();
    teleport.addProperty("warmupSeconds", warmupSeconds);
    teleport.addProperty("cooldownSeconds", cooldownSeconds);
    teleport.addProperty("cancelOnMove", cancelOnMove);
    teleport.addProperty("cancelOnDamage", cancelOnDamage);
    root.add("teleport", teleport);

    // Auto-save settings
    JsonObject autoSave = new JsonObject();
    autoSave.addProperty("enabled", autoSaveEnabled);
    autoSave.addProperty("intervalMinutes", autoSaveIntervalMinutes);
    root.add("autoSave", autoSave);

    // Message settings
    JsonObject messages = new JsonObject();
    JsonObject prefixObj = new JsonObject();
    prefixObj.addProperty("text", prefixText);
    prefixObj.addProperty("color", prefixColor);
    prefixObj.addProperty("bracketColor", prefixBracketColor);
    messages.add("prefix", prefixObj);
    messages.addProperty("primaryColor", primaryColor);
    root.add("messages", messages);

    // GUI settings
    JsonObject gui = new JsonObject();
    gui.addProperty("title", guiTitle);
    gui.addProperty("terrainMapEnabled", terrainMapEnabled);
    gui.addProperty("leaderboardKdRefreshSeconds", leaderboardKdRefreshSeconds);
    root.add("gui", gui);

    // Permission settings
    JsonObject permissions = new JsonObject();
    permissions.addProperty("adminRequiresOp", adminRequiresOp);
    permissions.addProperty("allowWithoutPermissionMod", allowWithoutPermissionMod);
    root.add("permissions", permissions);

    // Language / i18n settings
    JsonObject language = new JsonObject();
    language.addProperty("default", defaultLanguage);
    language.addProperty("usePlayerLanguage", usePlayerLanguage);
    root.add("language", language);

    // Mob clearing settings
    JsonObject mobClearing = new JsonObject();
    mobClearing.addProperty("enabled", mobClearEnabled);
    mobClearing.addProperty("intervalSeconds", mobClearIntervalSeconds);
    root.add("mobClearing", mobClearing);

    // Update settings
    JsonObject updates = new JsonObject();
    updates.addProperty("enabled", updateCheckEnabled);
    updates.addProperty("url", updateCheckUrl);
    updates.addProperty("releaseChannel", releaseChannel);
    JsonObject hyperProtect = new JsonObject();
    hyperProtect.addProperty("autoDownload", hyperProtectAutoDownload);
    hyperProtect.addProperty("autoUpdate", hyperProtectAutoUpdate);
    hyperProtect.addProperty("url", hyperProtectUpdateUrl);
    updates.add("hyperProtect", hyperProtect);
    root.add("updates", updates);
  }

  // === Getters ===

  /** Returns the config version. */
  public int getConfigVersion() {
    return configVersion;
  }

  // Teleport
  /** Returns the warmup seconds. */
  public int getWarmupSeconds() {
    return warmupSeconds;
  }

  /** Returns the cooldown seconds. */
  public int getCooldownSeconds() {
    return cooldownSeconds;
  }

  /** Checks if cancel on move. */
  public boolean isCancelOnMove() {
    return cancelOnMove;
  }

  /** Checks if cancel on damage. */
  public boolean isCancelOnDamage() {
    return cancelOnDamage;
  }

  // Auto-save
  /** Checks if auto save enabled. */
  public boolean isAutoSaveEnabled() {
    return autoSaveEnabled;
  }

  /** Returns the auto save interval minutes. */
  public int getAutoSaveIntervalMinutes() {
    return autoSaveIntervalMinutes;
  }

  // Messages
  /** Returns the prefix text. */
  @NotNull public String getPrefixText() {
    return prefixText;
  }

  @NotNull public String getPrefixColor() {
    return prefixColor;
  }

  /** Returns the prefix bracket color. */
  @NotNull public String getPrefixBracketColor() {
    return prefixBracketColor;
  }

  /** Returns the primary color. */
  @NotNull public String getPrimaryColor() {
    return primaryColor;
  }

  // GUI
  @NotNull public String getGuiTitle() {
    return guiTitle;
  }

  /** Checks if terrain map enabled. */
  public boolean isTerrainMapEnabled() {
    return terrainMapEnabled;
  }

  public int getLeaderboardKdRefreshSeconds() {
    return leaderboardKdRefreshSeconds;
  }

  // Permissions
  public boolean isAdminRequiresOp() {
    return adminRequiresOp;
  }

  /** Checks if allow without permission mod. */
  public boolean isAllowWithoutPermissionMod() {
    return allowWithoutPermissionMod;
  }

  // Updates
  public boolean isUpdateCheckEnabled() {
    return updateCheckEnabled;
  }

  /** Returns the update check url. */
  @NotNull public String getUpdateCheckUrl() {
    return updateCheckUrl;
  }

  /** Returns the release channel. */
  @NotNull public String getReleaseChannel() {
    return releaseChannel;
  }

  /** Checks if pre release channel. */
  public boolean isPreReleaseChannel() {
    return "prerelease".equals(releaseChannel);
  }

  // Mob clearing
  /** Whether periodic mob clearing is enabled. */
  public boolean isMobClearEnabled() {
    return mobClearEnabled;
  }

  /** Returns the mob clear sweep interval in seconds. */
  public int getMobClearIntervalSeconds() {
    return mobClearIntervalSeconds;
  }

  // Language / i18n
  /** Returns the default server language code (e.g. "en-US"). */
  @NotNull public String getDefaultLanguage() {
    return defaultLanguage;
  }

  /** Whether to respect each player's client language for translations. */
  public boolean isUsePlayerLanguage() {
    return usePlayerLanguage;
  }

  // HyperProtect-Mixin
  /** Checks if hyper protect auto download. */
  public boolean isHyperProtectAutoDownload() {
    return hyperProtectAutoDownload;
  }

  /** Checks if hyper protect auto update. */
  public boolean isHyperProtectAutoUpdate() {
    return hyperProtectAutoUpdate;
  }

  /** Returns the hyper protect update url. */
  @NotNull public String getHyperProtectUpdateUrl() {
    return hyperProtectUpdateUrl;
  }

  /** Sets the hyper protect auto download. */
  public void setHyperProtectAutoDownload(boolean value) {
    this.hyperProtectAutoDownload = value;
  }

  // === Setters (for admin config editor) ===

  /** Sets warmup seconds. */
  public void setWarmupSeconds(int value) { this.warmupSeconds = value; }

  /** Sets cooldown seconds. */
  public void setCooldownSeconds(int value) { this.cooldownSeconds = value; }

  /** Sets cancel on move. */
  public void setCancelOnMove(boolean value) { this.cancelOnMove = value; }

  /** Sets cancel on damage. */
  public void setCancelOnDamage(boolean value) { this.cancelOnDamage = value; }

  /** Sets auto save enabled. */
  public void setAutoSaveEnabled(boolean value) { this.autoSaveEnabled = value; }

  /** Sets auto save interval minutes. */
  public void setAutoSaveIntervalMinutes(int value) { this.autoSaveIntervalMinutes = value; }

  /** Sets prefix text. */
  public void setPrefixText(@NotNull String value) { this.prefixText = value; }

  /** Sets prefix color. */
  public void setPrefixColor(@NotNull String value) { this.prefixColor = value; }

  /** Sets prefix bracket color. */
  public void setPrefixBracketColor(@NotNull String value) { this.prefixBracketColor = value; }

  /** Sets primary color. */
  public void setPrimaryColor(@NotNull String value) { this.primaryColor = value; }

  /** Sets gui title. */
  public void setGuiTitle(@NotNull String value) { this.guiTitle = value; }

  /** Sets terrain map enabled. */
  public void setTerrainMapEnabled(boolean value) { this.terrainMapEnabled = value; }

  /** Sets admin requires op. */
  public void setAdminRequiresOp(boolean value) { this.adminRequiresOp = value; }

  /** Sets allow without permission mod. */
  public void setAllowWithoutPermissionMod(boolean value) { this.allowWithoutPermissionMod = value; }

  /** Sets update check enabled. */
  public void setUpdateCheckEnabled(boolean value) { this.updateCheckEnabled = value; }

  /** Sets release channel. */
  public void setReleaseChannel(@NotNull String value) { this.releaseChannel = value; }

  /** Sets mob clear enabled. */
  public void setMobClearEnabled(boolean value) { this.mobClearEnabled = value; }

  /** Sets mob clear interval seconds. */
  public void setMobClearIntervalSeconds(int value) { this.mobClearIntervalSeconds = value; }

  /** Sets default language. */
  public void setDefaultLanguage(@NotNull String value) { this.defaultLanguage = value; }

  /** Sets use player language. */
  public void setUsePlayerLanguage(boolean value) { this.usePlayerLanguage = value; }

  /** Sets leaderboard K/D refresh seconds. */
  public void setLeaderboardKdRefreshSeconds(int value) { this.leaderboardKdRefreshSeconds = value; }

  /** Sets hyper protect auto update. */
  public void setHyperProtectAutoUpdate(boolean value) { this.hyperProtectAutoUpdate = value; }

  // === Validation ===

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    warmupSeconds = validateMin(result, "teleport.warmupSeconds", warmupSeconds, 0, 5);
    cooldownSeconds = validateMin(result, "teleport.cooldownSeconds", cooldownSeconds, 0, 300);
    autoSaveIntervalMinutes = validateMin(result, "autoSave.intervalMinutes", autoSaveIntervalMinutes, 1, 5);
    leaderboardKdRefreshSeconds = validateMin(result, "gui.leaderboardKdRefreshSeconds", leaderboardKdRefreshSeconds, 30, 300);
    mobClearIntervalSeconds = validateMin(result, "mobClearing.intervalSeconds", mobClearIntervalSeconds, 5, 10);
    releaseChannel = validateEnum(result, "updates.releaseChannel", releaseChannel,
        new String[]{"stable", "prerelease"}, "stable");
    validateHexColor(result, "messages.primaryColor", primaryColor);

    return result;
  }
}
