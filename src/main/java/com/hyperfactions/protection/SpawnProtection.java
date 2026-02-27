package com.hyperfactions.protection;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents temporary spawn protection for a player after respawning.
 *
 * @param playerUuid      the protected player's UUID
 * @param protectedAt     when protection was applied (epoch millis)
 * @param durationSeconds the protection duration in seconds
 * @param world           the world where player spawned
 * @param chunkX          the spawn chunk X coordinate
 * @param chunkZ          the spawn chunk Z coordinate
 */
public record SpawnProtection(
  @NotNull UUID playerUuid,
  long protectedAt,
  int durationSeconds,
  @NotNull String world,
  int chunkX,
  int chunkZ
) {
  /**
   * Creates a new spawn protection.
   *
   * @param playerUuid      the player's UUID
   * @param durationSeconds the protection duration
   * @param world           the spawn world
   * @param chunkX          the spawn chunk X
   * @param chunkZ          the spawn chunk Z
   * @return a new SpawnProtection
   */
  public static SpawnProtection create(@NotNull UUID playerUuid, int durationSeconds,
                    @NotNull String world, int chunkX, int chunkZ) {
    return new SpawnProtection(playerUuid, System.currentTimeMillis(), durationSeconds, world, chunkX, chunkZ);
  }

  /**
   * Checks if this protection has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    long elapsed = System.currentTimeMillis() - protectedAt;
    return elapsed >= durationSeconds * 1000L;
  }

  /**
   * Gets the remaining protection time in seconds.
   *
   * @return remaining seconds, 0 if expired
   */
  public int getRemainingSeconds() {
    if (isExpired()) {
      return 0;
    }
    long elapsed = System.currentTimeMillis() - protectedAt;
    long remaining = (durationSeconds * 1000L) - elapsed;
    return (int) Math.ceil(remaining / 1000.0);
  }

  /**
   * Checks if the player has left their spawn chunk.
   *
   * @param currentWorld  the player's current world
   * @param currentChunkX the player's current chunk X
   * @param currentChunkZ the player's current chunk Z
   * @return true if player has left the spawn chunk
   */
  public boolean hasLeftSpawnChunk(@NotNull String currentWorld, int currentChunkX, int currentChunkZ) {
    return !world.equals(currentWorld) || chunkX != currentChunkX || chunkZ != currentChunkZ;
  }
}
