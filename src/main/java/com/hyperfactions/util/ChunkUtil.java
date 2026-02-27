package com.hyperfactions.util;

import com.hyperfactions.data.ChunkKey;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for chunk coordinate calculations.
 * Hytale uses 32-block chunks (shift by 5).
 */
public final class ChunkUtil {

  /** Hytale chunk size in blocks. */
  public static final int CHUNK_SIZE = 32;

  /** Bit shift for chunk calculations (log2 of CHUNK_SIZE). */
  private static final int CHUNK_SHIFT = 5;

  private ChunkUtil() {}

  /**
   * Converts a world coordinate to a chunk coordinate.
   *
   * @param coord the world coordinate
   * @return the chunk coordinate
   */
  public static int toChunkCoord(double coord) {
    return (int) Math.floor(coord) >> CHUNK_SHIFT;
  }

  /**
   * Converts a block coordinate to a chunk coordinate.
   *
   * @param blockCoord the block coordinate
   * @return the chunk coordinate
   */
  public static int blockToChunk(int blockCoord) {
    return blockCoord >> CHUNK_SHIFT;
  }

  /**
   * Gets the minimum block coordinate for a chunk.
   *
   * @param chunkCoord the chunk coordinate
   * @return the minimum block coordinate
   */
  public static int chunkToBlockMin(int chunkCoord) {
    return chunkCoord << CHUNK_SHIFT;
  }

  /**
   * Gets the maximum block coordinate for a chunk.
   *
   * @param chunkCoord the chunk coordinate
   * @return the maximum block coordinate
   */
  public static int chunkToBlockMax(int chunkCoord) {
    return (chunkCoord << CHUNK_SHIFT) + (CHUNK_SIZE - 1);
  }

  /**
   * Gets the center coordinate of a chunk.
   *
   * @param chunkCoord the chunk coordinate
   * @return the center coordinate
   */
  public static double chunkToCenter(int chunkCoord) {
    return (chunkCoord << CHUNK_SHIFT) + (CHUNK_SIZE / 2.0);
  }

  /**
   * Packs chunk coordinates into a single long value.
   * Used for efficient storage and lookup.
   *
   * @param chunkX the chunk X coordinate
   * @param chunkZ the chunk Z coordinate
   * @return the packed key
   */
  public static long packChunkKey(int chunkX, int chunkZ) {
    return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
  }

  /**
   * Unpacks the X coordinate from a packed chunk key.
   *
   * @param packedKey the packed key
   * @return the chunk X coordinate
   */
  public static int unpackChunkX(long packedKey) {
    return (int) (packedKey >> 32);
  }

  /**
   * Unpacks the Z coordinate from a packed chunk key.
   *
   * @param packedKey the packed key
   * @return the chunk Z coordinate
   */
  public static int unpackChunkZ(long packedKey) {
    return (int) packedKey;
  }

  /**
   * Checks if two chunks are adjacent (sharing an edge).
   *
   * @param x1 first chunk X
   * @param z1 first chunk Z
   * @param x2 second chunk X
   * @param z2 second chunk Z
   * @return true if adjacent
   */
  public static boolean isAdjacent(int x1, int z1, int x2, int z2) {
    int dx = Math.abs(x1 - x2);
    int dz = Math.abs(z1 - z2);
    return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
  }

  /**
   * Gets the Manhattan distance between two chunks.
   *
   * @param x1 first chunk X
   * @param z1 first chunk Z
   * @param x2 second chunk X
   * @param z2 second chunk Z
   * @return the Manhattan distance
   */
  public static int getDistance(int x1, int z1, int x2, int z2) {
    return Math.abs(x1 - x2) + Math.abs(z1 - z2);
  }

  /**
   * Gets the direction name from one chunk to another.
   *
   * @param fromX source chunk X
   * @param fromZ source chunk Z
   * @param toX   target chunk X
   * @param toZ   target chunk Z
   * @return the direction name (N, S, E, W, NE, NW, SE, SW)
   */
  @NotNull
  public static String getDirection(int fromX, int fromZ, int toX, int toZ) {
    int dx = toX - fromX;
    int dz = toZ - fromZ;

    StringBuilder sb = new StringBuilder();
    if (dz < 0) {
      sb.append("N");
    } else if (dz > 0) {
      sb.append("S");
    }

    if (dx > 0) {
      sb.append("E");
    } else if (dx < 0) {
      sb.append("W");
    }

    return sb.isEmpty() ? "=" : sb.toString();
  }

  /**
   * Gets the cardinal direction name from yaw.
   *
   * @param yaw the player's yaw
   * @return the direction name (N, S, E, W, NE, NW, SE, SW)
   */
  @NotNull
  public static String getDirectionFromYaw(float yaw) {
    // Normalize yaw to 0-360
    float normalizedYaw = ((yaw % 360) + 360) % 360;

    if (normalizedYaw >= 337.5 || normalizedYaw < 22.5) {
      return "S";
    }
    if (normalizedYaw >= 22.5 && normalizedYaw < 67.5) {
      return "SW";
    }
    if (normalizedYaw >= 67.5 && normalizedYaw < 112.5) {
      return "W";
    }
    if (normalizedYaw >= 112.5 && normalizedYaw < 157.5) {
      return "NW";
    }
    if (normalizedYaw >= 157.5 && normalizedYaw < 202.5) {
      return "N";
    }
    if (normalizedYaw >= 202.5 && normalizedYaw < 247.5) {
      return "NE";
    }
    if (normalizedYaw >= 247.5 && normalizedYaw < 292.5) {
      return "E";
    }
    if (normalizedYaw >= 292.5 && normalizedYaw < 337.5) {
      return "SE";
    }
    return "?";
  }

  /**
   * Generates a simple ASCII map character for a chunk.
   *
   * @param isOwned     true if owned by the viewing faction
   * @param isAlly      true if owned by an ally
   * @param isEnemy     true if owned by an enemy
   * @param isClaimed   true if claimed by any faction
   * @param isCenter    true if this is the center chunk (player location)
   * @param isSafeZone  true if this is a safe zone
   * @param isWarZone   true if this is a war zone
   * @return the map character with color code
   */
  @NotNull
  public static String getMapChar(boolean isOwned, boolean isAlly, boolean isEnemy,
                  boolean isClaimed, boolean isCenter, boolean isSafeZone, boolean isWarZone) {
    if (isCenter) {
      return "\u00A7e+\u00A7r"; // Yellow plus for player position
    }
    if (isSafeZone) {
      return "\u00A7a#\u00A7r"; // Green hash for safe zone
    }
    if (isWarZone) {
      return "\u00A7c#\u00A7r"; // Red hash for war zone
    }
    if (isOwned) {
      return "\u00A7b/\u00A7r"; // Cyan slash for own territory
    }
    if (isAlly) {
      return "\u00A7a/\u00A7r"; // Green slash for ally
    }
    if (isEnemy) {
      return "\u00A7c/\u00A7r"; // Red slash for enemy
    }
    if (isClaimed) {
      return "\u00A77/\u00A7r"; // Gray slash for neutral claimed
    }
    return "\u00A78-\u00A7r"; // Dark gray dash for wilderness
  }
}
