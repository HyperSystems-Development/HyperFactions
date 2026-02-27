package com.hyperfactions.command.member;

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
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f transfer {@code <player>}
 * Transfers faction leadership (leader only).
 */
public class TransferSubCommand extends FactionSubCommand {

  /** Creates a new TransferSubCommand. */
  public TransferSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("transfer", "Transfer leadership", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.TRANSFER)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to transfer leadership.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    // Check if leader
    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(prefix().insert(msg("Only the leader can transfer leadership.", COLOR_RED)));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f transfer <player>", COLOR_RED)));
      return;
    }

    String targetName = fctx.getArg(0);
    FactionMember target = faction.members().values().stream()
      .filter(m -> m.username().equalsIgnoreCase(targetName))
      .findFirst().orElse(null);

    if (target == null) {
      ctx.sendMessage(prefix().insert(msg("Player not found in your faction.", COLOR_RED)));
      return;
    }

    // GUI mode: open TransferConfirmPage
    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openTransferConfirm(playerEntity, ref, store, player, faction,
            target.uuid(), target.username());
        return;
      }
    }

    // Text mode: require confirmation
    ConfirmationManager confirmManager = hyperFactions.getConfirmationManager();
    ConfirmationResult confirmResult = confirmManager.checkOrCreate(
      player.getUuid(), ConfirmationType.TRANSFER, target.uuid()
    );

    switch (confirmResult) {
      case NEEDS_CONFIRMATION, EXPIRED_RECREATED -> {
        ctx.sendMessage(prefix().insert(msg("Are you sure you want to transfer leadership to ", COLOR_YELLOW))
          .insert(msg(target.username(), COLOR_WHITE)).insert(msg("?", COLOR_YELLOW)));
        ctx.sendMessage(prefix().insert(msg("Type ", COLOR_YELLOW))
          .insert(msg("/f transfer " + target.username() + " --text", COLOR_WHITE))
          .insert(msg(" again within " + confirmManager.getTimeoutSeconds() + " seconds to confirm.", COLOR_YELLOW)));
      }
      case CONFIRMED -> {
        FactionManager.FactionResult result = hyperFactions.getFactionManager().transferLeadership(
          faction.id(), target.uuid(), player.getUuid()
        );
        if (result == FactionManager.FactionResult.SUCCESS) {
          ctx.sendMessage(prefix().insert(msg("Transferred leadership to ", COLOR_GREEN))
            .insert(msg(target.username(), COLOR_YELLOW)).insert(msg("!", COLOR_GREEN)));
          broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
            .insert(msg(" is now the faction leader!", COLOR_GREEN)));
        } else {
          ctx.sendMessage(prefix().insert(msg("Failed to transfer leadership.", COLOR_RED)));
        }
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(prefix().insert(msg("Previous confirmation cancelled. Type again to confirm transfer.", COLOR_YELLOW)));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
