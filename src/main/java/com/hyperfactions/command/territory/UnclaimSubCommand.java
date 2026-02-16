package com.hyperfactions.command.territory;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.Permissions;
import com.hyperfactions.command.FactionCommandContext;
import com.hyperfactions.command.FactionSubCommand;
import com.hyperfactions.data.Faction;
import com.hyperfactions.util.MessageUtil;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.platform.HyperFactionsPlugin;
import com.hyperfactions.util.ChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Subcommand: /f unclaim
 * Unclaims the current chunk.
 */
public class UnclaimSubCommand extends FactionSubCommand {

    public UnclaimSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("unclaim", "Unclaim this chunk", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.UNCLAIM)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to unclaim territory.", COLOR_RED)));
            return;
        }

        Faction faction = requireFaction(ctx, player);
        if (faction == null) return;

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
        String[] rawArgs = parts.length > 2 ? java.util.Arrays.copyOfRange(parts, 2, parts.length) : new String[0];
        FactionCommandContext fctx = parseContext(rawArgs);

        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().unclaim(
            player.getUuid(), currentWorld.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Unclaimed chunk at " + chunkX + ", " + chunkZ + ".", COLOR_GREEN)));
                // Show map after unclaiming (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                    }
                }
            }
            case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to unclaim land.", COLOR_RED)));
            case CHUNK_NOT_CLAIMED -> ctx.sendMessage(prefix().insert(msg("This chunk is not claimed.", COLOR_RED)));
            case NOT_YOUR_CLAIM -> ctx.sendMessage(prefix().insert(msg("Your faction doesn't own this chunk.", COLOR_RED)));
            case CANNOT_UNCLAIM_HOME -> ctx.sendMessage(prefix().insert(msg("Cannot unclaim the chunk with faction home.", COLOR_RED)));
            case WOULD_DISCONNECT -> ctx.sendMessage(prefix().insert(msg("Cannot unclaim — it would disconnect your territory.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to unclaim chunk.", COLOR_RED)));
        }
    }
}
