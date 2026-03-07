package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ECS system that detects player respawn via DeathComponent removal.
 * Clears combat tag and applies spawn protection when a player respawns.
 *
 * <p>This follows Hytale's built-in pattern from {@code RespawnSystems.OnRespawnSystem}:
 * when a player respawns, the {@link DeathComponent} is removed from their entity.
 * This system listens for that removal and performs cleanup.</p>
 *
 * @see DeathComponent
 * @see com.hypixel.hytale.server.core.modules.entity.damage.RespawnSystems
 */
public class PlayerRespawnSystem extends RefChangeSystem<EntityStore, DeathComponent> {

  private final HyperFactions hyperFactions;

  /**
   * Creates a new PlayerRespawnSystem.
   *
   * @param hyperFactions the HyperFactions instance
   */
  public PlayerRespawnSystem(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Component Type. */
  @NotNull
  @Override
  public ComponentType<EntityStore, DeathComponent> componentType() {
    return DeathComponent.getComponentType();
  }

  /** Returns the query. */
  @NotNull
  @Override
  public Query<EntityStore> getQuery() {
    // Only process player entities
    return Player.getComponentType();
  }

  /** Called when component added. */
  @Override
  public void onComponentAdded(@NotNull Ref<EntityStore> ref,
                 @NotNull DeathComponent component,
                 @NotNull Store<EntityStore> store,
                 @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Death handling is done by PlayerDeathSystem
  }

  /** Called when component set. */
  @Override
  public void onComponentSet(@NotNull Ref<EntityStore> ref,
               @Nullable DeathComponent oldComponent,
               @NotNull DeathComponent newComponent,
               @NotNull Store<EntityStore> store,
               @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Not needed
  }

  /** Called when component removed. */
  @Override
  public void onComponentRemoved(@NotNull Ref<EntityStore> ref,
                  @NotNull DeathComponent component,
                  @NotNull Store<EntityStore> store,
                  @NotNull CommandBuffer<EntityStore> commandBuffer) {
    try {
      // Get the player reference from the entity
      PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
      if (playerRef == null) {
        return;
      }

      // Clear combat tag
      hyperFactions.getCombatTagManager().clearTag(playerRef.getUuid());

      // Apply spawn protection if enabled
      ConfigManager config = ConfigManager.get();
      if (config.isSpawnProtectionEnabled()) {
        // Get the player's current position (respawn location)
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
          Vector3d position = transform.getPosition();
          String worldName = store.getExternalData().getWorld().getName();
          int chunkX = ChunkUtil.toChunkCoord(position.getX());
          int chunkZ = ChunkUtil.toChunkCoord(position.getZ());
          int duration = config.getSpawnProtectionDurationSeconds();

          hyperFactions.getCombatTagManager().applySpawnProtection(
            playerRef.getUuid(), duration, worldName, chunkX, chunkZ
          );

          Logger.debugCombat("Player %s respawned, cleared combat tag and applied %ds spawn protection at chunk %d, %d",
            playerRef.getUuid(), duration, chunkX, chunkZ);
        } else {
          Logger.debugCombat("Player %s respawned, cleared combat tag (no spawn protection - missing transform)",
            playerRef.getUuid());
        }
      } else {
        Logger.debugCombat("Player %s respawned, cleared combat tag (spawn protection disabled)",
          playerRef.getUuid());
      }
    } catch (Exception e) {
      ErrorHandler.report("Error handling player respawn in ECS system", e);
    }
  }
}
