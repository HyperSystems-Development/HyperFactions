package com.hyperfactions.command.ui;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
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
 * Subcommand: /f settings
 * Opens the faction settings GUI.
 */
public class SettingsSubCommand extends FactionSubCommand {

    public SettingsSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("settings", "Open faction settings", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to access settings.", COLOR_RED)));
            return;
        }

        Player playerEntity = store.getComponent(ref, Player.getComponentType());
        if (playerEntity != null) {
            hyperFactions.getGuiManager().openFactionSettings(playerEntity, ref, store, player, faction);
        }
    }
}
