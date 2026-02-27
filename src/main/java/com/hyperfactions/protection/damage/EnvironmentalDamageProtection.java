package com.hyperfactions.protection.damage;

import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Handles environmental damage protection in zones.
 * Covers drowning, suffocation, out_of_world, and generic environment damage.
 */
public class EnvironmentalDamageProtection {

  private static final Set<String> ENVIRONMENTAL_CAUSES = Set.of(
    "drowning",
    "suffocation",
    "out_of_world",
    "environment"
  );

  private final ZoneDamageProtection zoneDamage;

  /** Creates a new EnvironmentalDamageProtection. */
  public EnvironmentalDamageProtection(@NotNull ZoneDamageProtection zoneDamage) {
    this.zoneDamage = zoneDamage;
  }

  /**
   * Checks if this damage event is environmental damage.
   *
   * @param cause the damage cause
   * @return true if this is environmental damage
   */
  public boolean isEnvironmentalDamage(DamageCause cause) {
    if (cause == null) {
      return false;
    }
    return ENVIRONMENTAL_CAUSES.contains(cause.getId().toLowerCase());
  }

  /**
   * Handles environmental damage protection.
   *
   * @param event     the damage event
   * @param worldName the world name
   * @param x         the X coordinate
   * @param z         the Z coordinate
   * @return true if the damage was handled (blocked or allowed), false to continue processing
   */
  public boolean handle(@NotNull Damage event, @NotNull String worldName, double x, double z) {
    DamageCause cause = event.getCause();
    if (!isEnvironmentalDamage(cause)) {
      return false; // Not environmental damage, continue processing
    }

    if (!zoneDamage.isEnvironmentalDamageAllowed(worldName, x, z)) {
      event.setCancelled(true);
      return true;
    }

    return true; // Handled - environmental damage doesn't need further checks
  }
}
