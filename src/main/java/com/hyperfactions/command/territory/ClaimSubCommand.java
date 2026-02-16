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

import java.util.UUID;

/**
 * Subcommand: /f claim
 * Claims the current chunk for your faction.
 */
public class ClaimSubCommand extends FactionSubCommand {

    public ClaimSubCommand(@NotNull HyperFactions hyperFactions, @NotNull HyperFactionsPlugin plugin) {
        super("claim", "Claim this chunk", hyperFactions, plugin);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef player,
                          @NotNull World currentWorld) {

        if (!hasPermission(player, Permissions.CLAIM)) {
            ctx.sendMessage(prefix().insert(msg("You don't have permission to claim territory.", COLOR_RED)));
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

        // Context-aware behavior - check current chunk status
        UUID playerFactionId = faction.id();
        UUID chunkOwner = hyperFactions.getClaimManager().getClaimOwner(currentWorld.getName(), chunkX, chunkZ);

        // If own territory and not text mode, just show map
        if (playerFactionId != null && playerFactionId.equals(chunkOwner) && !fctx.isTextMode()) {
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                ctx.sendMessage(prefix().insert(msg("Your faction already owns this chunk.", COLOR_GRAY)));
                hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                return;
            }
        }

        // If enemy territory and not text mode, suggest overclaim via map
        if (chunkOwner != null && !chunkOwner.equals(playerFactionId) && !fctx.isTextMode()) {
            boolean isAlly = playerFactionId != null && hyperFactions.getRelationManager().areAllies(playerFactionId, chunkOwner);
            if (isAlly) {
                ctx.sendMessage(prefix().insert(msg("You cannot claim ally territory.", COLOR_RED)));
            } else {
                ctx.sendMessage(prefix().insert(msg("This chunk is claimed. Use ", COLOR_RED))
                    .insert(msg("/f overclaim", COLOR_WHITE))
                    .insert(msg(" if they are raidable.", COLOR_RED)));
            }
            Player playerEntity = store.getComponent(ref, Player.getComponentType());
            if (playerEntity != null) {
                hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
            }
            return;
        }

        // Execute claim
        ClaimManager.ClaimResult result = hyperFactions.getClaimManager().claim(
            player.getUuid(), currentWorld.getName(), chunkX, chunkZ
        );

        switch (result) {
            case SUCCESS -> {
                ctx.sendMessage(prefix().insert(msg("Claimed chunk at " + chunkX + ", " + chunkZ + "!", COLOR_GREEN)));
                // Show map after claiming (if not text mode)
                if (!fctx.isTextMode()) {
                    Player playerEntity = store.getComponent(ref, Player.getComponentType());
                    if (playerEntity != null) {
                        hyperFactions.getGuiManager().openChunkMap(playerEntity, ref, store, player);
                    }
                }
            }
            case NOT_IN_FACTION -> ctx.sendMessage(MessageUtil.error("You are not in a faction."));
            case NOT_OFFICER -> ctx.sendMessage(prefix().insert(msg("You must be an officer to claim land.", COLOR_RED)));
            case ALREADY_CLAIMED_SELF -> ctx.sendMessage(prefix().insert(msg("Your faction already owns this chunk.", COLOR_RED)));
            case ALREADY_CLAIMED_OTHER -> ctx.sendMessage(prefix().insert(msg("This chunk is already claimed.", COLOR_RED)));
            case MAX_CLAIMS_REACHED -> ctx.sendMessage(prefix().insert(msg("Your faction has reached max claims. Get more power!", COLOR_RED)));
            case NOT_ADJACENT -> ctx.sendMessage(prefix().insert(msg("You must claim adjacent to existing territory.", COLOR_RED)));
            case WORLD_NOT_ALLOWED -> ctx.sendMessage(prefix().insert(msg("Claiming is not allowed in this world.", COLOR_RED)));
            case ORBISGUARD_PROTECTED -> ctx.sendMessage(prefix().insert(msg("This area is protected by OrbisGuard.", COLOR_RED)));
            case ZONE_PROTECTED -> ctx.sendMessage(prefix().insert(msg("This chunk is in a safezone or warzone.", COLOR_RED)));
            default -> ctx.sendMessage(prefix().insert(msg("Failed to claim chunk.", COLOR_RED)));
        }
    }
}
