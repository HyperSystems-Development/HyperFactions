package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling interactive/manual item pickup protection.
 *
 * <p>This handles InteractivelyPickupItemEvent (F-key/manual pickup).
 * Automatic pickup (walking near items) is handled internally by the engine.
 *
 * <p>This checks the ITEM_PICKUP_MANUAL zone flag, not ITEM_PICKUP (auto pickup).
 * SafeZones typically allow auto pickup but block manual F-key pickup.
 */
public class ItemPickupProtectionSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

  private final HyperFactions hyperFactions;

  private final ProtectionListener protectionListener;

  /** Creates a new ItemPickupProtectionSystem. */
  public ItemPickupProtectionSystem(@NotNull HyperFactions hyperFactions,
                   @NotNull ProtectionListener protectionListener) {
    super(InteractivelyPickupItemEvent.class);
    this.hyperFactions = hyperFactions;
    this.protectionListener = protectionListener;
  }

  /** Returns the query. */
  @Override
  public Query<EntityStore> getQuery() {
    // Must query for PlayerRef to receive events for player entities
    return PlayerRef.getComponentType();
  }

  /** Handles . */
  @Override
  public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
           InteractivelyPickupItemEvent event) {
    try {
      PlayerRef playerRef = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (playerRef == null) {
        return;
      }

      // Get world name and position from PlayerRef (like Hyfaction does)
      Player playerComponent = store.getComponent(chunk.getReferenceTo(entityIndex), Player.getComponentType());
      if (playerComponent == null) {
        return;
      }

      String worldName = playerComponent.getWorld().getName();
      var transform = playerRef.getTransform();
      if (transform == null) {
        return;
      }
      var position = transform.getPosition();
      int x = (int) Math.floor(position.getX());
      int y = (int) Math.floor(position.getY());
      int z = (int) Math.floor(position.getZ());

      Logger.debugInteraction("[ECS:ItemPickup] player=%s, world=%s, pos=(%d,%d,%d), alreadyCancelled=%b",
        playerRef.getUuid(), worldName, x, y, z, event.isCancelled());

      // 1. First check zone flags (ITEM_PICKUP_MANUAL for interactive/F-key pickup)
      ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
      boolean zoneAllows = zoneProtection.isManualPickupAllowed(worldName, x, z);

      if (!zoneAllows) {
        event.setCancelled(true);
        Logger.debugInteraction("[ECS:ItemPickup] BLOCKED by zone (ITEM_PICKUP_MANUAL=false) at %s/%d/%d for player %s",
          worldName, x, z, playerRef.getUuid());
        ProtectionMessageDebounce.sendDenial(playerRef, "item_pickup",
          "You can't pick up items in this zone.");
        return;
      }

      // 2. If zone allows (or not in zone), check faction permissions
      ProtectionChecker.ProtectionResult result = protectionListener.checkItemPickup(
        playerRef.getUuid(),
        worldName,
        x, y, z
      );
      ProtectionChecker checker = hyperFactions.getProtectionChecker();
      boolean blocked = !checker.isAllowed(result);

      Logger.debugInteraction("[ECS:ItemPickup] player=%s, blocked=%b, pos=(%d,%d,%d), world=%s",
        playerRef.getUuid(), blocked, x, y, z, worldName);

      if (blocked) {
        event.setCancelled(true);
        ProtectionMessageDebounce.sendDenial(playerRef, "item_pickup",
          protectionListener.getDenialMessage(result, ProtectionChecker.InteractionType.ITEM_PICKUP));
      }
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized item pickup
      event.setCancelled(true);
      ErrorHandler.report("Item pickup cancelled due to protection error (fail-closed)", e);
    }
  }
}
