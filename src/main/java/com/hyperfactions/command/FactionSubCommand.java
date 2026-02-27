package com.hyperfactions.command;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for faction subcommands.
 * Provides common functionality and access to HyperFactions services.
 */
public abstract class FactionSubCommand extends AbstractPlayerCommand {

  protected final HyperFactions hyperFactions;

  protected final HyperFactionsPlugin plugin;

  /**
   * Creates a new faction subcommand.
   *
   * @param name the command name
   * @param description the command description
   * @param hyperFactions the HyperFactions instance
   * @param plugin the plugin instance
   */
  protected FactionSubCommand(@NotNull String name,
               @NotNull String description,
               @NotNull HyperFactions hyperFactions,
               @NotNull HyperFactionsPlugin plugin) {
    super(name, description);
    this.hyperFactions = hyperFactions;
    this.plugin = plugin;
    setAllowsExtraArguments(true);
  }

  /**
   * Disable Hytale's auto-generated permissions for subcommands.
   * We use our own permission system via hasPermission() checks in execute().
   */
  @Override
  protected boolean canGeneratePermission() {
    return false;
  }

  // ==================== Utility Methods ====================

  /**
   * Creates the standard HyperFactions message prefix.
   */
  protected Message prefix() {
    return CommandUtil.prefix();
  }

  /**
   * Creates a colored message.
   */
  protected Message msg(String text, String color) {
    return CommandUtil.msg(text, color);
  }

  /**
   * Checks if a player has a specific permission.
   */
  protected boolean hasPermission(PlayerRef player, String permission) {
    return CommandUtil.hasPermission(player, permission);
  }

  /**
   * Finds an online player by name.
   */
  protected PlayerRef findOnlinePlayer(String name) {
    return CommandUtil.findOnlinePlayer(plugin, name);
  }

  /**
   * Broadcasts a message to all online members of a faction.
   */
  protected void broadcastToFaction(UUID factionId, Message message) {
    CommandUtil.broadcastToFaction(hyperFactions, plugin, factionId, message);
  }

  /**
   * Parses FactionCommandContext from remaining arguments.
   * This handles the --text flag extraction from args.
   *
   * @param args the raw command arguments
   * @return the parsed context
   */
  protected FactionCommandContext parseContext(String[] args) {
    return FactionCommandContext.parse(args);
  }

  /**
   * Gets the player's faction, sending an error message if they aren't in one.
   *
   * @param ctx the command context (for sending error message)
   * @param player the player to check
   * @return the player's faction, or null if not in a faction
   */
  @Nullable
  protected Faction requireFaction(@NotNull CommandContext ctx, @NotNull PlayerRef player) {
    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error("You are not in a faction."));
      return null;
    }
    return faction;
  }

  // Color constants for convenience
  protected static final String COLOR_CYAN = CommandUtil.COLOR_CYAN;

  protected static final String COLOR_GREEN = CommandUtil.COLOR_GREEN;

  protected static final String COLOR_RED = CommandUtil.COLOR_RED;

  protected static final String COLOR_YELLOW = CommandUtil.COLOR_YELLOW;

  protected static final String COLOR_GRAY = CommandUtil.COLOR_GRAY;

  protected static final String COLOR_WHITE = CommandUtil.COLOR_WHITE;
}
