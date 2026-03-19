package com.hyperfactions.integration.permissions;

import com.hyperfactions.util.ErrorHandler;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Method;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Integration with HyperPerms for permission checking.
 * Uses reflection to avoid hard dependency on HyperPerms.
 *
 * <p>IMPORTANT: When HyperPerms is not available OR permission check fails,
 * we default to ALLOWING access. This ensures the plugin works standalone
 * and OPs/admins aren't blocked by integration issues.
 */
public final class HyperPermsIntegration {

  private static boolean available = false;

  private static Object hyperPermsInstance = null;

  private static Method hasPermissionMethod = null;

  private static Method getUserManagerMethod = null;

  private static String initError = null;

  /**
   * When true, permission checks return false when HyperPerms is unavailable.
   * Used for testing to ensure protection logic is exercised.
   */
  private static boolean testMode = false;

  private HyperPermsIntegration() {}

  /**
   * Enables test mode where permission checks fail-closed instead of fail-open.
   * Should only be used in unit tests.
   *
   * @param enabled true to enable test mode
   */
  public static void setTestMode(boolean enabled) {
    testMode = enabled;
  }

  /**
   * @return true if test mode is enabled
   */
  public static boolean isTestMode() {
    return testMode;
  }

  /**
   * Initializes the HyperPerms integration.
   */
  public static void init() {
    try {
      // Try to load HyperPerms via HyperPermsBootstrap
      Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
      Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
      hyperPermsInstance = getInstanceMethod.invoke(null);

      if (hyperPermsInstance == null) {
        initError = "HyperPermsBootstrap.getInstance() returned null";
        available = false;
        Logger.warn("HyperPerms bootstrap returned null - permissions disabled");
        return;
      }

      Class<?> instanceClass = hyperPermsInstance.getClass();
      Logger.debug("HyperPerms instance class: %s", instanceClass.getName());

      // Get the hasPermission method on HyperPerms itself (not User)
      // Method signature: hasPermission(UUID uuid, String permission)
      hasPermissionMethod = instanceClass.getMethod("hasPermission", UUID.class, String.class);
      getUserManagerMethod = instanceClass.getMethod("getUserManager");

      available = true;
      Logger.info("[Integration] HyperPerms integration enabled");

    } catch (ClassNotFoundException e) {
      available = false;
      initError = "HyperPerms not found";
      Logger.info("[Integration] HyperPerms not found - all players will have full access");
    } catch (NoSuchMethodException e) {
      available = false;
      initError = "Method not found: " + e.getMessage();
      ErrorHandler.report("HyperPerms API mismatch - defaulting to allow all", e);
    } catch (Exception e) {
      available = false;
      initError = e.getClass().getSimpleName() + ": " + e.getMessage();
      ErrorHandler.report("Failed to initialize HyperPerms integration - defaulting to allow all", e);
    }
  }

  /**
   * Checks if HyperPerms is available.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    return available;
  }

  /**
   * Gets initialization error message if any.
   *
   * @return error message or null
   */
  public static String getInitError() {
    return initError;
  }

  /**
   * Gets detailed status of the integration for debugging.
   *
   * @return status string with all relevant info
   */
  public static String getDetailedStatus() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== HyperPerms Integration Status ===\n");
    sb.append("Available: ").append(available).append("\n");
    sb.append("Instance: ").append(hyperPermsInstance != null ? hyperPermsInstance.getClass().getName() : "null").append("\n");
    sb.append("hasPermission method: ").append(hasPermissionMethod != null ? "found" : "null").append("\n");
    sb.append("getUserManager method: ").append(getUserManagerMethod != null ? "found" : "null").append("\n");
    if (initError != null) {
      sb.append("Init error: ").append(initError).append("\n");
    }
    return sb.toString();
  }

  /**
   * Checks if a player has a permission.
   *
   * <p>IMPORTANT: Returns TRUE if:
   * - HyperPerms is not available (standalone mode) AND test mode is disabled
   * - Permission check fails for any reason (fail-open for safety) AND test mode is disabled
   * - Player actually has the permission
   *
   * <p>In test mode, returns FALSE when HyperPerms is unavailable to allow
   * testing of protection logic without bypass permissions.
   *
   * @param playerUuid the player's UUID
   * @param permission the permission to check
   * @return true if has permission or check cannot be performed (when not in test mode)
   */
  public static boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    // If HyperPerms not available, behavior depends on test mode
    if (!available || hyperPermsInstance == null || hasPermissionMethod == null) {
      // In test mode, return false to allow testing protection logic
      // In production, return true (fail-open) so admins aren't blocked
      return !testMode;
    }

    try {
      // Call hasPermission(UUID, String) on the HyperPerms instance directly
      Object result = hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);

      if (result instanceof Boolean) {
        return (Boolean) result;
      }

      // Unexpected result type, allow by default
      Logger.warn("[PERM] Unexpected result type: %s, ALLOWING %s for %s",
        result != null ? result.getClass().getName() : "null", permission, playerUuid);
      return true;

    } catch (Exception e) {
      // Any error in permission check = allow (fail-open)
      ErrorHandler.report("Exception checking permission " + permission + " for " + playerUuid + ", ALLOWING", e);
      return true;
    }
  }

  /**
   * Gets a numeric permission value from a permission pattern.
   * For example, "hyperfactions.limit.5" with prefix "hyperfactions.limit." returns 5.
   *
   * @param playerUuid   the player's UUID
   * @param prefix       the permission prefix
   * @param defaultValue the default value if not found
   * @return the numeric value, or defaultValue if not found
   */
  public static int getPermissionValue(@NotNull UUID playerUuid, @NotNull String prefix, int defaultValue) {
    if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
      return defaultValue;
    }

    try {
      // Get UserManager
      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) {
        return defaultValue;
      }

      // Get User
      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return defaultValue;
      }

      // Try to get effective permissions
      Method getPermissionsMethod = user.getClass().getMethod("getEffectivePermissions");
      Object permissions = getPermissionsMethod.invoke(user);

      if (permissions instanceof Iterable<?> iterable) {
        int highestValue = defaultValue;
        for (Object perm : iterable) {
          String permStr = perm.toString();
          if (permStr.startsWith(prefix)) {
            String valueStr = permStr.substring(prefix.length());
            try {
              int value = Integer.parseInt(valueStr);
              if (value > highestValue) {
                highestValue = value;
              }
            } catch (NumberFormatException ignored) {
              // Not a numeric permission
            }
          }
        }
        return highestValue;
      }
    } catch (Exception e) {
      Logger.debug("Failed to get permission value for %s with prefix %s: %s",
        playerUuid, prefix, e.getMessage());
    }

    return defaultValue;
  }

  /**
   * Gets the player's primary group name.
   *
   * @param playerUuid the player's UUID
   * @return the group name, or "default" if not found
   */
  @NotNull
  public static String getPrimaryGroup(@NotNull UUID playerUuid) {
    if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
      return "default";
    }

    try {
      // Get UserManager
      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) {
        return "default";
      }

      // Get User
      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return "default";
      }

      // Get primary group
      Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
      Object result = getPrimaryGroupMethod.invoke(user);
      return result != null ? result.toString() : "default";
    } catch (Exception e) {
      Logger.debug("Failed to get primary group for %s: %s", playerUuid, e.getMessage());
      return "default";
    }
  }
}
