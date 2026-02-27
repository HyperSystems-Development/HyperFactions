package com.hyperfactions.protection.zone;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.util.ChunkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for zone-based damage protection checks.
 * Provides reusable methods for checking various damage types in zones.
 */
public class ZoneDamageProtection {

  private final HyperFactions hyperFactions;

  /** Creates a new ZoneDamageProtection. */
  public ZoneDamageProtection(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /**
   * Checks if a specific damage type is allowed at a location based on zone flags.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @param flagName  the zone flag to check
   * @return true if allowed (no zone or zone allows it), false if blocked
   */
  public boolean isDamageAllowed(@NotNull String worldName, double x, double z, @NotNull String flagName) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
    if (zone == null) {
      // Not in a zone - damage allowed (wilderness rules apply)
      return true;
    }

    return zone.getEffectiveFlag(flagName);
  }

  /**
   * Checks if mob damage is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if mobs can damage players
   */
  public boolean isMobDamageAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.MOB_DAMAGE);
  }

  /**
   * Checks if projectile damage is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if projectiles can damage
   */
  public boolean isProjectileDamageAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.PROJECTILE_DAMAGE);
  }

  /**
   * Checks if fall damage is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if fall damage applies
   */
  public boolean isFallDamageAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.FALL_DAMAGE);
  }

  /**
   * Checks if environmental damage is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if environmental damage applies
   */
  public boolean isEnvironmentalDamageAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.ENVIRONMENTAL_DAMAGE);
  }

  /**
   * Checks if PvP is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if PvP is enabled
   */
  public boolean isPvPAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.PVP_ENABLED);
  }

  /**
   * Checks if friendly fire is allowed at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if friendly fire is enabled
   */
  public boolean isFriendlyFireAllowed(@NotNull String worldName, double x, double z) {
    return isDamageAllowed(worldName, x, z, ZoneFlags.FRIENDLY_FIRE);
  }

  /**
   * Gets the zone at a location.
   *
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return the zone, or null if not in a zone
   */
  @Nullable
  public Zone getZoneAt(@NotNull String worldName, double x, double z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    return hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
  }
}
