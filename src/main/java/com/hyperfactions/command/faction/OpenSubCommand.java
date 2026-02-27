package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionLog;
import com.hyperfactions.data.FactionMember;
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
 * Subcommand: /f open
 * Makes the faction open (anyone can join).
 */
public class OpenSubCommand extends FactionSubCommand {

  /** Creates a new OpenSubCommand. */
  public OpenSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("open", "Allow anyone to join", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    if (!hasPermission(player, Permissions.OPEN)) {
      ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
      return;
    }

    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isLeader()) {
      ctx.sendMessage(prefix().insert(msg("Only the leader can change this setting.", COLOR_RED)));
      return;
    }

    if (faction.open()) {
      ctx.sendMessage(prefix().insert(msg("Your faction is already open.", COLOR_YELLOW)));
      return;
    }

    Faction updated = faction.withOpen(true)
      .withLog(FactionLog.create(FactionLog.LogType.SETTINGS_CHANGE,
        "Faction set to open", player.getUuid()));

    hyperFactions.getFactionManager().updateFaction(updated);

    ctx.sendMessage(prefix().insert(msg("Your faction is now open! Anyone can join with /f join.", COLOR_GREEN)));
    broadcastToFaction(faction.id(), prefix().insert(msg(player.getUsername(), COLOR_YELLOW))
      .insert(msg(" opened the faction to public joining.", COLOR_GREEN)));

    // After action, open settings page if not text mode
    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    FactionCommandContext fctx = parseContext(rawArgs);

    if (!fctx.isTextMode()) {
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
