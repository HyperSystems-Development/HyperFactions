package com.hyperfactions.integration;

import com.hyperfactions.Permissions;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Registers all HyperFactions permission nodes with external permission plugins
 * for autocomplete/discovery. Currently supports LuckPerms' PermissionRegistry.
 *
 * <p>LuckPerms discovers permissions at boot by iterating registered commands.
 * Since HyperFactions uses internal subcommand routing (not Hytale's command system),
 * LuckPerms can't discover our permissions automatically. This class inserts them
 * directly into LuckPerms' PermissionRegistry via reflection.
 */
public final class PermissionRegistrar {

  private PermissionRegistrar() {}

  /**
   * Attempts to register all HyperFactions permission nodes with LuckPerms'
   * PermissionRegistry for web editor autocomplete and discovery.
   * Safe to call even if LuckPerms is not installed — fails silently.
   */
  public static void registerWithLuckPerms() {
    try {
      // Get LuckPerms API
      Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
      Method getMethod = providerClass.getMethod("get");
      Object luckPermsApi = getMethod.invoke(null);
      if (luckPermsApi == null) {
        return;
      }

      // Access internal plugin via private 'plugin' field on LuckPermsApiProvider
      // (LuckPermsApiProvider has no getPlugin() method — field access required)
      Object plugin = getPrivateField(luckPermsApi, "plugin");
      if (plugin == null) {
        return;
      }

      // Get PermissionRegistry from the plugin
      Method getRegistryMethod = findMethod(plugin.getClass(), "getPermissionRegistry");
      if (getRegistryMethod == null) {
        return;
      }

      Object registry = getRegistryMethod.invoke(plugin);
      if (registry == null) {
        return;
      }

      // Get the insert method
      Method insertMethod = registry.getClass().getMethod("insert", String.class);

      // Insert all known permissions
      int count = 0;
      for (String perm : Permissions.getAllPermissions()) {
        insertMethod.invoke(registry, perm);
        count++;
      }
      for (String wildcard : Permissions.getWildcards()) {
        insertMethod.invoke(registry, wildcard);
        count++;
      }

      Logger.info("[Permissions] Registered %d permission nodes with LuckPerms", count);

    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      // LuckPerms not installed — expected, no action needed
    } catch (IllegalStateException e) {
      // LuckPerms not loaded yet
      Logger.debug("[PermissionRegistrar] LuckPerms not ready: %s", e.getMessage());
    } catch (Throwable e) {
      // Any other failure (reflection errors, API changes, etc.) — never crash
      Logger.debug("[PermissionRegistrar] Could not register permissions with LuckPerms: %s", e.getMessage());
    }
  }

  /**
   * Gets a private field value from an object, searching the class hierarchy.
   */
  private static Object getPrivateField(Object obj, String fieldName) {
    Class<?> clazz = obj.getClass();
    while (clazz != null && clazz != Object.class) {
      try {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
      } catch (IllegalAccessException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Finds a method by name, checking interfaces and superclasses.
   */
  private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    try {
      return clazz.getMethod(name, paramTypes);
    } catch (NoSuchMethodException e) {
      for (Class<?> iface : clazz.getInterfaces()) {
        try {
          return iface.getMethod(name, paramTypes);
        } catch (NoSuchMethodException ignored) {}
      }
      Class<?> superclass = clazz.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return findMethod(superclass, name, paramTypes);
      }
      return null;
    }
  }
}
