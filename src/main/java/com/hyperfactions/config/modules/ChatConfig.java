package com.hyperfactions.config.modules;

import com.google.gson.JsonObject;
import com.hyperfactions.config.ModuleConfig;
import com.hyperfactions.config.ValidationResult;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the chat formatting system.
 *
 * <p>
 * Controls how faction tags appear in chat and the colors used
 * for different faction relationships.
 */
public class ChatConfig extends ModuleConfig {

  // Format settings
  private String format = "{faction_tag}{prefix}{player}{suffix}: {message}";

  private String tagDisplay = "tag";    // "tag", "name", or "none"
  private String tagFormat = "[{tag}] ";

  private String noFactionTag = "";           // Tag for non-faction players
  private String noFactionTagColor = "#555555"; // Color for no-faction tag (dark gray)
  private String playerNameColor = "#FFFFFF";  // Color for {player} in public chat
  private String priority = "LATE";             // Event priority

  // Relation colors
  private String relationColorOwn = "#00FF00";     // Green - same faction
  private String relationColorAlly = "#FF69B4";    // Pink - allies
  private String relationColorNeutral = "#AAAAAA"; // Gray - neutral
  private String relationColorEnemy = "#FF0000";   // Red - enemies

  // Faction chat settings
  private String factionChatColor = "#00FFFF";       // Cyan - faction messages
  private String factionChatPrefix = "[Faction]";

  private String allyChatColor = "#AA00AA";          // Purple - ally messages
  private String allyChatPrefix = "[Ally]";

  private String senderNameColor = "#FFFF55";        // Yellow - sender name
  private String messageColor = "#FFFFFF";            // White - message text
  private boolean historyEnabled = true;

  private int historyMaxMessages = 200;

  private int historyRetentionDays = 7;

  private int historyCleanupIntervalMinutes = 60;

  /**
   * Creates a new chat config.
   *
   * @param filePath path to config/chat.json
   */
  public ChatConfig(@NotNull Path filePath) {
    super(filePath);
  }

  /** Returns the module name. */
  @Override
  @NotNull
  public String getModuleName() {
    return "chat";
  }

  /** Creates defaults. */
  @Override
  protected void createDefaults() {
    enabled = true;
    format = "{faction_tag}{prefix}{player}{suffix}: {message}";
    tagDisplay = "tag";
    tagFormat = "[{tag}] ";
    noFactionTag = "";
    noFactionTagColor = "#555555";
    playerNameColor = "#FFFFFF";
    priority = "LATE";
    relationColorOwn = "#00FF00";
    relationColorAlly = "#FF69B4";
    relationColorNeutral = "#AAAAAA";
    relationColorEnemy = "#FF0000";
    factionChatColor = "#00FFFF";
    factionChatPrefix = "[Faction]";
    allyChatColor = "#AA00AA";
    allyChatPrefix = "[Ally]";
    senderNameColor = "#FFFF55";
    messageColor = "#FFFFFF";
    historyEnabled = true;
    historyMaxMessages = 200;
    historyRetentionDays = 7;
    historyCleanupIntervalMinutes = 60;
  }

  /** Loads module settings. */
  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    format = getString(root, "format", format);
    tagDisplay = getString(root, "tagDisplay", tagDisplay);
    tagFormat = getString(root, "tagFormat", tagFormat);
    noFactionTag = getString(root, "noFactionTag", noFactionTag);
    noFactionTagColor = getString(root, "noFactionTagColor", noFactionTagColor);
    playerNameColor = getString(root, "playerNameColor", playerNameColor);
    priority = getString(root, "priority", priority);

    // Load relation colors
    if (hasSection(root, "relationColors")) {
      JsonObject colors = root.getAsJsonObject("relationColors");
      relationColorOwn = getString(colors, "own", relationColorOwn);
      relationColorAlly = getString(colors, "ally", relationColorAlly);
      relationColorNeutral = getString(colors, "neutral", relationColorNeutral);
      relationColorEnemy = getString(colors, "enemy", relationColorEnemy);
    }

