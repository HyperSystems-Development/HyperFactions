package com.hyperfactions.command.info;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
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

import java.util.List;

/**
 * Subcommand: /f members
 * Views faction members.
 */
public class MembersSubCommand extends FactionSubCommand {

    public MembersSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("members", "View faction members", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.MEMBERS)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to view faction members.", COLOR_RED)));
            return;
        }

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        // GUI mode: open FactionMembersPage
        if (fctx.shouldOpenGui()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openFactionMembers(playerEntity, ref, store, player, faction);
                return;
            }
        }

        // Text mode: output member list to chat
        List<FactionMember> members = faction.getMembersSorted();
        ctx.sendMessage(msg("=== " + faction.name() + " Members (" + members.size() + ") ===", COLOR_CYAN).bold(true));

        for (FactionMember member : members) {
            String roleColor = switch (member.role()) {
                case LEADER -> COLOR_YELLOW;
                case OFFICER -> COLOR_GREEN;
                default -> COLOR_GRAY;
            };
            boolean isOnline = plugin.getTrackedPlayer(member.uuid()) != null;
            String status = isOnline ? " [Online]" : "";
            ctx.sendMessage(msg(member.role().getDisplayName() + " ", roleColor)
                .insert(msg(member.username(), COLOR_WHITE))
                .insert(msg(status, isOnline ? COLOR_GREEN : COLOR_GRAY)));
        }
    }
}
