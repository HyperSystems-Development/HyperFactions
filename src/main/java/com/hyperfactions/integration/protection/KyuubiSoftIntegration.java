package com.hyperfactions.integration.protection;

import com.hyperfactions.HyperFactions;
import com.hyperfactions.protection.ProtectionChecker;
import com.hyperfactions.protection.ProtectionMessageDebounce;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Reflection-based integration with KyuubiSoft Core (Citizens NPC system).
 *
 * <p>KyuubiSoft Citizens use their own NPC behavior tree ({@code ActionCitizenInteract})
 * which bypasses Hytale's native {@code PlayerInteractEvent}. This means our standard
 * NPC_INTERACT zone flag check (in {@code NpcInteractionProtectionHandler}) never fires
 * for KyuubiSoft NPCs.
 *
 * <p>This integration registers a {@code CitizenDialogInterceptor} via KyuubiSoft's
 * {@code CoreAPI} that checks zone flags before allowing citizen dialog/shop interactions.
 * The interceptor uses the player's position to look up zone flags and faction territory,
 * blocking the interaction if NPC_INTERACT is denied.
 *
 * <p>All KyuubiSoft classes are loaded via reflection so HyperFactions compiles
 * and runs without the KyuubiSoft Core JAR present.
 */
public class KyuubiSoftIntegration {

  private boolean available = false;

  // Cached reflection handles for cleanup
  private Object coreApiInstance;
  private Method removeInterceptorMethod;
  private Object interceptorProxy;

  /**
   * Initializes the KyuubiSoft Core integration using reflection.
   * Safe to call even if KyuubiSoft Core is not installed.
   *
   * @param hyperFactions    supplier for the HyperFactions instance
   */
  public void init(Supplier<HyperFactions> hyperFactions) {
    try {
      // Load KyuubiSoft API classes
      Class<?> coreApiClass = Class.forName("com.kyuubisoft.core.api.CoreAPI");
      Class<?> interceptorClass = Class.forName("com.kyuubisoft.core.citizen.CitizenDialogInterceptor");

      // Check if CoreAPI is available
      Method isAvailableMethod = coreApiClass.getMethod("isAvailable");
      boolean apiAvailable = (boolean) isAvailableMethod.invoke(null);
      if (!apiAvailable) {
        Logger.info("[Integration] KyuubiSoft Core detected but API not yet initialized");
        return;
      }

      // Get CoreAPI instance
      Method getInstanceMethod = coreApiClass.getMethod("getInstance");
      coreApiInstance = getInstanceMethod.invoke(null);
      if (coreApiInstance == null) {
        Logger.debugIntegration("[KyuubiSoft] CoreAPI instance is null");
        return;
      }

      // Cache removal method for cleanup
      removeInterceptorMethod = coreApiClass.getMethod("removeDialogInterceptor", interceptorClass);

      // Create and register the interceptor proxy
      registerInterceptor(coreApiClass, interceptorClass, hyperFactions);

      available = true;
      Logger.info("[Integration] KyuubiSoft Core detected — citizen zone protection enabled");

    } catch (LinkageError e) {
      // NoClassDefFoundError = mod not installed
      Logger.info("[Integration] KyuubiSoft Core not available — citizen integration disabled");
    } catch (ReflectiveOperationException e) {
      Logger.info("[Integration] KyuubiSoft Core reflection failed — citizen integration disabled");
      Logger.debugIntegration("[KyuubiSoft] Reflection error: %s", e.getMessage());
    }
  }

  /**
   * Registers a CitizenDialogInterceptor proxy that checks zone flags
   * before allowing KyuubiSoft citizen interactions.
   */
  private void registerInterceptor(Class<?> coreApiClass, Class<?> interceptorClass,
                   Supplier<HyperFactions> hyperFactionsSupplier)
      throws ReflectiveOperationException {

    InvocationHandler handler = (proxy, method, args) -> {
      // The functional interface method: interceptDialog(Player, PlayerRef, Ref, Store, String citizenId)
      if ("interceptDialog".equals(method.getName())) {
        return handleInterceptDialog(hyperFactionsSupplier, (Player) args[0]);
      }

      // Handle Object methods
      if ("toString".equals(method.getName())) {
        return "HyperFactions-KyuubiSoftInterceptor";
      }
      if ("hashCode".equals(method.getName())) {
        return System.identityHashCode(proxy);
      }
      if ("equals".equals(method.getName())) {
        return proxy == args[0];
      }
      return null;
    };

    interceptorProxy = Proxy.newProxyInstance(
        interceptorClass.getClassLoader(),
        new Class<?>[]{interceptorClass},
        handler
    );

    Method addInterceptorMethod = coreApiClass.getMethod("addDialogInterceptor", interceptorClass);
    addInterceptorMethod.invoke(coreApiInstance, interceptorProxy);
  }

  /**
   * Handles the interceptDialog callback. Returns true to BLOCK the interaction.
   * Checks the NPC_INTERACT zone flag at the player's current position.
   */
  private boolean handleInterceptDialog(Supplier<HyperFactions> hyperFactionsSupplier,
                      Player player) {
    try {
      HyperFactions hf = hyperFactionsSupplier.get();
      if (hf == null) {
        return false; // Fail-open
      }

      ProtectionChecker checker = hf.getProtectionChecker();
      if (checker == null) {
        return false; // Fail-open
      }

      UUID playerUuid = player.getUuid();
      if (playerUuid == null) {
        return false;
      }

      // Get player position
      World world = player.getWorld();
      if (world == null) {
        return false;
      }
      String worldName = world.getName();

      TransformComponent transform = player.getReference().getStore()
          .getComponent(player.getReference(), TransformComponent.getComponentType());
      if (transform == null) {
        return false;
      }
      Vector3d pos = transform.getPosition();

      // Check protection using NPC_INTERACT type
      ProtectionChecker.ProtectionResult result = checker.canInteract(
          playerUuid, worldName, pos.getX(), pos.getZ(),
          ProtectionChecker.InteractionType.NPC_INTERACT
      );

      boolean blocked = !checker.isAllowed(result);

      Logger.debugInteraction("[KyuubiSoft:Citizen] player=%s, world=%s, pos=(%.0f,%.0f,%.0f), blocked=%b",
          playerUuid, worldName, pos.getX(), pos.getY(), pos.getZ(), blocked);

      if (blocked) {
        String denyMsg = checker.getDenialMessage(result);
        ProtectionMessageDebounce.sendIfNotOnCooldown(player, "citizen_interact",
            Message.raw(denyMsg).color("#FF5555"));
        return true; // Block the interaction
      }

      return false; // Allow

    } catch (Exception e) {
      // Fail-open to avoid breaking KyuubiSoft NPC gameplay
      Logger.debugInteraction("[KyuubiSoft:Citizen] Error checking protection (fail-open): %s", e.getMessage());
      return false;
    }
  }

  /**
   * Removes the interceptor from KyuubiSoft Core. Called on plugin disable.
   */
  public void shutdown() {
    if (available && coreApiInstance != null && removeInterceptorMethod != null && interceptorProxy != null) {
      try {
        removeInterceptorMethod.invoke(coreApiInstance, interceptorProxy);
        Logger.debugIntegration("[KyuubiSoft] Dialog interceptor removed");
      } catch (Exception e) {
        Logger.debugIntegration("[KyuubiSoft] Failed to remove interceptor: %s", e.getMessage());
      }
    }
  }

  /**
   * Checks if KyuubiSoft Core is available and integrated.
   */
  public boolean isAvailable() {
    return available;
  }
}
