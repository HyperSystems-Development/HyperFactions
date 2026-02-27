package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
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
 * Subcommand: /f disband
 * Disbands the player's faction (leader only).
 */
public class DisbandSubCommand extends FactionSubCommand {

  /** Creates a new DisbandSubCommand. */
  public DisbandSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("disband", "Disband your faction", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.DISBAND)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to disband factions.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    // Check if leader
    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(prefix().insert(msg("Only the faction leader can disband.", COLOR_RED)));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: open DisbandConfirmPage
    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openDisbandConfirm(playerEntity, ref, store, player, faction);
        return;
      }
    }

    // Text mode: require confirmation
    ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
    ConfirmationResult confirmResult = confirmManager.checkOrCreate(
      player.getUuid(), ConfirmationType.DISBAND, null
    );

    switch (confirmResult) {
      case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
        ctx.sendMessage(prefix().insert(msg("Are you sure you want to disband your faction?", COLOR_YELLOW)));
        ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
          .insert(msg("/f disband --text", COLOR_WHITE))
          .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
      }
      case CONFIRMED -> {
        UUID factionId = faction.id();
        FactionManager.FactionResult result = hyperFactions.getFactionManager().disbandFaction(
          factionId, player.getUuid()
        );
        if (result == FactionManager.FactionResult.SUCCESS) {
          hyperFactions.getClaimManager().unclaimAll(factionId);
          hyperFactions.getInviteManager().clearFactionInvites(factionId);
          hyperFactions.getJoinRequestManager().clearFactionRequests(factionId);
          hyperFactions.getRelationManager().clearAllRelations(factionId);
          ctx.sendMessage(prefix().insert(msg("Your faction has been disbanded.", COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Failed to disband faction.", COLOR_RED)));
        }
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm disband.", COLOR_YELLOW)));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
