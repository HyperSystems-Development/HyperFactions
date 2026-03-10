package com.hyperfactions.protection.ecs;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.GravestoneConfig;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.PlayerData;
import com.hyperfactions.data.PlayerPower;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.manager.CombatTagManager;
import com.hyperfactions.manager.PowerManager;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ECS system that detects player death via DeathComponent addition.
 * Applies power penalty when a player dies.
 *
 * <p>This follows Hytale's built-in pattern from {@code DeathSystems.OnDeathSystem}:
 * when a player dies, a {@link DeathComponent} is added to their entity.
 * This system listens for that addition and applies faction power loss.</p>
 *
 * @see DeathComponent
 * @see com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems
 */
public class PlayerDeathSystem extends RefChangeSystem<EntityStore, DeathComponent> {

  private final HyperFactions hyperFactions;

  /**
   * Creates a new PlayerDeathSystem.
   *
   * @param hyperFactions the HyperFactions instance
   */
  public PlayerDeathSystem(@NotNull HyperFactions hyperFactions) {
    this.hyperFactions = hyperFactions;
  }

  /** Component Type. */
  @NotNull
  @Override
  public ComponentType<EntityStore, DeathComponent> componentType() {
    return DeathComponent.getComponentType();
  }

  /** Returns the query. */
  @NotNull
  @Override
  public Query<EntityStore> getQuery() {
    // Only process player entities
    return Player.getComponentType();
  }

