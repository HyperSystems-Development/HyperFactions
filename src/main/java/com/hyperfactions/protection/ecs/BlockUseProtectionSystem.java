package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionListener;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.protection.zone.ZoneInteractionProtection;
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
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * ECS system for handling block use/interact protection.
 * Checks zone flags first, then faction permissions.
 */
public class BlockUseProtectionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

  private final HyperFactions hyperFactions;

  private final ProtectionListener protectionListener;

  /** Creates a new BlockUseProtectionSystem. */
  public BlockUseProtectionSystem(@NotNull HyperFactions hyperFactions,
                  @NotNull ProtectionListener protectionListener) {
    super(UseBlockEvent.Pre.class);
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
           UseBlockEvent.Pre event) {
    try {
      PlayerRef player = chunk.getComponent(entityIndex, PlayerRef.getComponentType());
      if (player == null) {
        Logger.debugInteraction("UseBlockEvent.Pre: No PlayerRef component, skipping");
        return;
      }

      Vector3i pos = event.getTargetBlock();
      String worldName = getWorldName(store);
      if (worldName == null) {
        // Fail-closed: can't determine world, deny to be safe
        event.setCancelled(true);
        Logger.warn("Block use cancelled: could not determine world name for player %s", player.getUuid());
        return;
      }

      // Debug log the interaction details
      BlockType blockType = event.getBlockType();
      String blockId = blockType != null ? blockType.getId() : "null";

      // Get block state ID for zone flag checking (uses Hytale's state system)
      String stateId = getBlockStateId(blockType);

      Logger.debugInteraction("UseBlockEvent.Pre: player=%s, world=%s, pos=(%d,%d,%d), blockId=%s, stateId=%s, cancelled=%s",
        player.getUuid(), worldName, pos.getX(), pos.getY(), pos.getZ(),
        blockId, stateId, event.isCancelled());

      // Check bypass permissions first
      if (hyperFactions.isAdminBypassEnabled(player.getUuid())) {
        return;
      }

      // Gravestone block — bypass ALL normal protection when integration is active
      // Access control is handled by our registered GravestoneAccessChecker in the gravestone plugin
      if (isGravestoneBlock(blockId)) {
        var gsIntegration = hyperFactions.getProtectionChecker().getGravestoneIntegration();
        if (gsIntegration != null && gsIntegration.isAvailable()) {
          Logger.debugIntegration("Gravestone interaction bypassed normal protection for %s at (%d,%d,%d)",
              player.getUuid(), pos.getX(), pos.getY(), pos.getZ());
          return;  // Let gravestone plugin handle via AccessChecker
        }

        // No integration — fall through to normal protection
      }

      ZoneInteractionProtection zoneProtection = hyperFactions.getZoneInteractionProtection();

      // 1. Check if this is a crop/plant block (berry, etc.) - uses ITEM_PICKUP_MANUAL flag
      //    Crop harvesting is conceptually the same as F-key pickup (manual item acquisition)
      if (isCropBlock(blockId)) {
        boolean cropHarvestAllowed = zoneProtection.isManualPickupAllowed(worldName, pos.getX(), pos.getZ());
        if (!cropHarvestAllowed) {
          event.setCancelled(true);
          ProtectionMessageDebounce.sendDenial(player, "zone_harvest", "You can't harvest plants in this zone.");
          Logger.debugProtection("Plant harvest blocked by zone (ITEM_PICKUP_MANUAL=false) at %s/%d/%d for player %s",
            worldName, pos.getX(), pos.getZ(), player.getUuid());
          return;
        }

        // If crop harvest allowed by zone, still check faction permissions
        ProtectionChecker.ProtectionResult cropResult = hyperFactions.getProtectionChecker().canInteract(
          player.getUuid(), worldName, pos.getX(), pos.getZ(),
          ProtectionChecker.InteractionType.INTERACT
        );
        if (!hyperFactions.getProtectionChecker().isAllowed(cropResult)) {
          event.setCancelled(true);
          ProtectionMessageDebounce.sendDenial(player, "block_interact",
            protectionListener.getDenialMessage(cropResult, ProtectionChecker.InteractionType.INTERACT));
        }
        return;
      }

      // 2. For non-crop blocks, check zone flags based on block state
      boolean zoneAllows = zoneProtection.isBlockInteractionAllowed(stateId, worldName, pos.getX(), pos.getZ());

      if (!zoneAllows) {
        event.setCancelled(true);
        ZoneInteractionProtection.InteractionBlockType detectedType =
          zoneProtection.detectBlockTypeFromState(stateId != null ? stateId : "");
        String flagName = switch (detectedType) {
          case DOOR -> "door use";
          case CONTAINER -> "container use";
          case BENCH -> "bench use";
          case PROCESSING -> "processing use";
          case SEAT -> "seat use";
          case OTHER -> "block interaction";
        };
        ProtectionMessageDebounce.sendDenial(player, "zone_use", "You can't use " + flagName + " in this zone.");
        return;
      }

      // 2. If zone allows (or not in zone), check faction permissions
      // Map block type to specific InteractionType for fine-grained faction permission checks
      ZoneInteractionProtection.InteractionBlockType detectedBlockType =
        zoneProtection.detectBlockTypeFromState(stateId != null ? stateId : "");
      ProtectionChecker.InteractionType interactionType = switch (detectedBlockType) {
        case DOOR -> ProtectionChecker.InteractionType.DOOR;
        case CONTAINER -> ProtectionChecker.InteractionType.CONTAINER;
        case BENCH -> ProtectionChecker.InteractionType.BENCH;
        case PROCESSING -> ProtectionChecker.InteractionType.PROCESSING;
        case SEAT -> ProtectionChecker.InteractionType.SEAT;
        case OTHER -> ProtectionChecker.InteractionType.INTERACT;
      };

      ProtectionChecker.ProtectionResult factionResult = hyperFactions.getProtectionChecker().canInteract(
        player.getUuid(), worldName, pos.getX(), pos.getZ(), interactionType
      );

      boolean blocked = !hyperFactions.getProtectionChecker().isAllowed(factionResult);

      Logger.debugInteraction("[ECS:UseBlock] player=%s, result=%s, blocked=%b, interactionType=%s, blockId=%s, pos=(%d,%d,%d), world=%s",
        player.getUuid(), factionResult, blocked, interactionType, blockId, pos.getX(), pos.getY(), pos.getZ(), worldName);

      if (blocked) {
        event.setCancelled(true);
        ProtectionMessageDebounce.sendDenial(player, "block_use",
          protectionListener.getDenialMessage(factionResult, interactionType));
      }
    } catch (Exception e) {
      // Fail-closed: cancel on any exception to prevent unauthorized block interaction
      event.setCancelled(true);
      ErrorHandler.report("Block use cancelled due to protection error (fail-closed)", e);
    }
  }

  /**
   * Checks if a block ID indicates a gravestone block from GravestonePlugin.
   * Matches both standard and vanilla gravestone block IDs.
   */
  private boolean isGravestoneBlock(String blockId) {
    if (blockId == null) {
      return false;
    }
    return blockId.contains("Gravestone");
  }

  /**
   * Gets the state ID from a block type (e.g., "container", "Door", "processingBench").
   * Uses Hytale's native state system which works for both vanilla and modded blocks.
   */
  private String getBlockStateId(BlockType blockType) {
    if (blockType == null) {
      return null;
    }
    try {
      var state = blockType.getState();
      if (state != null) {
        return state.getId();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Checks if a state ID indicates a door/gate block.
   */
  private boolean isDoorState(String stateId) {
    if (stateId == null) {
      return false;
    }
    return "Door".equalsIgnoreCase(stateId);
  }

  /**
   * Checks if a block ID indicates a harvestable plant/crop/flower block.
   * These blocks use the ITEM_PICKUP_MANUAL flag since harvesting is conceptually
   * the same as F-key item pickup (manual item acquisition from the world).
   *
   * <p>Examples:
   * - *Plant_Crop_Berry_Block_State_Definitions_StageFinal (berry bush)
   * - *Plant_Crop_* (any crop at harvestable stage)
   * - *Plant_Flower_* (flowers like dandelions, roses, etc.)
   * - *Plant_Mushroom_* (mushrooms)
   * - *Plant_* (generic plant blocks)
   */
  private boolean isCropBlock(String blockId) {
    if (blockId == null) {
      return false;
    }
    String lower = blockId.toLowerCase();
    return lower.contains("plant_crop") || lower.contains("crop_")
      || lower.contains("plant_flower") || lower.contains("flower_")
      || lower.contains("plant_mushroom") || lower.contains("mushroom_")
      || lower.contains("plant_berry") || lower.contains("berry_bush")
      || lower.contains("plant_tall") || lower.contains("plant_fern")
      || lower.contains("plant_grass") || lower.contains("plant_vine")
      || lower.contains("plant_seagrass") || lower.contains("plant_kelp")
      || lower.contains("plant_cactus") || lower.contains("plant_sugar");
  }

  private String getWorldName(Store<EntityStore> store) {
    try {
      return store.getExternalData().getWorld().getName();
    } catch (Exception e) {
      return null;
    }
  }
}
