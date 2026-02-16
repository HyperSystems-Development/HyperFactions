package com.hyperfactions.integration.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.config.modules.GravestoneConfig;
import com.hyperfactions.data.RelationType;
import com.hyperfactions.data.Zone;
import com.hyperfactions.data.ZoneFlags;
import com.hyperfactions.integration.PermissionManager;
import com.hyperfactions.manager.ClaimManager;
import com.hyperfactions.manager.FactionManager;
import com.hyperfactions.manager.RelationManager;
import com.hyperfactions.manager.ZoneManager;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.util.ChunkUtil;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.event.EventRegistry;
import org.jetbrains.annotations.Nullable;
import zurku.gravestones.GravestoneAccessChecker;
import zurku.gravestones.GravestoneAccessChecker.AccessResult;
import zurku.gravestones.GravestoneManager;
import zurku.gravestones.GravestonePlugin;
import zurku.gravestones.event.GravestoneBrokenEvent;
import zurku.gravestones.event.GravestoneCollectedEvent;
import zurku.gravestones.event.GravestoneCreatedEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Direct API integration with Zurku's GravestonePlugin (v2).
 * <p>
 * Registers a {@link GravestoneAccessChecker} that applies faction-aware access rules
 * (territory, relations, zones) before the gravestone plugin's built-in ownership check.
 * Also registers event listeners for gravestone lifecycle events.
 * <p>
 * If GravestonePlugin is not installed, all methods return safe defaults and the
 * integration is marked as unavailable.
 */
public class GravestoneIntegration {

    private boolean available = false;
    private GravestonePlugin gravestonePlugin;
    private GravestoneManager gravestoneManager;

    /**
     * Initializes the GravestonePlugin integration using the direct v2 API.
     * Safe to call even if GravestonePlugin is not installed.
     *
     * @param hyperFactions    the HyperFactions instance (for admin bypass checks)
     * @param protectionChecker the protection checker (for manager access)
     * @param eventRegistry    the event registry for gravestone event listeners
     */
    public void init(Supplier<HyperFactions> hyperFactions,
                     ProtectionChecker protectionChecker,
                     EventRegistry eventRegistry) {
        try {
            gravestonePlugin = GravestonePlugin.getInstance();
            if (gravestonePlugin == null) {
                Logger.debugIntegration("[Gravestone] GravestonePlugin not loaded");
                return;
            }
            gravestoneManager = gravestonePlugin.getGravestoneManager();
            if (gravestoneManager == null) {
                Logger.debugIntegration("[Gravestone] GravestoneManager not available");
                return;
            }

            // Register faction-aware access checker
            registerAccessChecker(hyperFactions);

            // Register event listeners for logging
            registerEventListeners(eventRegistry);

            available = true;
            Logger.info("[Integration] GravestonePlugin v2 API detected — direct integration enabled");

        } catch (LinkageError e) {
            // NoClassDefFoundError = plugin not installed; NoSuchMethodError = old version without API
            Logger.info("[Integration] GravestonePlugin not available — gravestone integration disabled");
        }
    }

