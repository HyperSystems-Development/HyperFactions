package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.PlayerResolver;
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
 * Subcommand: /f power [player]
 * Views power information.
 */
public class PowerSubCommand extends FactionSubCommand {

  /** Creates a new PowerSubCommand. */
  public PowerSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("power", "View power level", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.POWER)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to view power info.", COLOR_RED)));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    UUID targetUuid;
    String targetName;

    if (!fctx.hasArgs()) {
      // Show own power
      targetUuid = player.getUuid();
      targetName = player.getUsername();
    } else {
      // Look up target player using centralized resolver
      var resolved = PlayerResolver.resolve(hyperFactions, fctx.getArg(0));
      if (resolved == null) {
        ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
        return;
      }
      targetUuid = resolved.uuid();
      targetName = resolved.username();
    }

    // Power info is text-only (no GUI mode needed)
    PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);
    ctx.sendMessage(msg(targetName + "'s Power:", COLOR_CYAN));
    ctx.sendMessage(msg("Current: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f (%d%%)",
      power.power(), power.getEffectiveMaxPower(), power.getPowerPercent()), COLOR_WHITE)));
  }
}
