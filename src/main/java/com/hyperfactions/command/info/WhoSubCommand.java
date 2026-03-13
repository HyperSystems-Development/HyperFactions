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
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
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
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Info.WHO_NO_PERMISSION));
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
        ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.PLAYER_NOT_FOUND));
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
    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.PLAYER_HEADER, targetName), COLOR_CYAN));

    if (faction != null && member != null) {
      ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_FACTION, faction.name()), COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_ROLE, ConfigManager.get().getRoleDisplayName(member.role())), COLOR_GRAY));
      ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_JOINED, TimeUtil.formatRelative(member.joinedAt())), COLOR_GRAY));
    } else {
      ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_FACTION_NONE), COLOR_GRAY));
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
    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_POWER, powerText), COLOR_GRAY));
    String statusText = isOnline ? HFMessages.get(player, MessageKeys.Common.ONLINE) : HFMessages.get(player, MessageKeys.Common.OFFLINE);
    ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_STATUS, statusText), COLOR_GRAY));

    if (!isOnline && member != null) {
      ctx.sendMessage(msg(HFMessages.get(player, MessageKeys.Info.WHO_LAST_SEEN, TimeUtil.formatRelative(member.lastOnline())), COLOR_GRAY));
    }
  }
}
