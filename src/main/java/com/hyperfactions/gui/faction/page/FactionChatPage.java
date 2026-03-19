package com.hyperfactions.gui.faction.page;

import com.hyperfactions.Permissions;
import com.hyperfactions.data.ChatMessage;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionChatHistory;
import com.hyperfactions.gui.ActivePageTracker;
import com.hyperfactions.gui.GuiManager;
import com.hyperfactions.gui.RefreshablePage;
import com.hyperfactions.gui.UIPaths;
import com.hyperfactions.gui.faction.FactionPageRegistry;
import com.hyperfactions.gui.faction.NavBarHelper;
import com.hyperfactions.gui.faction.data.FactionChatData;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.ChatHistoryManager;
import com.hyperfactions.manager.ChatManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.GuiKeys;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Faction Chat page — shows chat history with faction/ally tabs,
 * scrollable messages, and an input bar for sending messages from the GUI.
 */
public class FactionChatPage extends InteractiveCustomUIPage<FactionChatData> implements RefreshablePage {

  private static final String PAGE_ID = "chat";

  private static final int MESSAGES_PER_PAGE = 50;

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d HH:mm");

  private enum Tab {
    FACTION, ALLY;
  }

  private final PlayerRef playerRef;

  private final FactionManager factionManager;

  private final ChatManager chatManager;

  private final ChatHistoryManager chatHistoryManager;

  private final GuiManager guiManager;

  private final Faction faction;

  private Tab activeTab = Tab.FACTION;

  /** Creates a new FactionChatPage. */
  public FactionChatPage(@NotNull PlayerRef playerRef,
             @NotNull FactionManager factionManager,
             @NotNull ChatManager chatManager,
             @NotNull ChatHistoryManager chatHistoryManager,
             @NotNull GuiManager guiManager,
             @NotNull Faction faction) {
    super(playerRef, CustomPageLifetime.CanDismiss, FactionChatData.CODEC);
    this.playerRef = playerRef;
    this.factionManager = factionManager;
    this.chatManager = chatManager;
    this.chatHistoryManager = chatHistoryManager;
    this.guiManager = guiManager;
    this.faction = faction;
  }

  /** Builds . */
  @Override
  public void build(Ref<EntityStore> ref, UICommandBuilder cmd,
           UIEventBuilder events, Store<EntityStore> store) {

    cmd.append(UIPaths.FACTION_CHAT);

    // Localize static labels
    cmd.set("#ChatTitle.Text", HFMessages.get(playerRef, GuiKeys.ChatGui.TITLE));
    cmd.set("#TabFactionBtn.Text", HFMessages.get(playerRef, GuiKeys.ChatGui.TAB_FACTION));
    cmd.set("#TabAllyBtn.Text", HFMessages.get(playerRef, GuiKeys.ChatGui.TAB_ALLY));
    cmd.set("#SendBtn.Text", HFMessages.get(playerRef, GuiKeys.ChatGui.SEND_BTN));

    // Setup navigation bar
    NavBarHelper.setupBar(playerRef, faction, PAGE_ID, cmd, events);

    // Register with ActivePageTracker for real-time updates
    ActivePageTracker tracker = guiManager.getActivePageTracker();
    if (tracker != null) {
      tracker.register(playerRef.getUuid(), PAGE_ID, faction.id(), this);
    }

    // Build tab buttons
    buildTabButtons(cmd, events);

    // Build message list
    buildMessageList(cmd);

    // Chat input placeholder
    cmd.set("#ChatInput.PlaceholderText", HFMessages.get(playerRef, GuiKeys.ChatGui.PLACEHOLDER));

    // Build chat input bar events
    buildChatInputEvents(events);
  }

  // =========================================================================
  // TAB BUTTONS
  // =========================================================================

