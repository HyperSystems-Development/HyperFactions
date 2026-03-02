package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;

/**
 * ECS system for canceling teleports when players take damage.
 */
public class TeleportCancelOnDamageSystem extends EntityEventSystem<EntityStore, Damage> {
  private final HyperFactions hyperFactions;

  /** Creates a new TeleportCancelOnDamageSystem. */
  public TeleportCancelOnDamageSystem(HyperFactions hyperFactions) {
    super(Damage.class);
    this.hyperFactions = hyperFactions;
  }

  /** Returns the query. */
  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }

  /** Handles . */
  @Override
  public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
           Damage event) {
    try {
      // Only process if damage was not cancelled
      if (event.isCancelled()) {
        return;
      }

      // Get the player being damaged
      PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (player == null) {
        return;
      }

      UUID playerUuid = player.getUuid();

      // Check if player has a pending teleport and cancel it if configured
      hyperFactions.getTeleportManager().cancelOnDamage(
        playerUuid,
        player::sendMessage
      );
    } catch (Exception e) {
      ErrorHandler.report("Error processing damage event for teleport cancellation", e);
    }
  }
}
