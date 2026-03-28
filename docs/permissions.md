# HyperFactions Permission Framework

> **Version**: 0.13.0 | **76 permission constants** across **13 categories**

Architecture documentation for the HyperFactions permission system.

## Overview

HyperFactions uses a centralized permission system with:

- **Permission Constants** - All 76 constants (62 nodes + 11 wildcards + 2 limit prefixes + ROOT) defined in `Permissions.java`
- **Permission Manager** - Chain-based provider resolution (VaultUnlocked → HyperPerms → LuckPerms → HytaleNative)
- **Multiple Provider Support** - VaultUnlocked, HyperPerms, and LuckPerms adapters
- **Manager-Level Checks** - Permissions enforced in business logic, not just commands
- **Wildcard Resolution** - Category wildcards (`hyperfactions.teleport.*`) and root wildcard (`hyperfactions.*`)
- **Fallback Behavior** - Type-specific defaults when no provider responds

## Architecture

```
Permission Check Request
     │
     ▼
PermissionManager.hasPermission(uuid, node)
     │
     ├─► 1. VaultUnlockedProvider (always registered, lazy init)
     ├─► 2. HyperPermsProviderAdapter (only if available at startup)
     ├─► 3. LuckPermsProvider (always registered, lazy init)
     ├─► 4. HytaleNativeProvider (only if PermissionsModule available)
     │
     ├─► Category wildcard check (e.g., hyperfactions.teleport.*)
     ├─► Root wildcard check (hyperfactions.*)
     │
     └─► Fallback:
          ├─► admin.* → Require OP
          ├─► bypass.* → Deny
          ├─► limit.* → Deny (config defaults used)
          └─► user-level → Configurable (allowWithoutPermissionMod, default: deny)
```

