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
      ctx.sendMessage(prefix().insert(msg("You don't have permission to promote members.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.hasArgs()) {
      ctx.sendMessage(prefix().insert(msg("Usage: /f promote <player>", COLOR_RED)));
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

    FactionManager.FactionResult result = hyperFactions.getFactionManager().promoteMember(
      faction.id(), target.uuid(), player.getUuid()
    );

    switch (result) {
      case SUCCESS -> {
        String officerName = ConfigManager.get().getRoleDisplayName(FactionRole.OFFICER);
        ctx.sendMessage(prefix().insert(msg("Promoted ", COLOR_GREEN))
          .insert(msg(target.username(), COLOR_YELLOW)).insert(msg(" to " + officerName + "!", COLOR_GREEN)));
        broadcastToFaction(faction.id(), prefix().insert(msg(target.username(), COLOR_YELLOW))
          .insert(msg(" was promoted to " + officerName + "!", COLOR_GREEN)));
        // Show members page after action (if not text mode)
        if (!fctx.isTextMode()) {
          Player playerEntity = store.getComponent(ref, Player.getComponentType());
          if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
          }
        }
      }
      case NOT_LEADER -> ctx.sendMessage(prefix().insert(msg("Only the leader can promote members.", COLOR_RED)));
      case CANNOT_PROMOTE_LEADER -> ctx.sendMessage(prefix().insert(msg("Cannot promote further. Use /f transfer to change leader.", COLOR_RED)));
      default -> ctx.sendMessage(prefix().insert(msg("Failed to promote player.", COLOR_RED)));
    }
  }
}
