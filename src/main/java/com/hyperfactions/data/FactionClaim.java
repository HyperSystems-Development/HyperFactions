package com.hyperfactions.data;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a chunk claimed by a faction.
 *
 * @param world     the world name
 * @param chunkX    the chunk X coordinate
 * @param chunkZ    the chunk Z coordinate
 * @param claimedAt when the chunk was claimed (epoch millis)
 * @param claimedBy UUID of the player who claimed it
 */
public record FactionClaim(
  @NotNull String world,
  int chunkX,
  int chunkZ,
  long claimedAt,
  @NotNull UUID claimedBy
) {
  /**
   * Creates a new claim at the current time.
   *
   * @param world     the world name
   * @param chunkX    the chunk X coordinate
   * @param chunkZ    the chunk Z coordinate
   * @param claimedBy UUID of the player claiming
   * @return a new FactionClaim
   */
  public static FactionClaim create(@NotNull String world, int chunkX, int chunkZ, @NotNull UUID claimedBy) {
    return new FactionClaim(world, chunkX, chunkZ, System.currentTimeMillis(), claimedBy);
  }

  /**
   * Gets the ChunkKey for this claim.
   *
   * @return the chunk key
   */
  @NotNull
  public ChunkKey toChunkKey() {
    return new ChunkKey(world, chunkX, chunkZ);
  }

  /**
   * Checks if this claim is adjacent to another claim.
   *
   * @param other the other claim
   * @return true if adjacent (sharing an edge, not diagonal)
   */
  public boolean isAdjacentTo(@NotNull FactionClaim other) {
    if (!world.equals(other.world)) {
      return false;
    }
    int dx = Math.abs(chunkX - other.chunkX);
    int dz = Math.abs(chunkZ - other.chunkZ);
    return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
  }

  /**
   * Checks if this claim is at the given chunk coordinates.
   *
   * @param worldName the world name
   * @param x         the chunk X
   * @param z         the chunk Z
   * @return true if matching
   */
  public boolean isAt(@NotNull String worldName, int x, int z) {
    return world.equals(worldName) && chunkX == x && chunkZ == z;
  }
}
