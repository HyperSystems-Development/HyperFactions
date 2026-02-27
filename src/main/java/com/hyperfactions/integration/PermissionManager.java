package com.hyperfactions.integration;

import com.hyperfactions.config.ConfigManager;
import com.hyperfactions.integration.permissions.HyperPermsProviderAdapter;
import com.hyperfactions.integration.permissions.HytaleNativeProvider;
import com.hyperfactions.integration.permissions.LuckPermsProvider;
import com.hyperfactions.integration.permissions.VaultUnlockedProvider;
import com.hyperfactions.util.Logger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified permission manager with chain-of-responsibility pattern.
 * Tries providers in order: VaultUnlocked -> HyperPerms -> LuckPerms -> HytaleNative.
 *
 * <p>Fallback behavior when no provider can answer:
 * - Admin permissions (hyperfactions.admin.*): Require OP
 * - Normal permissions: Allow by default (configurable)
 */
public class PermissionManager {

  private static final PermissionManager INSTANCE = new PermissionManager();

  private final List<PermissionProvider> providers = new ArrayList<>();

  private Function<UUID, PlayerRef> playerLookup;

  private boolean initialized = false;

  private PermissionManager() {}

  /**
   * Gets the singleton instance.
   *
   * @return the PermissionManager instance
   */
  public static PermissionManager get() {
    return INSTANCE;
  }

  /**
   * Initializes all permission providers.
   * Should be called once during plugin startup.
   */
  public void init() {
    if (initialized) {
      Logger.warn("[PermissionManager] Already initialized, skipping");
      return;
    }

    providers.clear();

    // Initialize providers in priority order.
    // VaultUnlocked and LuckPerms support lazy init — if they load after
    // HyperFactions, they'll initialize on first actual use.

    // VaultUnlocked first (acts as abstraction layer for other plugins)
    VaultUnlockedProvider vaultProvider = new VaultUnlockedProvider();
    vaultProvider.init();
    providers.add(vaultProvider);

    // HyperPerms second (no lazy init — either available or not)
    HyperPermsProviderAdapter hyperPermsProvider = new HyperPermsProviderAdapter();
    hyperPermsProvider.init();
    if (hyperPermsProvider.isAvailable()) {
      providers.add(hyperPermsProvider);
    }

    // LuckPerms third (supports lazy init for timing issues)
    LuckPermsProvider luckPermsProvider = new LuckPermsProvider();
    luckPermsProvider.init();
    providers.add(luckPermsProvider);

    // HytaleNative last — catches any plugin that registers with PermissionsModule
    // (LuckPerms, PermissionsPlus, etc.) as a fallback for permission checks
    HytaleNativeProvider nativeProvider = new HytaleNativeProvider();
    if (nativeProvider.isAvailable()) {
      providers.add(nativeProvider);
    }

    initialized = true;

    if (providers.isEmpty()) {
      Logger.info("[Permissions] No permission providers found - using fallback mode");
    } else {
      Logger.info("[Permissions] Initialized with %d provider(s): %s",
        providers.size(), getProviderNames());
    }
  }

  /**
   * Sets the player lookup function for OP checks.
   *
   * @param lookup function to get PlayerRef by UUID
   */
  public void setPlayerLookup(@NotNull Function<UUID, PlayerRef> lookup) {
    this.playerLookup = lookup;
  }

  /**
   * Checks if a player has a permission.
   *
   * <p>Chain behavior:
   * 1. Try each provider in order for the specific permission
   * 2. If any provider returns true/false, use that result
   * 3. Check the category wildcard (e.g., hyperfactions.teleport.* for hyperfactions.teleport.home)
   * 4. Check hyperfactions.* wildcard
   * 5. If all providers return empty (undefined), use fallback:
   *    - Admin perms (hyperfactions.admin.*): Check if player is OP
   *    - Normal perms: Use config fallback behavior (default: deny)
   *
   * <p>Note: hyperfactions.use only grants access to the /f command itself,
   * not to subcommand actions. Players need explicit permissions or wildcards.
   *
   * @param playerUuid the player's UUID
   * @param permission the permission to check
   * @return true if the player has the permission
   */
  public boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    boolean isUserLevel = isUserLevelPermission(permission);

