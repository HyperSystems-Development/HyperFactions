package com.hyperfactions.chat;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.util.LegacyColorParser;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom chat formatter that adds faction tags with relation-based coloring.
 *
 * <p>Each viewer sees the sender's faction tag colored based on their relation:
 * - Green: Same faction (OWN)
 * - Pink: Ally
 * - Gray: Neutral
 * - Red: Enemy
 *
 * <p>Format placeholders:
 * - {faction_tag}: Faction tag with relation color
 * - {prefix}: Permission plugin prefix
 * - {suffix}: Permission plugin suffix
 * - {player}: Player username
 * - {message}: Chat message content
 */
public class FactionChatFormatter implements PlayerChatEvent.Formatter {

  private final FactionManager factionManager;

  private final RelationManager relationManager;

  private final ConfigManager config;

  /**
   * Creates a new FactionChatFormatter.
   *
   * @param factionManager  the faction manager
   * @param relationManager the relation manager
   */
  public FactionChatFormatter(@NotNull FactionManager factionManager,
                @NotNull RelationManager relationManager) {
    this.factionManager = factionManager;
    this.relationManager = relationManager;
    this.config = ConfigManager.get();
  }

  /** Formats . */
  @Override
  @NotNull
  public Message format(@NotNull PlayerRef target, @NotNull String content) {
    // Get sender from ThreadLocal context
    PlayerRef sender = ChatContext.getSender();
    if (sender == null) {
      // Fall back to default formatting if sender is not available
      return Message.translation("server.chat.playerMessage")
        .param("username", target.getUsername())
        .param("message", content);
    }

    UUID senderUuid = sender.getUuid();
    UUID targetUuid = target.getUuid();

    // Get factions
    Faction senderFaction = factionManager.getPlayerFaction(senderUuid);
    Faction targetFaction = factionManager.getPlayerFaction(targetUuid);

    // Determine relation from TARGET's perspective (what color THEY see)
    RelationType relation = determineRelation(senderFaction, targetFaction, senderUuid, targetUuid);
    String relationColor = getRelationColor(relation);

    // Get permission plugin prefix/suffix with world context
    String worldName = getWorldName(sender);
    String prefix = PermissionManager.get().getPrefix(senderUuid, worldName);
    String suffix = PermissionManager.get().getSuffix(senderUuid, worldName);

    // Build faction tag with relation color
    String factionTag = buildFactionTag(senderFaction, relationColor);

    // Parse format string and build message
    String format = config.getChatFormat();
    return buildFormattedMessage(format, factionTag, prefix, sender.getUsername(), suffix, content, relationColor);
  }

  /**
   * Determines the relation type from the target's perspective.
   */
  private RelationType determineRelation(@Nullable Faction senderFaction,
                      @Nullable Faction targetFaction,
                      @NotNull UUID senderUuid,
                      @NotNull UUID targetUuid) {
    // Own message (viewing your own chat)
    if (senderUuid.equals(targetUuid)) {
      return RelationType.OWN;
    }

    // Sender has no faction
    if (senderFaction == null) {
      return RelationType.NEUTRAL;
    }

    // Target has no faction
    if (targetFaction == null) {
      return RelationType.NEUTRAL;
    }

    // Same faction
    if (senderFaction.id().equals(targetFaction.id())) {
      return RelationType.OWN;
    }

    // Get relation from target's faction to sender's faction
    return relationManager.getRelation(targetFaction.id(), senderFaction.id());
  }

  /**
   * Gets the hex color for a relation type from config.
   */
  @NotNull
  private String getRelationColor(@NotNull RelationType relation) {
    return switch (relation) {
      case OWN -> config.getChatRelationColorOwn();
      case ALLY -> config.getChatRelationColorAlly();
      case NEUTRAL -> config.getChatRelationColorNeutral();
      case ENEMY -> config.getChatRelationColorEnemy();
    };
  }

