package com.hyperfactions.command.ui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.command.util.CommandUtil;
import com.hyperfactions.data.Faction;
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
 * Subcommand: /f settings [player]
 * Opens the faction settings GUI, or player settings with "player" argument.
 */
public class SettingsSubCommand extends FactionSubCommand {

  /** Creates a new SettingsSubCommand. */
  public SettingsSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
    super("settings", "Open faction or player settings", hyperFactions, plugin);
  }

  /** Executes the command. */
  @Override
  protected void execute(@NotNull CommandContext ctx,
             @NotNull Store<EntityStore> store,
             @NotNull Ref<EntityStore> ref,
             @NotNull PlayerRef player,
             @NotNull World currentWorld) {

    // Check for "player" argument — opens personal settings (no faction required)
    String[] rawArgs = CommandUtil.parseRawArgs(ctx.getInputString(), 2);
    if (rawArgs.length > 0 && "player".equalsIgnoreCase(rawArgs[0])) {
      Player playerEntity = store.getComponent(ref, Player.getComponentType());
      if (playerEntity != null) {
        hyperFactions.getGuiManager().openPlayerSettings(playerEntity, ref, store, player);
      }
      return;
    }

    // Default: open faction settings (requires faction + officer)
    Faction faction = requireFaction(ctx, player);
    if (faction == null) {
      return;
    }

    FactionMember member = faction.getMember(player.getUuid());
    if (member == null || !member.isOfficerOrHigher()) {
      ctx.sendMessage(MessageUtil.error(player, MessageKeys.Common.MUST_BE_OFFICER));
      return;
    }

    Player playerEntity = store.getComponent(ref, Player.getComponentType());
    if (playerEntity != null) {
      hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
    }
  }
}
