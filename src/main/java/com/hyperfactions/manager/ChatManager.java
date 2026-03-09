package com.hyperfactions.manager;

import com.hyperfactions.Permissions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.ChatMessage;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionRelation;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiUpdateService;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageKeys;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages chat channels for faction and ally chat.
 * Players can toggle between normal, faction, and ally chat modes.
 * Integrates with ChatHistoryManager for message persistence and
 * GuiUpdateService for real-time GUI refresh.
 */
public class ChatManager {

  /**
   * Chat channel types.
   */
  public enum ChatChannel {
    NORMAL,
    FACTION,
    ALLY
  }

  /**
   * Result of a chat channel toggle operation.
   */
  public enum ChatResult {
    SUCCESS,
    NO_PERMISSION
  }

  /**
   * Result of toggling a chat channel.
   *
   * @param result  the result of the operation
   * @param channel the new channel if successful, null otherwise
   */
  public record ToggleResult(
    @NotNull ChatResult result,
    @Nullable ChatChannel channel
  ) {
    /** Checks if success. */
    public boolean isSuccess() {
      return result == ChatResult.SUCCESS;
    }
  }

  /**
   * Listener for chat messages. Called after each message broadcast.
   * Useful for external integrations (e.g., Discord bridge).
   */
  @FunctionalInterface
  public interface ChatMessageListener {
    void onMessage(@NotNull ChatMessage message, @NotNull UUID factionId);
  }

  // Player UUID -> current chat channel
  private final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();

  private final FactionManager factionManager;

  private final RelationManager relationManager;

  private final Function<UUID, PlayerRef> playerLookup;

  private @Nullable ChatHistoryManager chatHistoryManager;

  private @Nullable GuiUpdateService guiUpdateService;

  private final List<ChatMessageListener> messageListeners = new CopyOnWriteArrayList<>();

  /**
   * Creates a new ChatManager.
   *
   * @param factionManager  the faction manager
   * @param relationManager the relation manager
   * @param playerLookup    function to look up online PlayerRef by UUID
   */
  public ChatManager(@NotNull FactionManager factionManager,
           @NotNull RelationManager relationManager,
           @NotNull Function<UUID, PlayerRef> playerLookup) {
    this.factionManager = factionManager;
    this.relationManager = relationManager;
    this.playerLookup = playerLookup;
  }

  /**
   * Sets the chat history manager for message persistence.
   *
   * @param chatHistoryManager the history manager
   */
  public void setChatHistoryManager(@Nullable ChatHistoryManager chatHistoryManager) {
    this.chatHistoryManager = chatHistoryManager;
  }

  /**
   * Sets the GUI update service for real-time page refresh.
   *
   * @param guiUpdateService the GUI update service
   */
  public void setGuiUpdateService(@Nullable GuiUpdateService guiUpdateService) {
    this.guiUpdateService = guiUpdateService;
  }

  /**
   * Adds a listener that is notified after each faction/ally message is broadcast.
   *
   * @param listener the listener
   */
  public void addMessageListener(@NotNull ChatMessageListener listener) {
    messageListeners.add(listener);
  }

  /**
   * Removes a message listener.
   *
   * @param listener the listener to remove
   */
  public void removeMessageListener(@NotNull ChatMessageListener listener) {
    messageListeners.remove(listener);
  }

  // ============================================================
  // Channel Management
  // ============================================================

  /**
   * Gets a player's current chat channel.
   *
   * @param playerUuid the player's UUID
   * @return the current channel (defaults to NORMAL)
   */
  @NotNull
  public ChatChannel getChannel(@NotNull UUID playerUuid) {
    return playerChannels.getOrDefault(playerUuid, ChatChannel.NORMAL);
  }

  /**
   * Sets a player's chat channel.
   *
   * @param playerUuid the player's UUID
   * @param channel    the channel to set
   */
  public void setChannel(@NotNull UUID playerUuid, @NotNull ChatChannel channel) {
    if (channel == ChatChannel.NORMAL) {
      playerChannels.remove(playerUuid);
    } else {
      playerChannels.put(playerUuid, channel);
    }
  }

