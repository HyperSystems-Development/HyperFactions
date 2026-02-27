package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to invite players.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isOfficerOrHigher()) {
      ctx.sendMessage(prefix().insert(msg("You must be an officer to invite players.", COLOR_RED)));
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
      ctx.sendMessage(prefix().insert(msg("Usage: /f invite <player>", COLOR_RED)));
      return;
    }

    String targetName = fctx.getArg(0);
    PlayerRef target = findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player '" + targetName + "' not found or offline.", COLOR_RED)));
      return;
    }

    if (hyperFactions.getFactionManager().isInFaction(target.getUuid())) {
      ctx.sendMessage(prefix().insert(msg("That player is already in a faction.", COLOR_RED)));
      return;
    }

    hyperFactions.getInviteManager().createInvite(faction.id(), target.getUuid(), player.getUuid());

    ctx.sendMessage(prefix().insert(msg("Invited ", COLOR_GREEN))
      .insert(msg(target.getUsername(), COLOR_YELLOW)).insert(msg(" to your faction.", COLOR_GREEN)));
    target.sendMessage(prefix().insert(msg("You have been invited to join ", COLOR_YELLOW))
      .insert(msg(faction.name(), COLOR_CYAN)).insert(msg("!", COLOR_YELLOW)));
    target.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
      .insert(msg("/f accept " + faction.name(), COLOR_GREEN)).insert(msg(" to join.", COLOR_YELLOW)));
  }
}