    /**
     * Registers the faction-aware {@link GravestoneAccessChecker} with the gravestone plugin.
     * This checker applies HyperFactions protection rules (admin bypass, permissions,
     * zone flags, territory context, relations) to gravestone access attempts.
     */
    private void registerAccessChecker(Supplier<HyperFactions> hyperFactionsSupplier) {
        gravestoneManager.setAccessChecker((accessorUuid, ownerUuid, x, y, z, worldName) -> {
            GravestoneConfig config = ConfigManager.get().gravestones();
            if (!config.isEnabled()) return AccessResult.DEFER;

            HyperFactions hyperFactions = hyperFactionsSupplier.get();
            if (hyperFactions == null) return AccessResult.DEFER;

            FactionManager factionManager = hyperFactions.getFactionManager();
            ClaimManager claimManager = hyperFactions.getClaimManager();
            ZoneManager zoneManager = hyperFactions.getZoneManager();
            RelationManager relationManager = hyperFactions.getRelationManager();

            // 1. Admin bypass — ONLY if bypass toggle is ON
            boolean isAdmin = PermissionManager.get().hasPermission(accessorUuid, "hyperfactions.admin.use");
            if (isAdmin && hyperFactions.isAdminBypassEnabled(accessorUuid)) {
                Logger.debugIntegration("[Gravestone] Admin bypass for %s", accessorUuid);
                return AccessResult.ALLOW;
            }

            // 2. Non-admin permission bypass — admins with toggle OFF do NOT get this
            if (!isAdmin && PermissionManager.get().hasPermission(accessorUuid, "hyperfactions.gravestone.bypass")) {
                Logger.debugIntegration("[Gravestone] Permission bypass for %s", accessorUuid);
                return AccessResult.ALLOW;
            }

            // 3. Owner accessing own → DEFER (let gravestone's built-in check handle it)
            if (ownerUuid != null && accessorUuid.equals(ownerUuid)) return AccessResult.DEFER;
            if (ownerUuid == null) return AccessResult.DEFER;

            int chunkX = ChunkUtil.toChunkCoord(x);
            int chunkZ = ChunkUtil.toChunkCoord(z);

            // 4. Zone flag check (overrides territory settings)
            Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
            if (zone != null) {
                boolean zoneAllows = zone.getEffectiveFlag(ZoneFlags.GRAVESTONE_ACCESS);
                Logger.debugIntegration("[Gravestone] Zone '%s' flag=%s for %s",
                        zone.name(), zoneAllows, accessorUuid);
                return zoneAllows ? AccessResult.ALLOW : AccessResult.DENY;
            }

            // 5. Territory + relation checks
            UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
            if (claimOwner == null) {
                // Wilderness
                boolean blocked = config.isProtectInWilderness();
                Logger.debugIntegration("[Gravestone] Wilderness: accessor=%s, blocked=%s",
                        accessorUuid, blocked);
                return blocked ? AccessResult.DENY : AccessResult.ALLOW;
            }

            // Determine relation context
            UUID accessorFactionId = factionManager.getPlayerFactionId(accessorUuid);
            UUID ownerFactionId = factionManager.getPlayerFactionId(ownerUuid);

            if (accessorFactionId != null && accessorFactionId.equals(claimOwner)) {
                // Accessor is in the faction that owns this territory
                if (!config.isProtectInOwnTerritory()) return AccessResult.ALLOW;
                if (ownerFactionId != null && ownerFactionId.equals(claimOwner)) {
                    // Same faction — check factionMembersCanAccess
                    boolean allowed = config.isFactionMembersCanAccess();
                    Logger.debugIntegration("[Gravestone] Same faction: accessor=%s, owner=%s, allowed=%s",
                            accessorUuid, ownerUuid, allowed);
                    return allowed ? AccessResult.ALLOW : AccessResult.DENY;
                }
                // Outsider's gravestone in our territory
                return AccessResult.ALLOW;
            }

            // Check relation between accessor's faction and territory owner
            if (accessorFactionId != null) {
                RelationType relation = relationManager.getRelation(accessorFactionId, claimOwner);
                if (relation == RelationType.ALLY) {
                    boolean allowed = config.isAlliesCanAccess();
                    Logger.debugIntegration("[Gravestone] Ally territory: accessor=%s, allowed=%s",
                            accessorUuid, allowed);
                    return allowed ? AccessResult.ALLOW : AccessResult.DENY;
                }
                if (relation == RelationType.ENEMY) {
                    boolean blocked = config.isProtectInEnemyTerritory();
                    Logger.debugIntegration("[Gravestone] Enemy territory: accessor=%s, blocked=%s",
                            accessorUuid, blocked);
                    return blocked ? AccessResult.DENY : AccessResult.ALLOW;
                }
            }

            // Neutral territory
            boolean blocked = config.isProtectInNeutralTerritory();
            Logger.debugIntegration("[Gravestone] Neutral territory: accessor=%s, blocked=%s",
                    accessorUuid, blocked);
            return blocked ? AccessResult.DENY : AccessResult.ALLOW;
        });
    }

    /**
     * Registers listeners for gravestone lifecycle events (for debug logging).
     */
    private void registerEventListeners(EventRegistry eventRegistry) {
        eventRegistry.registerGlobal(GravestoneCreatedEvent.class, event ->
            Logger.debugIntegration("[Gravestone] Created: owner=%s at (%d,%d,%d) in %s",
                event.getOwnerUuid(), event.getX(), event.getY(), event.getZ(), event.getWorldName())
        );

        eventRegistry.registerGlobal(GravestoneCollectedEvent.class, event ->
            Logger.debugIntegration("[Gravestone] Collected: collector=%s, owner=%s at (%d,%d,%d)",
                event.getCollectorUuid(), event.getOwnerUuid(),
                event.getX(), event.getY(), event.getZ())
        );

        eventRegistry.registerGlobal(GravestoneBrokenEvent.class, event ->
            Logger.debugIntegration("[Gravestone] Broken: breaker=%s, owner=%s at (%d,%d,%d)",
                event.getBreakerUuid(), event.getOwnerUuid(),
                event.getX(), event.getY(), event.getZ())
        );
    }

    /**
     * Checks if GravestonePlugin is available and integrated.
     *
     * @return true if GravestonePlugin is loaded and accessible
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets the UUID of the player who owns the gravestone at the given coordinates.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return the owner's UUID, or null if no gravestone exists or plugin unavailable
     */
    @Nullable
    public UUID getGravestoneOwner(int x, int y, int z) {
        if (!available || gravestoneManager == null) return null;
        try {
            return gravestoneManager.getGravestoneOwner(x, y, z);
        } catch (Exception e) {
            Logger.debugIntegration("[Gravestone] Failed to get gravestone owner at (%d,%d,%d): %s",
                    x, y, z, e.getMessage());
            return null;
        }
    }
}
