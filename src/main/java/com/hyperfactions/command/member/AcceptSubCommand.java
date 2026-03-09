package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.manager.FactionManager;
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
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f accept [faction]
 * Accepts a faction invite.
 * Aliases: join
 */
public class AcceptSubCommand extends FactionSubCommand {

  /** Creates a new AcceptSubCommand. */
  public AcceptSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("accept", "Accept an invite", hyperFactions, plugin);
    addAliases("join");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.JOIN)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.NO_PERMISSION));
      return;
    }

    if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
      Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
      if (existingFaction != null) {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.ALREADY_IN_NAMED, existingFaction.name()));
        ctx.sendMessage(MessageUtil.info(player, MessageKeys.Join.USE_LEAVE_HINT, COLOR_YELLOW));
      } else {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.ALREADY_IN_FACTION));
      }
      return;
    }

    List<PendingInvite> invites = hyperFactions.getInviteManager().getPlayerInvites(player.getUuid());

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open InvitesPage when no faction specified
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openInvitesPage(playerEntity, ref, store, player);
        return;
      }
    }

    if (invites.isEmpty()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.NO_INVITES));
      return;
    }

    PendingInvite invite;
    if (fctx.hasArgs()) {
      String factionName = fctx.joinArgs();
      Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
      if (targetFaction == null) {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.FACTION_NOT_FOUND, factionName));
        return;
      }
      invite = hyperFactions.getInviteManager().getInvite(targetFaction.id(), player.getUuid());
      if (invite == null) {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.NOT_INVITED));
        return;
      }
    } else {
      invite = invites.get(0);
    }

    Faction faction = hyperFactions.getFactionManager().getFaction(invite.factionId());
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.FACTION_GONE));
      hyperFactions.getInviteManager().removeInvite(invite.factionId(), player.getUuid());
      return;
    }

    FactionManager.FactionResult result = hyperFactions.getFactionManager().addMember(
      faction.id(), player.getUuid(), player.getUsername()
    );

    if (result == FactionManager.FactionResult.SUCCESS) {
      hyperFactions.getInviteManager().clearPlayerInvites(player.getUuid());
      hyperFactions.getJoinRequestManager().clearPlayerRequests(player.getUuid());
      ctx.sendMessage(MessageUtil.success(player, MessageKeys.Join.SUCCESS, faction.name()));
      broadcastToFaction(faction.id(), MessageUtil.success(player, MessageKeys.Join.BROADCAST, player.getUsername()));
    } else if (result == FactionManager.FactionResult.FACTION_FULL) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.FACTION_FULL));
    } else {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Join.FAILED));
    }
  }
}
