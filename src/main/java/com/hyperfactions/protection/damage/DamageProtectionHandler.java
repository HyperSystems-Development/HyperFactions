package com.hyperfactions.protection.damage;

import com.hyperfactions.manager.CombatTagManager;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.zone.ZoneDamageProtection;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * Coordinates all damage protection systems.
 * Delegates to specific protection handlers based on damage type.
 *
 * <p>Processing order:
 * 1. Fall damage - checked first (uses NULL_SOURCE in Hytale)
 * 2. Environmental damage - drowning, suffocation, etc. (uses NULL_SOURCE)
 * 3. Projectile damage - from bows, thrown items, etc.
 * 4. Mob damage - from non-player entities
 * 5. PvP damage - player vs player
 */
public class DamageProtectionHandler {

  private final FallDamageProtection fallDamage;

  private final EnvironmentalDamageProtection environmentalDamage;

  private final ProjectileDamageProtection projectileDamage;

  private final MobDamageProtection mobDamage;

  private final PvPDamageProtection pvpDamage;

  private final CombatTagManager combatTagManager;

  /** Creates a new DamageProtectionHandler. */
  public DamageProtectionHandler(@NotNull ZoneDamageProtection zoneDamage,
                  @NotNull ProtectionChecker protectionChecker,
                  @NotNull CombatTagManager combatTagManager,
                  @NotNull Function<ProtectionChecker.PvPResult, String> pvpDenialMessageProvider) {
    this.combatTagManager = combatTagManager;
    this.fallDamage = new FallDamageProtection(zoneDamage);
    this.environmentalDamage = new EnvironmentalDamageProtection(zoneDamage);
    this.projectileDamage = new ProjectileDamageProtection(zoneDamage);
    this.mobDamage = new MobDamageProtection(zoneDamage, combatTagManager);
    this.pvpDamage = new PvPDamageProtection(protectionChecker, combatTagManager, pvpDenialMessageProvider);
  }

  /**
   * Handles damage protection for a player.
   * Delegates to the appropriate protection handler based on damage type.
   *
   * @param event         the damage event
   * @param defender      the player being damaged
   * @param worldName     the world name
   * @param x             the X coordinate
   * @param z             the Z coordinate
   * @param commandBuffer the command buffer for component access
   */
  public void handleDamage(@NotNull Damage event,
              @NotNull PlayerRef defender,
              @NotNull String worldName,
              double x, double z,
              @NotNull CommandBuffer<EntityStore> commandBuffer) {

    UUID defenderUuid = defender.getUuid();

    // 1. Check fall damage (uses cause, not source)
    if (fallDamage.handle(event, worldName, x, z)) {
      if (!event.isCancelled()) {
        combatTagManager.recordDamageType(defenderUuid, CombatTagManager.DeathCauseType.ENVIRONMENTAL);
      }
      return; // Handled
    }

    // 2. Check environmental damage (uses cause, not source)
    if (environmentalDamage.handle(event, worldName, x, z)) {
      if (!event.isCancelled()) {
        combatTagManager.recordDamageType(defenderUuid, CombatTagManager.DeathCauseType.ENVIRONMENTAL);
      }
      return; // Handled
    }

    // 3. For entity-based damage, check source type
    Damage.Source source = event.getSource();
    if (!(source instanceof Damage.EntitySource entitySource)) {
      // Unknown non-entity source - record as environmental (damage goes through)
      combatTagManager.recordDamageType(defenderUuid, CombatTagManager.DeathCauseType.ENVIRONMENTAL);
      return;
    }

    // 4. Check projectile damage (may continue to PvP check if from player)
    boolean projectileBlocked = projectileDamage.handle(event, worldName, x, z);
    if (projectileBlocked) {
      return; // Projectile damage blocked by zone - don't record
    }

    // 5. Check mob damage (non-player entities)
    if (mobDamage.handle(event, entitySource, defender, worldName, x, z, commandBuffer)) {
      if (!event.isCancelled()) {
        combatTagManager.recordDamageType(defenderUuid, CombatTagManager.DeathCauseType.MOB);
      }
      return; // Handled (either blocked or allowed mob damage)
    }

    // 6. Check PvP damage (player vs player)
    pvpDamage.handle(event, entitySource, defender, worldName, x, z, commandBuffer);
    if (!event.isCancelled()) {
      combatTagManager.recordDamageType(defenderUuid, CombatTagManager.DeathCauseType.PVP);
    }
  }

  // Accessors for individual protection systems (for testing or direct access)

  /** Returns the fall damage. */
  public FallDamageProtection getFallDamage() {
    return fallDamage;
  }

  /** Returns the environmental damage. */
  public EnvironmentalDamageProtection getEnvironmentalDamage() {
    return environmentalDamage;
  }

  /** Returns the projectile damage. */
  public ProjectileDamageProtection getProjectileDamage() {
    return projectileDamage;
  }

  public MobDamageProtection getMobDamage() {
    return mobDamage;
  }

  /** Returns the pvp damage. */
  public PvPDamageProtection getPvpDamage() {
    return pvpDamage;
  }
}
