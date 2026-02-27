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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to view help.", COLOR_RED)));
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

    // Core - Basic faction management
    commands.add(new CommandHelp("/f create <name>", "Create a faction", "Core"));
    commands.add(new CommandHelp("/f disband", "Disband your faction", "Core"));
    commands.add(new CommandHelp("/f invite <player>", "Invite a player", "Core"));
    commands.add(new CommandHelp("/f accept [faction]", "Accept an invite", "Core"));
    commands.add(new CommandHelp("/f request <faction> [msg]", "Request to join a faction", "Core"));
    commands.add(new CommandHelp("/f leave", "Leave your faction", "Core"));
    commands.add(new CommandHelp("/f kick <player>", "Kick a member", "Core"));

    // Management - Faction settings
    commands.add(new CommandHelp("/f rename <name>", "Rename your faction", "Management"));
    commands.add(new CommandHelp("/f desc <text>", "Set faction description", "Management"));
    commands.add(new CommandHelp("/f color <code>", "Set faction color", "Management"));
    commands.add(new CommandHelp("/f open", "Allow anyone to join", "Management"));
    commands.add(new CommandHelp("/f close", "Require invite to join", "Management"));
    commands.add(new CommandHelp("/f promote <player>", "Promote to officer", "Management"));
    commands.add(new CommandHelp("/f demote <player>", "Demote to member", "Management"));
    commands.add(new CommandHelp("/f transfer <player>", "Transfer leadership", "Management"));

    // Territory - Land claims
    commands.add(new CommandHelp("/f claim", "Claim this chunk", "Territory"));
    commands.add(new CommandHelp("/f unclaim", "Unclaim this chunk", "Territory"));
    commands.add(new CommandHelp("/f overclaim", "Overclaim enemy territory", "Territory"));
    commands.add(new CommandHelp("/f map", "View territory map", "Territory"));

    // Relations - Diplomatic relations
    commands.add(new CommandHelp("/f ally <faction>", "Request alliance", "Relations"));
    commands.add(new CommandHelp("/f enemy <faction>", "Declare enemy", "Relations"));
    commands.add(new CommandHelp("/f neutral <faction>", "Set neutral relation", "Relations"));

    // Teleport - Home teleportation
    commands.add(new CommandHelp("/f home", "Teleport to faction home", "Teleport"));
    commands.add(new CommandHelp("/f sethome", "Set faction home", "Teleport"));
    commands.add(new CommandHelp("/f stuck", "Escape from enemy territory", "Teleport"));

    // Information - Viewing faction data
    commands.add(new CommandHelp("/f info [faction]", "View faction info", "Information"));
    commands.add(new CommandHelp("/f list", "List all factions", "Information"));
    commands.add(new CommandHelp("/f browse", "Browse factions (alias for list)", "Information"));
    commands.add(new CommandHelp("/f members", "View faction members", "Information"));
    commands.add(new CommandHelp("/f invites", "Manage invites/requests", "Information"));
    commands.add(new CommandHelp("/f who [player]", "View player info", "Information"));
    commands.add(new CommandHelp("/f power [player]", "View power level", "Information"));
    commands.add(new CommandHelp("/f gui", "Open faction GUI", "Information"));
    commands.add(new CommandHelp("/f settings", "Open faction settings", "Information"));

    // Other
    commands.add(new CommandHelp("/f chat <message>", "Send faction chat message", "Other"));
    commands.add(new CommandHelp("/f c <message>", "Faction chat (short)", "Other"));

    // Admin
    commands.add(new CommandHelp("/f admin", "Open admin GUI", "Admin"));
    commands.add(new CommandHelp("/f admin reload", "Reload config", "Admin"));
    commands.add(new CommandHelp("/f admin sync", "Sync data from disk", "Admin"));
    commands.add(new CommandHelp("/f admin factions", "Manage factions", "Admin"));
    commands.add(new CommandHelp("/f admin zones", "Manage zones", "Admin"));
    commands.add(new CommandHelp("/f admin config", "View/edit config", "Admin"));
    commands.add(new CommandHelp("/f admin backups", "Manage backups", "Admin"));
    commands.add(new CommandHelp("/f admin update", "Check for updates", "Admin"));
    commands.add(new CommandHelp("/f admin debug", "Debug commands", "Admin"));

    ctx.sendMessage(HelpFormatter.buildHelp("HyperFactions", "Faction management and territory control", commands, "Use /f <command> for more details"));
  }
}
