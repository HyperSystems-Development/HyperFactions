package com.hyperfactions.integration.permissions;

import com.hyperfactions.integration.PermissionProvider;
import com.hyperfactions.util.Logger;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapter wrapping existing HyperPermsIntegration as a PermissionProvider.
 * This allows HyperPerms to participate in the chain of permission providers.
 */
public class HyperPermsProviderAdapter implements PermissionProvider {

  private boolean available = false;

  private Object hyperPermsInstance = null;

  private Method hasPermissionMethod = null;

  private Method hasPermissionWithContextMethod = null;

  private Method getContextsMethod = null;

  private Method getUserManagerMethod = null;

  private Method getPrefixMethod = null;

  private Method getSuffixMethod = null;

  /** Returns the name. */
  @Override
  @NotNull
  public String getName() {
    return "HyperPerms";
  }

  /**
   * Initializes the HyperPerms provider.
   * Attempts to load HyperPerms via reflection.
   */
  public void init() {
    try {
      // Try to load HyperPerms via HyperPermsBootstrap
      Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
      Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
      hyperPermsInstance = getInstanceMethod.invoke(null);

      if (hyperPermsInstance == null) {
        available = false;
        Logger.debug("[HyperPermsProvider] Bootstrap returned null");
        return;
      }

      Class<?> instanceClass = hyperPermsInstance.getClass();

      // Get the hasPermission method (2-param fallback)
      hasPermissionMethod = instanceClass.getMethod("hasPermission", UUID.class, String.class);
      getUserManagerMethod = instanceClass.getMethod("getUserManager");

      // Try to get context-aware methods for world-specific permission support
      try {
        Class<?> contextSetClass = Class.forName("com.hyperperms.api.context.ContextSet");
        getContextsMethod = instanceClass.getMethod("getContexts", UUID.class);
        hasPermissionWithContextMethod = instanceClass.getMethod("hasPermission",
            UUID.class, String.class, contextSetClass);
        Logger.debug("[HyperPermsProvider] Context-aware permission checking available");
      } catch (ClassNotFoundException | NoSuchMethodException e) {
        Logger.debug("[HyperPermsProvider] Context-aware methods not available, "
            + "world-specific permissions won't be resolved: %s", e.getMessage());
      }

      // Try to find prefix/suffix methods
      try {
        Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
        if (userManager != null) {
          Class<?> userManagerClass = userManager.getClass();
          // Check if chat meta methods exist
          try {
            Method getUserMethod = userManagerClass.getMethod("getUser", UUID.class);
            Object testUser = getUserMethod.invoke(userManager, UUID.randomUUID());
            // Methods exist if we get this far without exception
          } catch (Exception e) {
            // User methods not available
          }
        }
      } catch (Exception e) {
        Logger.debug("[HyperPermsProvider] Prefix/suffix not available: %s", e.getMessage());
      }

      available = true;
      Logger.info("[Permissions] HyperPerms provider initialized");

    } catch (ClassNotFoundException e) {
      available = false;
      Logger.debug("[HyperPermsProvider] HyperPerms not found");
    } catch (Exception e) {
      available = false;
      Logger.debug("[HyperPermsProvider] Failed to initialize: %s", e.getMessage());
    }
  }

  /** Checks if available. */
  @Override
  public boolean isAvailable() {
    return available;
  }

  /** Checks if permission. */
  @Override
  @NotNull
  public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    if (!available || hyperPermsInstance == null || hasPermissionMethod == null) {
      return Optional.empty();
    }

    try {
      Object result;

      // Prefer context-aware check (resolves world-specific permissions)
      if (getContextsMethod != null && hasPermissionWithContextMethod != null) {
        Object contexts = getContextsMethod.invoke(hyperPermsInstance, playerUuid);
        result = hasPermissionWithContextMethod.invoke(hyperPermsInstance, playerUuid, permission, contexts);
      } else {
        // Fallback to context-less check (older HyperPerms versions)
        result = hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);
      }

      if (result instanceof Boolean) {
        return Optional.of((Boolean) result);
      }

