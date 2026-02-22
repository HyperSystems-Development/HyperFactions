package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling block break protection.
 * Checks both zone flags and faction permissions.
 */
public class BlockBreakProtectionSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    private final HyperFactions hyperFactions;
    private final ProtectionListener protectionListener;

    public BlockBreakProtectionSystem(@NotNull HyperFactions hyperFactions,
                                       @NotNull ProtectionListener protectionListener) {
        super(BreakBlockEvent.class);
        this.hyperFactions = hyperFactions;
        this.protectionListener = protectionListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       BreakBlockEvent event) {
        try {
            PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null) return;

            Vector3i pos = event.getTargetBlock();
            String worldName = getWorldName(store);
            if (worldName == null) return;

            // Gravestone block — bypass ALL normal protection when integration is active
            // Access control is handled by our registered GravestoneAccessChecker in the gravestone plugin
            BlockType blockType = event.getBlockType();
            String blockId = blockType != null ? blockType.getId() : null;

            // Debug: log bed block interactions to diagnose #54
            if (blockId != null && blockId.toLowerCase().contains("bed")) {
                Logger.debugProtection("BreakBlockEvent for BED: player=%s, blockId=%s, pos=(%d,%d,%d), world=%s",
                    player.getUuid(), blockId, pos.getX(), pos.getY(), pos.getZ(), worldName);
            }
            if (blockId != null && blockId.contains("Gravestone")) {
                var gsIntegration = hyperFactions.getProtectionChecker().getGravestoneIntegration();
                if (gsIntegration != null && gsIntegration.isAvailable()) {
                    Logger.debugIntegration("Gravestone break bypassed normal protection for %s at (%d,%d,%d)",
                            player.getUuid(), pos.getX(), pos.getY(), pos.getZ());
                    return;  // Let gravestone plugin handle via AccessChecker
                }
                // No integration — fall through to normal build protection
            }

            boolean blocked = protectionListener.onBlockBreak(
                player.getUuid(),
                worldName,
                pos.getX(), pos.getY(), pos.getZ()
            );

            // Debug: log bed break protection result to diagnose #54
            if (blockId != null && blockId.toLowerCase().contains("bed")) {
                ProtectionChecker.ProtectionResult bedResult = hyperFactions.getProtectionChecker().canInteract(
                    player.getUuid(), worldName, pos.getX(), pos.getZ(),
                    ProtectionChecker.InteractionType.BUILD
                );
                Logger.debugProtection("BED break result: player=%s, blocked=%s, result=%s, blockId=%s",
                    player.getUuid(), blocked, bedResult, blockId);
            }

            if (blocked) {
                event.setCancelled(true);
                player.sendMessage(Message.raw(protectionListener.getDenialMessage(
                    hyperFactions.getProtectionChecker().canInteract(
                        player.getUuid(), worldName, pos.getX(), pos.getZ(),
                        ProtectionChecker.InteractionType.BUILD
                    )
                )).color("#FF5555"));
            }
        } catch (Exception e) {
            Logger.severe("Error processing block break event", e);
        }
    }

    private String getWorldName(Store<EntityStore> store) {
        try {
            return store.getExternalData().getWorld().getName();
        } catch (Exception e) {
            return null;
        }
    }
}