  /** Called when component added. */
  @Override
  public void onComponentAdded(@NotNull Ref<EntityStore> ref,
                 @NotNull DeathComponent component,
                 @NotNull Store<EntityStore> store,
                 @NotNull CommandBuffer<EntityStore> commandBuffer) {
    try {
      // Get the player reference from the entity
      PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
      if (playerRef == null) {
        return;
      }

      UUID victimUuid = playerRef.getUuid();

      // Absolute bypass: per-player power loss disabled (takes priority over zone flags)
      PlayerPower victimPower = hyperFactions.getPowerManager().getPlayerPower(victimUuid);
      if (victimPower.powerLossDisabled()) {
        Logger.debugPower("Power loss bypassed for %s (powerLossDisabled=true)", victimUuid);
        announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);

        // Still process kill reward/neutral penalty for the killer (they aren't bypassed)
        UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
        if (killerUuid != null) {
          processKillerRewards(killerUuid, victimUuid);
        }

        // Still increment death counter
        trackKillDeath(victimUuid, killerUuid);
        return;
      }

      // Check if power loss is disabled in this zone
      TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        int chunkX = ChunkUtil.toChunkCoord(pos.getX());
        int chunkZ = ChunkUtil.toChunkCoord(pos.getZ());
        try {
          String worldName = store.getExternalData().getWorld().getName();
          Zone zone = hyperFactions.getZoneManager().getZone(worldName, chunkX, chunkZ);
          Logger.debugPower("Zone check: player=%s, world=%s, chunk=(%d,%d), zone=%s",
              victimUuid, worldName, chunkX, chunkZ,
              zone != null ? zone.name() + " (power_loss=" + zone.getEffectiveFlag(ZoneFlags.POWER_LOSS) + ")" : "null");
          if (zone != null && !zone.getEffectiveFlag(ZoneFlags.POWER_LOSS)) {
            Logger.debugPower("Power loss skipped for %s in zone '%s' (power_loss=false)", victimUuid, zone.name());
            // Skip power changes, still announce death location
            announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);
            return;
          }

          // Check per-world power loss setting
          if (!ConfigManager.get().isPowerLossEnabledInWorld(worldName)) {
            Logger.debugPower("Power loss skipped for %s in world '%s' (per-world powerLoss=false)", victimUuid, worldName);
            announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);

            UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
            if (killerUuid != null) {
              processKillerRewards(killerUuid, victimUuid);
            }

            trackKillDeath(victimUuid, killerUuid);
            return;
          }
        } catch (Exception e) {
          Logger.warn("Zone check failed for %s, defaulting to no power loss: %s", victimUuid, e.getMessage());
          announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);
          return;
        }
      } else {
        Logger.debugPower("TransformComponent is null for %s during death", victimUuid);
      }

      // Check if power loss is disabled for this death cause type
      CombatTagManager.DeathCauseType deathCause = hyperFactions.getCombatTagManager().getLastDamageType(victimUuid);
      hyperFactions.getCombatTagManager().clearDamageType(victimUuid);
      ConfigManager config = ConfigManager.get();

      if (deathCause == CombatTagManager.DeathCauseType.MOB && !config.isPowerLossOnMobDeath()) {
        Logger.debugPower("Power loss skipped for %s (mob death, powerLossOnMobDeath=false)", victimUuid);
        announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);

        UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
        if (killerUuid != null) {
          processKillerRewards(killerUuid, victimUuid);
        }

        trackKillDeath(victimUuid, killerUuid);
        return;
      }

      if (deathCause == CombatTagManager.DeathCauseType.ENVIRONMENTAL && !config.isPowerLossOnEnvironmentalDeath()) {
        Logger.debugPower("Power loss skipped for %s (environmental death, powerLossOnEnvironmentalDeath=false)", victimUuid);
        announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);

        UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
        if (killerUuid != null) {
          processKillerRewards(killerUuid, victimUuid);
        }

        trackKillDeath(victimUuid, killerUuid);
        return;
      }

      // Apply death power penalty (PVP, UNKNOWN, or enabled cause types)
      double newPower = hyperFactions.getPowerManager().applyDeathPenalty(victimUuid);
      Logger.debugPower("Player %s died, power now %.2f", victimUuid, newPower);

      // Kill reward / neutral kill penalty
      UUID killerUuid = hyperFactions.getCombatTagManager().getLastAttacker(victimUuid);
      if (killerUuid != null) {
        processKillerRewards(killerUuid, victimUuid);
      }

      // Increment kill/death counters
      trackKillDeath(victimUuid, killerUuid);

      // Death location announcement for gravestone integration
      announceDeathLocation(victimUuid, playerRef, store, commandBuffer, ref);
    } catch (Exception e) {
      ErrorHandler.report("Error handling player death in ECS system", e);
    }
  }

  /**
   * Atomically increments death counter for the victim and kill counter for the killer.
   */
  private void trackKillDeath(@NotNull UUID victimUuid, @Nullable UUID killerUuid) {
    hyperFactions.getPlayerStorage().updatePlayerData(victimUuid, PlayerData::incrementDeaths);
    if (killerUuid != null) {
      hyperFactions.getPlayerStorage().updatePlayerData(killerUuid, PlayerData::incrementKills);
    }
  }

  /**
   * Processes kill rewards and neutral kill penalties for the killer.
   */
  private void processKillerRewards(UUID killerUuid, UUID victimUuid) {
    ConfigManager config = ConfigManager.get();
    PowerManager pm = hyperFactions.getPowerManager();

    // Kill reward
    double reward = config.getKillReward();
    if (reward > 0) {
      // Check if victim must be in a faction for kill reward
      if (config.isKillRewardRequiresFaction()) {
        Faction victimFaction = hyperFactions.getFactionManager().getPlayerFaction(victimUuid);
        if (victimFaction == null) {
          Logger.debugPower("Kill reward skipped: victim=%s has no faction (killRewardRequiresFaction=true)", victimUuid);
        } else {
          double killerPower = pm.applyKillReward(killerUuid, reward);
          Logger.debugPower("Kill reward: killer=%s gained %.2f power (now %.2f)", killerUuid, reward, killerPower);
        }
      } else {
        double killerPower = pm.applyKillReward(killerUuid, reward);
        Logger.debugPower("Kill reward: killer=%s gained %.2f power (now %.2f)", killerUuid, reward, killerPower);
      }
    }

    // Neutral kill penalty
    double neutralPenalty = config.getNeutralAttackPenalty();
    if (neutralPenalty > 0) {
      RelationType relation = hyperFactions.getRelationManager()
        .getPlayerRelation(killerUuid, victimUuid);
      if (relation == null || relation == RelationType.NEUTRAL) {
        double killerPower = pm.applyNeutralKillPenalty(killerUuid, neutralPenalty);
        Logger.debugPower("Neutral kill penalty: killer=%s lost %.2f power (now %.2f)", killerUuid, neutralPenalty, killerPower);
      }
    }
  }

  /**
   * Announces death location to faction members if gravestone integration is enabled.
   */
  private void announceDeathLocation(UUID victimUuid, PlayerRef playerRef,
                    Store<EntityStore> store,
                    CommandBuffer<EntityStore> commandBuffer,
                    Ref<EntityStore> ref) {
    try {
      GravestoneConfig gsConfig = ConfigManager.get().gravestones();
      if (!gsConfig.isEnabled() || !gsConfig.isAnnounceDeathLocation()) {
        return;
      }

      // Only announce if GravestonePlugin is available
      var gsIntegration = hyperFactions.getProtectionChecker().getGravestoneIntegration();
      if (gsIntegration == null || !gsIntegration.isAvailable()) {
        return;
      }

      // Only announce if victim is in a faction
      Faction faction = hyperFactions.getFactionManager().getPlayerFaction(victimUuid);
      if (faction == null) {
        return;
      }

      // Get death position
      TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        return;
      }

      Vector3d pos = transform.getPosition();
      int x = (int) Math.floor(pos.getX());
      int y = (int) Math.floor(pos.getY());
      int z = (int) Math.floor(pos.getZ());

      // Get world name
      String worldName;
      try {
        worldName = store.getExternalData().getWorld().getName();
      } catch (Exception e) {
        return;
      }

      // Build and send message to faction members
      String playerName = playerRef.getUsername();
      Message deathMsg = Message.raw("[").color("#555555")
          .insert(Message.raw("HF").color("#55FFFF"))
          .insert(Message.raw("] ").color("#555555"))
          .insert(Message.raw(playerName).color("#FFAA00"))
          .insert(Message.raw(" died at ").color("#AAAAAA"))
          .insert(Message.raw("(" + x + ", " + y + ", " + z + ")").color("#55FF55"))
          .insert(Message.raw(" in ").color("#AAAAAA"))
          .insert(Message.raw(worldName).color("#55FFFF"));

      for (UUID memberUuid : faction.members().keySet()) {
        if (memberUuid.equals(victimUuid)) { // Don't notify the dead player
          continue;
        }
        PlayerRef member = hyperFactions.lookupPlayer(memberUuid);
        if (member != null) {
          // Check member's death announcement preference
          final PlayerRef finalMember = member;
          final Message finalMsg = deathMsg;
          hyperFactions.getPlayerStorage().loadPlayerData(memberUuid).thenAccept(opt -> {
            boolean enabled = opt.map(PlayerData::isDeathAnnouncementsEnabled).orElse(true);
            if (enabled) {
              finalMember.sendMessage(finalMsg);
            }
          });
        }
      }

      Logger.debug("Announced death location for %s at (%d,%d,%d) in %s to %d faction members",
          playerName, x, y, z, worldName, faction.members().size() - 1);
    } catch (Exception e) {
      Logger.debug("Failed to announce death location: %s", e.getMessage());
    }
  }

  /** Called when component set. */
  @Override
  public void onComponentSet(@NotNull Ref<EntityStore> ref,
               @Nullable DeathComponent oldComponent,
               @NotNull DeathComponent newComponent,
               @NotNull Store<EntityStore> store,
               @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Not needed - death component is added once, not updated
  }

  /** Called when component removed. */
  @Override
  public void onComponentRemoved(@NotNull Ref<EntityStore> ref,
                  @NotNull DeathComponent component,
                  @NotNull Store<EntityStore> store,
                  @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Respawn handling is done by PlayerRespawnSystem
  }
}
