package com.hyperfactions.command.social;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.ChatManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f chat [faction|ally|off]
 * Toggles between chat modes or sets a specific mode.
 * Aliases: c
 *
 * <p>Usage:
 *   /f c       - Cycle: Normal -> Faction -> Ally -> Normal
 *   /f c f     - Set to Faction chat
 *   /f c a     - Set to Ally chat
 *   /f c off   - Set to Normal/public chat
 */
public class ChatSubCommand extends FactionSubCommand {

  /** Creates a new ChatSubCommand. */
  public ChatSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("chat", "Toggle faction/ally chat mode", hyperFactions, plugin);
    addAliases("c");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    ChatManager chatManager = hyperFactions.getChatManager();

    // Parse arguments
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    // parts[0] = "faction/f/hf", parts[1] = "chat/c", parts[2] = mode (optional)
    String mode = parts.length > 2 ? parts[2].toLowerCase() : null;

    ChatManager.ToggleResult result;

    if (mode == null) {
      // No argument - cycle through modes
      result = chatManager.cycleChannelChecked(player.getUuid());
    } else {
      result = switch (mode) {
        case "f", "faction" -> chatManager.setFactionChatChecked(player.getUuid());
        case "a", "ally" -> chatManager.setAllyChatChecked(player.getUuid());
        case "off", "normal", "public" -> {
          chatManager.setNormalChat(player.getUuid());
          yield new ChatManager.ToggleResult(ChatManager.ChatResult.SUCCESS, ChatManager.ChatChannel.NORMAL);
        }
        default -> {
          ctx.sendMessage(MessageUtil.error(player, CommandKeys.Chat.USAGE));
          yield null;
        }
      };
    }

    if (result == null) {
      return;
    }

    if (!result.isSuccess()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Chat.NO_PERMISSION));
      return;
    }

    ChatManager.ChatChannel channel = result.channel();
    String display = ChatManager.getChannelDisplay(channel);
    String color = ChatManager.getChannelColor(channel);

    ctx.sendMessage(MessageUtil.info(player, CommandKeys.Chat.MODE_SET, color, display));
  }
}