  /**
   * Cycles through chat channels: Normal -> Faction -> Ally -> Normal,
   * skipping modes the player lacks permission for.
   *
   * @param playerUuid the player's UUID
   * @return the result with new channel state
   */
  @NotNull
  public ToggleResult cycleChannelChecked(@NotNull UUID playerUuid) {
    ChatChannel current = getChannel(playerUuid);
    boolean hasFaction = PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_FACTION);
    boolean hasAlly = PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_ALLY);

    ChatChannel next = switch (current) {
      case NORMAL -> {
        if (hasFaction) {
          yield ChatChannel.FACTION;
        }
        if (hasAlly) {
          yield ChatChannel.ALLY;
        }
        yield ChatChannel.NORMAL;
      }
      case FACTION -> {
        if (hasAlly) {
          yield ChatChannel.ALLY;
        }
        yield ChatChannel.NORMAL;
      }
      case ALLY -> ChatChannel.NORMAL;
    };

    setChannel(playerUuid, next);
    return new ToggleResult(ChatResult.SUCCESS, next);
  }

  /**
   * Sets a player directly to faction chat mode with permission check.
   *
   * @param playerUuid the player's UUID
   * @return the toggle result
   */
  @NotNull
  public ToggleResult setFactionChatChecked(@NotNull UUID playerUuid) {
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_FACTION)) {
      return new ToggleResult(ChatResult.NO_PERMISSION, null);
    }
    setChannel(playerUuid, ChatChannel.FACTION);
    return new ToggleResult(ChatResult.SUCCESS, ChatChannel.FACTION);
  }

  /**
   * Sets a player directly to ally chat mode with permission check.
   *
   * @param playerUuid the player's UUID
   * @return the toggle result
   */
  @NotNull
  public ToggleResult setAllyChatChecked(@NotNull UUID playerUuid) {
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_ALLY)) {
      return new ToggleResult(ChatResult.NO_PERMISSION, null);
    }
    setChannel(playerUuid, ChatChannel.ALLY);
    return new ToggleResult(ChatResult.SUCCESS, ChatChannel.ALLY);
  }

  /**
   * Sets a player's chat to normal mode (no permission check needed).
   *
   * @param playerUuid the player's UUID
   */
  public void setNormalChat(@NotNull UUID playerUuid) {
    setChannel(playerUuid, ChatChannel.NORMAL);
  }

  /**
   * Toggles faction chat for a player with permission check.
   *
   * @param playerUuid the player's UUID
   * @return the result with new channel state if successful
   */
  @NotNull
  public ToggleResult toggleFactionChatChecked(@NotNull UUID playerUuid) {
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_FACTION)) {
      return new ToggleResult(ChatResult.NO_PERMISSION, null);
    }

    ChatChannel newChannel = toggleFactionChat(playerUuid);
    return new ToggleResult(ChatResult.SUCCESS, newChannel);
  }

  /**
   * Toggles faction chat for a player.
   *
   * @param playerUuid the player's UUID
   * @return the new channel state
   */
  @NotNull
  public ChatChannel toggleFactionChat(@NotNull UUID playerUuid) {
    ChatChannel current = getChannel(playerUuid);
    if (current == ChatChannel.FACTION) {
      setChannel(playerUuid, ChatChannel.NORMAL);
      return ChatChannel.NORMAL;
    } else {
      setChannel(playerUuid, ChatChannel.FACTION);
      return ChatChannel.FACTION;
    }
  }

  /**
   * Toggles ally chat for a player with permission check.
   *
   * @param playerUuid the player's UUID
   * @return the result with new channel state if successful
   */
  @NotNull
  public ToggleResult toggleAllyChatChecked(@NotNull UUID playerUuid) {
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CHAT_ALLY)) {
      return new ToggleResult(ChatResult.NO_PERMISSION, null);
    }

    ChatChannel newChannel = toggleAllyChat(playerUuid);
    return new ToggleResult(ChatResult.SUCCESS, newChannel);
  }

  /**
   * Toggles ally chat for a player.
   *
   * @param playerUuid the player's UUID
   * @return the new channel state
   */
  @NotNull
  public ChatChannel toggleAllyChat(@NotNull UUID playerUuid) {
    ChatChannel current = getChannel(playerUuid);
    if (current == ChatChannel.ALLY) {
      setChannel(playerUuid, ChatChannel.NORMAL);
      return ChatChannel.NORMAL;
    } else {
      setChannel(playerUuid, ChatChannel.ALLY);
      return ChatChannel.ALLY;
    }
  }

  /**
   * Resets a player's chat channel to normal.
   * Call when player leaves faction or disconnects.
   *
   * @param playerUuid the player's UUID
   */
  public void resetChannel(@NotNull UUID playerUuid) {
    playerChannels.remove(playerUuid);
  }

  // ============================================================
  // Message Processing
  // ============================================================

  /**
   * Processes a chat message based on the player's current channel.
   * Returns true if the message was handled (faction/ally chat), false if normal chat.
   *
   * @param sender  the sender's PlayerRef
   * @param message the message content
   * @return true if message was handled as faction/ally chat
   */
  public boolean processChatMessage(@NotNull PlayerRef sender, @NotNull String message) {
    UUID senderUuid = sender.getUuid();
    ChatChannel channel = getChannel(senderUuid);

    if (channel == ChatChannel.NORMAL) {
      return false;
    }

    Faction senderFaction = factionManager.getPlayerFaction(senderUuid);
    if (senderFaction == null) {
      resetChannel(senderUuid);
      return false;
    }

    if (channel == ChatChannel.FACTION) {
      sendFactionMessage(sender, senderFaction, message);
      return true;
    } else if (channel == ChatChannel.ALLY) {
      sendAllyMessage(sender, senderFaction, message);
      return true;
    }

    return false;
  }

  /**
   * Sends a message from the GUI on a specific channel.
   * Used by the chat history page to send messages without toggling the player's mode.
   *
   * @param sender  the sender's PlayerRef
   * @param faction the sender's faction
   * @param channel the target channel
   * @param message the message text
   */
  public void sendFromGui(@NotNull PlayerRef sender, @NotNull Faction faction,
              @NotNull ChatMessage.Channel channel, @NotNull String message) {
    if (channel == ChatMessage.Channel.FACTION) {
      sendFactionMessage(sender, faction, message);
    } else {
      sendAllyMessage(sender, faction, message);
    }
  }

  /**
   * Sends a message to all faction members.
   */
  private void sendFactionMessage(@NotNull PlayerRef sender, @NotNull Faction faction, @NotNull String message) {
    ConfigManager config = ConfigManager.get();
    String prefix = config.getFactionChatPrefix();
    String prefixColor = config.getFactionChatColor();
    String nameColor = config.getSenderNameColor();
    String msgColor = config.getMessageColor();

    Message formatted = Message.raw(prefix + " ").color(prefixColor)
        .insert(Message.raw(sender.getUsername()).color(nameColor))
        .insert(Message.raw(": ").color("#AAAAAA"))
        .insert(Message.raw(message).color(msgColor));

    // Send to all online faction members
    for (UUID memberUuid : faction.members().keySet()) {
      PlayerRef member = playerLookup.apply(memberUuid);
      if (member != null) {
        member.sendMessage(formatted);
      }
    }

    // Record in history
    String tag = faction.tag() != null ? faction.tag() : faction.name();
    ChatMessage chatMessage = ChatMessage.create(
        sender.getUuid(), sender.getUsername(), tag,
        ChatMessage.Channel.FACTION, message);

    if (chatHistoryManager != null) {
      chatHistoryManager.recordMessage(faction.id(), chatMessage);
    }

    // Notify listeners
    notifyListeners(chatMessage, faction.id());

    // Refresh chat GUI pages for faction members
    if (guiUpdateService != null) {
      guiUpdateService.onChatMessage(faction.id());
    }

    Logger.debug("[FChat] %s: %s", sender.getUsername(), message);
  }

  /**
   * Sends a message to all faction and ally faction members.
   */
  private void sendAllyMessage(@NotNull PlayerRef sender, @NotNull Faction faction, @NotNull String message) {
    ConfigManager config = ConfigManager.get();
    String prefix = config.getAllyChatPrefix();
    String prefixColor = config.getAllyChatColor();
    String nameColor = config.getSenderNameColor();
    String msgColor = config.getMessageColor();
    String tag = faction.tag() != null ? faction.tag() : faction.name();

    Message formatted = Message.raw(prefix + " ").color(prefixColor)
        .insert(Message.raw("[" + tag + "] ").color("#AAAAAA"))
        .insert(Message.raw(sender.getUsername()).color(nameColor))
        .insert(Message.raw(": ").color("#AAAAAA"))
        .insert(Message.raw(message).color(msgColor));

    // Send to sender's faction members
    for (UUID memberUuid : faction.members().keySet()) {
      PlayerRef member = playerLookup.apply(memberUuid);
      if (member != null) {
        member.sendMessage(formatted);
      }
    }

    // Send to all ally faction members
    for (UUID allyFactionId : faction.relations().keySet()) {
      FactionRelation relation = faction.relations().get(allyFactionId);
      if (relation != null && relation.isAlly()) {
        Faction allyFaction = factionManager.getFaction(allyFactionId);
        if (allyFaction != null) {
          for (UUID allyMemberUuid : allyFaction.members().keySet()) {
            PlayerRef allyMember = playerLookup.apply(allyMemberUuid);
            if (allyMember != null) {
              allyMember.sendMessage(formatted);
            }
          }
        }
      }
    }

    // Record in sender's faction history only (ally tab merges at read time)
    ChatMessage chatMessage = ChatMessage.create(
        sender.getUuid(), sender.getUsername(), tag,
        ChatMessage.Channel.ALLY, message);

    if (chatHistoryManager != null) {
      chatHistoryManager.recordMessage(faction.id(), chatMessage);
    }

    // Notify listeners
    notifyListeners(chatMessage, faction.id());

    // Refresh chat GUI pages for sender's faction + all allies
    if (guiUpdateService != null) {
      guiUpdateService.onChatMessage(faction.id());
      for (UUID allyFactionId : faction.relations().keySet()) {
        FactionRelation relation = faction.relations().get(allyFactionId);
        if (relation != null && relation.isAlly()) {
          guiUpdateService.onChatMessage(allyFactionId);
        }
      }
    }

    Logger.debug("[AChat] %s: %s", sender.getUsername(), message);
  }

  private void notifyListeners(@NotNull ChatMessage message, @NotNull UUID factionId) {
    for (ChatMessageListener listener : messageListeners) {
      try {
        listener.onMessage(message, factionId);
      } catch (Exception e) {
        Logger.warn("Chat message listener threw exception: %s", e.getMessage());
      }
    }
  }

  // ============================================================
  // Display Helpers
  // ============================================================

  /**
   * Gets a display string for a chat channel.
   *
   * @param channel the channel
   * @return display string
   */
  @NotNull
  public static String getChannelDisplay(@NotNull ChatChannel channel) {
    return switch (channel) {
      case NORMAL -> HFMessages.get((PlayerRef) null, MessageKeys.ChatDisplay.PUBLIC);
      case FACTION -> HFMessages.get((PlayerRef) null, MessageKeys.ChatDisplay.FACTION);
      case ALLY -> HFMessages.get((PlayerRef) null, MessageKeys.ChatDisplay.ALLY);
    };
  }

  /**
   * Gets the color for a chat channel from config.
   *
   * @param channel the channel
   * @return hex color string
   */
  @NotNull
  public static String getChannelColor(@NotNull ChatChannel channel) {
    return switch (channel) {
      case NORMAL -> "#FFFFFF";
      case FACTION -> ConfigManager.get().getFactionChatColor();
      case ALLY -> ConfigManager.get().getAllyChatColor();
    };
  }
}
