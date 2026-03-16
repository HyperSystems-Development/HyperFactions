package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.api.events.EventBus;
import com.hyperfactions.api.events.FactionRenameEvent;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.CommandKeys;
import com.hyperfactions.util.CommonKeys;
import com.hyperfactions.util.GuiKeys;
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
 * Subcommand: /f desc {@code <text>}
 * Sets the faction description (officer+).
 */
public class DescSubCommand extends FactionSubCommand {

  /** Creates a new DescSubCommand. */
  public DescSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("desc", "Set faction description", hyperFactions, plugin);
    addAliases("description");
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.DESC)) {
      ctx.sendMessage(MessageUtil.error(player, CommonKeys.Common.NO_PERMISSION));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isOfficerOrHigher()) {
      ctx.sendMessage(MessageUtil.error(player, CommandKeys.Desc.NOT_OFFICER));
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

    // Text mode without args clears description
    String description = fctx.hasArgs() ? fctx.joinArgs() : null;

    Faction updated = faction.withDescription(description)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        description != null ? "Description set" : "Description cleared", player.getUuid(),
        description != null ? GuiKeys.LogsGui.MSG_DESC_SET : GuiKeys.LogsGui.MSG_DESC_CLEARED));

    hyperFactions.getFactionManager().updateFaction(updated);
    EventBus.publish(new FactionRenameEvent(faction.id(), FactionRenameEvent.Field.DESCRIPTION,
        faction.description(), description, player.getUuid()));

    if (description != null) {
      ctx.sendMessage(MessageUtil.success(player, CommandKeys.Desc.SET));
    } else {
      ctx.sendMessage(MessageUtil.success(player, CommandKeys.Desc.CLEARED));
    }

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
