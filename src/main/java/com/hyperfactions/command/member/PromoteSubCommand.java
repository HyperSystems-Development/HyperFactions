package com.hyperfactions.command.member;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionRole;
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
 * Subcommand: /f promote {@code <player>}
 * Promotes a member to officer (leader only).
 */
public class PromoteSubCommand extends FactionSubCommand {

  /** Creates a new PromoteSubCommand. */
  public PromoteSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("promote", "Promote to officer", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.PROMOTE)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.PROMOTE_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.PROMOTE_USAGE));
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

    FactionManager.FactionResult result = hyperFactions.getFactionManager().promoteMember(
      faction.id(), target.uuid(), player.getUuid()
    );

    switch (result) {
      case SUCCESS -> {
        String officerName = ConfigManager.get().getRoleDisplayName(FactionRole.OFFICER);
        ctx.sendMessage(MessageUtil.success(player, CommandKeys.Rank.PROMOTED, target.username(), officerName));
        broadcastToFaction(faction.id(), MessageUtil.success(player, CommandKeys.Rank.PROMOTE_BROADCAST, target.username(), officerName));
        // Show members page after action (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
          }
        }
      }
      case NOT_LEADER -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.MUST_BE_LEADER));
      case CANNOT_PROMOTE_LEADER -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.ALREADY_HIGHEST));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.PROMOTE_FAILED));
    }
  }
}
