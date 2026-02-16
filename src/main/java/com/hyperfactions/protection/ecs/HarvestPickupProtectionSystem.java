package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * ECS system for protecting interactive/manual item pickups.
 *
 * This handles the InteractivelyPickupItemEvent which fires when players manually
 * interact to pick up items (F-key on rubble, harvesting crops, etc.).
 *
 * This checks the ITEM_PICKUP_MANUAL zone flag, not ITEM_PICKUP (auto pickup).
 * SafeZones typically allow auto pickup but block manual F-key pickup.
 */
public class HarvestPickupProtectionSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

    private final HyperFactions hyperFactions;
    private final ProtectionListener protectionListener;

    public HarvestPickupProtectionSystem(@NotNull HyperFactions hyperFactions,
                                          @NotNull ProtectionListener protectionListener) {
        super(InteractivelyPickupItemEvent.class);
        this.hyperFactions = hyperFactions;
        this.protectionListener = protectionListener;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // Match OrbisGuard's pattern - query for entities with PlayerRef component
        return PlayerRef.getComponentType();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Run first to ensure we can cancel before other systems process
        return Collections.singleton(RootDependency.first());
    }

    @Override
    public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                       InteractivelyPickupItemEvent event) {
        // Skip if already cancelled by another system
        if (event.isCancelled()) {
            return;
        }

        try {
            // Get entity reference first (matching OrbisGuard's pattern)
            Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);

            // Get both Player and PlayerRef components from the store
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (player == null || playerRef == null) {
                return;
            }

            // Check bypass permissions FIRST (before any protection logic)
            if (hyperFactions.isAdminBypassEnabled(playerRef.getUuid())) {
                return;
            }
            if (player.hasPermission("hyperfactions.bypass.pickup")) {
                return;
            }

            // Get player position for protection check
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null || transform.getPosition() == null) {
                return;
            }

            int x = (int) transform.getPosition().getX();
            int y = (int) transform.getPosition().getY();
            int z = (int) transform.getPosition().getZ();

            String worldName = getWorldName(store);
            if (worldName == null) {
                return;
            }

            Logger.debugInteraction("HarvestPickup: player=%s, world=%s, pos=(%d,%d,%d)",
                playerRef.getUuid(), worldName, x, y, z);

            // 1. First check zone flags (ITEM_PICKUP_MANUAL for interactive/F-key pickup)
            ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
            boolean zoneAllows = zoneProtection.isManualPickupAllowed(worldName, x, z);

            if (!zoneAllows) {
                event.setCancelled(true);
                event.setItemStack(ItemStack.EMPTY);  // Also clear the item stack
                player.sendMessage(MessageUtil.errorText("You cannot pick up items manually in this zone."));
                Logger.debugProtection("Manual pickup blocked by zone (ITEM_PICKUP_MANUAL=false) at %s/%d/%d for player %s",
                    worldName, x, z, playerRef.getUuid());
                return;
            }

            // 2. Check faction permissions
            boolean blocked = protectionListener.onItemPickup(
                playerRef.getUuid(),
                worldName,
                x, y, z
            );

            if (blocked) {
                event.setCancelled(true);
                event.setItemStack(ItemStack.EMPTY);  // Also clear the item stack
                ProtectionChecker.ProtectionResult result = hyperFactions.getProtectionChecker().canInteract(
                    playerRef.getUuid(), worldName, x, z,
                    ProtectionChecker.InteractionType.INTERACT
                );
                player.sendMessage(Message.raw(protectionListener.getDenialMessage(result)).color("#FF5555"));
                Logger.debugProtection("Harvest pickup blocked by faction protection at %s/%d/%d for player %s: %s",
                    worldName, x, z, playerRef.getUuid(), result);
            }
        } catch (Exception e) {
            Logger.severe("Error processing harvest pickup event", e);
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
