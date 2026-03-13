package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f open
 * Makes the faction open (anyone can join).
 */
public class OpenSubCommand extends FactionSubCommand {

  /** Creates a new OpenSubCommand. */
  public OpenSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("open", "Allow anyone to join", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.OPEN)) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Open.NOT_LEADER));
      return;
    }

    if (faction.open()) {
      ctx.sendMessage(MessageUtil.info(player, CommandKeys.Open.ALREADY_OPEN, COLOR_YELLOW));
      return;
    }

    Faction updated = faction.withOpen(true)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        "Faction set to open", player.getUuid(),
        GuiKeys.LogsGui.MSG_SET_OPEN));

    hyperFactions.getFactionManager().updateFaction(updated);

    ctx.sendMessage(MessageUtil.success(player, CommandKeys.Open.SUCCESS));
    broadcastToFaction(faction.id(), MessageUtil.success(player, CommandKeys.Open.BROADCAST, player.getUsername()));

    // After action, open settings page if not text mode
    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
        if (refreshed != null) {
          hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
        }
      }
    }
  }
}
