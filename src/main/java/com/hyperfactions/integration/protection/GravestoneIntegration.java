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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * Reflection-based integration with Zurku's GravestonePlugin (v2).
 *
 * <p>
 * Registers a faction-aware access checker that applies faction-aware access rules
 * (territory, relations, zones) before the gravestone plugin's built-in ownership check.
 * Also registers event listeners for gravestone lifecycle events.
 *
 * <p>
 * All GravestonePlugin classes are loaded via reflection so HyperFactions compiles
 * and runs without the GravestonePlugin JAR present.
 */
public class GravestoneIntegration {

  private boolean available = false;

  // Cached reflection handles
  private Object gravestoneManager;

  private Method getGravestoneOwnerMethod;

  // AccessResult enum constants
  private Object ALLOW;

  private Object DENY;

  private Object DEFER;

  /**
   * Initializes the GravestonePlugin integration using reflection.
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
      // Load all required classes
      Class<?> pluginClass = Class.forName("zurku.gravestones.GravestonePlugin");
      Class<?> managerClass = Class.forName("zurku.gravestones.GravestoneManager");
      Class<?> checkerClass = Class.forName("zurku.gravestones.GravestoneAccessChecker");
      Class<?> resultClass = Class.forName("zurku.gravestones.GravestoneAccessChecker$AccessResult");

      // Cache AccessResult enum constants
      ALLOW = Enum.valueOf(resultClass.asSubclass(Enum.class), "ALLOW");
      DENY = Enum.valueOf(resultClass.asSubclass(Enum.class), "DENY");
      DEFER = Enum.valueOf(resultClass.asSubclass(Enum.class), "DEFER");

      // Get plugin instance
      Method getInstanceMethod = pluginClass.getMethod("getInstance");
      Object plugin = getInstanceMethod.invoke(null);
      if (plugin == null) {
        Logger.debugIntegration("[Gravestone] GravestonePlugin not loaded");
        return;
      }

      // Get gravestone manager
      Method getManagerMethod = pluginClass.getMethod("getGravestoneManager");
      gravestoneManager = getManagerMethod.invoke(plugin);
      if (gravestoneManager == null) {
        Logger.debugIntegration("[Gravestone] GravestoneManager not available");
        return;
      }

      // Cache getGravestoneOwner for later use
      getGravestoneOwnerMethod = managerClass.getMethod("getGravestoneOwner", int.class, int.class, int.class);

      // Register faction-aware access checker via dynamic proxy
      registerAccessChecker(managerClass, checkerClass, hyperFactions);

      // Register event listeners for logging
      registerEventListeners(eventRegistry);

      available = true;
      Logger.info("[Integration] GravestonePlugin v2 API detected — direct integration enabled");

    } catch (LinkageError e) {
      // NoClassDefFoundError = plugin not installed; NoSuchMethodError = old version without API
      Logger.info("[Integration] GravestonePlugin not available — gravestone integration disabled");
    } catch (ReflectiveOperationException e) {
      Logger.info("[Integration] GravestonePlugin reflection failed — gravestone integration disabled");
      Logger.debugIntegration("[Gravestone] Reflection error: %s", e.getMessage());
    }
  }

  /**
   * Registers the faction-aware access checker with the gravestone plugin using a dynamic proxy.
   * The proxy implements GravestoneAccessChecker and applies HyperFactions protection rules
   * (admin bypass, permissions, zone flags, territory context, relations) to gravestone access.
   */
  private void registerAccessChecker(Class<?> managerClass, Class<?> checkerClass,
                   Supplier<HyperFactions> hyperFactionsSupplier)
      throws ReflectiveOperationException {

    InvocationHandler handler = (proxy, method, args) -> {
      // The functional interface method: check(UUID accessor, UUID owner, int x, int y, int z, String world)
      if (!method.getName().equals("check")) {
        // Handle Object methods (toString, equals, hashCode)
        if (method.getName().equals("toString")) {
          return "HyperFactions-GravestoneAccessChecker";
        }
        if (method.getName().equals("hashCode")) {
          return System.identityHashCode(proxy);
        }
        if (method.getName().equals("equals")) {
          return proxy == args[0];
        }
        return null;
      }

      UUID accessorUuid = (UUID) args[0];
      UUID ownerUuid = (UUID) args[1];
      int x = (int) args[2];
      int y = (int) args[3];
      int z = (int) args[4];
      String worldName = (String) args[5];

      return evaluateAccess(hyperFactionsSupplier, accessorUuid, ownerUuid, x, y, z, worldName);
    };

    Object accessChecker = Proxy.newProxyInstance(
        checkerClass.getClassLoader(),
        new Class<?>[]{checkerClass},
        handler
    );

    Method setCheckerMethod = managerClass.getMethod("setAccessChecker", checkerClass);
    setCheckerMethod.invoke(gravestoneManager, accessChecker);
  }

