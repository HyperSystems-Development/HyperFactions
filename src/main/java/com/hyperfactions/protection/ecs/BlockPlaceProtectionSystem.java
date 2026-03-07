package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling block place protection.
 * Checks both zone flags and faction permissions.
 */
public class BlockPlaceProtectionSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

  private final HyperFactions hyperFactions;

  private final ProtectionListener protectionListener;

  /** Creates a new BlockPlaceProtectionSystem. */
  public BlockPlaceProtectionSystem(@NotNull HyperFactions hyperFactions,
                   @NotNull ProtectionListener protectionListener) {
    super(PlaceBlockEvent.class);
    this.hyperFactions = hyperFactions;
    this.protectionListener = protectionListener;
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
           PlaceBlockEvent event) {
    try {
      PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (player == null) { // Non-player entity — allow
        return;
      }

      Vector3i pos = event.getTargetBlock();
      String worldName = getWorldName(store);

      Logger.debugInteraction("[ECS:PlaceBlock] player=%s, world=%s, pos=(%d,%d,%d), alreadyCancelled=%b",
        player.getUuid(), worldName, pos.getX(), pos.getY(), pos.getZ(), event.isCancelled());

      if (worldName == null) {
        // Fail-closed: can't determine world, deny to be safe
        event.setCancelled(true);
        Logger.warn("Block place cancelled: could not determine world name for player %s", player.getUuid());
        return;
      }

      // Evaluate protection once and cache result
      ProtectionChecker.ProtectionResult result = hyperFactions.getProtectionChecker().canInteract(
        player.getUuid(), worldName, pos.getX(), pos.getZ(),
        ProtectionChecker.InteractionType.BUILD
      );

      boolean blocked = !hyperFactions.getProtectionChecker().isAllowed(result);

      Logger.debugInteraction("[ECS:PlaceBlock] player=%s, result=%s, blocked=%b, pos=(%d,%d,%d), world=%s",
        player.getUuid(), result, blocked, pos.getX(), pos.getY(), pos.getZ(), worldName);

      if (blocked) {
        event.setCancelled(true);
        ProtectionMessageDebounce.sendIfNotOnCooldown(player, "block_place",
          Message.raw(protectionListener.getDenialMessage(result, ProtectionChecker.InteractionType.BUILD)).color("#FF5555"));

        // Anti-pillar: teleport player to their current position with velocity reset.
        // This prevents exploiting client-side block prediction to gain height on ghost blocks.
        // The teleport resets velocity so any upward momentum from jumping is killed.
        applyPositionCorrection(entityIndex, chunk, store, commandBuffer, player);
      }
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized block placement
      event.setCancelled(true);
      Logger.severe("Block place cancelled due to protection error (fail-closed)", e);
    }
  }

  /**
   * Teleports the player to their current position with velocity reset.
   * Prevents the ghost-block pillar exploit where the client predicts a placed block,
   * the player jumps on it, and gains height before the server correction arrives.
   */
  private void applyPositionCorrection(int entityIndex, ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
                     PlayerRef player) {
    try {
      Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null || transform.getPosition() == null) {
        return;
      }

      Vector3d currentPos = transform.getPosition();

      // Use head rotation (pitch+yaw) so the player's view direction is preserved.
      // TransformComponent.getRotation() is body-only (yaw, no pitch).
      HeadRotation headRot = store.getComponent(ref, HeadRotation.getComponentType());
      Vector3f lookRot = (headRot != null) ? headRot.getRotation() : transform.getRotation();
      if (lookRot == null) {
        lookRot = new Vector3f(0, 0, 0);
      }

      Teleport teleport = Teleport.createForPlayer(currentPos, lookRot);
      commandBuffer.addComponent(ref, Teleport.getComponentType(), teleport);

      Logger.debugProtection("[ECS:PlaceBlock] Anti-pillar correction for %s at (%.1f,%.1f,%.1f)",
        player.getUuid(), currentPos.getX(), currentPos.getY(), currentPos.getZ());
    } catch (Exception e) {
      Logger.debugProtection("[ECS:PlaceBlock] Failed to apply anti-pillar correction: %s", e.getMessage());
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