      // Unexpected result type
      Logger.debug("[HyperPermsProvider] Unexpected result type: %s",
        result != null ? result.getClass().getName() : "null");
      return Optional.empty();

    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Exception checking permission: %s", e.getMessage());
      return Optional.empty();
    }
  }

  /** Returns the prefix. */
  @Override
  @Nullable
  public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
    if (!available || hyperPermsInstance == null) {
      Logger.debug("[HyperPermsProvider] getPrefix: not available or instance null");
      return null;
    }

    try {
      // Try to get effective prefix via ChatManager.getPrefix(UUID) (includes group inheritance)
      Method getChatManagerMethod = hyperPermsInstance.getClass().getMethod("getChatManager");
      Object chatManager = getChatManagerMethod.invoke(hyperPermsInstance);
      Logger.debug("[HyperPermsProvider] ChatManager: %s", chatManager);
      if (chatManager != null) {
        // ChatManager.getPrefix(UUID) returns CompletableFuture<String>
        Method getPrefixMethod = chatManager.getClass().getMethod("getPrefix", UUID.class);
        Object future = getPrefixMethod.invoke(chatManager, playerUuid);
        Logger.debug("[HyperPermsProvider] getPrefix future: %s", future);
        if (future instanceof java.util.concurrent.CompletableFuture<?> cf) {
          Object prefix = cf.join();
          Logger.debug("[HyperPermsProvider] Resolved prefix for %s: '%s'", playerUuid, prefix);
          if (prefix != null && !prefix.toString().isEmpty()) {
            return prefix.toString();
          }
        }
      } else {
        Logger.debug("[HyperPermsProvider] ChatManager is null");
      }
    } catch (NoSuchMethodException e) {
      Logger.debug("[HyperPermsProvider] ChatManager.getPrefix not found: %s", e.getMessage());
    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Failed to get prefix via ChatManager: %s", e.getMessage());
    }

    // Fallback: get user's custom prefix directly (not resolved from groups)
    try {
      if (getUserManagerMethod == null) {
        return null;
      }

      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) {
        return null;
      }

      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return null;
      }

      // Try getCustomPrefix method (user-specific prefix)
      try {
        Method getCustomPrefixMethod = user.getClass().getMethod("getCustomPrefix");
        Object prefix = getCustomPrefixMethod.invoke(user);
        if (prefix != null && !prefix.toString().isEmpty()) {
          Logger.debug("[HyperPermsProvider] Got custom prefix for %s: '%s'", playerUuid, prefix);
          return prefix.toString();
        }
      } catch (NoSuchMethodException ignored) {
        // Try legacy getPrefix method
        try {
          Method getPrefixMethod = user.getClass().getMethod("getPrefix");
          Object prefix = getPrefixMethod.invoke(user);
          if (prefix != null && !prefix.toString().isEmpty()) {
            return prefix.toString();
          }
        } catch (NoSuchMethodException ignored2) {
          // Method doesn't exist
        }
      }
    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Failed to get user prefix: %s", e.getMessage());
    }

    return null;
  }

  /** Returns the suffix. */
  @Override
  @Nullable
  public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
    if (!available || hyperPermsInstance == null) {
      return null;
    }

    try {
      // Try to get effective suffix via ChatManager.getSuffix(UUID) (includes group inheritance)
      Method getChatManagerMethod = hyperPermsInstance.getClass().getMethod("getChatManager");
      Object chatManager = getChatManagerMethod.invoke(hyperPermsInstance);
      if (chatManager != null) {
        // ChatManager.getSuffix(UUID) returns CompletableFuture<String>
        Method getSuffixMethod = chatManager.getClass().getMethod("getSuffix", UUID.class);
        Object future = getSuffixMethod.invoke(chatManager, playerUuid);
        if (future instanceof java.util.concurrent.CompletableFuture<?> cf) {
          Object suffix = cf.join();
          if (suffix != null && !suffix.toString().isEmpty()) {
            Logger.debug("[HyperPermsProvider] Got suffix for %s: '%s'", playerUuid, suffix);
            return suffix.toString();
          }
        }
      }
    } catch (NoSuchMethodException e) {
      Logger.debug("[HyperPermsProvider] ChatManager.getSuffix not found, trying User method");
    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Failed to get suffix via ChatManager: %s", e.getMessage());
    }

    // Fallback: get user's custom suffix directly (not resolved from groups)
    try {
      if (getUserManagerMethod == null) {
        return null;
      }

      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) {
        return null;
      }

      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return null;
      }

      // Try getCustomSuffix method (user-specific suffix)
      try {
        Method getCustomSuffixMethod = user.getClass().getMethod("getCustomSuffix");
        Object suffix = getCustomSuffixMethod.invoke(user);
        if (suffix != null && !suffix.toString().isEmpty()) {
          Logger.debug("[HyperPermsProvider] Got custom suffix for %s: '%s'", playerUuid, suffix);
          return suffix.toString();
        }
      } catch (NoSuchMethodException ignored) {
        // Try legacy getSuffix method
        try {
          Method getSuffixMethod = user.getClass().getMethod("getSuffix");
          Object suffix = getSuffixMethod.invoke(user);
          if (suffix != null && !suffix.toString().isEmpty()) {
            return suffix.toString();
          }
        } catch (NoSuchMethodException ignored2) {
          // Method doesn't exist
        }
      }
    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Failed to get user suffix: %s", e.getMessage());
    }

    return null;
  }

  /** Returns the primary group. */
  @Override
  @NotNull
  public String getPrimaryGroup(@NotNull UUID playerUuid) {
    if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
      return "default";
    }

    try {
      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) {
        return "default";
      }

      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) {
        return "default";
      }

      Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
      Object result = getPrimaryGroupMethod.invoke(user);
      return result != null ? result.toString() : "default";

    } catch (Exception e) {
      Logger.debug("[HyperPermsProvider] Failed to get primary group: %s", e.getMessage());
      return "default";
    }
  }
}