    // Try each provider in order for the specific permission
    for (PermissionProvider provider : providers) {
      Optional<Boolean> result = provider.hasPermission(playerUuid, permission);
      if (result.isPresent()) {
        if (result.get()) {
          // Permission explicitly granted
          Logger.debug("[PermissionManager] %s answered %s for %s: true",
            provider.getName(), permission, playerUuid);
          return true;
        } else {
          // Permission returned false
          // For non-user-level permissions (admin, bypass, limit), false means denied
          if (!isUserLevel) {
            Logger.debug("[PermissionManager] %s answered %s for %s: false (denied)",
              provider.getName(), permission, playerUuid);
            return false;
          }

          // For user-level permissions, false means "not specifically granted"
          // Continue to check wildcards below
          Logger.debug("[PermissionManager] %s returned false for %s, checking wildcards",
            provider.getName(), permission);
          break;
        }
      }
    }

    // Check category wildcard (e.g., hyperfactions.teleport.* for hyperfactions.teleport.home)
    String categoryWildcard = getCategoryWildcard(permission);
    if (categoryWildcard != null) {
      for (PermissionProvider provider : providers) {
        Optional<Boolean> result = provider.hasPermission(playerUuid, categoryWildcard);
        if (result.isPresent() && result.get()) {
          Logger.debug("[PermissionManager] %s granted via category wildcard %s for %s",
            permission, categoryWildcard, playerUuid);
          return true;
        }
      }
    }

    // Check root wildcard (hyperfactions.*)
    for (PermissionProvider provider : providers) {
      Optional<Boolean> result = provider.hasPermission(playerUuid, "hyperfactions.*");
      if (result.isPresent() && result.get()) {
        Logger.debug("[PermissionManager] %s granted via hyperfactions.* for %s",
          permission, playerUuid);
        return true;
      }
    }

    // Note: hyperfactions.use only grants access to the /f command itself,
    // not to subcommand actions. Players need explicit permissions or wildcards
    // (e.g., hyperfactions.teleport.* or hyperfactions.teleport.home) for actions.

