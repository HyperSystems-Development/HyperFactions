package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.PlayerResolver;
import com.hyperfactions.util.TimeUtil;
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
 * Subcommand: /f who [player]
 * Views player faction information.
 */
public class WhoSubCommand extends FactionSubCommand {

  /** Creates a new WhoSubCommand. */
  public WhoSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("who", "View player info", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.WHO)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission to view player info.", COLOR_RED)));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    UUID targetUuid;
    String targetName;

    if (!fctx.hasArgs()) {
      // Show own info
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

    // GUI mode - open player info page
    if (!fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openPlayerInfo(playerEntity, ref, store, player, targetUuid, targetName);
        return;
      }
    }

    // Text mode: output to chat
    // Get faction info
    Faction faction = hyperFactions.getFactionManager().getPlayerFaction(targetUuid);
    FactionMember member = faction != null ? faction.getMember(targetUuid) : null;

    // Check if online
    boolean isOnline = plugin.getTrackedPlayer(targetUuid) != null;

    // Display info
    ctx.sendMessage(msg("=== " + targetName + " ===", COLOR_CYAN));

    if (faction != null && member != null) {
      ctx.sendMessage(msg("Faction: ", COLOR_GRAY).insert(msg(faction.name(), COLOR_WHITE)));
      ctx.sendMessage(msg("Role: ", COLOR_GRAY).insert(msg(ConfigManager.get().getRoleDisplayName(member.role()), COLOR_WHITE)));
      ctx.sendMessage(msg("Joined: ", COLOR_GRAY).insert(msg(TimeUtil.formatRelative(member.joinedAt()), COLOR_WHITE)));
    } else {
      ctx.sendMessage(msg("Faction: ", COLOR_GRAY).insert(msg("None", COLOR_WHITE)));
    }

    // Power display — hardcore mode shows faction power, normal mode shows player power
    String powerText;
    if (ConfigManager.get().isHardcoreMode()) {
      if (faction != null) {
        double fPower = hyperFactions.getPowerManager().getFactionPower(faction.id());
        double fMaxPower = hyperFactions.getPowerManager().getFactionMaxPower(faction.id());
        powerText = String.format("%.1f/%.1f", fPower, fMaxPower);
      } else {
        powerText = "0.0/0.0";
      }
    } else {
      PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);
      powerText = String.format("%.1f/%.1f", power.power(), power.getEffectiveMaxPower());
    }
    ctx.sendMessage(msg("Power: ", COLOR_GRAY).insert(msg(powerText, COLOR_WHITE)));
    ctx.sendMessage(msg("Status: ", COLOR_GRAY).insert(msg(isOnline ? "Online" : "Offline", isOnline ? COLOR_GREEN : COLOR_RED)));

    if (!isOnline && member != null) {
      ctx.sendMessage(msg("Last seen: ", COLOR_GRAY).insert(msg(TimeUtil.formatRelative(member.lastOnline()), COLOR_WHITE)));
    }
  }
}
