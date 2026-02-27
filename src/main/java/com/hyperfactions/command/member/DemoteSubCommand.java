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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to demote members.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f demote <player>", COLOR_RED)));
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

    FactionManager.FactionResult result = hyperFactions.getFactionManager().demoteMember(
      faction.id(), target.uuid(), player.getUuid()
    );

    switch (result) {
      case SUCCESS -> {
        String memberName = ConfigManager.get().getRoleDisplayName(FactionRole.MEMBER);
        ctx.sendMessage(prefix().insert(msg("Demoted ", COLOR_GREEN))
          .insert(msg(target.username(), COLOR_YELLOW)).insert(msg(" to " + memberName + ".", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
          .insert(msg(" was demoted to " + memberName + ".", COLOR_RED)));
        // Show members page after action (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
          }
        }
      }
      case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can demote members.", COLOR_RED)));
      case CANNOT_DEMOTE_MEMBER -> ctx.sendMessage(prefix().insert(msg("That player is already a Member.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to demote player.", COLOR_RED)));
    }
  }
}