    // Fallback behavior
    return handleFallback(playerUuid, permission);
  }

  /**
   * Gets the category wildcard for a permission.
   * For example, hyperfactions.teleport.home -> hyperfactions.teleport.*
   *
   * @param permission the specific permission
   * @return the category wildcard, or null if not applicable
   */
  @Nullable
  private String getCategoryWildcard(@NotNull String permission) {
    if (!permission.startsWith("hyperfactions.")) {
      return null;
    }

    // Count dots to determine depth
    // hyperfactions.teleport.home has 2 dots = 3 segments
    // We want hyperfactions.teleport.* which has the first 2 segments + .*
    int lastDot = permission.lastIndexOf('.');
    if (lastDot <= "hyperfactions".length()) {
      // Already at root level (e.g., hyperfactions.use)
      return null;
    }

    return permission.substring(0, lastDot) + ".*";
  }

  /**
   * Checks if a permission is a user-level permission (not admin or bypass).
   * User-level permissions can be granted via hyperfactions.use.
   *
   * @param permission the permission to check
   * @return true if it's a user-level permission
   */
  private boolean isUserLevelPermission(@NotNull String permission) {
    // Admin and bypass permissions require explicit grants
    if (permission.startsWith("hyperfactions.admin")) {
      return false;
    }
    if (permission.startsWith("hyperfactions.bypass")) {
      return false;
    }
    if (permission.startsWith("hyperfactions.limit")) {
      return false;
    }

    // All other hyperfactions permissions are user-level
    return permission.startsWith("hyperfactions.");
  }

  /**
   * Handles fallback when no provider can answer.
   */
  private boolean handleFallback(@NotNull UUID playerUuid, @NotNull String permission) {
    ConfigManager config = ConfigManager.get();

    // Admin permissions always require OP (regardless of allowWithoutPermissionMod)
    if (permission.startsWith("hyperfactions.admin")) {
      boolean isOp = isPlayerOp(playerUuid);
      Logger.debug("[PermissionManager] Admin fallback for %s: isOp=%s", playerUuid, isOp);
      return isOp;
    }

    // Bypass permissions should always be denied unless explicitly granted
    if (permission.startsWith("hyperfactions.bypass")) {
      Logger.debug("[PermissionManager] Bypass fallback for %s: denied (requires explicit grant)", playerUuid);
      return false;
    }

    // Limit permissions should be denied (defaults are used instead)
    if (permission.startsWith("hyperfactions.limit")) {
      Logger.debug("[PermissionManager] Limit fallback for %s: denied (uses config defaults)", playerUuid);
      return false;
    }

    // Normal user permissions: allow if configured (no permission mod installed)
    boolean allow = config.isAllowWithoutPermissionMod();
    Logger.debug("[PermissionManager] Normal fallback for %s: %s (allowWithoutPermissionMod: %s)",
      playerUuid, allow ? "allow" : "deny", allow);
    return allow;
  }

  /**
   * Checks if a player is OP using Hytale's PermissionsModule.
   * OP players are in the "OP" group which has the "*" wildcard permission.
   *
   * @param playerUuid the player's UUID
   * @return true if the player is OP
   */
  private boolean isPlayerOp(@NotNull UUID playerUuid) {
    try {
      // Try to use Hytale's PermissionsModule
      Class<?> permModuleClass = Class.forName("com.hypixel.hytale.server.core.permissions.PermissionsModule");
      java.lang.reflect.Method getMethod = permModuleClass.getMethod("get");
      Object permModule = getMethod.invoke(null);
      if (permModule == null) {
        return false;
      }

      // Check if player is in the "OP" group
      java.lang.reflect.Method getGroupsMethod = permModuleClass.getMethod("getGroupsForUser", UUID.class);
      Object groupsObj = getGroupsMethod.invoke(permModule, playerUuid);
      if (groupsObj instanceof java.util.Set<?> groups) {
        if (groups.contains("OP")) {
          return true;
        }
      }

      // Alternative: check if player has "*" permission (OP wildcard)
      java.lang.reflect.Method hasPermMethod = permModuleClass.getMethod("hasPermission", UUID.class, String.class);
      Object result = hasPermMethod.invoke(permModule, playerUuid, "*");
      if (result instanceof Boolean) {
        return (Boolean) result;
      }
    } catch (ClassNotFoundException e) {
      Logger.debug("[PermissionManager] PermissionsModule not found");
    } catch (Exception e) {
      Logger.debug("[PermissionManager] Error checking OP status: %s", e.getMessage());
    }
    return false;
  }

  /**
   * Gets the player's chat prefix from the first available provider.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name for context (may be null)
   * @return the prefix string, or empty string if not found
   */
  @NotNull
  public String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName) {
    for (PermissionProvider provider : providers) {
      String prefix = provider.getPrefix(playerUuid, worldName);
      if (prefix != null && !prefix.isEmpty()) {
        return prefix;
      }
    }
    return "";
  }

  /**
   * Gets the player's chat suffix from the first available provider.
   *
   * @param playerUuid the player's UUID
   * @param worldName  the world name for context (may be null)
   * @return the suffix string, or empty string if not found
   */
  @NotNull
  public String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName) {
    for (PermissionProvider provider : providers) {
      String suffix = provider.getSuffix(playerUuid, worldName);
      if (suffix != null && !suffix.isEmpty()) {
        return suffix;
      }
    }
    return "";
  }

  /**
   * Gets the player's primary group from the first available provider.
   *
   * @param playerUuid the player's UUID
   * @return the primary group name, or "default" if not found
   */
  @NotNull
  public String getPrimaryGroup(@NotNull UUID playerUuid) {
    for (PermissionProvider provider : providers) {
      String group = provider.getPrimaryGroup(playerUuid);
      if (group != null && !group.isEmpty() && !"default".equals(group)) {
        return group;
      }
    }
    return "default";
  }

  /**
   * Gets the list of active provider names.
   *
   * @return comma-separated list of provider names
   */
  @NotNull
  public String getProviderNames() {
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (PermissionProvider provider : providers) {
      if (provider.isAvailable()) {
        if (count > 0) {
          sb.append(", ");
        }
        sb.append(provider.getName());
        count++;
      }
    }
    return count == 0 ? "none" : sb.toString();
  }

  /**
   * Gets the number of active providers.
   *
   * @return the provider count
   */
  public int getProviderCount() {
    return providers.size();
  }

  /**
   * Checks if any permission provider is available.
   *
   * @return true if at least one provider is available
   */
  public boolean hasProviders() {
    return !providers.isEmpty();
  }

  /**
   * Gets detailed status information for debugging.
   *
   * @return status string with provider information
   */
  @NotNull
  public String getDetailedStatus() {
    StringBuilder sb = new StringBuilder();
    sb.append("=== Permission Manager Status ===\n");
    sb.append("Initialized: ").append(initialized).append("\n");
    sb.append("Active Providers: ").append(providers.size()).append("\n");
    for (PermissionProvider provider : providers) {
      sb.append("  - ").append(provider.getName())
          .append(" (available: ").append(provider.isAvailable()).append(")\n");
    }
    sb.append("Allow Without Permission Mod: ").append(ConfigManager.get().isAllowWithoutPermissionMod()).append("\n");
    sb.append("Admin Requires OP: always (OP group)\n");
    return sb.toString();
  }
}
