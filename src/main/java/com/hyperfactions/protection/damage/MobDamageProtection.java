package com.hyperfactions.protection.damage;

import com.hyperfactions.manager.CombatTagManager;
import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Handles mob damage protection in zones.
 * Blocks damage from non-player entities (mobs) based on the zone's mob_damage flag.
 */
public class MobDamageProtection {

  private final ZoneDamageProtection zoneDamage;

  private final CombatTagManager combatTagManager;

  /** Creates a new MobDamageProtection. */
  public MobDamageProtection(@NotNull ZoneDamageProtection zoneDamage,
               @NotNull CombatTagManager combatTagManager) {
    this.zoneDamage = zoneDamage;
    this.combatTagManager = combatTagManager;
  }

  /**
   * Checks if this is mob damage (entity source but attacker is not a player).
   *
   * @param entitySource  the entity source
   * @param commandBuffer the command buffer for component access
   * @return true if this is mob damage
   */
  public boolean isMobDamage(@NotNull Damage.EntitySource entitySource,
               @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Projectiles are handled separately
    if (entitySource instanceof Damage.ProjectileSource) {
      return false;
    }

    // Check if attacker is a player
    PlayerRef attacker = commandBuffer.getComponent(entitySource.getRef(), PlayerRef.getComponentType());
    return attacker == null; // No PlayerRef = mob
  }

  /**
   * Handles mob damage protection.
   *
   * @param event         the damage event
   * @param entitySource  the entity source
   * @param defender      the player being damaged
   * @param worldName     the world name
   * @param x             the X coordinate
   * @param z             the Z coordinate
   * @param commandBuffer the command buffer
   * @return true if the damage was handled (blocked or allowed), false to continue processing
   */
  public boolean handle(@NotNull Damage event,
             @NotNull Damage.EntitySource entitySource,
             @NotNull PlayerRef defender,
             @NotNull String worldName,
             double x, double z,
             @NotNull CommandBuffer<EntityStore> commandBuffer) {

    if (!isMobDamage(entitySource, commandBuffer)) {
      return false; // Not mob damage, continue processing
    }

    if (!zoneDamage.isMobDamageAllowed(worldName, x, z)) {
      event.setCancelled(true);
      return true;
    }

    // Tag the player in combat (PvE combat tagging)
    combatTagManager.tagPlayer(defender.getUuid());

    return true; // Handled
  }
}
