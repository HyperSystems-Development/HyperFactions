package com.hyperfactions.command.faction;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
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
 * Subcommand: /f desc <text>
 * Sets the faction description (officer+).
 */
public class DescSubCommand extends FactionSubCommand {

    public DescSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("desc", "Set faction description", hyperFactions, plugin);
        addAliases("description");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.DESC)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission.", COLOR_RED)));
            return;
        }

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        FactionMember member = faction.getMember(player.getUuid());
        if (member == null || !member.isOfficerOrHigher()) {
            ctx.sendMessage(prefix().insert(msg("You must be an officer to set the description.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
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
                description != null ? "Description set" : "Description cleared", player.getUuid()));

        hyperFactions.getFactionManager().updateFaction(updated);

        if (description != null) {
            ctx.sendMessage(prefix().insert(msg("Faction description set!", COLOR_GREEN)));
        } else {
            ctx.sendMessage(prefix().insert(msg("Faction description cleared.", COLOR_GREEN)));
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
