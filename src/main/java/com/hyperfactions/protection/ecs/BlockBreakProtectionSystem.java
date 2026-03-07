package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
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

  /** Creates a new BlockBreakProtectionSystem. */
  public BlockBreakProtectionSystem(@NotNull HyperFactions hyperFactions,
                   @NotNull ProtectionListener protectionListener) {
    super(BreakBlockEvent.class);
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
           BreakBlockEvent event) {
    try {
      PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (player == null) { // Non-player entity (NPC) — allow
        return;
      }

      Vector3i pos = event.getTargetBlock();
      String worldName = getWorldName(store);

      BlockType blockType = event.getBlockType();
      String blockId = blockType != null ? blockType.getId() : null;

      Logger.debugInteraction("[ECS:BreakBlock] player=%s, world=%s, pos=(%d,%d,%d), blockId=%s, alreadyCancelled=%b",
        player.getUuid(), worldName, pos.getX(), pos.getY(), pos.getZ(), blockId, event.isCancelled());

      if (worldName == null) {
        // Fail-closed: can't determine world, deny to be safe
        event.setCancelled(true);
        Logger.warn("Block break cancelled: could not determine world name for player %s", player.getUuid());
        return;
      }

      // Gravestone block — bypass ALL normal protection when integration is active
      // Access control is handled by our registered GravestoneAccessChecker in the gravestone plugin
      if (blockId != null && blockId.contains("Gravestone")) {
        var gsIntegration = hyperFactions.getProtectionChecker().getGravestoneIntegration();
        if (gsIntegration != null && gsIntegration.isAvailable()) {
          Logger.debugInteraction("[ECS:BreakBlock] BYPASS gravestone for %s at (%d,%d,%d)",
              player.getUuid(), pos.getX(), pos.getY(), pos.getZ());
          return;  // Let gravestone plugin handle via AccessChecker
        }

        // No integration — fall through to normal build protection
      }

      // Evaluate protection once and cache result
      ProtectionChecker.ProtectionResult result = hyperFactions.getProtectionChecker().canInteract(
        player.getUuid(), worldName, pos.getX(), pos.getZ(),
        ProtectionChecker.InteractionType.BUILD
      );

      boolean blocked = !hyperFactions.getProtectionChecker().isAllowed(result);

      Logger.debugInteraction("[ECS:BreakBlock] player=%s, result=%s, blocked=%b, pos=(%d,%d,%d), world=%s",
        player.getUuid(), result, blocked, pos.getX(), pos.getY(), pos.getZ(), worldName);

      if (blocked) {
        event.setCancelled(true);
        ProtectionMessageDebounce.sendDenial(player, "block_break",
          protectionListener.getDenialMessage(result, ProtectionChecker.InteractionType.BUILD));
      }
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized block breaks
      event.setCancelled(true);
      ErrorHandler.report("Block break cancelled due to protection error (fail-closed)", e);
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
