package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f list
 * Lists all factions.
 * Aliases: browse
 */
public class ListSubCommand extends FactionSubCommand {

  /** Creates a new ListSubCommand. */
  public ListSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("list", "List all factions", hyperFactions, plugin);
    addAliases("browse");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.LIST)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Info.LIST_NO_PERMISSION));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open FactionBrowserPage
    if (fctx.shouldOpenGui()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionBrowser(playerEntity, ref, store, player);
        return;
      }
    }

    // Text mode: output to chat
    Collection<Faction> factions = hyperFactions.getFactionManager().getAllFactions();
    if (factions.isEmpty()) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Info.LIST_EMPTY, COLOR_GRAY));
      return;
    }

    ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Info.LIST_HEADER, factions.size()), COLOR_CYAN).bold(true));
    for (Faction faction : factions) {
      PowerManager.FactionPowerStats stats = hyperFactions.getPowerManager().getFactionPowerStats(faction.id());
      String key = stats.isRaidable() ? CommandKeys.Info.LIST_ENTRY_RAIDABLE : CommandKeys.Info.LIST_ENTRY;
      ctx.sendMessage(msg(HFMessages.get(player, key,
        faction.name(), faction.getMemberCount(), String.format("%.0f", stats.currentPower())), COLOR_GRAY));
    }
  }
}
