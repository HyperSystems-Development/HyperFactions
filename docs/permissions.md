# HyperFactions Permission Framework

> **Version**: 0.11.0 | **76 permission nodes** across **12 categories**

Architecture documentation for the HyperFactions permission system.

## Overview

HyperFactions uses a centralized permission system with:

- **Permission Constants** - All 76 nodes defined in `Permissions.java`
- **Permission Manager** - Chain-based provider resolution (VaultUnlocked → HyperPerms → LuckPerms)
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
     ├─► 1. VaultUnlockedProvider (if available)
     ├─► 2. HyperPermsProviderAdapter (if available)
     ├─► 3. LuckPermsProvider (if available)
     │
     ├─► Category wildcard check (e.g., hyperfactions.teleport.*)
     ├─► Root wildcard check (hyperfactions.*)
     │
     └─► Fallback:
          ├─► admin.* → Require OP
          ├─► bypass.* → Deny
          ├─► limit.* → Deny (config defaults used)
          └─► user-level → Configurable (allowWithoutPermissionMod)
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
├── hyperfactions.bypass.*            # Protection bypass
│   ├── hyperfactions.bypass.build
│   ├── hyperfactions.bypass.interact
│   ├── hyperfactions.bypass.container
│   ├── hyperfactions.bypass.damage
│   ├── hyperfactions.bypass.use
│   ├── hyperfactions.bypass.warmup
│   └── hyperfactions.bypass.cooldown
│
├── hyperfactions.admin.*             # Administration
│   ├── hyperfactions.admin.use
│   ├── hyperfactions.admin.reload
│   ├── hyperfactions.admin.debug
│   ├── hyperfactions.admin.zones
│   ├── hyperfactions.admin.disband
│   ├── hyperfactions.admin.modify
│   ├── hyperfactions.admin.bypass.limits
│   └── hyperfactions.admin.backup
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

    private static PermissionManager instance;

    private Function<UUID, PlayerRef> playerLookup;
    private List<PermissionProvider> providers = new ArrayList<>();

    public static PermissionManager get() {
        if (instance == null) {
            instance = new PermissionManager();
        }
        return instance;
    }

    public void init() {
        // Register HyperPerms provider if available
        if (HyperPermsIntegration.isAvailable()) {
            providers.add(new HyperPermsProviderAdapter());
        }
    }

    public boolean hasPermission(UUID playerUuid, String permission) {
        // 1. Check providers in order
        for (PermissionProvider provider : providers) {
            Boolean result = provider.hasPermission(playerUuid, permission);
            if (result != null) {
                return result;
            }
        }

        // 2. For admin permissions, check OP
        if (permission.startsWith("hyperfactions.admin")) {
            if (ConfigManager.get().isAdminRequiresOp()) {
                return isOp(playerUuid);
            }
        }

        // 3. Fallback behavior
        return getFallbackResult(permission);
    }
}
```

### Resolution Order

1. **HyperPerms** (if available) - Full permission system with groups, inheritance
2. **OP Check** (for admin permissions) - Server operator status
3. **Fallback** - Config-based default behavior

## Permission Provider Interface

[`integration/PermissionProvider.java`](../src/main/java/com/hyperfactions/integration/PermissionProvider.java) (implementations in `integration/permissions/`)

```java
public interface PermissionProvider {
    /**
     * Check if player has permission.
     *
     * @return true if has permission, false if denied, null if unknown
     */
    @Nullable
    Boolean hasPermission(UUID playerUuid, String permission);
}
```

The `null` return allows providers to "pass" on permissions they don't handle, letting the next provider in the chain respond.

## HyperPerms Integration

[`integration/permissions/HyperPermsIntegration.java`](../src/main/java/com/hyperfactions/integration/permissions/HyperPermsIntegration.java)

Soft dependency detection via reflection:

```java
public class HyperPermsIntegration {

    private static boolean available = false;

    public static void init() {
        try {
            Class.forName("com.hyperperms.HyperPerms");
            available = true;
            Logger.info("HyperPerms detected - using for permissions");
        } catch (ClassNotFoundException e) {
            available = false;
            Logger.info("HyperPerms not found - using fallback permissions");
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static boolean hasPermission(UUID playerUuid, String permission) {
        if (!available) return false;
        // Call HyperPerms API via reflection or direct call
        return HyperPerms.get().hasPermission(playerUuid, permission);
    }
}
```

## Fallback Behavior

When no provider gives a definitive answer:

| Permission Type | Fallback |
|-----------------|----------|
| User permissions | `allow` (configurable) |
| Admin permissions | Requires OP (configurable) |
| Bypass permissions | **Always deny** |
| Limit permissions | **Always deny** (uses config defaults) |

Configuration in `config.json`:

```json
{
  "permissions": {
    "adminRequiresOp": true,
    "fallbackBehavior": "deny"
  }
}
```

**Security Note:** Bypass and limit permissions are never granted by fallback - they always require explicit permission grants.

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
