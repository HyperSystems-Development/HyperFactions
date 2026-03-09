package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
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
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f kick {@code <player>}
 * Kicks a member from the faction (officer+).
 */
public class KickSubCommand extends FactionSubCommand {

  /** Creates a new KickSubCommand. */
  public KickSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("kick", "Kick a member", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.KICK)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.USAGE));
      return;
    }

    String targetName = fctx.getArg(0);
    FactionMember target = faction.members().values().stream()
      .filter(m -> m.username().equalsIgnoreCase(targetName))
      .findFirst().orElse(null);

    if (target == null) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.NOT_IN_YOUR_FACTION, targetName));
      return;
    }

    FactionManager.FactionResult result = hyperFactions.getFactionManager().removeMember(
      faction.id(), target.uuid(), player.getUuid(), true
    );

    switch (result) {
      case SUCCESS -> {
        ctx.sendMessage(MessageUtil.success(player, MessageKeys.Kick.SUCCESS, target.username()));
        broadcastToFaction(faction.id(), MessageUtil.error(player, MessageKeys.Kick.BROADCAST, target.username()));
        PlayerRef targetPlayer = plugin.getTrackedPlayer(target.uuid());
        if (targetPlayer != null) {
          targetPlayer.sendMessage(MessageUtil.error(targetPlayer, MessageKeys.Kick.KICKED));
        }

        // Show members page after action (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
          }
        }
      }
      case NOT_OFFICER -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.CANNOT_KICK_HIGHER));
      case CANNOT_KICK_LEADER -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.CANNOT_KICK_LEADER));
      default -> ctx.sendMessage(MessageUtil.error(player, MessageKeys.Kick.FAILED));
    }
  }
}
