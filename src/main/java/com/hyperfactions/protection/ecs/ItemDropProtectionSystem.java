package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.MessageUtil;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling item drop protection.
 * Checks zone flags to allow/block item dropping.
 */
public class ItemDropProtectionSystem extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

  private final HyperFactions hyperFactions;

  /** Creates a new ItemDropProtectionSystem. */
  public ItemDropProtectionSystem(@NotNull HyperFactions hyperFactions) {
    super(DropItemEvent.PlayerRequest.class);
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
           DropItemEvent.PlayerRequest event) {
    try {
      PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (player == null) {
        return;
      }

      // Check admin bypass first
      if (hyperFactions.isAdminBypassEnabled(player.getUuid())) {
        Logger.debugInteraction("[ECS:ItemDrop] BYPASS admin for player=%s", player.getUuid());
        return;
      }

      TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
      if (transform == null) {
        return;
      }

      String worldName = getWorldName(store);
      if (worldName == null) {
        // Fail-closed: can't determine world, deny to be safe
        event.setCancelled(true);
        Logger.warn("Item drop cancelled: could not determine world name for player %s", player.getUuid());
        return;
      }

      Vector3d position = transform.getPosition();
      double x = position.getX();
      double z = position.getZ();

      Logger.debugInteraction("[ECS:ItemDrop] player=%s, world=%s, pos=(%.1f,%.1f), alreadyCancelled=%b",
        player.getUuid(), worldName, x, z, event.isCancelled());

      // Check zone flag for item drop
      ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();
      boolean zoneAllows = zoneProtection.isItemDropAllowed(worldName, x, z);

      if (!zoneAllows) {
        event.setCancelled(true);
        Logger.debugInteraction("[ECS:ItemDrop] BLOCKED by zone at %s/%.1f/%.1f for player %s",
          worldName, x, z, player.getUuid());
        player.sendMessage(MessageUtil.errorText("You cannot drop items in this zone."));
        return;
      }

      // Check claim-based outsider drop restriction
      if (!ConfigManager.get().isOutsiderDropAllowed()) {
        int chunkX = ChunkUtil.toChunkCoord(x);
        int chunkZ = ChunkUtil.toChunkCoord(z);
        UUID claimOwner = hyperFactions.getClaimManager().getClaimOwner(worldName, chunkX, chunkZ);
        if (claimOwner != null) {
          UUID playerFactionId = hyperFactions.getFactionManager().getPlayerFactionId(player.getUuid());
          if (playerFactionId == null || !playerFactionId.equals(claimOwner)) {
            // Check ally relation
            boolean isAlly = playerFactionId != null
                && hyperFactions.getRelationManager().getRelation(playerFactionId, claimOwner)
                    == com.hyperfactions.data.RelationType.ALLY;
            if (!isAlly) {
              event.setCancelled(true);
              player.sendMessage(MessageUtil.errorText("You cannot drop items in this territory."));
              return;
            }
          }
        }
      }
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized item drop
      event.setCancelled(true);
      ErrorHandler.report("Item drop cancelled due to protection error (fail-closed)", e);
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