    // Load faction chat settings
    if (hasSection(root, "factionChat")) {
      JsonObject fc = root.getAsJsonObject("factionChat");
      factionChatColor = getString(fc, "factionChatColor", factionChatColor);
      factionChatPrefix = getString(fc, "factionChatPrefix", factionChatPrefix);
      allyChatColor = getString(fc, "allyChatColor", allyChatColor);
      allyChatPrefix = getString(fc, "allyChatPrefix", allyChatPrefix);
      senderNameColor = getString(fc, "senderNameColor", senderNameColor);
      messageColor = getString(fc, "messageColor", messageColor);
      historyEnabled = getBool(fc, "historyEnabled", historyEnabled);
      historyMaxMessages = getInt(fc, "historyMaxMessages", historyMaxMessages);
      historyRetentionDays = getInt(fc, "historyRetentionDays", historyRetentionDays);
      historyCleanupIntervalMinutes = getInt(fc, "historyCleanupIntervalMinutes", historyCleanupIntervalMinutes);
    }
  }

  /** Write Module Settings. */
  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("_formatNote", "format is only used if no permissions plugin is detected");
    root.addProperty("format", format);
    root.addProperty("tagDisplay", tagDisplay);
    root.addProperty("tagFormat", tagFormat);
    root.addProperty("noFactionTag", noFactionTag);
    root.addProperty("noFactionTagColor", noFactionTagColor);
    root.addProperty("playerNameColor", playerNameColor);
    root.addProperty("priority", priority);

    JsonObject colors = new JsonObject();
    colors.addProperty("own", relationColorOwn);
    colors.addProperty("ally", relationColorAlly);
    colors.addProperty("neutral", relationColorNeutral);
    colors.addProperty("enemy", relationColorEnemy);
    root.add("relationColors", colors);

    JsonObject fc = new JsonObject();
    fc.addProperty("factionChatColor", factionChatColor);
    fc.addProperty("factionChatPrefix", factionChatPrefix);
    fc.addProperty("allyChatColor", allyChatColor);
    fc.addProperty("allyChatPrefix", allyChatPrefix);
    fc.addProperty("senderNameColor", senderNameColor);
    fc.addProperty("messageColor", messageColor);
    fc.addProperty("historyEnabled", historyEnabled);
    fc.addProperty("historyMaxMessages", historyMaxMessages);
    fc.addProperty("historyRetentionDays", historyRetentionDays);
    fc.addProperty("historyCleanupIntervalMinutes", historyCleanupIntervalMinutes);
    root.add("factionChat", fc);
  }

  // === Getters ===

  /**
   * Gets the chat format template.
   *
   * @return format string with placeholders
   */
  @NotNull
  public String getFormat() {
    return format;
  }

  /**
   * Gets the tag display mode.
   *
   * @return "tag", "name", or "none"
   */
  @NotNull
  public String getTagDisplay() {
    return tagDisplay;
  }

  /**
   * Gets the tag format template.
   *
   * @return tag format string
   */
  @NotNull
  public String getTagFormat() {
    return tagFormat;
  }

  /**
   * Gets the tag shown for players without a faction.
   *
   * @return no-faction tag (may be empty)
   */
  @NotNull
  public String getNoFactionTag() {
    return noFactionTag;
  }

  /** Returns the no faction tag color. */
  @NotNull
  public String getNoFactionTagColor() {
    return noFactionTagColor;
  }

  /**
   * Gets the color for player names in public chat ({player} placeholder).
   *
   * @return hex color (e.g., "#FFFFFF")
   */
  @NotNull
  public String getPlayerNameColor() {
    return playerNameColor;
  }

  /**
   * Gets the event priority for chat formatting.
   *
   * @return priority string (e.g., "LATE")
   */
  @NotNull
  public String getPriority() {
    return priority;
  }

  /**
   * Gets the color for same-faction members.
   *
   * @return hex color (e.g., "#00FF00")
   */
  @NotNull
  public String getRelationColorOwn() {
    return relationColorOwn;
  }

  /**
   * Gets the color for allied factions.
   *
   * @return hex color (e.g., "#FF69B4")
   */
  @NotNull
  public String getRelationColorAlly() {
    return relationColorAlly;
  }

  /**
   * Gets the color for neutral factions.
   *
   * @return hex color (e.g., "#AAAAAA")
   */
  @NotNull
  public String getRelationColorNeutral() {
    return relationColorNeutral;
  }

  /**
   * Gets the color for enemy factions.
   *
   * @return hex color (e.g., "#FF0000")
   */
  @NotNull
  public String getRelationColorEnemy() {
    return relationColorEnemy;
  }

  // === Faction Chat Getters ===

  /** Returns the faction chat color. */
  @NotNull
  public String getFactionChatColor() {
    return factionChatColor;
  }

  /** Returns the faction chat prefix. */
  @NotNull
  public String getFactionChatPrefix() {
    return factionChatPrefix;
  }

  /** Returns the ally chat color. */
  @NotNull
  public String getAllyChatColor() {
    return allyChatColor;
  }

  /** Returns the ally chat prefix. */
  @NotNull
  public String getAllyChatPrefix() {
    return allyChatPrefix;
  }

  /** Returns the sender name color. */
  @NotNull
  public String getSenderNameColor() {
    return senderNameColor;
  }

  /** Returns the message color. */
  @NotNull
  public String getMessageColor() {
    return messageColor;
  }

  /** Checks if history enabled. */
  public boolean isHistoryEnabled() {
    return historyEnabled;
  }

  /** Returns the history max messages. */
  public int getHistoryMaxMessages() {
    return historyMaxMessages;
  }

  /** Returns the history retention days. */
  public int getHistoryRetentionDays() {
    return historyRetentionDays;
  }

  public int getHistoryCleanupIntervalMinutes() {
    return historyCleanupIntervalMinutes;
  }

  // === Validation ===

  /** Validates . */
  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();

    // Validate tagDisplay
    tagDisplay = validateEnum(result, "tagDisplay", tagDisplay,
        new String[]{"tag", "name", "none"}, "tag");

    // Validate priority
    priority = validateEnum(result, "priority", priority,
        new String[]{"EARLIEST", "EARLY", "NORMAL", "LATE", "LATEST"}, "LATE");

    // Validate hex colors (warn but don't correct)
    validateHexColor(result, "noFactionTagColor", noFactionTagColor);
    validateHexColor(result, "playerNameColor", playerNameColor);
    validateHexColor(result, "relationColors.own", relationColorOwn);
    validateHexColor(result, "relationColors.ally", relationColorAlly);
    validateHexColor(result, "relationColors.neutral", relationColorNeutral);
    validateHexColor(result, "relationColors.enemy", relationColorEnemy);

    // Validate faction chat colors
    validateHexColor(result, "factionChat.factionChatColor", factionChatColor);
    validateHexColor(result, "factionChat.allyChatColor", allyChatColor);
    validateHexColor(result, "factionChat.senderNameColor", senderNameColor);
    validateHexColor(result, "factionChat.messageColor", messageColor);

    // Validate faction chat numeric settings
    if (historyMaxMessages < 10) {
      result.addWarning(getConfigName(), "factionChat.historyMaxMessages",
          "Must be at least 10", historyMaxMessages, 10);
      historyMaxMessages = 10;
    } else if (historyMaxMessages > 1000) {
      result.addWarning(getConfigName(), "factionChat.historyMaxMessages",
          "Capped at 1000", historyMaxMessages, 1000);
      historyMaxMessages = 1000;
    }

    if (historyRetentionDays < 1) {
      result.addWarning(getConfigName(), "factionChat.historyRetentionDays",
          "Must be at least 1", historyRetentionDays, 1);
      historyRetentionDays = 1;
    }

    if (historyCleanupIntervalMinutes < 5) {
      result.addWarning(getConfigName(), "factionChat.historyCleanupIntervalMinutes",
          "Must be at least 5", historyCleanupIntervalMinutes, 5);
      historyCleanupIntervalMinutes = 5;
    }

    return result;
  }
}
