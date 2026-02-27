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
  private int configVersion = 6;

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

  private String updateCheckUrl = "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";

  private String releaseChannel = "stable";

  // HyperProtect-Mixin management
  private boolean hyperProtectAutoDownload = false;

  private boolean hyperProtectAutoUpdate = true;

  private String hyperProtectUpdateUrl = "https://api.github.com/repos/HyperSystemsDev/HyperProtect-Mixin/releases/latest";

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
    configVersion = 6;
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

    // Update settings
    if (hasSection(root, "updates")) {
      JsonObject updates = root.getAsJsonObject("updates");
      updateCheckEnabled = getBool(updates, "enabled", updateCheckEnabled);
      updateCheckUrl = getString(updates, "url", updateCheckUrl);
      if (OLD_ZENITH_URL.equals(updateCheckUrl)) {
        updateCheckUrl = "https://api.github.com/repos/HyperSystemsDev/HyperFactions/releases/latest";
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
    releaseChannel = validateEnum(result, "updates.releaseChannel", releaseChannel,
        new String[]{"stable", "prerelease"}, "stable");
    validateHexColor(result, "messages.primaryColor", primaryColor);

    return result;
  }
}