  /**
   * Evaluates gravestone access using faction rules. Returns an AccessResult enum constant.
   */
  private Object evaluateAccess(Supplier<HyperFactions> hyperFactionsSupplier,
                 UUID accessorUuid, @Nullable UUID ownerUuid,
                 int x, int y, int z, String worldName) {
    GravestoneConfig config = ConfigManager.get().gravestones();
    if (!config.isEnabled()) {
      return DEFER;
    }

    HyperFactions hyperFactions = hyperFactionsSupplier.get();
    if (hyperFactions == null) {
      return DEFER;
    }

    FactionManager factionManager = hyperFactions.getFactionManager();
    ClaimManager claimManager = hyperFactions.getClaimManager();
    ZoneManager zoneManager = hyperFactions.getZoneManager();
    RelationManager relationManager = hyperFactions.getRelationManager();

    // 1. Admin bypass — ONLY if bypass toggle is ON
    boolean isAdmin = PermissionManager.get().hasPermission(accessorUuid, "hyperfactions.admin.use");
    if (isAdmin && hyperFactions.isAdminBypassEnabled(accessorUuid)) {
      Logger.debugIntegration("[Gravestone] Admin bypass for %s", accessorUuid);
      return ALLOW;
    }

    // 2. Non-admin permission bypass — admins with toggle OFF do NOT get this
    if (!isAdmin && PermissionManager.get().hasPermission(accessorUuid, "hyperfactions.gravestone.bypass")) {
      Logger.debugIntegration("[Gravestone] Permission bypass for %s", accessorUuid);
      return ALLOW;
    }

    // 3. Owner accessing own → DEFER (let gravestone's built-in check handle it)
    if (ownerUuid != null && accessorUuid.equals(ownerUuid)) {
      return DEFER;
    }
    if (ownerUuid == null) {
      return DEFER;
    }

    int chunkX = ChunkUtil.toChunkCoord(x);
    int chunkZ = ChunkUtil.toChunkCoord(z);

    // 4. Zone flag check (overrides territory settings)
    Zone zone = zoneManager.getZone(worldName, chunkX, chunkZ);
    if (zone != null) {
      boolean zoneAllows = zone.getEffectiveFlag(ZoneFlags.GRAVESTONE_ACCESS);
      Logger.debugIntegration("[Gravestone] Zone '%s' flag=%s for %s",
          zone.name(), zoneAllows, accessorUuid);
      return zoneAllows ? ALLOW : DENY;
    }

    // 5. Territory + relation checks
    UUID claimOwner = claimManager.getClaimOwner(worldName, chunkX, chunkZ);
    if (claimOwner == null) {
      // Wilderness
      boolean blocked = config.isProtectInWilderness();
      Logger.debugIntegration("[Gravestone] Wilderness: accessor=%s, blocked=%s",
          accessorUuid, blocked);
      return blocked ? DENY : ALLOW;
    }

    // Determine relation context
    UUID accessorFactionId = factionManager.getPlayerFactionId(accessorUuid);
    UUID ownerFactionId = factionManager.getPlayerFactionId(ownerUuid);

    if (accessorFactionId != null && accessorFactionId.equals(claimOwner)) {
      // Accessor is in the faction that owns this territory
      if (!config.isProtectInOwnTerritory()) {
        return ALLOW;
      }
      if (ownerFactionId != null && ownerFactionId.equals(claimOwner)) {
        // Same faction — check factionMembersCanAccess
        boolean allowed = config.isFactionMembersCanAccess();
        Logger.debugIntegration("[Gravestone] Same faction: accessor=%s, owner=%s, allowed=%s",
            accessorUuid, ownerUuid, allowed);
        return allowed ? ALLOW : DENY;
      }

      // Outsider's gravestone in our territory
      return ALLOW;
    }

    // Check relation between accessor's faction and territory owner
    if (accessorFactionId != null) {
      RelationType relation = relationManager.getRelation(accessorFactionId, claimOwner);
      if (relation == RelationType.ALLY) {
        boolean allowed = config.isAlliesCanAccess();
        Logger.debugIntegration("[Gravestone] Ally territory: accessor=%s, allowed=%s",
            accessorUuid, allowed);
        return allowed ? ALLOW : DENY;
      }
      if (relation == RelationType.ENEMY) {
        boolean blocked = config.isProtectInEnemyTerritory();
        Logger.debugIntegration("[Gravestone] Enemy territory: accessor=%s, blocked=%s",
            accessorUuid, blocked);
        return blocked ? DENY : ALLOW;
      }
    }

    // Neutral territory
    boolean blocked = config.isProtectInNeutralTerritory();
    Logger.debugIntegration("[Gravestone] Neutral territory: accessor=%s, blocked=%s",
        accessorUuid, blocked);
    return blocked ? DENY : ALLOW;
  }

