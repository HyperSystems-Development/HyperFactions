package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.manager.ConfirmationManager.ConfirmationResult;
import com.hyperfactions.manager.ConfirmationManager.ConfirmationType;
import com.hyperfactions.manager.ConfirmationManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f leave
 * Leaves the current faction.
 */
public class LeaveSubCommand extends FactionSubCommand {

  /** Creates a new LeaveSubCommand. */
  public LeaveSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("leave", "Leave your faction", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.LEAVE)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to leave factions.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open appropriate confirm page based on role
    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        FactionMember member = faction.getMember(player.getUuid());
        boolean isLeader = member != null && member.role() == FactionRole.LEADER;
        if (isLeader) {
          hyperFactions.getGuiManager().openLeaderLeaveConfirm(playerEntity, ref, store, player, faction);
        } else {
          hyperFactions.getGuiManager().openLeaveConfirm(playerEntity, ref, store, player, faction);
        }
        return;
      }
    }

    // Text mode: require confirmation
    ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
    ConfirmationResult confirmResult = confirmManager.checkOrCreate(
      player.getUuid(), ConfirmationType.LEAVE, null
    );

    switch (confirmResult) {
      case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
        ctx.sendMessage(prefix().insert(msg("Are you sure you want to leave your faction?", COLOR_YELLOW)));
        ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
          .insert(msg("/f leave --text", COLOR_WHITE))
          .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
      }
      case CONFIRMED -> {
        UUID factionId = faction.id();
        FactionManager.FactionResult result = hyperFactions.getFactionManager().removeMember(
          factionId, player.getUuid(), player.getUuid(), false
        );
        if (result == FactionManager.FactionResult.SUCCESS) {
          ctx.sendMessage(prefix().insert(msg("You have left your faction.", COLOR_GREEN)));
          broadcastToFaction(factionId, prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
            .insert(msg(" has left the faction.", COLOR_RED)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Failed to leave faction.", COLOR_RED)));
        }
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm leave.", COLOR_YELLOW)));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
