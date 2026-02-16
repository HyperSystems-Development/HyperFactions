package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling item drop protection.
 * Checks zone flags to allow/block item dropping.
 */
public class ItemDropProtectionSystem extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    private final HyperFactions hyperFactions;

    public ItemDropProtectionSystem(@NotNull HyperFactions hyperFactions) {
        super(DropItemEvent.PlayerRequest.class);
        this.hyperFactions = hyperFactions;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       DropItemEvent.PlayerRequest event) {
        try {
            PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
            if (player == null) return;

            // Check admin bypass first
            if (hyperFactions.isAdminBypassEnabled(player.getUuid())) {
                return;
            }

            TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
            if (transform == null) return;

            String worldName = getWorldName(store);
            if (worldName == null) return;

            Vector3d position = transform.getPosition();
            double x = position.getX();
            double z = position.getZ();

            // Check zone flag for item drop
            ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
            boolean zoneAllows = zoneProtection.isItemDropAllowed(worldName, x, z);

            if (!zoneAllows) {
                event.setCancelled(true);
                player.sendMessage(MessageUtil.errorText("You cannot drop items in this zone."));
            }
        } catch (Exception e) {
            Logger.severe("Error processing item drop event", e);
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
