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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to join factions.", COLOR_RED)));
      return;
    }

    if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
      Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
      if (existingFaction != null) {
        ctx.sendMessage(prefix().insert(msg("You are already in ", COLOR_RED))
          .insert(msg(existingFaction.name(), COLOR_CYAN))
          .insert(msg(".", COLOR_RED)));
        ctx.sendMessage(prefix().insert(msg("Use /f leave first if you want to join another faction.", COLOR_YELLOW)));
      } else {
        ctx.sendMessage(prefix().insert(msg("You are already in a faction.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("You have no pending invites.", COLOR_RED)));
      return;
    }

    PendingInvite invite;
    if (fctx.hasArgs()) {
      String factionName = fctx.joinArgs();
      Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
      if (targetFaction == null) {
        ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
        return;
      }
      invite = hyperFactions.getInviteManager().getInvite(targetFaction.id(), player.getUuid());
      if (invite == null) {
        ctx.sendMessage(prefix().insert(msg("You have no invite from that faction.", COLOR_RED)));
        return;
      }
    } else {
      invite = invites.get(0);
    }

    Faction faction = hyperFactions.getFactionManager().getFaction(invite.factionId());
    if (faction == null) {
      ctx.sendMessage(prefix().insert(msg("That faction no longer exists.", COLOR_RED)));
      hyperFactions.getInviteManager().removeInvite(invite.factionId(), player.getUuid());
      return;
    }

    FactionManager.FactionResult result = hyperFactions.getFactionManager().addMember(
      faction.id(), player.getUuid(), player.getUsername()
    );

    if (result == FactionManager.FactionResult.SUCCESS) {
      hyperFactions.getInviteManager().clearPlayerInvites(player.getUuid());
      hyperFactions.getJoinRequestManager().clearPlayerRequests(player.getUuid());
      ctx.sendMessage(prefix().insert(msg("You have joined ", COLOR_GREEN))
        .insert(msg(faction.name(), COLOR_CYAN)).insert(msg("!", COLOR_GREEN)));
      broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
        .insert(msg(" has joined the faction!", COLOR_GREEN)));
    } else if (result == FactionManager.FactionResult.FACTION_FULL) {
      ctx.sendMessage(prefix().insert(msg("That faction is full.", COLOR_RED)));
    } else {
      ctx.sendMessage(prefix().insert(msg("Failed to join faction.", COLOR_RED)));
    }
  }
}
