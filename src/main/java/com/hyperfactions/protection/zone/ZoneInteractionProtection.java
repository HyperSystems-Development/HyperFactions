package com.hyperfactions.protection.zone;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.util.ChunkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for zone-based interaction protection checks.
 * Provides methods for checking various interaction types against zone flags.
 *
 * <p>Interaction types:
 * - Doors/Gates: Uses DOOR_USE flag
 * - Containers (chests, barrels): Uses CONTAINER_USE flag
 * - Benches (crafting tables): Uses BENCH_USE flag
 * - Processing (furnaces, smelters): Uses PROCESSING_USE flag
 * - Seats: Uses SEAT_USE flag
 * - Item drop: Uses ITEM_DROP flag
 * - Item pickup: Uses ITEM_PICKUP flag
 */
public class ZoneInteractionProtection {

  private final HyperFactions hyperFactions;

  /** Creates a new ZoneInteractionProtection. */
  public ZoneInteractionProtection(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Checks if an interaction is allowed at a location based on zone flags.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @param flagName  the zone flag to check
   * @return true if allowed (no zone or zone allows it), false if blocked
   */
  public boolean isInteractionAllowed(@NotNull String worldName, double x, double z, @NotNull String flagName) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
    if (zone == null) {
      // Not in a zone - interaction allowed (use claim rules instead)
      return true;
    }

    return zone.getEffectiveFlag(flagName);
  }

  /**
   * Checks if door use is allowed at a location.
   */
  public boolean isDoorUseAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.DOOR_USE);
  }

  /**
   * Checks if container use (chests, barrels) is allowed at a location.
   */
  public boolean isContainerUseAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.CONTAINER_USE);
  }

  /**
   * Checks if bench use (crafting tables) is allowed at a location.
   */
  public boolean isBenchUseAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.BENCH_USE);
  }

  /**
   * Checks if processing use (furnaces, smelters) is allowed at a location.
   */
  public boolean isProcessingUseAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.PROCESSING_USE);
  }

  /**
   * Checks if seat use is allowed at a location.
   */
  public boolean isSeatUseAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.SEAT_USE);
  }

  /**
   * Checks if general block interaction is allowed at a location.
   * Used as a fallback for unrecognized block types.
   */
  public boolean isBlockInteractAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.BLOCK_INTERACT);
  }

  /**
   * Checks if item drop is allowed at a location.
   */
  public boolean isItemDropAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.ITEM_DROP);
  }

  /**
   * Checks if automatic item pickup (walking over items) is allowed at a location.
   */
  public boolean isItemPickupAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.ITEM_PICKUP);
  }

  /**
   * Checks if manual item pickup (F-key / InteractivelyPickupItemEvent) is allowed at a location.
   * This is separate from auto pickup because SafeZones may allow auto pickup but block F-key pickup.
   */
  public boolean isManualPickupAllowed(@NotNull String worldName, double x, double z) {
    return isInteractionAllowed(worldName, x, z, ZoneFlags.ITEM_PICKUP_MANUAL);
  }

  /**
   * Determines the interaction type from a block's state ID and checks if it's allowed.
   * Uses Hytale's block state system (e.g., "container", "Door", "processingBench")
   * instead of block IDs, so it works with any mod-added blocks.
   *
   * @param stateId   the block state ID from BlockType.getState().getId()
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if the interaction is allowed
   */
  public boolean isBlockInteractionAllowed(@Nullable String stateId, @NotNull String worldName, double x, double z) {
    if (stateId == null) {
      return isBlockInteractAllowed(worldName, x, z);
    }

    InteractionBlockType type = detectBlockTypeFromState(stateId);

    return switch (type) {
      case DOOR -> isDoorUseAllowed(worldName, x, z);
      case CONTAINER -> isContainerUseAllowed(worldName, x, z);
      case BENCH -> isBenchUseAllowed(worldName, x, z);
      case PROCESSING -> isProcessingUseAllowed(worldName, x, z);
      case SEAT -> isSeatUseAllowed(worldName, x, z);
      case OTHER -> isBlockInteractAllowed(worldName, x, z);
    };
  }

  /**
   * Detects the interaction type from a block's state ID.
   * Uses Hytale's native state types:
   * - "Door" for doors/gates
   * - "container" for storage containers
   * - "processingBench" for furnaces/smelters
   * - "bench" or similar for crafting benches
   * - "seat" for sittable blocks
   *
   * @param stateId the state ID (case-insensitive)
   * @return the detected interaction type
   */
  public InteractionBlockType detectBlockTypeFromState(@NotNull String stateId) {
    String lower = stateId.toLowerCase();

    // Door state
    if (lower.equals("door") || lower.contains("door") || lower.contains("gate")) {
      return InteractionBlockType.DOOR;
    }

    // Processing bench (furnaces, smelters, etc.)
    if (lower.equals("processingbench") || lower.contains("processing")) {
      return InteractionBlockType.PROCESSING;
    }

    // Container state (chests, storage)
    if (lower.equals("container") || lower.contains("container") || lower.contains("storage")) {
      return InteractionBlockType.CONTAINER;
    }

    // Bench state (crafting tables, workbenches)
    if (lower.equals("bench") || lower.contains("bench") || lower.contains("crafting")) {
      return InteractionBlockType.BENCH;
    }

    // Seat state
    if (lower.equals("seat") || lower.contains("seat") || lower.contains("sittable")) {
      return InteractionBlockType.SEAT;
    }

    return InteractionBlockType.OTHER;
  }

  /**
   * Gets the zone at a location.
   */
  @Nullable
  public Zone getZoneAt(@NotNull String worldName, double x, double z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    return hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
  }

  /**
   * Block interaction types for zone flag checks.
   */
  public enum InteractionBlockType {
    DOOR,
    CONTAINER,
    BENCH,
    PROCESSING,
    SEAT,
    OTHER
  }
}
