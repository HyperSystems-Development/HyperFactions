package com.hyperfactions.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.data.Faction;
import com.hyperfactions.data.FactionMember;
import com.hyperfactions.data.FactionPermissions;
import com.hyperfactions.data.FactionRole;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.integration.protection.OrbisMixinsIntegration;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.HFMessages;
import com.hyperfactions.util.Logger;
import com.hyperfactions.util.CommonKeys;
import java.util.UUID;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central class for all protection checks.
 */
public class ProtectionChecker {

  private final Supplier<HyperFactions> plugin;

  private final FactionManager factionManager;

  private final ClaimManager claimManager;

  private final ZoneManager zoneManager;

  private final RelationManager relationManager;

  private final CombatTagManager combatTagManager;

  private GravestoneIntegration gravestoneIntegration;

  /** Creates a new ProtectionChecker. */
  public ProtectionChecker(
    @NotNull FactionManager factionManager,
    @NotNull ClaimManager claimManager,
    @NotNull ZoneManager zoneManager,
    @NotNull RelationManager relationManager,
    @NotNull CombatTagManager combatTagManager
  ) {
    this(null, factionManager, claimManager, zoneManager, relationManager, combatTagManager);
  }

  /** Creates a new ProtectionChecker. */
  public ProtectionChecker(
    @Nullable Supplier<HyperFactions> plugin,
    @NotNull FactionManager factionManager,
    @NotNull ClaimManager claimManager,
    @NotNull ZoneManager zoneManager,
    @NotNull RelationManager relationManager,
    @NotNull CombatTagManager combatTagManager
  ) {
    this.plugin = plugin;
    this.factionManager = factionManager;
    this.claimManager = claimManager;
    this.zoneManager = zoneManager;
    this.relationManager = relationManager;
    this.combatTagManager = combatTagManager;
  }

  /**
   * Result of a protection check for interactions.
   */
  public enum ProtectionResult {
    ALLOWED,
    ALLOWED_BYPASS,
    ALLOWED_WILDERNESS,
    ALLOWED_SAFEZONE,
    ALLOWED_OWN_CLAIM,
    ALLOWED_ALLY_CLAIM,
    ALLOWED_WARZONE,
    DENIED_SAFEZONE,
    DENIED_WARZONE,
    DENIED_ENEMY_CLAIM,
    DENIED_NEUTRAL_CLAIM,
    DENIED_NO_PERMISSION
  }

  /**
   * Result of a PvP check.
   */
  public enum PvPResult {
    ALLOWED,
    ALLOWED_WARZONE,
    DENIED_SAFEZONE,
    DENIED_SAME_FACTION,
    DENIED_ALLY,
    DENIED_ATTACKER_SAFEZONE,
    DENIED_DEFENDER_SAFEZONE,
    DENIED_SPAWN_PROTECTED,
    DENIED_TERRITORY_NO_PVP
  }

  /**
   * Types of interactions to check.
   */
  public enum InteractionType {
    BUILD,       // Place/break blocks
    INTERACT,    // General block interaction (fallback)
    CONTAINER,   // Open chests, etc.
    DOOR,        // Use doors/gates
    BENCH,       // Crafting tables
    PROCESSING,  // Furnaces/smelters
    SEAT,        // Seats/mounts
    LIGHT,       // Lights/lanterns/campfires
    DAMAGE,        // Damage entities (not players)
    USE,           // Use items (fallback)
    TELEPORTER,    // Use teleporter blocks
    PORTAL,        // Use portal blocks
    CRATE_PICKUP,  // Capture crate entity pickup
    CRATE_PLACE,   // Capture crate entity release
    NPC_TAME,      // F-key NPC taming
    NPC_INTERACT,  // NPC shops/dialogue interaction
    MOUNT,         // Mount/ride entities
    PVE_DAMAGE,    // Damage non-player entities (mobs)
    ITEM_DROP,     // Drop items
    ITEM_PICKUP    // Pick up items
  }

  // === Interaction Protection ===

