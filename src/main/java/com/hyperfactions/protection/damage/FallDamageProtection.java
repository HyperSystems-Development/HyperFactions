package com.hyperfactions.protection.damage;

import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import org.jetbrains.annotations.NotNull;

/**
 * Handles fall damage protection in zones.
 * Checks if fall damage is allowed based on the zone's fall_damage flag.
 */
public class FallDamageProtection {

  private final ZoneDamageProtection zoneDamage;

  /** Creates a new FallDamageProtection. */
  public FallDamageProtection(@NotNull ZoneDamageProtection zoneDamage) {
    this.zoneDamage = zoneDamage;
  }

  /**
   * Checks if this damage event is fall damage.
   *
   * @param cause the damage cause
   * @return true if this is fall damage
   */
  public boolean isFallDamage(DamageCause cause) {
    if (cause == null) {
      return false;
    }
    return "fall".equalsIgnoreCase(cause.getId());
  }

  /**
   * Handles fall damage protection.
   *
   * @param event     the damage event
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if the damage was handled (blocked or allowed), false to continue processing
   */
  public boolean handle(@NotNull Damage event, @NotNull String worldName, double x, double z) {
    DamageCause cause = event.getCause();
    if (!isFallDamage(cause)) {
      return false; // Not fall damage, continue processing
    }

    if (!zoneDamage.isFallDamageAllowed(worldName, x, z)) {
      event.setCancelled(true);
      return true;
    }

    return true; // Handled - fall damage doesn't need further checks
  }
}
