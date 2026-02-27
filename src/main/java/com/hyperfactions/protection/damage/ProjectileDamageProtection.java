package com.hyperfactions.protection.damage;

import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import org.jetbrains.annotations.NotNull;

/**
 * Handles projectile damage protection in zones.
 * Blocks projectile damage based on the zone's projectile_damage flag.
 */
public class ProjectileDamageProtection {

  private final ZoneDamageProtection zoneDamage;

  /** Creates a new ProjectileDamageProtection. */
  public ProjectileDamageProtection(@NotNull ZoneDamageProtection zoneDamage) {
    this.zoneDamage = zoneDamage;
  }

  /**
   * Checks if this is projectile damage.
   *
   * @param source the damage source
   * @return true if this is projectile damage
   */
  public boolean isProjectileDamage(Damage.Source source) {
    return source instanceof Damage.ProjectileSource;
  }

  /**
   * Handles projectile damage protection.
   * Note: This only blocks the projectile damage itself, not the follow-up mob damage
   * that may occur from the projectile's shooter.
   *
   * @param event     the damage event
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if the damage should be blocked, false to allow (continue processing)
   */
  public boolean handle(@NotNull Damage event, @NotNull String worldName, double x, double z) {
    if (!isProjectileDamage(event.getSource())) {
      return false; // Not projectile damage, continue processing
    }

    if (!zoneDamage.isProjectileDamageAllowed(worldName, x, z)) {
      event.setCancelled(true);
      return true;
    }

    // Return false to allow further processing (e.g., PvP checks for player-shot projectiles)
    return false;
  }
}
