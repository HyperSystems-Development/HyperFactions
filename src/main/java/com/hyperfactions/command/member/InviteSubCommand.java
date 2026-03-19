package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
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
 * Subcommand: /f invite {@code <player>}
 * Invites a player to the faction (officer+).
 */
public class InviteSubCommand extends FactionSubCommand {

  /** Creates a new InviteSubCommand. */
  public InviteSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("invite", "Invite a player", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.INVITE)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invite.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isOfficerOrHigher()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invite.NOT_OFFICER));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open FactionInvitesPage when no player specified
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionInvites(playerEntity, ref, store, player, faction);
        return;
      }
    }

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invite.USAGE));
      return;
    }

    String targetName = fctx.getArg(0);
    PlayerRef target = findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invite.PLAYER_NOT_FOUND, targetName));
      return;
    }

    if (hyperFactions.getFactionManager().isInFaction(target.getUuid())) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Invite.TARGET_IN_FACTION));
      return;
    }

    hyperFactions.getInviteManager().createInvite(faction.id(), target.getUuid(), player.getUuid());

    ctx.sendMessage(MessageUtil.success(player, CommandKeys.Invite.SENT, target.getUsername()));
    target.sendMessage(MessageUtil.info(target, CommandKeys.Invite.RECEIVED, COLOR_YELLOW, faction.name()));
    target.sendMessage(MessageUtil.info(target, CommandKeys.Invite.ACCEPT_HINT, COLOR_YELLOW, faction.name()));
  }
}
