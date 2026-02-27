package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.damage.DamageProtectionHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling all damage protection.
 * Delegates to DamageProtectionHandler which coordinates individual protection systems.
 */
public class DamageProtectionSystem extends EntityEventSystem<EntityStore, Damage> {

  private final HyperFactions hyperFactions;

  /** Creates a new DamageProtectionSystem. */
  public DamageProtectionSystem(@NotNull HyperFactions hyperFactions,
                 @NotNull ProtectionListener protectionListener) {
    super(Damage.class);
    this.hyperFactions = hyperFactions;
  }

  /** Returns the query. */
  @Override
  public Query<EntityStore> getQuery() {
    return Archetype.empty();
  }

  /**
   * Return the FilterDamageGroup so this system runs BEFORE damage is applied.
   * Without this, setCancelled(true) has no effect because ApplyDamage runs first.
   */
  @Override
  public SystemGroup<EntityStore> getGroup() {
    return DamageModule.get().getFilterDamageGroup();
  }

  /** Handles . */
  @Override
  public void handle(int entityIndex, ArchetypeChunk<EntityStore> chunk,
           Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer,
           Damage event) {
    try {
      if (event.isCancelled()) {
        return;
      }

      // Only process player damage
      PlayerRef defender = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (defender == null) {
        return;
      }

      TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
      if (transform == null) {
        return;
      }

      String worldName = getWorldName(store);
      if (worldName == null) {
        return;
      }

      double x = transform.getPosition().getX();
      double z = transform.getPosition().getZ();

      Logger.debugInteraction("[ECS:Damage] defender=%s, world=%s, pos=(%.1f,%.1f), source=%s, alreadyCancelled=%b",
        defender.getUuid(), worldName, x, z, event.getClass().getSimpleName(), event.isCancelled());

      // Delegate to damage protection handler
      DamageProtectionHandler handler = hyperFactions.getDamageProtectionHandler();
      if (handler == null) {
        return;
      }

      handler.handleDamage(event, defender, worldName, x, z, commandBuffer);

      Logger.debugInteraction("[ECS:Damage] defender=%s, cancelledAfterHandler=%b",
        defender.getUuid(), event.isCancelled());
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized damage
      event.setCancelled(true);
      Logger.severe("Damage protection cancelled due to error (fail-closed)", e);
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
