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
 * Subcommand: /f demote {@code <player>}
 * Demotes an officer to member (leader only).
 */
public class DemoteSubCommand extends FactionSubCommand {

  /** Creates a new DemoteSubCommand. */
  public DemoteSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("demote", "Demote to member", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.DEMOTE)) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.DEMOTE_NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.DEMOTE_USAGE));
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

    FactionManager.FactionResult result = hyperFactions.getFactionManager().demoteMember(
      faction.id(), target.uuid(), player.getUuid()
    );

    switch (result) {
      case SUCCESS -> {
        String memberName = ConfigManager.get().getRoleDisplayName(FactionRole.MEMBER);
        ctx.sendMessage(MessageUtil.success(player, CommandKeys.Rank.DEMOTED, target.username(), memberName));
        broadcastToFaction(faction.id(), MessageUtil.error(player, CommandKeys.Rank.DEMOTE_BROADCAST, target.username(), memberName));
        // Show members page after action (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
          }
        }
      }
      case NOT_LEADER -> ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.MUST_BE_LEADER));
      case CANNOT_DEMOTE_MEMBER -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.ALREADY_LOWEST));
      default -> ctx.sendMessage(MessageUtil.error(player, CommandKeys.Rank.DEMOTE_FAILED));
    }
  }
}
