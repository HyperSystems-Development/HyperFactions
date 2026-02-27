package com.hyperfactions.integration.permissions;

import com.hyperfactions.integration.PermissionProvider;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Permission provider for LuckPerms.
 * Uses reflection to integrate with LuckPerms API.
 *
 * <p>LuckPerms uses Tristate for permission results:
 * - TRUE: Permission is explicitly granted
 * - FALSE: Permission is explicitly denied
 * - UNDEFINED: Permission is not set
 */
public class LuckPermsProvider implements PermissionProvider {

  private volatile boolean available = false;

  private volatile boolean permanentFailure = false;

  // Reflection references
  private Object luckPermsApi = null;

  private Method getUserManagerMethod = null;

  private Method getUserMethod = null;

  private Method getCachedDataMethod = null;

  private Method getPermissionDataMethod = null;

  private Method checkPermissionMethod = null;

  private Method getMetaDataMethod = null;

  private Method getPrefixMethod = null;

  private Method getSuffixMethod = null;

  private Method getPrimaryGroupMethod = null;

  // Tristate enum values
  private Object tristateTrue = null;

  private Object tristateFalse = null;

  private Object tristateUndefined = null;

  /** Returns the name. */
  @Override
  @NotNull
  public String getName() {
    return "LuckPerms";
  }

  /**
   * Initializes the LuckPerms provider.
   * Attempts to load LuckPerms API via reflection.
   * Safe to call multiple times — returns immediately if already initialized
   * or permanently failed (ClassNotFoundException = not installed).
   */
  public void init() {
    if (available || permanentFailure) {
      return;
    }

    try {
      // Load Tristate enum
      Class<?> tristateClass = Class.forName("net.luckperms.api.util.Tristate");
      tristateTrue = Enum.valueOf((Class<Enum>) tristateClass, "TRUE");
      tristateFalse = Enum.valueOf((Class<Enum>) tristateClass, "FALSE");
      tristateUndefined = Enum.valueOf((Class<Enum>) tristateClass, "UNDEFINED");

      // Get LuckPerms API instance via LuckPermsProvider.get()
      Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Method getMethod = providerClass.getMethod("get");
      luckPermsApi = getMethod.invoke(null);

      if (luckPermsApi == null) {
        Logger.debug("[LuckPermsProvider] API returned null");
        return;
      }

      // Get UserManager
      Class<?> apiClass = luckPermsApi.getClass();
      getUserManagerMethod = findMethod(apiClass, "getUserManager");

      if (getUserManagerMethod == null) {
        Logger.debug("[LuckPermsProvider] UserManager method not found");
        return;
      }

      Object userManager = getUserManagerMethod.invoke(luckPermsApi);
      if (userManager == null) {
        Logger.debug("[LuckPermsProvider] UserManager is null");
        return;
      }

      // Get User by UUID method
      Class<?> userManagerClass = userManager.getClass();
      getUserMethod = findMethod(userManagerClass, "getUser", UUID.class);

      if (getUserMethod == null) {
        Logger.debug("[LuckPermsProvider] getUser method not found");
        return;
      }

      available = true;
      Logger.info("[Permissions] LuckPerms provider initialized");

    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      permanentFailure = true;
      Logger.debug("[LuckPermsProvider] LuckPerms not installed");
    } catch (IllegalStateException e) {
      // LuckPerms not loaded yet — will retry on next use
      Logger.debug("[LuckPermsProvider] LuckPerms not loaded yet, will retry: %s", e.getMessage());
    } catch (Exception e) {
      // Other errors may be temporary (service not ready, etc.)
      Logger.debug("[LuckPermsProvider] Init deferred: %s", e.getMessage());
    }
  }

  /**
   * Ensures the provider is initialized. Called before every operation
   * to support lazy initialization when LuckPerms loads after HyperFactions.
   */
  private void ensureInitialized() {
    if (!available && !permanentFailure) {
      init();
    }
  }

