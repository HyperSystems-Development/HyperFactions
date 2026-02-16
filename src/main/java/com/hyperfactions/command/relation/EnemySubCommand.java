package com.hyperfactions.command.relation;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.manager.RelationManager;
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
 * Subcommand: /f enemy <faction>
 * Declares another faction as an enemy.
 */
public class EnemySubCommand extends FactionSubCommand {

    public EnemySubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("enemy", "Declare enemy", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.ENEMY)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to declare enemies.", COLOR_RED)));
            return;
        }

        Faction myFaction = requireFaction(ctx, player);
        if (myFaction == null) return;

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        // GUI mode: open SetRelationModal when no faction specified
        if (!fctx.hasArgs() && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openSetRelationModal(playerEntity, ref, store, player, myFaction);
                return;
            }
        }

        if (!fctx.hasArgs()) {
            ctx.sendMessage(prefix().insert(msg("Usage: /f enemy <faction>", COLOR_RED)));
            return;
        }

        String factionName = fctx.joinArgs();
        Faction targetFaction = hyperFactions.getFactionManager().getFactionByName(factionName);
        if (targetFaction == null) {
            ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
            return;
        }

        RelationManager.RelationResult result = hyperFactions.getRelationManager().setEnemy(player.getUuid(), targetFaction.id());

        switch (result) {
            case SUCCESS -> ctx.sendMessage(prefix().insert(msg(targetFaction.name(), COLOR_RED))
                .insert(msg(" is now your enemy!", COLOR_RED)));
            case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to manage relations.", COLOR_RED)));
            case ALREADY_ENEMY -> ctx.sendMessage(prefix().insert(msg("You are already enemies with that faction.", COLOR_RED)));
            case ENEMY_LIMIT_REACHED -> ctx.sendMessage(prefix().insert(msg("You have reached the maximum number of enemies.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to set enemy.", COLOR_RED)));
        }
    }
}