  /**
   * Builds the faction tag string based on config settings.
   */
  @NotNull
  private String buildFactionTag(@Nullable Faction faction, @NotNull String color) {
    if (faction == null) {
      // Player has no faction
      return config.getChatNoFactionTag();
    }

    String tagDisplay = config.getChatTagDisplay();
    String tagFormat = config.getChatTagFormat();

    // Determine what to display
    String tagValue;
    switch (tagDisplay.toLowerCase()) {
      case "name":
        tagValue = faction.name();
        break;
      case "none":
        return "";
      case "tag":
      default:
        tagValue = faction.tag();
        if (tagValue == null || tagValue.isEmpty()) {
          // Fall back to first 3 chars of name if no tag
          String name = faction.name();
          tagValue = name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
        }
        break;
    }

    // Apply format: e.g., "[{tag}] " -> "[ABC] "
    return tagFormat.replace("{tag}", tagValue);
  }

  /**
   * Builds the formatted message from the format string and components.
   */
  @NotNull
  private Message buildFormattedMessage(@NotNull String format,
                     @NotNull String factionTag,
                     @NotNull String prefix,
                     @NotNull String playerName,
                     @NotNull String suffix,
                     @NotNull String messageContent,
                     @NotNull String factionTagColor) {
    // Split the format into segments and build Message with proper coloring
    // Format example: "{faction_tag}{prefix}{player}{suffix}: {message}"

    Message result = Message.empty();

    // Parse format string
    int lastIndex = 0;
    while (lastIndex < format.length()) {
      int nextPlaceholder = format.indexOf('{', lastIndex);

      if (nextPlaceholder == -1) {
        // No more placeholders, append rest as literal text
        if (lastIndex < format.length()) {
          String literal = format.substring(lastIndex);
          result = result.insert(Message.raw(literal).color("#AAAAAA"));
        }
        break;
      }

      // Append literal text before placeholder
      if (nextPlaceholder > lastIndex) {
        String literal = format.substring(lastIndex, nextPlaceholder);
        result = result.insert(Message.raw(literal).color("#AAAAAA"));
      }

      // Find end of placeholder
      int endPlaceholder = format.indexOf('}', nextPlaceholder);
      if (endPlaceholder == -1) {
        // Malformed format, treat rest as literal
        String literal = format.substring(nextPlaceholder);
        result = result.insert(Message.raw(literal).color("#AAAAAA"));
        break;
      }

      // Extract placeholder name
      String placeholder = format.substring(nextPlaceholder + 1, endPlaceholder);

      // Replace placeholder with content
      switch (placeholder) {
        case "faction_tag":
          if (!factionTag.isEmpty()) {
            result = result.insert(Message.raw(factionTag).color(factionTagColor));
          }
          break;
        case "prefix":
          if (!prefix.isEmpty()) {
            // Prefix may contain color codes, parse them
            result = result.insert(parseColoredString(prefix));
          }
          break;
        case "suffix":
          if (!suffix.isEmpty()) {
            result = result.insert(parseColoredString(suffix));
          }
          break;
        case "player":
          result = result.insert(Message.raw(playerName).color(config.getChatPlayerNameColor()));
          break;
        case "message":
          result = result.insert(Message.raw(messageContent).color("#FFFFFF"));
          break;
        default:
          // Unknown placeholder, keep as-is
          result = result.insert(Message.raw("{" + placeholder + "}").color("#AAAAAA"));
          break;
      }

      lastIndex = endPlaceholder + 1;
    }

    return result;
  }

  /**
   * Parses a string that may contain legacy color codes (&amp;a, &amp;b, etc.),
   * section symbol codes (§a, §b, etc.), or hex colors (&#RRGGBB) into a
   * Message with proper coloring preserved.
   *
   * <p>This is essential for displaying HyperPerms prefixes/suffixes with
   * their intended colors in chat.
   */
  @NotNull
  private Message parseColoredString(@NotNull String text) {
    if (text == null || text.isEmpty()) {
      return Message.raw("");
    }
    return LegacyColorParser.parse(text);
  }

  /**
   * Gets the world name for a player, used for contextual prefix/suffix lookup.
   * Note: PlayerRef doesn't expose world directly in the chat formatter context,
   * so we return null to use the default (non-world-specific) prefix/suffix.
   * For world-specific lookups, the caller would need to pass the world context.
   *
   * @param player the player reference
   * @return the world name, or null if not available
   */
  @Nullable
  private String getWorldName(@NotNull PlayerRef player) {
    // PlayerRef doesn't directly expose world in this context
    // The prefix/suffix lookup will use the default (non-world-specific) values
    // For full world context support, the format() method would need the World parameter
    return null;
  }
}
