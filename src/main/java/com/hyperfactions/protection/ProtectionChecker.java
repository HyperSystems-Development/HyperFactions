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
import com.hyperfactions.integration.protection.GravestoneIntegration;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.*;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

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

    public ProtectionChecker(
        @NotNull FactionManager factionManager,
        @NotNull ClaimManager claimManager,
        @NotNull ZoneManager zoneManager,
        @NotNull RelationManager relationManager,
        @NotNull CombatTagManager combatTagManager
    ) {
        this(null, factionManager, claimManager, zoneManager, relationManager, combatTagManager);
    }

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
        DAMAGE,      // Damage entities (not players)
        USE          // Use items (fallback)
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
                case INTERACT, DOOR, BENCH, PROCESSING, SEAT -> "hyperfactions.bypass.interact";
                case CONTAINER -> "hyperfactions.bypass.container";
                case DAMAGE -> "hyperfactions.bypass.damage";
                case USE -> "hyperfactions.bypass.use";
            };

            if (PermissionManager.get().hasPermission(playerUuid, bypassPerm) ||
                PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
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
                case DAMAGE -> ZoneFlags.PVP_ENABLED;
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

        // 3. Check claim owner
        UUID claimOwner = claimManager.getClaimOwner(world, chunkX, chunkZ);

        if (claimOwner == null) {
            // Wilderness - anyone can interact
            return ProtectionResult.ALLOWED_WILDERNESS;
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
            boolean isOfficerOrLeader = factionMember != null &&
                factionMember.role().getLevel() >= FactionRole.OFFICER.getLevel();

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
            case DAMAGE -> !"outsider".equals(level); // outsiders can't damage
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
            // Zone has PvP enabled - check friendly fire if in zone
            boolean friendlyFireAllowed = zone.getEffectiveFlag(ZoneFlags.FRIENDLY_FIRE);

            // Check same faction
            if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
                if (!friendlyFireAllowed && !config.isFactionDamage()) {
                    return PvPResult.DENIED_SAME_FACTION;
                }
            }

            // Check ally
            RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
            if (relation == RelationType.ALLY) {
                if (!friendlyFireAllowed && !config.isAllyDamage()) {
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

        // 3. Check same faction
        if (factionManager.areInSameFaction(attackerUuid, defenderUuid)) {
            if (!config.isFactionDamage()) {
                return PvPResult.DENIED_SAME_FACTION;
            }
        }

        // 4. Check ally
        RelationType relation = relationManager.getPlayerRelation(attackerUuid, defenderUuid);
        if (relation == RelationType.ALLY) {
            if (!config.isAllyDamage()) {
                Logger.debugProtection("PvP denied: attacker=%s, defender=%s, chunk=%s/%d/%d, result=ALLY",
                    attackerUuid, defenderUuid, world, chunkX, chunkZ);
                return PvPResult.DENIED_ALLY;
            }
        }

        // 5. Default: allow PvP
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
     * This is for native ECS events (InteractivelyPickupItemEvent) and always
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
     * This is called by OrbisGuard-Mixins hook for F-key and auto pickup events.
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
        if (PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.pickup") ||
            PermissionManager.get().hasPermission(playerUuid, "hyperfactions.bypass.*")) {
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

        // 8. Check outsider pickup flag on the owning faction
        // For now, deny pickup in enemy/neutral territory by default
        // This could be made configurable via faction permissions in the future
        Logger.debug("[Pickup:%s] Blocked in other faction's territory for %s at %s/%d/%d",
                mode, playerUuid, worldName, chunkX, chunkZ);
        return false;
    }

    /**
     * Checks if a protection result is "allowed".
     *
     * @param result the result
     * @return true if allowed
     */
    public boolean isAllowed(@NotNull ProtectionResult result) {
        return switch (result) {
            case ALLOWED, ALLOWED_BYPASS, ALLOWED_WILDERNESS,
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
     * Gets a user-friendly denial message.
     *
     * @param result the protection result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull ProtectionResult result) {
        return switch (result) {
            case DENIED_SAFEZONE -> "You cannot do that in a SafeZone.";
            case DENIED_WARZONE -> "You cannot do that in a WarZone.";
            case DENIED_ENEMY_CLAIM -> "You cannot do that in enemy territory.";
            case DENIED_NEUTRAL_CLAIM -> "You cannot do that in claimed territory.";
            case DENIED_NO_PERMISSION -> "You don't have permission to do that.";
            default -> "You cannot do that here.";
        };
    }

    /**
     * Gets a user-friendly PvP denial message.
     *
     * @param result the PvP result
     * @return the denial message
     */
    @NotNull
    public String getDenialMessage(@NotNull PvPResult result) {
        return switch (result) {
            case DENIED_SAFEZONE -> "PvP is disabled in SafeZones.";
            case DENIED_SAME_FACTION -> "You cannot attack faction members.";
            case DENIED_ALLY -> "You cannot attack allies.";
            case DENIED_ATTACKER_SAFEZONE, DENIED_DEFENDER_SAFEZONE -> "PvP is disabled in SafeZones.";
            case DENIED_SPAWN_PROTECTED -> "That player has spawn protection.";
            case DENIED_TERRITORY_NO_PVP -> "PvP is disabled in this territory.";
            default -> "You cannot attack this player.";
        };
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
     * Note: The mixin hook doesn't pass NPC type, so this can only do a blanket
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
        Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
        if (zone != null) {
            boolean mobSpawningAllowed = zone.getEffectiveFlag(ZoneFlags.MOB_SPAWNING);
            boolean npcSpawningAllowed = zone.getEffectiveFlag(ZoneFlags.NPC_SPAWNING);

            if (!mobSpawningAllowed || !npcSpawningAllowed) {
                Logger.debugSpawning("[Protection] Spawn BLOCKED in zone '%s' at chunk (%d,%d)",
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
}
