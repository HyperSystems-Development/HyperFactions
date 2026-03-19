package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.gui.help.HelpCategory;
import com.hyperfactions.gui.help.HelpRegistry;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandHelp;
import com.hyperfactions.util.HelpFormatter;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.HelpKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f help
 * Shows command help.
 * Aliases: ?
 */
public class HelpSubCommand extends FactionSubCommand {

  /** Creates a new HelpSubCommand. */
  public HelpSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("help", "View help", hyperFactions, plugin);
    addAliases("?");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.HELP)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.HELP_NO_PERMISSION));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // Text mode: show chat-based help
    if (fctx.isTextMode()) {
      showHelpText(ctx, player);
      return;
    }

    // GUI mode: open help page
    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openHelpPage(playerEntity, ref, store, player);
    } else {
      showHelpText(ctx, player);
    }
  }

  /**
   * Shows text-based help in chat (fallback for --text mode or when GUI unavailable).
   */
  private void showHelpText(CommandContext ctx, PlayerRef player) {
    List<CommandHelp> commands = new ArrayList<>();

    // Core - Basic faction management (sortOrder 0)
    commands.add(new CommandHelp("/f create <name>", HelpKeys.Help.CMD_CREATE, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f disband", HelpKeys.Help.CMD_DISBAND, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f invite <player>", HelpKeys.Help.CMD_INVITE, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f accept [faction]", HelpKeys.Help.CMD_ACCEPT, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f request <faction> [msg]", HelpKeys.Help.CMD_REQUEST, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f leave", HelpKeys.Help.CMD_LEAVE, HelpKeys.Help.SECTION_CORE, 0));
    commands.add(new CommandHelp("/f kick <player>", HelpKeys.Help.CMD_KICK, HelpKeys.Help.SECTION_CORE, 0));

    // Management - Faction settings (sortOrder 1)
    commands.add(new CommandHelp("/f rename <name>", HelpKeys.Help.CMD_RENAME, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f desc <text>", HelpKeys.Help.CMD_DESC, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f color <code>", HelpKeys.Help.CMD_COLOR, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f open", HelpKeys.Help.CMD_OPEN, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f close", HelpKeys.Help.CMD_CLOSE, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f promote <player>", HelpKeys.Help.CMD_PROMOTE, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f demote <player>", HelpKeys.Help.CMD_DEMOTE, HelpKeys.Help.SECTION_MANAGEMENT, 1));
    commands.add(new CommandHelp("/f transfer <player>", HelpKeys.Help.CMD_TRANSFER, HelpKeys.Help.SECTION_MANAGEMENT, 1));

    // Territory - Land claims (sortOrder 2)
    commands.add(new CommandHelp("/f claim", HelpKeys.Help.CMD_CLAIM, HelpKeys.Help.SECTION_TERRITORY, 2));
    commands.add(new CommandHelp("/f unclaim", HelpKeys.Help.CMD_UNCLAIM, HelpKeys.Help.SECTION_TERRITORY, 2));
    commands.add(new CommandHelp("/f overclaim", HelpKeys.Help.CMD_OVERCLAIM, HelpKeys.Help.SECTION_TERRITORY, 2));
    commands.add(new CommandHelp("/f map", HelpKeys.Help.CMD_MAP, HelpKeys.Help.SECTION_TERRITORY, 2));

    // Relations - Diplomatic relations (sortOrder 3)
    commands.add(new CommandHelp("/f ally <faction>", HelpKeys.Help.CMD_ALLY, HelpKeys.Help.SECTION_RELATIONS, 3));
    commands.add(new CommandHelp("/f enemy <faction>", HelpKeys.Help.CMD_ENEMY, HelpKeys.Help.SECTION_RELATIONS, 3));
    commands.add(new CommandHelp("/f neutral", HelpKeys.Help.CMD_NEUTRAL, HelpKeys.Help.SECTION_RELATIONS, 3));

    // Teleport - Home teleportation (sortOrder 4)
    commands.add(new CommandHelp("/f home", HelpKeys.Help.CMD_HOME, HelpKeys.Help.SECTION_TELEPORT, 4));
    commands.add(new CommandHelp("/f sethome", HelpKeys.Help.CMD_SETHOME, HelpKeys.Help.SECTION_TELEPORT, 4));
    commands.add(new CommandHelp("/f stuck", HelpKeys.Help.CMD_STUCK, HelpKeys.Help.SECTION_TELEPORT, 4));

    // Information - Viewing faction data (sortOrder 5)
    commands.add(new CommandHelp("/f info [faction]", HelpKeys.Help.CMD_INFO, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f list", HelpKeys.Help.CMD_LIST, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f browse", HelpKeys.Help.CMD_BROWSE, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f members", HelpKeys.Help.CMD_MEMBERS, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f invites", HelpKeys.Help.CMD_INVITES, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f who [player]", HelpKeys.Help.CMD_WHO, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f power [player]", HelpKeys.Help.CMD_POWER, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f gui", HelpKeys.Help.CMD_GUI, HelpKeys.Help.SECTION_INFORMATION, 5));
    commands.add(new CommandHelp("/f settings", HelpKeys.Help.CMD_SETTINGS, HelpKeys.Help.SECTION_INFORMATION, 5));

    // Other (sortOrder 6)
    commands.add(new CommandHelp("/f chat <message>", HelpKeys.Help.CMD_CHAT, HelpKeys.Help.SECTION_OTHER, 6));
    commands.add(new CommandHelp("/f c <message>", HelpKeys.Help.CMD_CHAT_SHORT, HelpKeys.Help.SECTION_OTHER, 6));

    // Admin (sortOrder 7)
    commands.add(new CommandHelp("/f admin", HelpKeys.Help.CMD_ADMIN, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin reload", HelpKeys.Help.CMD_ADMIN_RELOAD, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin sync", HelpKeys.Help.CMD_ADMIN_SYNC, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin factions", HelpKeys.Help.CMD_ADMIN_FACTIONS, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin zones", HelpKeys.Help.CMD_ADMIN_ZONES, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin config", HelpKeys.Help.CMD_ADMIN_CONFIG, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin backups", HelpKeys.Help.CMD_ADMIN_BACKUPS, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin update", HelpKeys.Help.CMD_ADMIN_UPDATE, HelpKeys.Help.SECTION_ADMIN, 7));
    commands.add(new CommandHelp("/f admin debug", HelpKeys.Help.CMD_ADMIN_DEBUG, HelpKeys.Help.SECTION_ADMIN, 7));

    ctx.sendMessage(HelpFormatter.buildHelp(HelpKeys.Help.TITLE, HelpKeys.Help.DESCRIPTION, commands, HelpKeys.Help.DEFAULT_FOOTER, player));
  }
}
