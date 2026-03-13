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
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.TRANSFER_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    // Check if leader
    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.MUST_BE_LEADER));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.TRANSFER_USAGE));
      return;
    }

    String targetName = fctx.getArg(0);
    FactionMember target = faction.members().values().stream()
      .filter(m -> m.username().equalsIgnoreCase(targetName))
      .findFirst().orElse(null);

    if (target == null) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.PLAYER_NOT_IN_FACTION));
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
        ctx.sendMessage(MessageUtil.info(player, CommandKeys.Rank.TRANSFER_CONFIRM, COLOR_YELLOW, target.username()));
        ctx.sendMessage(MessageUtil.info(player, CommandKeys.Rank.TRANSFER_CONFIRM_INSTRUCTION, COLOR_YELLOW,
          target.username(), confirmManager.getTimeoutSeconds()));
      }
      case CONFIRMED -> {
        FactionManager.FactionResult result = hyperFactions.getFactionManager().transferLeadership(
          faction.id(), target.uuid(), player.getUuid()
        );
        if (result == FactionManager.FactionResult.SUCCESS) {
          ctx.sendMessage(MessageUtil.success(player, CommandKeys.Rank.TRANSFERRED, target.username()));
          broadcastToFaction(faction.id(), MessageUtil.success(player, CommandKeys.Rank.TRANSFER_BROADCAST, target.username()));
        } else {
          ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.TRANSFER_FAILED));
        }
      }
      case DIFFERENT_ACTION -> {
        ctx.sendMessage(MessageUtil.info(player, CommandKeys.Rank.TRANSFER_CANCELLED, COLOR_YELLOW));
      }
      default -> throw new IllegalStateException("Unexpected value");
    }
  }
}