  private void buildTabButtons(UICommandBuilder cmd, UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    boolean hasFactionPerm = PermissionManager.get().hasPermission(uuid, Permissions.CHAT_FACTION);
    boolean hasAllyPerm = PermissionManager.get().hasPermission(uuid, Permissions.CHAT_ALLY);

    // Active tab is disabled (non-clickable), inactive tab is clickable
    if (hasFactionPerm) {
      cmd.set("#TabFactionBtn.Style", Value.ref(UIPaths.STYLES,
          activeTab == Tab.FACTION ? "DisabledButtonStyle" : "ButtonStyle"));
      if (activeTab != Tab.FACTION) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabFactionBtn",
            EventData.of("Button", "TabFaction"), false);
      }
    } else {
      cmd.set("#TabFactionBtn.Visible", false);
    }

    if (hasAllyPerm) {
      cmd.set("#TabAllyBtn.Style", Value.ref(UIPaths.STYLES,
          activeTab == Tab.ALLY ? "DisabledButtonStyle" : "ButtonStyle"));
      if (activeTab != Tab.ALLY) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabAllyBtn",
            EventData.of("Button", "TabAlly"), false);
      }
    } else {
      cmd.set("#TabAllyBtn.Visible", false);
    }
  }

  // =========================================================================
  // MESSAGE LIST
  // =========================================================================

  private void buildMessageList(UICommandBuilder cmd) {
    List<ChatMessage> messages = loadMessages();

    if (messages.isEmpty()) {
      cmd.appendInline("#MessageList",
          "Label { Text: \"" + HFMessages.get(playerRef, GuiKeys.ChatGui.NO_MESSAGES) + "\"; Style: (FontSize: 12, TextColor: #555555); "
          + "Anchor: (Height: 30); }");
      return;
    }

    // Create IndexCards container for indexed selector access
    cmd.appendInline("#MessageList", "Group #MsgCards { LayoutMode: Bottom; }");

    // Messages are newest-first in storage (index 0 = newest).
    // BottomScrolling: first append = bottom. Append newest first for newest at bottom.
    int count = Math.min(MESSAGES_PER_PAGE, messages.size());
    for (int i = 0; i < count; i++) {
      appendMessageEntry(cmd, messages.get(i), i);
    }
  }

  private void appendMessageEntry(UICommandBuilder cmd, ChatMessage msg, int index) {
    cmd.append("#MsgCards", UIPaths.CHAT_MESSAGE_ENTRY);

    // Use indexed selector to target this specific entry
    String idx = "#MsgCards[" + index + "]";

    cmd.set(idx + " #MsgTime.Text", formatTimestamp(msg.timestamp()));

    // Tag — only show on ally tab
    if (activeTab == Tab.ALLY && msg.senderFactionTag() != null) {
      cmd.set(idx + " #MsgTag.Visible", true);
      cmd.set(idx + " #MsgTag.Text", "[" + msg.senderFactionTag() + "] ");
    }

    cmd.set(idx + " #MsgSender.Text", msg.senderName() + ":");
    cmd.set(idx + " #MsgText.Text", msg.message());
  }

  private List<ChatMessage> loadMessages() {
    try {
      if (activeTab == Tab.ALLY) {
        return chatHistoryManager.getAlliedHistory(faction.id(), factionManager).join();
      } else {
        FactionChatHistory history = chatHistoryManager.getHistory(faction.id()).join();
        // Filter to FACTION channel only
        return history.messages().stream()
            .filter(m -> m.channel() == ChatMessage.Channel.FACTION)
            .toList();
      }
    } catch (Exception e) {
      ErrorHandler.report("Failed to load chat messages", e);
      return List.of();
    }
  }

  // =========================================================================
  // CHAT INPUT EVENTS
  // =========================================================================

  private void buildChatInputEvents(UIEventBuilder events) {
    // Send button click — captures the text field value at click time
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SendBtn",
        EventData.of("Button", "SendChat")
            .append("@ChatInput", "#ChatInput.Value"), false);
  }

  // =========================================================================
  // TIMESTAMP FORMATTING
  // =========================================================================

  private String formatTimestamp(long timestamp) {
    long now = System.currentTimeMillis();
    long ageMs = now - timestamp;

    // Recent: show relative time
    if (ageMs < 60_000) {
      return HFMessages.get(playerRef, GuiKeys.ChatGui.TIME_NOW);
    } else if (ageMs < 3_600_000) {
      long minutes = ageMs / 60_000;
      return HFMessages.get(playerRef, GuiKeys.ChatGui.TIME_MINUTES, minutes);
    } else if (ageMs < 86_400_000) {
      long hours = ageMs / 3_600_000;
      return HFMessages.get(playerRef, GuiKeys.ChatGui.TIME_HOURS, hours);
    }

    // Older: show date + time
    LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    return dt.format(DATE_FORMAT);
  }

  // =========================================================================
  // EVENT HANDLING
  // =========================================================================

  /** Handles data event. */
  @Override
  public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store,
                FactionChatData data) {
    super.handleDataEvent(ref, store, data);

    Player player = store.getComponent(ref, Player.getComponentType());
    PlayerRef pRef = store.getComponent(ref, PlayerRef.getComponentType());

    if (player == null || pRef == null || data.button == null) {
      rebuild();
      return;
    }

    // Handle navigation
    if ("Nav".equals(data.button) && data.navBar != null) {
      unregisterTracker();
      FactionPageRegistry.Entry entry = FactionPageRegistry.getInstance().getEntry(data.navBar);
      if (entry != null) {
        Faction currentFaction = factionManager.getFaction(faction.id());
        var page = entry.guiSupplier().create(player, ref, store, pRef, currentFaction, guiManager);
        if (page != null) {
          player.getPageManager().openCustomPage(ref, store, page);
          return;
        }
      }
      rebuild();
      return;
    }

    switch (data.button) {
      case "TabFaction" -> {
        activeTab = Tab.FACTION;
        rebuild();
      }
      case "TabAlly" -> {
        if (!PermissionManager.get().hasPermission(pRef.getUuid(), Permissions.CHAT_ALLY)) {
          player.sendMessage(MessageUtil.errorText(pRef, GuiKeys.ChatGui.NO_ALLY_PERMISSION));
          rebuild();
          return;
        }
        activeTab = Tab.ALLY;
        rebuild();
      }
      case "SendChat" -> handleSendChat(player, pRef, data);
      default -> rebuild();
    }
  }

  private void handleSendChat(Player player, PlayerRef pRef, FactionChatData data) {
    String text = data.chatInput;
    if (text == null || text.trim().isEmpty()) {
      rebuild();
      return;
    }

    String message = text.trim();
    UUID uuid = pRef.getUuid();

    // Determine channel from active tab
    ChatMessage.Channel channel = (activeTab == Tab.ALLY)
        ? ChatMessage.Channel.ALLY : ChatMessage.Channel.FACTION;

    // Permission check
    String requiredPerm = (channel == ChatMessage.Channel.ALLY)
        ? Permissions.CHAT_ALLY : Permissions.CHAT_FACTION;
    if (!PermissionManager.get().hasPermission(uuid, requiredPerm)) {
      player.sendMessage(MessageUtil.errorText(pRef, GuiKeys.ChatGui.NO_PERMISSION));
      rebuild();
      return;
    }

    // Get fresh faction data
    Faction currentFaction = factionManager.getFaction(faction.id());
    if (currentFaction == null) {
      player.sendMessage(MessageUtil.errorText(pRef, GuiKeys.ChatGui.FACTION_GONE));
      rebuild();
      return;
    }

    // Send through ChatManager (handles broadcast, history, and real-time GUI push)
    chatManager.sendFromGui(pRef, currentFaction, channel, message);

    // ChatManager triggers refreshContent() on all viewers for real-time push.
    // We also rebuild here to show the new message and clear the input field.
    rebuild();
  }

  // =========================================================================
  // REFRESH (real-time push)
  // =========================================================================

  /** Refresh Content. */
  @Override
  public void refreshContent() {
    rebuild();
  }

  // =========================================================================
  // CLEANUP
  // =========================================================================

  private void unregisterTracker() {
    ActivePageTracker tracker = guiManager.getActivePageTracker();
    if (tracker != null) {
      tracker.unregister(playerRef.getUuid());
    }
  }

  /** Called when dismiss. */
  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    super.onDismiss(ref, store);
    unregisterTracker();
  }
}
