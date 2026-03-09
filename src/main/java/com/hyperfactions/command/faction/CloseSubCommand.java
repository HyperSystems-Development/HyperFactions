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
import com.hyperfactions.util.MessageKeys;
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
 * Subcommand: /f close
 * Makes the faction invite-only.
 */
public class CloseSubCommand extends FactionSubCommand {

  /** Creates a new CloseSubCommand. */
  public CloseSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("close", "Require invite to join", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.CLOSE)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Close.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Close.NOT_LEADER));
      return;
    }

    if (!faction.open()) {
      ctx.sendMessage(MessageUtil.info(player, MessageKeys.Close.ALREADY_CLOSED, COLOR_YELLOW));
      return;
    }

    Faction updated = faction.withOpen(false)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        "Faction set to invite-only", player.getUuid()));

    hyperFactions.getFactionManager().updateFaction(updated);

    ctx.sendMessage(MessageUtil.success(player, MessageKeys.Close.SUCCESS));
    broadcastToFaction(faction.id(), MessageUtil.success(player, MessageKeys.Close.BROADCAST, player.getUsername()));

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
