package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Subcommand: /f relations
 * Views faction diplomatic relations.
 */
public class RelationsSubCommand extends FactionSubCommand {

    public RelationsSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("relations", "View faction relations", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.RELATIONS)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view relations.", COLOR_RED)));
            return;
        }

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        // GUI mode: open FactionRelationsPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionRelations(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: list relations
        List<UUID> allies = hyperFactions.getRelationManager().getAllies(faction.id());
        List<UUID> enemies = hyperFactions.getRelationManager().getEnemies(faction.id());

        ctx.sendMessage(msg("=== Faction Relations ===", COLOR_CYAN).bold(true));

        ctx.sendMessage(msg("Allies (" + allies.size() + "):", COLOR_GREEN));
        if (allies.isEmpty()) {
            ctx.sendMessage(msg("  (none)", COLOR_GRAY));
        } else {
            for (UUID allyId : allies) {
                Faction ally = hyperFactions.getFactionManager().getFaction(allyId);
                if (ally != null) {
                    ctx.sendMessage(msg("  - ", COLOR_GRAY).insert(msg(ally.name(), COLOR_GREEN)));
                }
            }
        }

        ctx.sendMessage(msg("Enemies (" + enemies.size() + "):", COLOR_RED));
        if (enemies.isEmpty()) {
            ctx.sendMessage(msg("  (none)", COLOR_GRAY));
        } else {
            for (UUID enemyId : enemies) {
                Faction enemy = hyperFactions.getFactionManager().getFaction(enemyId);
                if (enemy != null) {
                    ctx.sendMessage(msg("  - ", COLOR_GRAY).insert(msg(enemy.name(), COLOR_RED)));
                }
            }
        }
    }
}
