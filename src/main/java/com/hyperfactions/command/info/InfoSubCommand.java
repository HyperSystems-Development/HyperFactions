package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.manager.PowerManager;
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
 * Subcommand: /f info [faction]
 * Views faction information.
 * Aliases: show
 */
public class InfoSubCommand extends FactionSubCommand {

    public InfoSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("info", "View faction info", hyperFactions, plugin);
        addAliases("show");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.INFO)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction info.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        Faction faction;
        if (fctx.hasArgs()) {
            String factionName = fctx.joinArgs();
            faction = hyperFactions.getFactionManager().getFactionByName(factionName);
            if (faction == null) {
                ctx.sendMessage(prefix().insert(msg("Faction '" + factionName + "' not found.", COLOR_RED)));
                return;
            }
        } else {
            faction = hyperFactions.getFactionManager().getPlayerFaction(player.getUuid());
            if (faction == null) {
                ctx.sendMessage(MessageUtil.error("You are not in a faction. Use /f info <faction>"));
                return;
            }
        }

        // GUI mode: open FactionInfoPage (default when no args and no --text flag)
        if (!fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionInfo(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: output to chat
        PowerManager.FactionPowerStats stats = hyperFactions.getPowerManager().getFactionPowerStats(faction.id());
        FactionMember leader = faction.getLeader();

        ctx.sendMessage(msg("=== " + faction.name() + " ===", COLOR_CYAN).bold(true));
        ctx.sendMessage(msg("Leader: ", COLOR_GRAY).insert(msg(leader != null ? leader.username() : "None", COLOR_YELLOW)));
        ctx.sendMessage(msg("Members: ", COLOR_GRAY).insert(msg(faction.getMemberCount() + "/" + ConfigManager.get().getMaxMembers(), COLOR_WHITE)));
        ctx.sendMessage(msg("Power: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f", stats.currentPower(), stats.maxPower()), COLOR_WHITE)));
        ctx.sendMessage(msg("Claims: ", COLOR_GRAY).insert(msg(stats.currentClaims() + "/" + stats.maxClaims(), COLOR_WHITE)));
        if (stats.isRaidable()) {
            ctx.sendMessage(msg("RAIDABLE!", COLOR_RED).bold(true));
        }
    }
}