  /**
   * Checks if a player can interact at a location.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the world X coordinate
   * @param z          the world Z coordinate
   * @param type       the interaction type
   * @return the protection result
   */
  @NotNull
  public ProtectionResult canInteract(@NotNull UUID playerUuid, @NotNull String world,
                    double x, double z, @NotNull InteractionType type) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    return canInteractChunk(playerUuid, world, chunkX, chunkZ, type);
  }

  /**
   * Checks if a player can interact in a chunk.
   * Fail-closed: returns DENIED on any exception.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param chunkX     the chunk X coordinate
   * @param chunkZ     the chunk Z coordinate
   * @param type       the interaction type
   * @return the protection result
   */
  @NotNull
  public ProtectionResult canInteractChunk(@NotNull UUID playerUuid, @NotNull String world,
                      int chunkX, int chunkZ, @NotNull InteractionType type) {
    try {
      // 1. Check if player is an admin (has admin.use permission)
      boolean isAdmin = PermissionManager.get().hasPermission(playerUuid, "hyperfactions.admin.use");

      // 2. Admin bypass check - admins ONLY bypass via toggle (not standard bypass perms)
      if (isAdmin) {
        if (plugin != null) {
          HyperFactions hyperFactions = plugin.get();
          if (hyperFactions != null && hyperFactions.isAdminBypassEnabled(playerUuid)) {
            return ProtectionResult.ALLOWED_BYPASS;
          }
        }

        // Admin with bypass OFF - continue to normal protection checks (no standard bypass)
      } else {
        // 3. Non-admin: Check standard bypass permissions
        String bypassPerm = switch (type) {
          case BUILD -> "hyperfactions.bypass.build";
          case INTERACT, DOOR, BENCH, PROCESSING, SEAT, LIGHT, MOUNT, TELEPORTER, PORTAL,
            CRATE_PICKUP, CRATE_PLACE, NPC_TAME, NPC_INTERACT,
            ITEM_DROP, ITEM_PICKUP -> "hyperfactions.bypass.interact";
          case CONTAINER -> "hyperfactions.bypass.container";
          case DAMAGE, PVE_DAMAGE -> "hyperfactions.bypass.damage";
          case USE -> "hyperfactions.bypass.use";
        };

        if (PermissionManager.get().hasPermission(playerUuid, bypassPerm)
          || PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
          return ProtectionResult.ALLOWED_BYPASS;
        }
      }

      // 2. Check zone
      Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
      if (zone != null) {
        // Get the appropriate flag for the interaction type
        String flagName = switch (type) {
          case BUILD -> ZoneFlags.BUILD_ALLOWED;
          case INTERACT, USE -> ZoneFlags.BLOCK_INTERACT;
          case DOOR -> ZoneFlags.DOOR_USE;
          case CONTAINER -> ZoneFlags.CONTAINER_USE;
          case BENCH -> ZoneFlags.BENCH_USE;
          case PROCESSING -> ZoneFlags.PROCESSING_USE;
          case SEAT -> ZoneFlags.SEAT_USE;
          case LIGHT -> ZoneFlags.LIGHT_USE;
          case TELEPORTER -> ZoneFlags.TELEPORTER_USE;
          case PORTAL -> ZoneFlags.PORTAL_USE;
          case DAMAGE -> ZoneFlags.PVP_ENABLED;
          case PVE_DAMAGE -> ZoneFlags.PVE_DAMAGE;
          case CRATE_PICKUP -> ZoneFlags.CRATE_PICKUP;
          case CRATE_PLACE -> ZoneFlags.CRATE_PLACE;
          case NPC_TAME -> ZoneFlags.NPC_TAME;
          case NPC_INTERACT -> ZoneFlags.NPC_INTERACT;
          case MOUNT -> ZoneFlags.MOUNT_USE;
          case ITEM_DROP, ITEM_PICKUP -> ZoneFlags.BLOCK_INTERACT; // zone checks handled by ECS systems
        };

        boolean allowed = zone.getEffectiveFlag(flagName);

        // Debug: Log zone protection check
        Logger.debug("[Protection] Zone '%s' (%s) flag '%s' = %s for player %s at %s/%d/%d",
          zone.name(), zone.type().name(), flagName, allowed, playerUuid, world, chunkX, chunkZ);

        if (!allowed) {
          ProtectionResult result = zone.isSafeZone() ? ProtectionResult.DENIED_SAFEZONE
              : zone.isWarZone() ? ProtectionResult.DENIED_WARZONE
              : ProtectionResult.DENIED_NO_PERMISSION;
          Logger.debug("[Protection] Zone blocked: %s", result);
          return result;
        }

        // If zone allows this interaction, still need to check claim ownership below
        // For WarZones with build allowed, anyone can interact
        if (zone.isWarZone() && allowed) {
          Logger.debug("[Protection] WarZone allowed: %s", ProtectionResult.ALLOWED_WARZONE);
          return ProtectionResult.ALLOWED_WARZONE;
        }
      }

      // Track whether we came from a zone for the wilderness result
      boolean inSafeZone = zone != null && zone.isSafeZone();

      // 3. Check claim owner
      UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);

      if (claimOwner == null) {
        // No faction claim — return zone-aware result
        return inSafeZone ? ProtectionResult.ALLOWED_SAFEZONE : ProtectionResult.ALLOWED_WILDERNESS;
      }

      // 4. Get player's faction
      UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);

      // 5. Get faction and its effective permissions
      Faction ownerFaction = factionManager.getFaction(claimOwner);
      FactionPermissions perms = null;
      if (ownerFaction != null) {
        perms = ConfigManager.get().getEffectiveFactionPermissions(
          ownerFaction.getEffectivePermissions()
        );
      }

      // 6. Check if same faction (member or officer)
      if (playerFactionId != null && playerFactionId.equals(claimOwner)) {
        // Determine if officer/leader or regular member
        FactionMember factionMember = ownerFaction != null ? ownerFaction.getMember(playerUuid) : null;
        boolean isOfficerOrLeader = factionMember != null
          && factionMember.role().getLevel() >= FactionRole.OFFICER.getLevel();

        if (isOfficerOrLeader) {
          if (perms != null && !checkPermission(perms, "officer", type)) {
            Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=OFFICER_NO_PERM, claimOwner=%s",
              playerUuid, world, chunkX, chunkZ, type, claimOwner);
            return ProtectionResult.DENIED_NO_PERMISSION;
          }
        } else {
          if (perms != null && !checkMemberPermission(perms, type)) {
            Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=MEMBER_NO_PERM, claimOwner=%s",
              playerUuid, world, chunkX, chunkZ, type, claimOwner);
            return ProtectionResult.DENIED_NO_PERMISSION;
          }
        }
        return ProtectionResult.ALLOWED_OWN_CLAIM;
      }

      // 7. Check ally relation
      if (playerFactionId != null) {
        RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
        if (relation == RelationType.ALLY) {
          // Check ally permissions
          if (perms != null && checkAllyPermission(perms, type)) {
            return ProtectionResult.ALLOWED_ALLY_CLAIM;
          }

          // Ally but no permission for this type
          Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=ALLY_NO_PERM, claimOwner=%s",
            playerUuid, world, chunkX, chunkZ, type, claimOwner);
          return ProtectionResult.DENIED_NO_PERMISSION;
        }
      }

      // 8. Check outsider permissions (neutral, enemy, or no faction)
      if (perms != null && checkOutsiderPermission(perms, type)) {
        return ProtectionResult.ALLOWED;
      }

      // 9. Denied - either enemy or neutral claim without permission
      if (playerFactionId != null) {
        RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
        if (relation == RelationType.ENEMY) {
          Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=ENEMY_CLAIM, claimOwner=%s",
            playerUuid, world, chunkX, chunkZ, type, claimOwner);
          return ProtectionResult.DENIED_ENEMY_CLAIM;
        }
      }

      Logger.debugProtection("Interaction denied: player=%s, chunk=%s/%d/%d, type=%s, result=NEUTRAL_CLAIM, claimOwner=%s",
        playerUuid, world, chunkX, chunkZ, type, claimOwner);
      return ProtectionResult.DENIED_NEUTRAL_CLAIM;
    } catch (Exception e) {
      // Fail-closed: deny on any exception to prevent unauthorized actions
      ErrorHandler.report(String.format("Protection check error (fail-closed) for player %s at %s/%d/%d type=%s",
        playerUuid, world, chunkX, chunkZ, type), e);
      return ProtectionResult.DENIED_NO_PERMISSION;
    }
  }

  /**
   * Unified permission check for any level and interaction type.
   * Uses parent-child logic built into FactionPermissions.get().
   *
   * @param perms the faction permissions
   * @param level the level (outsider, ally, member, officer)
   * @param type  the interaction type
   * @return true if allowed
   */
  private boolean checkPermission(FactionPermissions perms, String level, InteractionType type) {
    return switch (type) {
      case BUILD -> perms.get(level + "Break") || perms.get(level + "Place");
      case INTERACT, USE -> perms.get(level + "Interact");
      case DOOR -> perms.get(level + "DoorUse");
      case CONTAINER -> perms.get(level + "ContainerUse");
      case BENCH -> perms.get(level + "BenchUse");
      case PROCESSING -> perms.get(level + "ProcessingUse");
      case SEAT -> perms.get(level + "SeatUse");
      case LIGHT -> perms.get(level + "Interact"); // Light use shares general interact permission
      case TELEPORTER, PORTAL -> perms.get(level + "TransportUse");
      case CRATE_PICKUP, CRATE_PLACE -> perms.get(level + "CrateUse");
      case NPC_TAME -> perms.get(level + "NpcTame");
      case NPC_INTERACT -> perms.get(level + "NpcInteract");
      case MOUNT -> perms.get(level + "SeatUse"); // Mount shares seat permission
      case PVE_DAMAGE -> perms.get(level + "PveDamage");
      case DAMAGE -> !"outsider".equals(level); // outsiders can't damage (PvP handled separately)
      case ITEM_DROP, ITEM_PICKUP -> perms.get(level + "Interact"); // item drop/pickup uses interact permission
    };
  }

  private boolean checkOutsiderPermission(FactionPermissions perms, InteractionType type) {
    return checkPermission(perms, "outsider", type);
  }

  private boolean checkAllyPermission(FactionPermissions perms, InteractionType type) {
    return checkPermission(perms, "ally", type);
  }

  private boolean checkMemberPermission(FactionPermissions perms, InteractionType type) {
    return checkPermission(perms, "member", type);
  }

  // === PvP Protection ===

  /**
   * Checks if a player can damage another player.
   *
   * @param attackerUuid the attacker's UUID
   * @param defenderUuid the defender's UUID
   * @param world        the world name
   * @param x            the location X
   * @param z            the location Z
   * @return the PvP result
   */
  @NotNull
  public PvPResult canDamagePlayer(@NotNull UUID attackerUuid, @NotNull UUID defenderUuid,
                  @NotNull String world, double x, double z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    return canDamagePlayerChunk(attackerUuid, defenderUuid, world, chunkX, chunkZ);
  }

  /**
   * Checks if a player can damage another player in a chunk.
   *
   * @param attackerUuid the attacker's UUID
   * @param defenderUuid the defender's UUID
   * @param world        the world name
   * @param chunkX       the chunk X
   * @param chunkZ       the chunk Z
   * @return the PvP result
   */
  @NotNull
  public PvPResult canDamagePlayerChunk(@NotNull UUID attackerUuid, @NotNull UUID defenderUuid,
                     @NotNull String world, int chunkX, int chunkZ) {
    ConfigManager config = ConfigManager.get();

    // 0. Check defender's spawn protection
    if (combatTagManager.hasSpawnProtection(defenderUuid)) {
      return PvPResult.DENIED_SPAWN_PROTECTED;
    }

    // 0b. Break attacker's spawn protection if they attack (if configured)
    if (config.isSpawnProtectionBreakOnAttack() && combatTagManager.hasSpawnProtection(attackerUuid)) {
      combatTagManager.clearSpawnProtection(attackerUuid);
    }

    // 1. Check zone for PvP flag
    Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
    if (zone != null) {
      boolean pvpEnabled = zone.getEffectiveFlag(ZoneFlags.PVP_ENABLED);
      if (!pvpEnabled) {
        return PvPResult.DENIED_SAFEZONE;
      }

      // Zone has PvP enabled - check friendly fire hierarchy
      boolean friendlyFireAllowed = zone.getEffectiveFlag(ZoneFlags.FRIENDLY_FIRE);

      // Check same faction (zone flag → child flag → config fallback)
      if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
        if (friendlyFireAllowed) {
          // Parent on — check granular faction child flag
          if (!zone.getEffectiveFlag(ZoneFlags.FRIENDLY_FIRE_FACTION)) {
            return PvPResult.DENIED_SAME_FACTION;
          }
        } else if (!config.isFactionDamage()) {
          // Parent off and config doesn't allow — deny
          return PvPResult.DENIED_SAME_FACTION;
        }
      }

      // Check ally (zone flag → child flag → config fallback)
      RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
      if (relation == RelationType.ALLY) {
        if (friendlyFireAllowed) {
          // Parent on — check granular ally child flag
          if (!zone.getEffectiveFlag(ZoneFlags.FRIENDLY_FIRE_ALLY)) {
            return PvPResult.DENIED_ALLY;
          }
        } else if (!config.isAllyDamage()) {
          // Parent off and config doesn't allow — deny
          return PvPResult.DENIED_ALLY;
        }
      }

      // PvP is enabled in this zone
      return zone.isWarZone() ? PvPResult.ALLOWED_WARZONE : PvPResult.ALLOWED;
    }

    // Not in a zone - use standard checks

    // 2. Check faction territory PvP setting
    UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);
    if (claimOwner != null) {
      Faction ownerFaction = factionManager.getFaction(claimOwner);
      if (ownerFaction != null) {
        FactionPermissions perms = config.getEffectiveFactionPermissions(
          ownerFaction.getEffectivePermissions()
        );
        if (!perms.pvpEnabled()) {
          Logger.debugProtection("PvP denied: attacker=%s, defender=%s, chunk=%s/%d/%d, result=TERRITORY_NO_PVP, claimOwner=%s",
            attackerUuid, defenderUuid, world, chunkX, chunkZ, claimOwner);
          return PvPResult.DENIED_TERRITORY_NO_PVP;
        }
      }
    }

    // 3. Check same faction (with per-world override)
    if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
      if (!config.isFactionDamage(world)) {
        return PvPResult.DENIED_SAME_FACTION;
      }
    }

    // 4. Check ally (with per-world override)
    RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
    if (relation == RelationType.ALLY) {
      if (!config.isAllyDamage(world)) {
        Logger.debugProtection("PvP denied: attacker=%s, defender=%s, chunk=%s/%d/%d, result=ALLY",
          attackerUuid, defenderUuid, world, chunkX, chunkZ);
        return PvPResult.DENIED_ALLY;
      }
    }

    // 5. Check outsider damage config in claimed territory
    if (claimOwner != null) {
      UUID attackerFactionId = factionManager.getPlayerFactionId(attackerUuid);
      if (attackerFactionId == null || !attackerFactionId.equals(claimOwner)) {
        // Attacker is not the claim owner — check outsider damage config
        if (attackerFactionId == null) {
          // Factionless attacker
          if (!config.isFactionlessDamageAllowed()) {
            return PvPResult.DENIED_TERRITORY_NO_PVP;
          }
        } else if (relation == RelationType.ENEMY) {
          if (!config.isEnemyDamageAllowed()) {
            return PvPResult.DENIED_TERRITORY_NO_PVP;
          }
        } else if (relation == RelationType.NEUTRAL) {
          if (!config.isNeutralDamageAllowed()) {
            return PvPResult.DENIED_TERRITORY_NO_PVP;
          }
        }
      }
    }

    // 6. Default: allow PvP
    Logger.debugProtection("PvP allowed: attacker=%s, defender=%s, chunk=%s/%d/%d, relation=%s",
      attackerUuid, defenderUuid, world, chunkX, chunkZ, relation);
    return PvPResult.ALLOWED;
  }

  // === Convenience Methods ===

  /**
   * Checks if a player can build at a location.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the X coordinate
   * @param z          the Z coordinate
   * @return true if allowed
   */
  public boolean canBuild(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    ProtectionResult result = canInteract(playerUuid, world, x, z, InteractionType.BUILD);
    return isAllowed(result);
  }

  /**
   * Checks if a player can access containers at a location.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the X coordinate
   * @param z          the Z coordinate
   * @return true if allowed
   */
  public boolean canAccessContainer(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    ProtectionResult result = canInteract(playerUuid, world, x, z, InteractionType.CONTAINER);
    return isAllowed(result);
  }

  /**
   * Checks if a player can pick up items at a location (auto pickup mode).
   *
   * <p>This is for native ECS events (InteractivelyPickupItemEvent) and always
   * checks ITEM_PICKUP flag. For mode-aware pickup checks, use the overload
   * that accepts a mode parameter.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name
   * @param x          the X coordinate
   * @param y          the Y coordinate (unused, but included for API consistency)
   * @param z          the Z coordinate
   * @return true if pickup is allowed
   */
  public boolean canPickupItem(@NotNull UUID playerUuid, @NotNull String worldName, double x, double y, double z) {
    return canPickupItem(playerUuid, worldName, x, y, z, "auto");
  }

  /**
   * Checks if a player can pick up items at a location with pickup mode awareness.
   *
   * <p>This is called by OrbisGuard-Mixins hook for F-key and auto pickup events.
   * It checks:
   * 1. Admin bypass toggle
   * 2. Bypass permission (hyperfactions.bypass.pickup)
   * 3. Zone flags (ITEM_PICKUP for auto, ITEM_PICKUP_MANUAL for F-key)
   * 4. Faction claim permissions
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name
   * @param x          the X coordinate
   * @param y          the Y coordinate (unused, but included for API consistency)
   * @param z          the Z coordinate
   * @param mode       the pickup mode: "auto" for walking over items, "manual" for F-key
   * @return true if pickup is allowed
   */
  public boolean canPickupItem(@NotNull UUID playerUuid, @NotNull String worldName, double x, double y, double z, @NotNull String mode) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Determine which flag to check based on pickup mode
    // "manual" = F-key pickup → ITEM_PICKUP_MANUAL
    // "auto" or anything else = auto pickup → ITEM_PICKUP
    boolean isManualPickup = "manual".equalsIgnoreCase(mode);
    String flagToCheck = isManualPickup ? ZoneFlags.ITEM_PICKUP_MANUAL : ZoneFlags.ITEM_PICKUP;

    // 1. Check admin bypass toggle
    if (plugin != null) {
      HyperFactions hyperFactions = plugin.get();
      if (hyperFactions != null && hyperFactions.isAdminBypassEnabled(playerUuid)) {
        Logger.debug("[Pickup:%s] Admin bypass enabled for %s", mode, playerUuid);
        return true;
      }
    }

    // 2. Check bypass permission
    if (PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.pickup")
      || PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
      Logger.debug("[Pickup:%s] Bypass permission for %s", mode, playerUuid);
      return true;
    }

    // 3. Check zone flags (check appropriate flag based on mode)
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      boolean pickupAllowed = zone.getEffectiveFlag(flagToCheck);
      if (!pickupAllowed) {
        Logger.debug("[Pickup:%s] Blocked by zone '%s' flag '%s'=false for %s at %s/%d/%d",
            mode, zone.name(), flagToCheck, playerUuid, worldName, chunkX, chunkZ);
        return false;
      }
    }

    // 4. Check faction claim
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner == null) {
      // Wilderness - pickup allowed
      return true;
    }

    // 5. Get player's faction
    UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);

    // 6. Check if same faction (members can always pick up in own territory)
    if (playerFactionId != null && playerFactionId.equals(claimOwner)) {
      return true;
    }

    // 7. Check ally relation (allies can pick up in allied territory)
    if (playerFactionId != null) {
      RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
      if (relation == RelationType.ALLY) {
        return true;
      }
    }

    // 8. Outsider pickup — configurable via server settings
    if (!ConfigManager.get().isOutsiderPickupAllowed()) {
      Logger.debug("[Pickup:%s] Blocked in other faction's territory for %s at %s/%d/%d",
          mode, playerUuid, worldName, chunkX, chunkZ);
      return false;
    }
    return true;
  }

  /**
   * Checks if a protection result is "allowed".
   *
   * @param result the result
   * @return true if allowed
   */
  public boolean isAllowed(@NotNull ProtectionResult result) {
    return switch (result) {
      case ALLOWED, ALLOWED_BYPASS, ALLOWED_WILDERNESS, ALLOWED_SAFEZONE,
        ALLOWED_OWN_CLAIM, ALLOWED_ALLY_CLAIM, ALLOWED_WARZONE -> true;
      default -> false;
    };
  }

  /**
   * Checks if a PvP result is "allowed".
   *
   * @param result the result
   * @return true if allowed
   */
  public boolean isAllowed(@NotNull PvPResult result) {
    return switch (result) {
      case ALLOWED, ALLOWED_WARZONE -> true;
      default -> false;
    };
  }

  /**
   * Looks up a PlayerRef from a UUID for i18n message resolution.
   * Returns null if the player is offline or plugin is unavailable.
   */
  @Nullable
  private PlayerRef lookupPlayerRef(@Nullable UUID uuid) {
    if (uuid == null || plugin == null) {
      return null;
    }
    HyperFactions hf = plugin.get();
    return hf != null ? hf.lookupPlayer(uuid) : null;
  }

  /**
   * Gets a user-friendly denial message with generic action wording (server default language).
   */
  @NotNull
  public String getDenialMessage(@NotNull ProtectionResult result) {
    return getDenialMessage(null, result, null);
  }

  /**
   * Gets a user-friendly denial message with specific action context (server default language).
   */
  @NotNull
  public String getDenialMessage(@NotNull ProtectionResult result, @Nullable InteractionType type) {
    return getDenialMessage(null, result, type);
  }

  /**
   * Gets a user-friendly denial message localized to the player's language.
   *
   * @param player the player (null for server default language)
   * @param result the protection result
   * @param type   the interaction type (null for generic messages)
   * @return the denial message
   */
  @NotNull
  public String getDenialMessage(@Nullable PlayerRef player, @NotNull ProtectionResult result,
                  @Nullable InteractionType type) {
    String action = getActionPhrase(player, type);
    return switch (result) {
      case DENIED_SAFEZONE -> HFMessages.get(player, CommonKeys.Protection.DENIED_SAFEZONE, action);
      case DENIED_WARZONE -> HFMessages.get(player, CommonKeys.Protection.DENIED_WARZONE, action);
      case DENIED_ENEMY_CLAIM -> HFMessages.get(player, CommonKeys.Protection.DENIED_ENEMY_CLAIM, action);
      case DENIED_NEUTRAL_CLAIM -> HFMessages.get(player, CommonKeys.Protection.DENIED_CLAIMED, action);
      case DENIED_NO_PERMISSION -> HFMessages.get(player, CommonKeys.Protection.DENIED_HERE, action);
      default -> HFMessages.get(player, CommonKeys.Protection.DENIED_HERE, action);
    };
  }

  /**
   * Gets a player-friendly action phrase for the given interaction type.
   *
   * @param player the player (null for server default language)
   * @param type   the interaction type, or null for generic
   * @return phrase like "You can't build or break blocks"
   */
  @NotNull
  private String getActionPhrase(@Nullable PlayerRef player, @Nullable InteractionType type) {
    if (type == null) {
      return HFMessages.get(player, CommonKeys.Protection.ACTION_GENERIC);
    }
    return switch (type) {
      case BUILD -> HFMessages.get(player, CommonKeys.Protection.ACTION_BUILD);
      case INTERACT, USE -> HFMessages.get(player, CommonKeys.Protection.ACTION_INTERACT);
      case DOOR -> HFMessages.get(player, CommonKeys.Protection.ACTION_DOOR);
      case CONTAINER -> HFMessages.get(player, CommonKeys.Protection.ACTION_CONTAINER);
      case BENCH -> HFMessages.get(player, CommonKeys.Protection.ACTION_BENCH);
      case PROCESSING -> HFMessages.get(player, CommonKeys.Protection.ACTION_PROCESSING);
      case SEAT -> HFMessages.get(player, CommonKeys.Protection.ACTION_SEAT);
      case LIGHT -> HFMessages.get(player, CommonKeys.Protection.ACTION_LIGHT);
      case TELEPORTER, PORTAL -> HFMessages.get(player, CommonKeys.Protection.ACTION_TELEPORTER);
      case CRATE_PICKUP, CRATE_PLACE -> HFMessages.get(player, CommonKeys.Protection.ACTION_CRATE);
      case NPC_TAME -> HFMessages.get(player, CommonKeys.Protection.ACTION_TAME);
      case NPC_INTERACT -> HFMessages.get(player, CommonKeys.Protection.ACTION_NPC);
      case MOUNT -> HFMessages.get(player, CommonKeys.Protection.ACTION_MOUNT);
      case PVE_DAMAGE -> HFMessages.get(player, CommonKeys.Protection.ACTION_PVE);
      case DAMAGE -> HFMessages.get(player, CommonKeys.Protection.ACTION_GENERIC);
      case ITEM_DROP -> HFMessages.get(player, CommonKeys.Protection.ACTION_ITEM_DROP);
      case ITEM_PICKUP -> HFMessages.get(player, CommonKeys.Protection.ACTION_ITEM_PICKUP);
    };
  }

  /**
   * Gets a user-friendly PvP denial message (server default language).
   */
  @NotNull
  public String getDenialMessage(@NotNull PvPResult result) {
    return getDenialMessage(null, result);
  }

  /**
   * Gets a user-friendly PvP denial message localized to the player's language.
   *
   * @param player the player (null for server default language)
   * @param result the PvP result
   * @return the denial message
   */
  @NotNull
  public String getDenialMessage(@Nullable PlayerRef player, @NotNull PvPResult result) {
    return switch (result) {
      case DENIED_SAFEZONE -> HFMessages.get(player, CommonKeys.Protection.PVP_SAFEZONE);
      case DENIED_SAME_FACTION -> HFMessages.get(player, CommonKeys.Protection.PVP_SAME_FACTION);
      case DENIED_ALLY -> HFMessages.get(player, CommonKeys.Protection.PVP_ALLY);
      case DENIED_ATTACKER_SAFEZONE, DENIED_DEFENDER_SAFEZONE -> HFMessages.get(player, CommonKeys.Protection.PVP_SAFEZONE);
      case DENIED_SPAWN_PROTECTED -> HFMessages.get(player, CommonKeys.Protection.PVP_SPAWN_PROTECTED);
      case DENIED_TERRITORY_NO_PVP -> HFMessages.get(player, CommonKeys.Protection.PVP_TERRITORY_DISABLED);
      default -> HFMessages.get(player, CommonKeys.Protection.PVP_GENERIC);
    };
  }

  // === Mixin Hook Protection Methods ===
  // These methods are called by HyperProtect/OrbisGuard mixin hooks.
  // They accept block coordinates (int x, y, z) and return a String denial
  // message (null = allowed).

  /**
   * Common protection check for mixin hooks.
   * Checks bypass, specific zone flag, and faction claim permissions.
   * Fail-closed: returns a denial message on any exception.
   *
   * @param playerUuid   the player's UUID
   * @param worldName    the world name
   * @param x            the block X coordinate
   * @param y            the block Y coordinate
   * @param z            the block Z coordinate
   * @param zoneFlag     the specific zone flag to check
   * @param factionType  the interaction type for faction permission resolution
   * @return null if allowed, denial message if denied
   */
  @Nullable
  private String checkMixinProtection(@NotNull UUID playerUuid, @NotNull String worldName,
                    int x, int y, int z,
                    @NotNull String zoneFlag,
                    @NotNull InteractionType factionType) {
    try {
      int chunkX = ChunkUtil.toChunkCoord(x);
      int chunkZ = ChunkUtil.toChunkCoord(z);

      // 1. Admin bypass
      if (plugin != null) {
        HyperFactions hf = plugin.get();
        if (hf != null && hf.isAdminBypassEnabled(playerUuid)) {
          return null;
        }
      }

      // 2. Standard bypass
      boolean isAdmin = PermissionManager.get().hasPermission(playerUuid, "hyperfactions.admin.use");
      if (!isAdmin) {
        String bypassPerm = switch (factionType) {
          case BUILD -> "hyperfactions.bypass.build";
          case INTERACT, DOOR, BENCH, PROCESSING, SEAT, LIGHT, MOUNT, TELEPORTER, PORTAL,
            CRATE_PICKUP, CRATE_PLACE, NPC_TAME, NPC_INTERACT,
            ITEM_DROP, ITEM_PICKUP -> "hyperfactions.bypass.interact";
          case CONTAINER -> "hyperfactions.bypass.container";
          case DAMAGE, PVE_DAMAGE -> "hyperfactions.bypass.damage";
          case USE -> "hyperfactions.bypass.use";
        };
        if (PermissionManager.get().hasPermission(playerUuid, bypassPerm)
          || PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
          return null;
        }
      }

      // Resolve player's locale for localized denial messages
      PlayerRef playerRef = lookupPlayerRef(playerUuid);

      // 3. Zone flag check
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null) {
        if (!zone.getEffectiveFlag(zoneFlag)) {
          String action = getActionPhrase(playerRef, factionType);
          if (zone.isSafeZone()) {
            return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_SAFEZONE, action);
          }
          if (zone.isWarZone()) {
            return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_WARZONE, action);
          }
          return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_ZONE, action);
        }
        if (zone.isWarZone()) {
          return null;
        }
      }

      // 4. Faction claim check
      UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
      if (claimOwner == null) { // Wilderness
        return null;
      }

      UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);
      Faction ownerFaction = factionManager.getFaction(claimOwner);
      FactionPermissions perms = null;
      if (ownerFaction != null) {
        perms = ConfigManager.get().getEffectiveFactionPermissions(ownerFaction.getEffectivePermissions());
      }

      // Same faction
      if (playerFactionId != null && playerFactionId.equals(claimOwner)) {
        FactionMember member = ownerFaction != null ? ownerFaction.getMember(playerUuid) : null;
        boolean isOfficerOrLeader = member != null
          && member.role().getLevel() >= FactionRole.OFFICER.getLevel();
        String level = isOfficerOrLeader ? "officer" : "member";
        if (perms != null && !checkPermission(perms, level, factionType)) {
          return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_FACTION_PERM, getActionPhrase(playerRef, factionType), level);
        }
        return null;
      }

      // Ally
      if (playerFactionId != null) {
        RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
        if (relation == RelationType.ALLY) {
          if (perms != null && checkPermission(perms, "ally", factionType)) {
            return null;
          }
          return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_ALLY_TERRITORY, getActionPhrase(playerRef, factionType));
        }
      }

      // Outsider
      if (perms != null && checkPermission(perms, "outsider", factionType)) {
        return null;
      }

      // Determine territory context for the message
      if (playerFactionId != null) {
        RelationType relation = relationManager.getRelation(playerFactionId, claimOwner);
        if (relation == RelationType.ENEMY) {
          return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_ENEMY_CLAIM, getActionPhrase(playerRef, factionType));
        }
      }
      return HFMessages.get(playerRef, CommonKeys.Protection.DENIED_CLAIMED, getActionPhrase(playerRef, factionType));
    } catch (Exception e) {
      // Fail-closed: deny on any exception to prevent unauthorized actions
      ErrorHandler.report(String.format("Protection check error (fail-closed) for player %s at %s/%d/%d/%d type=%s",
        playerUuid, worldName, x, y, z, factionType), e);
      return HFMessages.get(lookupPlayerRef(playerUuid), CommonKeys.Protection.DENIED_ERROR);
    }
  }

  /**
   * Checks if a player can break/build blocks (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkBuild(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.BUILD_ALLOWED, InteractionType.BUILD);
  }

  /**
   * Checks if a player can place blocks (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkPlace(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.BLOCK_PLACE, InteractionType.BUILD);
  }

  /**
   * Checks if a player can use the hammer (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkHammer(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.HAMMER_USE, InteractionType.BUILD);
  }

  /**
   * Checks if a player can use builder tools (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkBuilderTool(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.BUILDER_TOOLS_USE, InteractionType.BUILD);
  }

  /**
   * Checks if a player can interact with blocks (mixin hook version for ChangeState).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkUse(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkUse(playerUuid, worldName, x, y, z, InteractionType.INTERACT);
  }

  /**
   * Checks if a player can use/interact at block coordinates with explicit interaction type.
   * Routes to the correct zone flag and faction permission based on the interaction type.
   *
   * @param type the specific interaction type (CRATE_PICKUP, CRATE_PLACE, NPC_TAME, or INTERACT)
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkUse(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z,
             @NotNull InteractionType type) {
    String zoneFlag = switch (type) {
      case CRATE_PICKUP -> ZoneFlags.CRATE_PICKUP;
      case CRATE_PLACE -> ZoneFlags.CRATE_PLACE;
      case NPC_TAME -> ZoneFlags.NPC_TAME;
      case MOUNT -> ZoneFlags.MOUNT_USE;
      case LIGHT -> ZoneFlags.LIGHT_USE;
      default -> ZoneFlags.BLOCK_INTERACT;
    };
    return checkMixinProtection(playerUuid, worldName, x, y, z, zoneFlag, type);
  }

  /**
   * Checks if a player can sit on seats (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkSeat(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.SEAT_USE, InteractionType.SEAT);
  }

  /**
   * Checks if a player can mount rideable entities (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkMount(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.MOUNT_USE, InteractionType.MOUNT);
  }

  /**
   * Checks if a player can launch projectiles (mixin hook version).
   * Uses PROJECTILE_DAMAGE zone flag for the check.
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkProjectileLaunch(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.PROJECTILE_DAMAGE, InteractionType.DAMAGE);
  }

  /**
   * Checks if a player can trade at NPC barter shops (mixin hook version).
   * Uses NPC_INTERACT zone flag for the check.
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkTrade(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.NPC_INTERACT, InteractionType.NPC_INTERACT);
  }

  /**
   * Checks if a player can use teleporter blocks (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkTeleporter(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.TELEPORTER_USE, InteractionType.TELEPORTER);
  }

  /**
   * Checks if a player can use portal blocks (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkPortal(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.PORTAL_USE, InteractionType.PORTAL);
  }

  /**
   * Checks if a player can use crafting benches (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkBench(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.BENCH_USE, InteractionType.BENCH);
  }

  /**
   * Checks if a player can open containers (mixin hook version).
   *
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkContainer(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkMixinProtection(playerUuid, worldName, x, y, z, ZoneFlags.CONTAINER_USE, InteractionType.CONTAINER);
  }

  /**
   * Checks if entity damage should be blocked (PvP and mob damage combined).
   *
   * @param attackerUuid the attacker's UUID (null if non-player entity)
   * @param targetUuid   the target entity's UUID (null if non-player entity)
   * @param worldName    the world name
   * @param x            the block X coordinate
   * @param y            the block Y coordinate
   * @param z            the block Z coordinate
   * @return null if allowed, denial message if denied
   */
  @Nullable
  public String checkEntityDamage(@Nullable UUID attackerUuid, @Nullable UUID targetUuid,
                  @NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // PvE: mob attacking player — check mob_damage flag
    if (attackerUuid == null && targetUuid != null) {
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null && !zone.getEffectiveFlag(ZoneFlags.MOB_DAMAGE)) {
        PlayerRef targetRef = lookupPlayerRef(targetUuid);
        return HFMessages.get(targetRef, CommonKeys.Protection.MOB_DAMAGE_DISABLED);
      }
      return null;
    }

    // PvE: player attacking mob — check zone flag first, then territory claim
    if (attackerUuid != null && targetUuid == null) {
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null && !zone.getEffectiveFlag(ZoneFlags.PVE_DAMAGE)) {
        PlayerRef attackerRef = lookupPlayerRef(attackerUuid);
        return HFMessages.get(attackerRef, CommonKeys.Protection.PVE_DAMAGE_DISABLED);
      }
      // Check territory claim permissions
      return checkPveInTerritory(attackerUuid, worldName, chunkX, chunkZ);
    }

    // PvP check using existing canDamagePlayerChunk
    PvPResult result = canDamagePlayerChunk(attackerUuid, targetUuid, worldName, chunkX, chunkZ);
    return isAllowed(result) ? null : getDenialMessage(lookupPlayerRef(attackerUuid), result);
  }

  /**
   * Checks if a player can damage mobs in faction territory.
   * Follows the same pattern as canInteractChunk but for PVE_DAMAGE.
   *
   * @param attackerUuid the attacker's UUID
   * @param worldName    the world name
   * @param chunkX       the chunk X
   * @param chunkZ       the chunk Z
   * @return null if allowed, denial message if denied
   */
  @Nullable
  private String checkPveInTerritory(@NotNull UUID attackerUuid, @NotNull String worldName,
                     int chunkX, int chunkZ) {
    // Admin bypass
    boolean isAdmin = PermissionManager.get().hasPermission(attackerUuid, "hyperfactions.admin.use");
    if (isAdmin && plugin != null) {
      HyperFactions hf = plugin.get();
      if (hf != null && hf.isAdminBypassEnabled(attackerUuid)) {
        return null;
      }
    }
    if (!isAdmin && (PermissionManager.get().hasPermission(attackerUuid, "hyperfactions.bypass.damage")
        || PermissionManager.get().hasPermission(attackerUuid, "hyperfactions.bypass.*"))) {
      return null;
    }

    // Unclaimed — allow
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner == null) {
      return null;
    }

    // Get faction permissions
    Faction ownerFaction = factionManager.getFaction(claimOwner);
    if (ownerFaction == null) {
      return null;
    }
    FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
        ownerFaction.getEffectivePermissions()
    );

    // Resolve attacker's level relative to claiming faction
    UUID attackerFactionId = factionManager.getPlayerFactionId(attackerUuid);
    String level;
    if (attackerFactionId != null && attackerFactionId.equals(claimOwner)) {
      FactionMember member = ownerFaction.getMember(attackerUuid);
      if (member != null && member.role().getLevel() >= FactionRole.OFFICER.getLevel()) {
        level = "officer";
      } else {
        level = "member";
      }
    } else if (attackerFactionId != null) {
      RelationType relation = relationManager.getRelation(attackerFactionId, claimOwner);
      level = (relation == RelationType.ALLY) ? "ally" : "outsider";
    } else {
      level = "outsider";
    }

    if (!checkPermission(perms, level, InteractionType.PVE_DAMAGE)) {
      PlayerRef attackerRef = lookupPlayerRef(attackerUuid);
      return HFMessages.get(attackerRef, CommonKeys.Protection.PVE_TERRITORY_DENIED);
    }
    return null;
  }

  // === World-Level Mixin Checks (no player UUID) ===

  /**
   * Checks if explosions should be blocked at a location.
   * Called by mixin hooks for world-level explosion events.
   *
   * @param worldName the world name
   * @param x         the block X coordinate
   * @param y         the block Y coordinate
   * @param z         the block Z coordinate
   * @return true if explosion should be BLOCKED
   */
  public boolean shouldBlockExplosion(@NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flag
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      boolean allowed = zone.getEffectiveFlag(ZoneFlags.EXPLOSION_DAMAGE);
      if (!allowed) {
        return true;
      }
      return false;
    }

    // Check faction claims — block explosions unless all 3 explosion sources are allowed.
    // Explosion hooks lack player UUID (mixin receives only world coords), so we can't
    // determine the source faction. We block if ANY source type is denied.
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner != null) {
      ConfigManager config = ConfigManager.get();
      return !(config.isFactionlessExplosionsAllowed()
          && config.isEnemyExplosionsAllowed()
          && config.isNeutralExplosionsAllowed());
    }

    return false; // Wilderness — allow
  }

  /**
   * Checks if fire spread should be blocked at a location.
   * Called by mixin hooks for world-level fire spread events.
   *
   * @param worldName the world name
   * @param x         the block X coordinate
   * @param y         the block Y coordinate
   * @param z         the block Z coordinate
   * @return true if fire spread should be BLOCKED
   */
  public boolean shouldBlockFireSpread(@NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flag
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      boolean allowed = zone.getEffectiveFlag(ZoneFlags.FIRE_SPREAD);
      if (!allowed) {
        return true;
      }
      return false;
    }

    // Check faction claims — block fire spread in claimed territory unless config allows
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner != null) {
      return !ConfigManager.get().isFireSpreadAllowed();
    }

    return false; // Wilderness — allow
  }

  /**
   * Checks if fluid spread (water/lava) should be blocked at a location.
   * Only blocks in zones with FIRE_SPREAD disabled. Faction claims always allow
   * fluid spread — unlike fire, fluid is intentionally placed by players and
   * blocking it prevents water/lava from working in own territory.
   *
   * @param worldName the world name
   * @param x         the block X coordinate
   * @param y         the block Y coordinate
   * @param z         the block Z coordinate
   * @return true if fluid spread should be BLOCKED
   */
  public boolean shouldBlockFluidSpread(@NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flag — zones can explicitly block fluid spread
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      return !zone.getEffectiveFlag(ZoneFlags.FIRE_SPREAD);
    }

    // Faction claims — always allow fluid spread (players place water/lava intentionally)
    return false;
  }

  // === Player-Level Mixin Checks (boolean return) ===

  /**
   * Checks if a player should keep inventory on death.
   * Called by mixin hooks for death loot events.
   *
   * @return true if inventory should be KEPT (drop blocked)
   */
  public boolean shouldKeepInventory(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flag
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      return zone.getEffectiveFlag(ZoneFlags.KEEP_INVENTORY);
    }

    // In claims, default: don't keep inventory (deaths have consequences)
    return false;
  }

  /**
   * Checks if durability loss should be prevented.
   * Called by mixin hooks for item wear events.
   *
   * @return true if durability loss should be PREVENTED
   */
  public boolean shouldPreventDurability(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flag
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      return zone.getEffectiveFlag(ZoneFlags.INVINCIBLE_ITEMS);
    }

    // In claims, default: don't prevent durability loss
    return false;
  }

  // === Command Protection ===

  /**
   * Checks if a command should be blocked for a player.
   * Called by mixin hooks for command interception.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name (may be empty for position-less checks)
   * @param x          the player's X coordinate
   * @param y          the player's Y coordinate
   * @param z          the player's Z coordinate
   * @param command    the command being executed
   * @return result containing whether to block and denial message
   */
  @NotNull
  public OrbisMixinsIntegration.CommandCheckResult checkCommandBlock(
      @NotNull UUID playerUuid, @NotNull String worldName,
      int x, int y, int z, @NotNull String command) {
    // Admin bypass
    if (plugin != null) {
      HyperFactions hf = plugin.get();
      if (hf != null && hf.isAdminBypassEnabled(playerUuid)) {
        return OrbisMixinsIntegration.CommandCheckResult.allow();
      }
    }

    // Bypass permission
    if (PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.command")
      || PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
      return OrbisMixinsIntegration.CommandCheckResult.allow();
    }

    // Check zone-based command blocking
    if (!worldName.isEmpty()) {
      int chunkX = ChunkUtil.toChunkCoord(x);
      int chunkZ = ChunkUtil.toChunkCoord(z);
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null) {
        // Zones can define blocked commands via config (future expansion)
        // For now, no zone-level command blocking
      }
    }

    // Check combat tag command blocking (same rules as PlayerListener.onCommandPreprocess)
    if (combatTagManager.isTagged(playerUuid)) {
      String lowerCmd = command.toLowerCase();
      if (lowerCmd.startsWith("/f home") || lowerCmd.startsWith("/faction home")
        || lowerCmd.startsWith("/home") || lowerCmd.startsWith("/spawn")
        || lowerCmd.startsWith("/tp") || lowerCmd.startsWith("/tpa")) {
        return OrbisMixinsIntegration.CommandCheckResult.deny(
          HFMessages.get(lookupPlayerRef(playerUuid), CommonKeys.Protection.COMBAT_TAG_COMMAND));
      }
    }

    return OrbisMixinsIntegration.CommandCheckResult.allow();
  }

  // === Respawn Override ===

  /**
   * Gets a custom respawn location override.
   * Called by mixin hooks for respawn interception.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name where the player died
   * @param x          the death X coordinate
   * @param y          the death Y coordinate
   * @param z          the death Z coordinate
   * @return double[3] with [x, y, z] to override respawn location, or null for default
   */
  @Nullable
  public double[] getRespawnOverride(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check faction home — if player dies in own faction territory, respawn at faction home
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner != null) {
      UUID playerFactionId = factionManager.getPlayerFactionId(playerUuid);
      if (playerFactionId != null && playerFactionId.equals(claimOwner)) {
        Faction faction = factionManager.getFaction(claimOwner);
        if (faction != null && faction.hasHome()) {
          var home = faction.home();
          return new double[] { home.x(), home.y(), home.z() };
        }
      }
    }

    return null; // Use default respawn
  }

  // === Convenience Boolean Methods (for OG hook lambdas) ===

  /**
   * Checks if a player can build at block coordinates.
   *
   * @return true if allowed
   */
  public boolean canBuildAt(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkBuild(playerUuid, worldName, x, y, z) == null;
  }

  /**
   * Checks if a player can interact (use) at block coordinates.
   *
   * @return true if allowed
   */
  public boolean canInteractAt(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkUse(playerUuid, worldName, x, y, z) == null;
  }

  /**
   * Checks if a player can sit at block coordinates.
   *
   * @return true if allowed
   */
  public boolean canSeatAt(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkSeat(playerUuid, worldName, x, y, z) == null;
  }

  /**
   * Checks if a player can place blocks at block coordinates.
   *
   * @return true if allowed
   */
  public boolean canPlaceAt(@NotNull UUID playerUuid, @NotNull String worldName, int x, int y, int z) {
    return checkPlace(playerUuid, worldName, x, y, z) == null;
  }

  // === Gravestone Integration ===

  /**
   * Sets the gravestone integration bridge.
   *
   * @param integration the gravestone integration (may be null if plugin not available)
   */
  public void setGravestoneIntegration(@Nullable GravestoneIntegration integration) {
    this.gravestoneIntegration = integration;
  }

  /**
   * Gets the gravestone integration bridge.
   *
   * @return the gravestone integration, or null if not set
   */
  @Nullable
  public GravestoneIntegration getGravestoneIntegration() {
    return gravestoneIntegration;
  }

  // === Zone Damage Flags ===

  /**
   * Checks if a specific damage type is allowed at a location based on zone flags.
   *
   * @param world  the world name
   * @param x      the X coordinate
   * @param z      the Z coordinate
   * @param flagName the zone flag to check
   * @return true if allowed, false if blocked by zone flag
   */
  public boolean isDamageAllowed(@NotNull String world, double x, double z, @NotNull String flagName) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);
    return isDamageAllowedChunk(world, chunkX, chunkZ, flagName);
  }

  /**
   * Checks if a specific damage type is allowed in a chunk based on zone flags.
   *
   * @param world    the world name
   * @param chunkX   the chunk X
   * @param chunkZ   the chunk Z
   * @param flagName the zone flag to check
   * @return true if allowed, false if blocked by zone flag
   */
  public boolean isDamageAllowedChunk(@NotNull String world, int chunkX, int chunkZ, @NotNull String flagName) {
    Zone zone = zoneManager.getZone(world, chunkX, chunkZ);
    if (zone == null) {
      // Not in a zone - all damage allowed
      return true;
    }

    boolean allowed = zone.getEffectiveFlag(flagName);
    Logger.debug("[Protection] Zone '%s' (%s) flag '%s' = %s at %s/%d/%d",
      zone.name(), zone.type().name(), flagName, allowed, world, chunkX, chunkZ);
    return allowed;
  }

  /**
   * Checks if mob damage is allowed at a location.
   *
   * @param world the world name
   * @param x     the X coordinate
   * @param z     the Z coordinate
   * @return true if mobs can damage players
   */
  public boolean isMobDamageAllowed(@NotNull String world, double x, double z) {
    return isDamageAllowed(world, x, z, ZoneFlags.MOB_DAMAGE);
  }

  /**
   * Checks if projectile damage is allowed at a location.
   *
   * @param world the world name
   * @param x     the X coordinate
   * @param z     the Z coordinate
   * @return true if projectiles can damage
   */
  public boolean isProjectileDamageAllowed(@NotNull String world, double x, double z) {
    return isDamageAllowed(world, x, z, ZoneFlags.PROJECTILE_DAMAGE);
  }

  /**
   * Checks if fall damage is allowed at a location.
   *
   * @param world the world name
   * @param x     the X coordinate
   * @param z     the Z coordinate
   * @return true if fall damage applies
   */
  public boolean isFallDamageAllowed(@NotNull String world, double x, double z) {
    return isDamageAllowed(world, x, z, ZoneFlags.FALL_DAMAGE);
  }

  /**
   * Checks if environmental damage is allowed at a location.
   *
   * @param world the world name
   * @param x     the X coordinate
   * @param z     the Z coordinate
   * @return true if environmental damage applies
   */
  public boolean isEnvironmentalDamageAllowed(@NotNull String world, double x, double z) {
    return isDamageAllowed(world, x, z, ZoneFlags.ENVIRONMENTAL_DAMAGE);
  }

  /**
   * Gets the location description for action bar display.
   *
   * @param world  the world name
   * @param chunkX the chunk X
   * @param chunkZ the chunk Z
   * @param viewerFactionId the viewer's faction ID (nullable)
   * @return the formatted location description
   */
  @NotNull
  public String getLocationDescription(@NotNull String world, int chunkX, int chunkZ,
                    @Nullable UUID viewerFactionId) {
    // Check zone first
    if (zoneManager.isInSafeZone(world, chunkX, chunkZ)) {
      return "\u00A7a\u00A7lSafeZone";
    }
    if (zoneManager.isInWarZone(world, chunkX, chunkZ)) {
      return "\u00A7c\u00A7lWarZone";
    }

    // Check claim
    UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);
    if (claimOwner == null) {
      return "\u00A78Wilderness";
    }

    Faction ownerFaction = factionManager.getFaction(claimOwner);
    if (ownerFaction == null) {
      return "\u00A78Wilderness";
    }

    // Determine relation color
    String color = "\u00A77"; // Neutral default
    if (viewerFactionId != null) {
      if (viewerFactionId.equals(claimOwner)) {
        color = "\u00A7b"; // Cyan for own
      } else {
        RelationType relation = relationManager.getRelation(viewerFactionId, claimOwner);
        color = switch (relation) {
          case OWN -> "\u00A7b";    // Cyan (shouldn't happen via getRelation, but handle for completeness)
          case ALLY -> "\u00A7a";   // Green
          case ENEMY -> "\u00A7c"; // Red
          case NEUTRAL -> "\u00A77"; // Gray
        };
      }
    }

    return color + ownerFaction.name();
  }

  // === Spawn Protection (via OrbisGuard-Mixins hook) ===

  /**
   * Checks if NPC spawning should be blocked at a location.
   * Called by OrbisGuard-Mixins spawn hook.
   *
   * <p>Note: The mixin hook doesn't pass NPC type, so this can only do a blanket
   * check. For type-specific spawn control, use the native SpawnSuppressionController.
   *
   * @param worldName the world name
   * @param x         the spawn X coordinate
   * @param y         the spawn Y coordinate
   * @param z         the spawn Z coordinate
   * @return true if spawn should be BLOCKED, false if allowed
   */
  public boolean shouldBlockSpawn(@NotNull String worldName, int x, int y, int z) {
    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // Check zone flags
    // Note: MOB_SPAWNING is NOT checked here — natural mob spawning is handled
    // by SpawnSuppressionController at the chunk level. This mixin hook only
    // checks NPC_SPAWNING to control admin-placed/command-spawned NPCs.
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      boolean npcSpawningAllowed = zone.getEffectiveFlag(ZoneFlags.NPC_SPAWNING);

      if (!npcSpawningAllowed) {
        Logger.debugSpawning("[Protection] Spawn BLOCKED in zone '%s' at chunk (%d,%d) (npcSpawning=false)",
          zone.name(), chunkX, chunkZ);
        return true;
      }
      return false;
    }

    // Check faction claims — use faction permissions for mob spawning
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner != null) {
      Faction ownerFaction = factionManager.getFaction(claimOwner);
      if (ownerFaction != null) {
        FactionPermissions perms = ConfigManager.get().getEffectiveFactionPermissions(
          ownerFaction.getEffectivePermissions()
        );
        if (!perms.get(FactionPermissions.MOB_SPAWNING)) {
          Logger.debugSpawning("[Protection] Spawn BLOCKED in faction claim at chunk (%d,%d) (mobSpawning=false)",
            chunkX, chunkZ);
          return true;
        }

        // mobSpawning is true — sub-type control handled by SpawnSuppressionManager
        return false;
      }

      // No faction data — block by default
      Logger.debugSpawning("[Protection] Spawn BLOCKED in faction claim at chunk (%d,%d) (no faction data)",
        chunkX, chunkZ);
      return true;
    }

    // Wilderness - allow spawn
    return false;
  }

  /**
   * Enhanced spawn check with mob type information.
   * Called by HP 1.2.0+ enhanced spawn hooks.
   *
   * <p>Currently delegates to the position-only check — mob type filtering
   * can be added later via SpawnSuppressionManager integration.
   *
   * @param worldName the world name
   * @param npcType   the NPC type name (e.g. "ZombieSoldier")
   * @param x         the spawn X coordinate
   * @param y         the spawn Y coordinate
   * @param z         the spawn Z coordinate
   * @return true if spawn should be BLOCKED
   */
  public boolean shouldBlockSpawn(@NotNull String worldName, @Nullable String npcType,
                  int x, int y, int z) {
    // Delegate to position-only check. Mob type filtering can be added later
    // by checking npcType against per-faction allowed/denied mob lists.
    return shouldBlockSpawn(worldName, x, y, z);
  }

  // === Map Marker Visibility ===

  /**
   * Determines if a player's world map marker should be hidden from another player.
   * Based on faction relationships and config settings.
   *
   * <p>Hiding is controlled by two layers:
   * <ul>
   *   <li>Global config: {@code worldmap.json playerVisibility} settings</li>
   *   <li>Zone override: {@code show_on_map} + {@code map_visibility} zone flag (when true, disables hiding in that zone)</li>
   * </ul>
   *
   * @param viewer    the UUID of the player viewing the map
   * @param target    the UUID of the player being shown on the map
   * @param worldName the world name
   * @param x         the target's X coordinate
   * @param z         the target's Z coordinate
   * @return true if the marker should be HIDDEN
   */
  public boolean shouldHideMapMarker(@NotNull UUID viewer, @NotNull UUID target,
                    @NotNull String worldName, int x, int z) {
    try {
      // Use worldmap.json playerVisibility config (same as native player filtering)
      var wmConfig = ConfigManager.get().worldMap();
      if (!wmConfig.isPlayerVisibilityEnabled()) {
        return false; // Feature disabled — show all (vanilla)
      }

      UUID viewerFactionId = factionManager.getPlayerFactionId(viewer);
      UUID targetFactionId = factionManager.getPlayerFactionId(target);

      // Both factionless
      if (viewerFactionId == null && targetFactionId == null) {
        return !wmConfig.isShowFactionlessToFactionless();
      }
      // Viewer factionless, target in faction
      if (viewerFactionId == null) {
        return true; // Hide
      }
      // Target factionless
      if (targetFactionId == null) {
        return !wmConfig.isShowFactionlessPlayers();
      }
      // Same faction
      if (viewerFactionId.equals(targetFactionId)) {
        return !wmConfig.isShowOwnFaction();
      }

      // Check relation
      RelationType relation = relationManager.getRelation(viewerFactionId, targetFactionId);
      boolean shouldHide = switch (relation) {
        case ALLY, OWN -> !wmConfig.isShowAllies();
        case ENEMY -> !wmConfig.isShowEnemies();
        case NEUTRAL -> !wmConfig.isShowNeutrals();
      };

      if (!shouldHide) {
        return false;
      }

      // Check zone override at target's position
      int chunkX = ChunkUtil.toChunkCoord(x);
      int chunkZ = ChunkUtil.toChunkCoord(z);
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null && zone.getEffectiveFlag(ZoneFlags.SHOW_ON_MAP)) {
        String visibility = zone.getEffectiveSetting(ZoneFlags.MAP_VISIBILITY);
        if (ZoneFlags.MAP_VISIBILITY_ALL.equals(visibility)) {
          return false; // Show everyone
        }
        if (ZoneFlags.MAP_VISIBILITY_ALLY.equals(visibility) && relation == RelationType.ALLY) {
          return false; // Show ally markers
        }
      }

      return true; // Hide based on config + relation
    } catch (Exception e) {
      ErrorHandler.report("Map marker visibility check error (fail-open)", e);
      return false;
    }
  }

  /**
   * Determines if a shared map marker should be hidden from a player.
   * Based on faction relationships between the viewer and the marker creator.
   *
   * <p>Hiding is controlled by two layers:
   * <ul>
   *   <li>Global config: {@code worldmap.json playerVisibility} settings</li>
   *   <li>Zone override: {@code show_on_map} + {@code map_visibility} zone flag (when true, shows all markers)</li>
   * </ul>
   *
   * @param viewer    the UUID of the player viewing the map
   * @param creatorId the UUID of the player who placed the marker (may be null)
   * @param worldName the world name
   * @param markerX   the marker's X coordinate
   * @param markerZ   the marker's Z coordinate
   * @return true if the marker should be HIDDEN
   */
  public boolean shouldHideSharedMarker(@NotNull UUID viewer, @Nullable UUID creatorId,
                      @NotNull String worldName, float markerX, float markerZ) {
    try {
      if (creatorId == null) {
        return false; // Unknown creator — show (fail-open)
      }
      if (viewer.equals(creatorId)) {
        return false; // Own marker — always show
      }

      // Use worldmap.json playerVisibility config (same as player icon filtering)
      var wmConfig = ConfigManager.get().worldMap();
      if (!wmConfig.isPlayerVisibilityEnabled()) {
        return false; // Feature disabled — show all (vanilla)
      }

      UUID viewerFactionId = factionManager.getPlayerFactionId(viewer);
      UUID creatorFactionId = factionManager.getPlayerFactionId(creatorId);

      // Both factionless
      if (viewerFactionId == null && creatorFactionId == null) {
        return !wmConfig.isShowFactionlessToFactionless();
      }
      // Viewer factionless, creator in faction
      if (viewerFactionId == null) {
        return true; // Hide
      }
      // Creator factionless
      if (creatorFactionId == null) {
        return !wmConfig.isShowFactionlessPlayers();
      }
      // Same faction
      if (viewerFactionId.equals(creatorFactionId)) {
        return !wmConfig.isShowOwnFaction();
      }

      // Check relation
      RelationType relation = relationManager.getRelation(viewerFactionId, creatorFactionId);
      boolean shouldHide = switch (relation) {
        case ALLY, OWN -> !wmConfig.isShowAllies();
        case ENEMY -> !wmConfig.isShowEnemies();
        case NEUTRAL -> !wmConfig.isShowNeutrals();
      };

      if (!shouldHide) {
        return false;
      }

      // Check zone override at marker's position
      int chunkX = ChunkUtil.toChunkCoord((int) markerX);
      int chunkZ = ChunkUtil.toChunkCoord((int) markerZ);
      Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
      if (zone != null && zone.getEffectiveFlag(ZoneFlags.SHOW_ON_MAP)) {
        String visibility = zone.getEffectiveSetting(ZoneFlags.MAP_VISIBILITY);
        if (ZoneFlags.MAP_VISIBILITY_ALL.equals(visibility)) {
          return false; // Show all markers
        }
        if (ZoneFlags.MAP_VISIBILITY_ALLY.equals(visibility) && relation == RelationType.ALLY) {
          return false; // Show ally markers
        }
      }

      return true;
    } catch (Exception e) {
      ErrorHandler.report("Shared marker visibility check error (fail-open)", e);
      return false;
    }
  }
}