See [Integrations](integrations.md#permission-system) for the full permission resolution flow with Mermaid diagram.

## Key Classes

| Class | Path | Purpose |
|-------|------|---------|
| Permissions | [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) | All permission node constants |
| PermissionManager | [`integration/PermissionManager.java`](../src/main/java/com/hyperfactions/integration/PermissionManager.java) | Chain-based permission resolution |
| PermissionProvider | [`integration/PermissionProvider.java`](../src/main/java/com/hyperfactions/integration/PermissionProvider.java) | Provider interface |
| HyperPermsIntegration | [`integration/permissions/HyperPermsIntegration.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsIntegration.java) | HyperPerms detection and access |
| HyperPermsProviderAdapter | [`integration/permissions/HyperPermsProviderAdapter.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsProviderAdapter.java) | HyperPerms → PermissionProvider adapter |
| HytaleNativeProvider | [`integration/permissions/HytaleNativeProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/HytaleNativeProvider.java) | Hytale native OP provider |
| LuckPermsProvider | [`integration/permissions/LuckPermsProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/LuckPermsProvider.java) | LuckPerms permission provider |
| VaultUnlockedProvider | [`integration/permissions/VaultUnlockedProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/VaultUnlockedProvider.java) | VaultUnlocked permission provider |

## Permission Constants

[`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java)

All permission nodes are centralized as constants:

```java
public final class Permissions {

    private Permissions() {}

    // Root
    public static final String ROOT = "hyperfactions";
    public static final String WILDCARD = "hyperfactions.*";

    // Basic access
    public static final String USE = "hyperfactions.use";

    // Faction management
    public static final String FACTION_WILDCARD = "hyperfactions.faction.*";
    public static final String CREATE = "hyperfactions.faction.create";
    public static final String DISBAND = "hyperfactions.faction.disband";
    // ...

    // Helper methods
    public static String[] getAllPermissions() { ... }
    public static String[] getWildcards() { ... }
    public static String[] getUserPermissions() { ... }
    public static String[] getBypassPermissions() { ... }
    public static String[] getAdminPermissions() { ... }
}
```

### Node Hierarchy

```
hyperfactions.*                       # All permissions
├── hyperfactions.use                 # Access to /f command and GUI (does NOT grant actions)
│
├── hyperfactions.faction.*           # Faction management
│   ├── hyperfactions.faction.create
│   ├── hyperfactions.faction.disband
│   ├── hyperfactions.faction.rename
│   ├── hyperfactions.faction.description
│   ├── hyperfactions.faction.tag
│   ├── hyperfactions.faction.color
│   ├── hyperfactions.faction.open
│   ├── hyperfactions.faction.close
│   └── hyperfactions.faction.permissions
│
├── hyperfactions.member.*            # Membership
│   ├── hyperfactions.member.invite
│   ├── hyperfactions.member.join
│   ├── hyperfactions.member.leave
│   ├── hyperfactions.member.kick
│   ├── hyperfactions.member.promote
│   ├── hyperfactions.member.demote
│   └── hyperfactions.member.transfer
│
├── hyperfactions.territory.*         # Territory
│   ├── hyperfactions.territory.claim
│   ├── hyperfactions.territory.unclaim
│   ├── hyperfactions.territory.overclaim
│   └── hyperfactions.territory.map
│
├── hyperfactions.teleport.*          # Teleportation
│   ├── hyperfactions.teleport.home
│   ├── hyperfactions.teleport.sethome
│   ├── hyperfactions.teleport.delhome
│   └── hyperfactions.teleport.stuck
│
├── hyperfactions.relation.*          # Diplomacy
│   ├── hyperfactions.relation.ally
│   ├── hyperfactions.relation.enemy
│   ├── hyperfactions.relation.neutral
│   └── hyperfactions.relation.view
│
├── hyperfactions.chat.*              # Communication
│   ├── hyperfactions.chat.faction
│   └── hyperfactions.chat.ally
│
├── hyperfactions.info.*              # Information
│   ├── hyperfactions.info.faction
│   ├── hyperfactions.info.list
│   ├── hyperfactions.info.player
│   ├── hyperfactions.info.power
│   ├── hyperfactions.info.members
│   ├── hyperfactions.info.logs
│   └── hyperfactions.info.help
│
├── hyperfactions.economy.*           # Economy/treasury
│   ├── hyperfactions.economy.balance
│   ├── hyperfactions.economy.deposit
│   ├── hyperfactions.economy.withdraw
│   ├── hyperfactions.economy.transfer
│   └── hyperfactions.economy.log
│
├── hyperfactions.bypass.*            # Protection bypass
│   ├── hyperfactions.bypass.build
│   ├── hyperfactions.bypass.interact
│   ├── hyperfactions.bypass.container
│   ├── hyperfactions.bypass.damage
│   ├── hyperfactions.bypass.use
│   ├── hyperfactions.bypass.warmup
│   ├── hyperfactions.bypass.cooldown
│   ├── hyperfactions.bypass.mapvisibility
│   └── hyperfactions.bypass.mapfilter
│
├── hyperfactions.admin.*             # Administration
│   ├── hyperfactions.admin.use
│   ├── hyperfactions.admin.reload
│   ├── hyperfactions.admin.debug
│   ├── hyperfactions.admin.zones
│   ├── hyperfactions.admin.disband
│   ├── hyperfactions.admin.modify
│   ├── hyperfactions.admin.bypass.limits
│   ├── hyperfactions.admin.backup
│   ├── hyperfactions.admin.power
│   └── hyperfactions.admin.economy
│
└── hyperfactions.limit.*             # Numeric limits
    ├── hyperfactions.limit.claims.<N>
    └── hyperfactions.limit.power.<N>
```

## Permission Manager

[`integration/PermissionManager.java`](../src/main/java/com/hyperfactions/integration/PermissionManager.java)

Singleton that coordinates permission checks. Provider implementations are in `integration/permissions/`:

```java
public class PermissionManager {

    private static final PermissionManager INSTANCE = new PermissionManager();

    private final List<PermissionProvider> providers = new ArrayList<>();
    private Function<UUID, PlayerRef> playerLookup;
    private boolean initialized = false;

    private PermissionManager() {}

    public static PermissionManager get() {
        return INSTANCE;
    }

    public void init() {
        // Register providers in priority order:
        // 1. VaultUnlocked (always registered, lazy init)
        // 2. HyperPerms (only if available at startup)
        // 3. LuckPerms (always registered, lazy init)
        // 4. HytaleNative (only if PermissionsModule available)
    }

    public boolean hasPermission(UUID playerUuid, String permission) {
        // 1. Check providers in order for the specific permission
        for (PermissionProvider provider : providers) {
            Optional<Boolean> result = provider.hasPermission(playerUuid, permission);
            if (result.isPresent()) {
                return result.get(); // (with special user-level wildcard fallthrough)
            }
        }

        // 2. Check category wildcard (e.g., hyperfactions.teleport.*)
        // 3. Check root wildcard (hyperfactions.*)
        // 4. Fallback behavior
        return handleFallback(playerUuid, permission);
    }
}
```

### Resolution Order

1. **VaultUnlocked** (always registered, lazy init) - Economy/permission abstraction layer
2. **HyperPerms** (if available at startup) - Full permission system with groups, inheritance
3. **LuckPerms** (always registered, lazy init) - Permission system
4. **HytaleNative** (if PermissionsModule available) - Hytale built-in permissions
5. **Category wildcard check** (e.g., `hyperfactions.teleport.*`)
6. **Root wildcard check** (`hyperfactions.*`)
7. **Fallback** - Admin: OP check, Bypass/Limit: deny, User: `allowWithoutPermissionMod` (default: deny)

## Permission Provider Interface

[`integration/PermissionProvider.java`](../src/main/java/com/hyperfactions/integration/PermissionProvider.java) (implementations in `integration/permissions/`)

```java
public interface PermissionProvider {

    @NotNull String getName();

    boolean isAvailable();

    /**
     * Check if player has permission.
     *
     * @return Optional containing true/false if the provider can answer,
     *         or empty if the provider cannot determine (e.g., player not found)
     */
    @NotNull
    Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission);

    @Nullable String getPrefix(@NotNull UUID playerUuid, @Nullable String worldName);
    @Nullable String getSuffix(@NotNull UUID playerUuid, @Nullable String worldName);
    @NotNull String getPrimaryGroup(@NotNull UUID playerUuid);
}
```

The empty `Optional` return allows providers to "pass" on permissions they don't handle, letting the next provider in the chain respond. The interface also exposes `getPrefix()`, `getSuffix()`, and `getPrimaryGroup()` for chat formatting integration.

## HyperPerms Integration

[`integration/permissions/HyperPermsIntegration.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsIntegration.java)

Soft dependency detection via reflection:

```java
public final class HyperPermsIntegration {

    private static boolean available = false;
    private static Object hyperPermsInstance = null;
    private static Method hasPermissionMethod = null;
    private static Method getUserManagerMethod = null;

    public static void init() {
        try {
            // Loads via HyperPermsBootstrap (not HyperPerms directly)
            Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
            Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
            hyperPermsInstance = getInstanceMethod.invoke(null);

            hasPermissionMethod = hyperPermsInstance.getClass()
                .getMethod("hasPermission", UUID.class, String.class);
            getUserManagerMethod = hyperPermsInstance.getClass()
                .getMethod("getUserManager");

            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
            // HyperPerms not installed — fail-open in production
        }
    }

    /**
     * Returns true if HyperPerms is unavailable (fail-open) unless test mode is on.
     * When available, delegates to HyperPerms via reflection.
     */
    public static boolean hasPermission(UUID playerUuid, String permission) {
        if (!available) return !testMode; // fail-open in production, fail-closed in tests
        // Call hasPermission(UUID, String) via reflection
        return (Boolean) hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);
    }
}
```

## Fallback Behavior

When no provider gives a definitive answer:

| Permission Type | Fallback |
|-----------------|----------|
| User permissions | `deny` by default (configurable via `allowWithoutPermissionMod`) |
| Admin permissions | Requires OP (always) |
| Bypass permissions | **Always deny** |
| Limit permissions | **Always deny** (uses config defaults) |

Configuration in `config/server.json`:

```json
{
  "permissions": {
    "allowWithoutPermissionMod": false
  }
}
```

**Security Note:** Bypass and limit permissions are never granted by fallback - they always require explicit permission grants. Admin fallback always checks OP status regardless of the `allowWithoutPermissionMod` setting.

## Manager-Level Permission Checks

Permissions are checked in managers (not just commands) to ensure all entry points are protected:

```java
// In ClaimManager
public ClaimResult claim(UUID playerUuid, String world, int chunkX, int chunkZ) {
    // Permission check FIRST
    if (!PermissionManager.get().hasPermission(playerUuid, Permissions.CLAIM)) {
        return ClaimResult.NO_PERMISSION;
    }

    // Business logic...
}
```

### Why Manager-Level Checks?

1. **GUI Protection** - GUI pages call managers directly, bypassing commands
2. **API Protection** - External plugins calling managers are also protected
3. **Single Source of Truth** - Permission logic in one place per operation

### Managers with Permission Checks

| Manager | Method | Permission |
|---------|--------|------------|
| FactionManager | `createFaction()` | `faction.create` |
| FactionManager | `disbandFaction()` | `faction.disband` |
| FactionManager | `promoteMember()` | `member.promote` |
| FactionManager | `demoteMember()` | `member.demote` |
| FactionManager | `transferLeadership()` | `member.transfer` |
| FactionManager | `setHome()` | `teleport.sethome` |
| ClaimManager | `claim()` | `territory.claim` |
| ClaimManager | `unclaim()` | `territory.unclaim` |
| ClaimManager | `overclaim()` | `territory.overclaim` |
| RelationManager | `requestAlly()` | `relation.ally` |
| RelationManager | `setEnemy()` | `relation.enemy` |
| RelationManager | `setNeutral()` | `relation.neutral` |
| TeleportManager | `teleportToHome()` | `teleport.home` |
| InviteManager | `createInviteChecked()` | `member.invite` |
| JoinRequestManager | `createRequestChecked()` | `member.join` |
| ChatManager | `toggleFactionChatChecked()` | `chat.faction` |
| ChatManager | `toggleAllyChatChecked()` | `chat.ally` |

## Bypass Permissions

Bypass permissions allow players to ignore protection rules:

| Permission | Bypasses |
|------------|----------|
| `hyperfactions.bypass.build` | Block place/break protection |
| `hyperfactions.bypass.interact` | Door/button protection |
| `hyperfactions.bypass.container` | Chest access protection |
| `hyperfactions.bypass.damage` | Entity damage protection |
| `hyperfactions.bypass.use` | Item use protection |
| `hyperfactions.bypass.warmup` | Teleport warmup delay |
| `hyperfactions.bypass.cooldown` | Teleport cooldown timer |
| `hyperfactions.bypass.mapvisibility` | Always visible to everyone on map (admin/staff) |
| `hyperfactions.bypass.mapfilter` | Can see all players on map regardless of faction filter |

**Admin Bypass Toggle:**
Admins with `hyperfactions.admin.use` can toggle bypass mode via `/f admin bypass`. This is separate from bypass permissions and requires explicit toggle.

## Limit Permissions

Limit permissions set per-player numeric caps:

```
hyperfactions.limit.claims.50   → Max 50 claims for this player
hyperfactions.limit.power.100   → Max 100 power for this player
```

Usage in code:

```java
public int getMaxClaims(UUID playerUuid) {
    // Check for limit permission
    for (int i = 1000; i >= 1; i--) {
        if (PermissionManager.get().hasPermission(playerUuid,
                Permissions.LIMIT_CLAIMS_PREFIX + i)) {
            return i;
        }
    }
    // Fall back to config default
    return ConfigManager.get().getMaxClaims();
}
```

## Adding New Permissions

1. Add constant to `Permissions.java`:
   ```java
   public static final String NEW_PERM = "hyperfactions.category.action";
   ```

2. Add to appropriate helper method (`getUserPermissions()`, `getAdminPermissions()`, etc.)

3. Use in manager or command:
   ```java
   if (!PermissionManager.get().hasPermission(uuid, Permissions.NEW_PERM)) {
       return Result.NO_PERMISSION;
   }
   ```

4. Document in user-facing permission list (README.md)

## Code Links

| Class | Path |
|-------|------|
| Permissions | [`Permissions.java`](../src/main/java/com/hyperfactions/Permissions.java) |
| PermissionManager | [`integration/PermissionManager.java`](../src/main/java/com/hyperfactions/integration/PermissionManager.java) |
| PermissionProvider | [`integration/PermissionProvider.java`](../src/main/java/com/hyperfactions/integration/PermissionProvider.java) |
| HyperPermsIntegration | [`integration/permissions/HyperPermsIntegration.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsIntegration.java) |
| HyperPermsProviderAdapter | [`integration/permissions/HyperPermsProviderAdapter.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsProviderAdapter.java) |
| HytaleNativeProvider | [`integration/permissions/HytaleNativeProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/HytaleNativeProvider.java) |
| LuckPermsProvider | [`integration/permissions/LuckPermsProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/LuckPermsProvider.java) |
| VaultUnlockedProvider | [`integration/permissions/VaultUnlockedProvider.java`](../src/main/java/com/hyperfactions/integration/permissions/VaultUnlockedProvider.java) |
| ProtectionChecker | [`protection/ProtectionChecker.java`](../src/main/java/com/hyperfactions/protection/ProtectionChecker.java) |