  /**
   * Registers listeners for gravestone lifecycle events (for debug logging).
   * Event classes are loaded via reflection.
   */
  @SuppressWarnings("unchecked")
  private void registerEventListeners(EventRegistry eventRegistry) {
    registerEventLogger(eventRegistry,
        "zurku.gravestones.event.GravestoneCreatedEvent",
        event -> {
          try {
            Method getOwner = event.getClass().getMethod("getOwnerUuid");
            Method getX = event.getClass().getMethod("getX");
            Method getY = event.getClass().getMethod("getY");
            Method getZ = event.getClass().getMethod("getZ");
            Method getWorld = event.getClass().getMethod("getWorldName");
            Logger.debugIntegration("[Gravestone] Created: owner=%s at (%d,%d,%d) in %s",
                getOwner.invoke(event), getX.invoke(event), getY.invoke(event),
                getZ.invoke(event), getWorld.invoke(event));
          } catch (Exception e) {
            Logger.debugIntegration("[Gravestone] Failed to log created event: %s", e.getMessage());
          }
        });

    registerEventLogger(eventRegistry,
        "zurku.gravestones.event.GravestoneCollectedEvent",
        event -> {
          try {
            Method getCollector = event.getClass().getMethod("getCollectorUuid");
            Method getOwner = event.getClass().getMethod("getOwnerUuid");
            Method getX = event.getClass().getMethod("getX");
            Method getY = event.getClass().getMethod("getY");
            Method getZ = event.getClass().getMethod("getZ");
            Logger.debugIntegration("[Gravestone] Collected: collector=%s, owner=%s at (%d,%d,%d)",
                getCollector.invoke(event), getOwner.invoke(event),
                getX.invoke(event), getY.invoke(event), getZ.invoke(event));
          } catch (Exception e) {
            Logger.debugIntegration("[Gravestone] Failed to log collected event: %s", e.getMessage());
          }
        });

    registerEventLogger(eventRegistry,
        "zurku.gravestones.event.GravestoneBrokenEvent",
        event -> {
          try {
            Method getBreaker = event.getClass().getMethod("getBreakerUuid");
            Method getOwner = event.getClass().getMethod("getOwnerUuid");
            Method getX = event.getClass().getMethod("getX");
            Method getY = event.getClass().getMethod("getY");
            Method getZ = event.getClass().getMethod("getZ");
            Logger.debugIntegration("[Gravestone] Broken: breaker=%s, owner=%s at (%d,%d,%d)",
                getBreaker.invoke(event), getOwner.invoke(event),
                getX.invoke(event), getY.invoke(event), getZ.invoke(event));
          } catch (Exception e) {
            Logger.debugIntegration("[Gravestone] Failed to log broken event: %s", e.getMessage());
          }
        });
  }

  /**
   * Registers a single event logger for a gravestone event class loaded by name.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void registerEventLogger(EventRegistry eventRegistry, String eventClassName,
                  Consumer<Object> logAction) {
    try {
      Class eventClass = Class.forName(eventClassName);
      eventRegistry.registerGlobal(eventClass, logAction::accept);
    } catch (ClassNotFoundException e) {
      Logger.debugIntegration("[Gravestone] Event class not found: %s", eventClassName);
    }
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
    if (!available || gravestoneManager == null || getGravestoneOwnerMethod == null) {
      return null;
    }
    try {
      return (UUID) getGravestoneOwnerMethod.invoke(gravestoneManager, x, y, z);
    } catch (Exception e) {
      Logger.debugIntegration("[Gravestone] Failed to get gravestone owner at (%d,%d,%d): %s",
          x, y, z, e.getMessage());
      return null;
    }
  }
}
