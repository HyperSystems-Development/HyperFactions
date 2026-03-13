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
 * Subcommand: /f rename {@code <name>}
 * Renames the faction (leader only).
 */
public class RenameSubCommand extends FactionSubCommand {

  /** Creates a new RenameSubCommand. */
  public RenameSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("rename", "Rename your faction", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.RENAME)) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.NOT_LEADER));
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
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.USAGE));
      return;
    }

    String newName = fctx.joinArgs();
    ConfigManager config = ConfigManager.get();

    if (newName.length() < config.getMinNameLength()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.TOO_SHORT, config.getMinNameLength()));
      return;
    }
    if (newName.length() > config.getMaxNameLength()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.TOO_LONG, config.getMaxNameLength()));
      return;
    }
    if (hyperFactions.getFactionManager().isNameTaken(newName) && !newName.equalsIgnoreCase(faction.name())) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Rename.NAME_TAKEN));
      return;
    }

    String oldName = faction.name();
    Faction updated = faction.withName(newName)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        "Renamed from '" + oldName + "' to '" + newName + "'", player.getUuid(),
        MessageKeys.LogsGui.MSG_RENAMED, oldName, newName));

    hyperFactions.getFactionManager().updateFaction(updated);

    // Refresh world maps to show new faction name (respects configured refresh mode)
    if (hyperFactions.getWorldMapService() != null) {
      hyperFactions.getWorldMapService().triggerFactionWideRefresh(faction.id());
    }

    ctx.sendMessage(MessageUtil.success(player, MessageKeys.Rename.SUCCESS, newName));
    broadcastToFaction(faction.id(), MessageUtil.success(player, MessageKeys.Rename.BROADCAST, player.getUsername(), newName));

    // After action, open settings page if not text mode
    if (fctx.shouldOpenGuiAfterAction()) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        // Refresh faction to get updated data
        Faction refreshed = hyperFactions.getFactionManager().getFaction(faction.id());
        if (refreshed != null) {
          hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, refreshed);
        }
      }
    }
  }
}
