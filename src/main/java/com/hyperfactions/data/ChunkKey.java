package com.hyperfactions.data;

import org.jetbrains.annotations.NotNull;

/**
 * Immutable key for identifying chunks in maps.
 * Used for O(1) chunk lookups.
 *
 * <p>Note: Hytale uses 32-block chunks (shift by 5), not 16-block chunks.
 *
 * @param world  the world name
 * @param chunkX the chunk X coordinate
 * @param chunkZ the chunk Z coordinate
 */
public record ChunkKey(
  @NotNull String world,
  int chunkX,
  int chunkZ
) {
  /** Hytale chunk size in blocks. */
  private static final int CHUNK_SIZE = 32;

  /** Bit shift for chunk calculations (log2 of CHUNK_SIZE). */
  private static final int CHUNK_SHIFT = 5;

  /**
   * Creates a ChunkKey from world coordinates (not chunk coordinates).
   *
   * @param world the world name
   * @param x     the world X coordinate
   * @param z     the world Z coordinate
   * @return a ChunkKey for the chunk containing these coordinates
   */
  public static ChunkKey fromWorldCoords(@NotNull String world, double x, double z) {
    return new ChunkKey(world, (int) Math.floor(x) >> CHUNK_SHIFT, (int) Math.floor(z) >> CHUNK_SHIFT);
  }

  /**
   * Creates a ChunkKey from block coordinates.
   *
   * @param world  the world name
   * @param blockX the block X coordinate
   * @param blockZ the block Z coordinate
   * @return a ChunkKey for the chunk containing these coordinates
   */
  public static ChunkKey fromBlockCoords(@NotNull String world, int blockX, int blockZ) {
    return new ChunkKey(world, blockX >> CHUNK_SHIFT, blockZ >> CHUNK_SHIFT);
  }

  /**
   * Gets the minimum block X coordinate in this chunk.
   *
   * @return the minimum X
   */
  public int getMinBlockX() {
    return chunkX << CHUNK_SHIFT;
  }

  /**
   * Gets the maximum block X coordinate in this chunk.
   *
   * @return the maximum X
   */
  public int getMaxBlockX() {
    return (chunkX << CHUNK_SHIFT) + (CHUNK_SIZE - 1);
  }

  /**
   * Gets the minimum block Z coordinate in this chunk.
   *
   * @return the minimum Z
   */
  public int getMinBlockZ() {
    return chunkZ << CHUNK_SHIFT;
  }

  /**
   * Gets the maximum block Z coordinate in this chunk.
   *
   * @return the maximum Z
   */
  public int getMaxBlockZ() {
    return (chunkZ << CHUNK_SHIFT) + (CHUNK_SIZE - 1);
  }

  /**
   * Gets the center X coordinate of this chunk.
   *
   * @return the center X
   */
  public double getCenterX() {
    return (chunkX << CHUNK_SHIFT) + (CHUNK_SIZE / 2.0);
  }

  /**
   * Gets the center Z coordinate of this chunk.
   *
   * @return the center Z
   */
  public double getCenterZ() {
    return (chunkZ << CHUNK_SHIFT) + (CHUNK_SIZE / 2.0);
  }

  /**
   * Checks if this chunk is adjacent to another chunk.
   *
   * @param other the other chunk
   * @return true if adjacent (sharing an edge)
   */
  public boolean isAdjacentTo(@NotNull ChunkKey other) {
    if (!world.equals(other.world)) {
      return false;
    }
    int dx = Math.abs(chunkX - other.chunkX);
    int dz = Math.abs(chunkZ - other.chunkZ);
    return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
  }

  /**
   * Gets the chunk key to the north.
   *
   * @return the north chunk key
   */
  @NotNull
  public ChunkKey north() {
    return new ChunkKey(world, chunkX, chunkZ - 1);
  }

  /**
   * Gets the chunk key to the south.
   *
   * @return the south chunk key
   */
  @NotNull
  public ChunkKey south() {
    return new ChunkKey(world, chunkX, chunkZ + 1);
  }

  /**
   * Gets the chunk key to the east.
   *
   * @return the east chunk key
   */
  @NotNull
  public ChunkKey east() {
    return new ChunkKey(world, chunkX + 1, chunkZ);
  }

  /**
   * Gets the chunk key to the west.
   *
   * @return the west chunk key
   */
  @NotNull
  public ChunkKey west() {
    return new ChunkKey(world, chunkX - 1, chunkZ);
  }
}