  /**
   * Finds a method by name, checking interfaces and superclasses.
   */
  private Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    try {
      return clazz.getMethod(name, paramTypes);
    } catch (NoSuchMethodException e) {
      // Try interfaces
      for (Class<?> iface : clazz.getInterfaces()) {
        try {
          return iface.getMethod(name, paramTypes);
        } catch (NoSuchMethodException ignored) {
        }
      }

      // Try superclass
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return findMethod(superclass, name, paramTypes);
      }
      return null;
    }
  }

  /** Checks if available. */
  @Override
  public boolean isAvailable() {
    ensureInitialized();
    return available;
  }

  /** Checks if permission. */
  @Override
  @NotNull
  public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    ensureInitialized();
    if (!available || luckPermsApi == null) {
      return Optional.empty();
    }

    try {
      // Get user from UserManager
      Object userManager = getUserManagerMethod.invoke(luckPermsApi);
      if (userManager == null) {
        return Optional.empty();
      }

      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        // User not loaded/cached
        return Optional.empty();
      }

      // Get cached data
      Method getCachedData = findMethod(user.getClass(), "getCachedData");
      if (getCachedData == null) {
        return Optional.empty();
      }

      Object cachedData = getCachedData.invoke(user);
      if (cachedData == null) {
        return Optional.empty();
      }

      // Get permission data
      Method getPermissionData = findMethod(cachedData.getClass(), "getPermissionData");
      if (getPermissionData == null) {
        return Optional.empty();
      }

      Object permissionData = getPermissionData.invoke(cachedData);
      if (permissionData == null) {
        return Optional.empty();
      }

      // Check permission: checkPermission(String) -> Tristate
      Method checkPermission = findMethod(permissionData.getClass(), "checkPermission", String.class);
      if (checkPermission == null) {
        return Optional.empty();
      }

      Object result = checkPermission.invoke(permissionData, permission);
      if (result == null) {
        return Optional.empty();
      }

      // Check Tristate value
      if (result.equals(tristateTrue)) {
        return Optional.of(true);
      } else if (result.equals(tristateFalse)) {
        return Optional.of(false);
      } else if (result.equals(tristateUndefined)) {
        // UNDEFINED means fall through to next provider
        return Optional.empty();
      }

      return Optional.empty();

    } catch (Exception e) {
      Logger.debug("[LuckPermsProvider] Exception checking permission: %s", e.getMessage());
      return Optional.empty();
    }
  }

  /** Returns the prefix. */
  @Override
  @Nullable
  public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
    ensureInitialized();
    if (!available || luckPermsApi == null) {
      return null;
    }

    try {
      Object metaData = getMetaData(playerUuid);
      if (metaData == null) {
        return null;
      }

      Method getPrefix = findMethod(metaData.getClass(), "getPrefix");
      if (getPrefix == null) {
        return null;
      }

      Object result = getPrefix.invoke(metaData);
      return result != null ? result.toString() : null;

    } catch (Exception e) {
      Logger.debug("[LuckPermsProvider] Exception getting prefix: %s", e.getMessage());
      return null;
    }
  }

  /** Returns the suffix. */
  @Override
  @Nullable
  public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
    ensureInitialized();
    if (!available || luckPermsApi == null) {
      return null;
    }

    try {
      Object metaData = getMetaData(playerUuid);
      if (metaData == null) {
        return null;
      }

      Method getSuffix = findMethod(metaData.getClass(), "getSuffix");
      if (getSuffix == null) {
        return null;
      }

      Object result = getSuffix.invoke(metaData);
      return result != null ? result.toString() : null;

    } catch (Exception e) {
      Logger.debug("[LuckPermsProvider] Exception getting suffix: %s", e.getMessage());
      return null;
    }
  }

  /** Returns the primary group. */
  @Override
  @NotNull
  public String getPrimaryGroup(@NotNull UUID playerUuid) {
    ensureInitialized();
    if (!available || luckPermsApi == null) {
      return "default";
    }

    try {
      Object userManager = getUserManagerMethod.invoke(luckPermsApi);
      if (userManager == null) {
        return "default";
      }

      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return "default";
      }

      Method getPrimaryGroup = findMethod(user.getClass(), "getPrimaryGroup");
      if (getPrimaryGroup == null) {
        return "default";
      }

      Object result = getPrimaryGroup.invoke(user);
      return result != null ? result.toString() : "default";

    } catch (Exception e) {
      Logger.debug("[LuckPermsProvider] Exception getting primary group: %s", e.getMessage());
      return "default";
    }
  }

  /**
   * Gets the meta data for a player.
   */
  private Object getMetaData(UUID playerUuid) {
    try {
      Object userManager = getUserManagerMethod.invoke(luckPermsApi);
      if (userManager == null) {
        return null;
      }

      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return null;
      }

      Method getCachedData = findMethod(user.getClass(), "getCachedData");
      if (getCachedData == null) {
        return null;
      }

      Object cachedData = getCachedData.invoke(user);
      if (cachedData == null) {
        return null;
      }

      Method getMetaData = findMethod(cachedData.getClass(), "getMetaData");
      if (getMetaData == null) {
        return null;
      }

      return getMetaData.invoke(cachedData);

    } catch (Exception e) {
      return null;
    }
  }
}
