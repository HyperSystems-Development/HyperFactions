package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Subcommand: /f power [player]
 * Views power information.
 */
public class PowerSubCommand extends FactionSubCommand {

    public PowerSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("power", "View power level", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.POWER)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view power info.", COLOR_RED)));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        UUID targetUuid;
        String targetName;

        if (!fctx.hasArgs()) {
            // Show own power
            targetUuid = player.getUuid();
            targetName = player.getUsername();
        } else {
            // Look up target player
            targetName = fctx.getArg(0);
            targetUuid = null;

            // First check online players
            for (PlayerRef online : plugin.getTrackedPlayers().values()) {
                if (online.getUsername().equalsIgnoreCase(targetName)) {
                    targetUuid = online.getUuid();
                    targetName = online.getUsername();
                    break;
                }
            }

            // If not online, search faction members
            if (targetUuid == null) {
                for (Faction faction : hyperFactions.getFactionManager().getAllFactions()) {
                    for (FactionMember member : faction.getMembersSorted()) {
                        if (member.username().equalsIgnoreCase(targetName)) {
                            targetUuid = member.uuid();
                            targetName = member.username();
                            break;
                        }
                    }
                    if (targetUuid != null) break;
                }
            }

            if (targetUuid == null) {
                ctx.sendMessage(prefix().insert(msg("Player not found.", COLOR_RED)));
                return;
            }
        }

        // TODO: GUI mode disabled - PlayerInfoPage UI templates don't exist yet
        // When player_info.ui, info_section.ui, info_row.ui, progress_bar.ui are created,
        // uncomment this block to enable GUI mode:
        // if (!fctx.isTextMode()) {
        //     Player playerEntity = store.getComponent(ref, Player.getComponentType());
        //     if (playerEntity != null) {
        //         hyperFactions.getGuiManager().openPlayerInfo(playerEntity, ref, store, player, targetUuid, targetName);
        //         return;
        //     }
        // }

        // Text mode: output power to chat
        PlayerPower power = hyperFactions.getPowerManager().getPlayerPower(targetUuid);
        ctx.sendMessage(msg(targetName + "'s Power:", COLOR_CYAN));
        ctx.sendMessage(msg("Current: ", COLOR_GRAY).insert(msg(String.format("%.1f/%.1f (%d%%)",
            power.power(), power.getEffectiveMaxPower(), power.getPowerPercent()), COLOR_WHITE)));
    }
}
