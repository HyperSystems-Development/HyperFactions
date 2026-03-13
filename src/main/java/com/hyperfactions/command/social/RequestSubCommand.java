package com.hyperfactions.command.social;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.manager.InviteManager;
import com.hyperfactions.manager.JoinRequestManager;
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
import java.util.Arrays;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f request {@code <faction>} [message]
 * Sends a join request to a faction.
 */
public class RequestSubCommand extends FactionSubCommand {

  /** Creates a new RequestSubCommand. */
  public RequestSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("request", "Request to join a faction", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.JOIN)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Request.NO_PERMISSION));
      return;
    }

    // Check if player is already in a faction
    if (hyperFactions.getFactionManager().isInFaction(player.getUuid())) {
      Faction existingFaction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
      if (existingFaction != null) {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Request.ALREADY_IN_NAMED, existingFaction.name()));
        ctx.sendMessage(MessageUtil.info(player, MessageKeys.Request.USE_LEAVE_HINT, COLOR_YELLOW));
      } else {
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.ALREADY_IN_FACTION));
      }
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: Open faction browser if no args and not text mode
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionBrowser(playerEntity, ref, store, player);
      }
      return;
    }

    // Text mode requires faction name
    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Request.USAGE));
      return;
    }

    // Find the target faction
    String factionName = fctx.getArg(0);
    Faction faction = hyperFactions.getFactionManager().getFactionByName(factionName);
    if (faction == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.FACTION_NOT_FOUND));
      return;
    }

    // Check if faction is open (if open, just join directly)
    if (faction.open()) {
      ctx.sendMessage(MessageUtil.info(player, MessageKeys.Request.FACTION_OPEN, COLOR_YELLOW, faction.name()));
      return;
    }

    // Check if player already has a pending request
    JoinRequestManager requestManager = hyperFactions.getJoinRequestManager();
    if (requestManager.hasRequest(faction.id(), player.getUuid())) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Request.ALREADY_REQUESTED));
      return;
    }

    // Check if player has an invite to this faction (they should accept it instead)
    InviteManager inviteManager = hyperFactions.getInviteManager();
    if (inviteManager.hasInvite(faction.id(), player.getUuid())) {
      ctx.sendMessage(MessageUtil.info(player, MessageKeys.Request.HAS_INVITE, COLOR_YELLOW, faction.name()));
      return;
    }

    // Build the optional message (rest of args)
    String message = null;
    String[] args = fctx.getArgs();
    if (args.length > 1) {
      message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
      if (message.length() > 200) {
        message = message.substring(0, 200); // Truncate if too long
      }
    }

    // Create the join request
    requestManager.createRequest(faction.id(), player.getUuid(), player.getUsername(), message);

    ctx.sendMessage(MessageUtil.success(player, MessageKeys.Request.SENT, faction.name()));
    if (message != null) {
      ctx.sendMessage(MessageUtil.info(player, MessageKeys.Request.YOUR_MESSAGE, COLOR_GRAY, message));
    }
    ctx.sendMessage(MessageUtil.info(player, MessageKeys.Request.OFFICER_REVIEW, COLOR_YELLOW));

    // Notify online officers
    for (UUID memberUuid : faction.members().keySet()) {
      FactionMember member = faction.getMember(memberUuid);
      if (member != null && member.isOfficerOrHigher()) {
        PlayerRef officer = plugin.getTrackedPlayer(memberUuid);
        if (officer != null) {
          officer.sendMessage(MessageUtil.success(officer, MessageKeys.Request.OFFICER_NOTIFY, player.getUsername()));
          officer.sendMessage(MessageUtil.info(officer, MessageKeys.Request.OFFICER_REVIEW_HINT, COLOR_YELLOW));
        }
      }
    }
  }
}
