package com.hyperfactions.command.social;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.JoinRequest;
import com.hyperfactions.data.PendingInvite;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
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
 * Subcommand: /f invites
 * Manages faction invites (outgoing/incoming).
 */
public class InvitesSubCommand extends FactionSubCommand {

  /** Creates a new InvitesSubCommand. */
  public InvitesSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("invites", "Manage invites/requests", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // Player has a faction - show FactionInvitesPage (outgoing invites, incoming requests)
    if (faction != null) {
      FactionMember member = faction.getMember(player.getUuid());
      if (member == null || !member.isOfficerOrHigher()) {
        ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invites.NOT_OFFICER));
        return;
      }

      // GUI mode: open FactionInvitesPage
      if (fctx.shouldOpenGui()) {
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openFactionInvites(playerEntity, ref, store, player, faction);
          return;
        }
      }

      // Text mode: show outgoing invites and incoming requests
      List<PendingInvite> invites = hyperFactions.getInviteManager().getFactionInvitesList(faction.id());
      List<JoinRequest> requests = hyperFactions.getJoinRequestManager().getFactionRequests(faction.id());

      ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.HEADER), COLOR_CYAN).bold(true));

      if (invites.isEmpty() && requests.isEmpty()) {
        ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.NO_PENDING), COLOR_GRAY));
        return;
      }

      if (!invites.isEmpty()) {
        ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.OUTGOING), COLOR_YELLOW));
        for (PendingInvite invite : invites) {
          String inviterName = plugin.getTrackedPlayer(invite.invitedBy()) != null
            ? plugin.getTrackedPlayer(invite.invitedBy()).getUsername()
            : HFMessages.get(player, CommonKeys.Common.UNKNOWN);
          ctx.sendMessage(msg("  ", COLOR_GRAY)
            .insert(msg(HFMessages.get(player, CommandKeys.Invites.OUTGOING_ENTRY,
              invite.playerUuid().toString().substring(0, 8), inviterName), COLOR_WHITE)));
        }
      }

      if (!requests.isEmpty()) {
        ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.REQUESTS), COLOR_GREEN));
        for (JoinRequest request : requests) {
          String message = request.message() != null ? " \"" + request.message() + "\"" : "";
          ctx.sendMessage(msg("  ", COLOR_GRAY)
            .insert(msg(HFMessages.get(player, CommandKeys.Invites.REQUEST_ENTRY,
              request.playerName(), message), COLOR_WHITE)));
        }
      }
    } else {
      // Player has no faction - show InvitesPage (incoming invites)
      // GUI mode: open InvitesPage
      if (fctx.shouldOpenGui()) {
        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
          hyperFactions.getGuiManager().openInvitesPage(playerEntity, ref, store, player);
          return;
        }
      }

      // Text mode: show incoming invites
      List<PendingInvite> invites = hyperFactions.getInviteManager().getPlayerInvites(player.getUuid());

      ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.YOUR_INVITES_HEADER), COLOR_CYAN).bold(true));

      if (invites.isEmpty()) {
        ctx.sendMessage(msg(HFMessages.get(player, CommandKeys.Invites.NO_INVITES), COLOR_GRAY));
        return;
      }

      for (PendingInvite invite : invites) {
        Faction invitingFaction = hyperFactions.getFactionManager().getFaction(invite.factionId());
        if (invitingFaction != null) {
          ctx.sendMessage(msg("  ", COLOR_GRAY)
            .insert(msg(HFMessages.get(player, CommandKeys.Invites.INVITE_ENTRY,
              invitingFaction.name(), invitingFaction.name()), COLOR_YELLOW)));
        }
      }
    }
  }
}
