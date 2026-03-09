package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.MessageKeys;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f color <code>
 * Sets the faction color (officer+).
 */
public class ColorSubCommand extends FactionSubCommand {

  /** Creates a new ColorSubCommand. */
  public ColorSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("color", "Set faction color", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.COLOR)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Color.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isOfficerOrHigher()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Color.NOT_OFFICER));
      return;
    }

    if (!ConfigManager.get().isAllowColors()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Color.COLORS_DISABLED));
      return;
    }

    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    // GUI mode: Open settings page if no args and not text mode
    if (!fctx.hasArgs() && !fctx.isTextMode()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
      }
      return;
    }

    // Text mode requires args
    if (!fctx.hasArgs()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Color.USAGE));
      ctx.sendMessage(Message.raw(HFMessages.get(player, MessageKeys.Color.USAGE_HINT)).color(COLOR_GRAY));
      return;
    }

    String colorInput = fctx.getArg(0).toLowerCase();
    String hexColor;
    if (colorInput.startsWith("#") && colorInput.length() == 7 && colorInput.substring(1).matches("[0-9a-f]+")) {
      // Direct hex input
      hexColor = colorInput.toUpperCase();
    } else if (colorInput.length() == 1 && colorInput.matches("[0-9a-f]")) {
      // Legacy color code - convert to hex
      hexColor = com.hyperfactions.util.LegacyColorParser.codeToHex(colorInput.charAt(0));
    } else {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Color.INVALID));
      return;
    }

    Faction updated = faction.withColor(hexColor)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        "Color changed to '" + hexColor + "'", player.getUuid()));

    hyperFactions.getFactionManager().updateFaction(updated);

    // Refresh world maps to show new faction color (respects configured refresh mode)
    hyperFactions.getWorldMapService().triggerFactionWideRefresh(faction.id());

    // Show success with the actual color swatch
    ctx.sendMessage(MessageUtil.prefix().insert(
      Message.raw(HFMessages.get(player, MessageKeys.Color.SUCCESS) + " ").color(COLOR_GREEN))
      .insert(Message.raw("\u2588\u2588").color(hexColor)));

    // After action, open settings page if not text mode
    if (fctx.shouldOpenGuiAfterAction()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
        if (refreshed != null) {
          hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
        }
      }
    }
  }
}
