---
id: admin_permissions
---
# Admin Permissions

All admin features are gated behind permission nodes in the `hyperfactions.admin` namespace.

## Permission Nodes

| Permission | Description |
|-----------|-------------|
| `hyperfactions.admin.*` | Grants **all** admin permissions |
| `hyperfactions.admin.use` | Access `/f admin` dashboard |
| `hyperfactions.admin.reload` | Reload configuration files |
| `hyperfactions.admin.debug` | Toggle debug logging categories |
| `hyperfactions.admin.zones` | Create, edit, and delete zones |
| `hyperfactions.admin.disband` | Force-disband any faction |
| `hyperfactions.admin.modify` | Modify any faction's settings |
| `hyperfactions.admin.bypass.limits` | Bypass claim and power limits |
| `hyperfactions.admin.backup` | Create and restore backups |
| `hyperfactions.admin.power` | Override player power values |
| `hyperfactions.admin.economy` | Manage faction treasuries |

## Fallback Behavior

When **no permission plugin** is installed, admin permissions fall back to server operator (OP) status. This is controlled by `adminRequiresOp` in the server config (default: `true`).

>[!NOTE] The `hyperfactions.admin.*` wildcard grants every admin permission. Use individual nodes for granular control over your staff team.

## Permission Resolution Order

1. **VaultUnlocked** provider (if available)
2. **HyperPerms** provider (if available)
3. **LuckPerms** provider (if available)
4. **OP check** for admin nodes (fallback)

>[!WARNING] Without a permission plugin and with `adminRequiresOp` disabled, admin commands are **open to all players**. Always use a permission plugin in production.
