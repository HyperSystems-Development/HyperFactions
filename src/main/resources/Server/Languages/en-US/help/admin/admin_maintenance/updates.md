---
id: admin_updates
---
# Update Checking

HyperFactions can check for new versions and manage
the HyperProtect-Mixin dependency.

## Update Commands

| Command | Description |
|---------|-------------|
| `/f admin update` | Check for HyperFactions updates |
| `/f admin update mixin` | Check/download HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Toggle auto-download |
| `/f admin version` | Show current version and build info |

## Release Channels

| Channel | Description |
|---------|-------------|
| **Stable** | Recommended for production servers |
| **Pre-release** | Early access to upcoming features |

>[!INFO] The update checker only notifies about new versions. It does **not** automatically install updates to HyperFactions itself.

## HyperProtect-Mixin

HyperProtect-Mixin is the recommended protection
mixin that enables advanced zone flags (explosions,
fire spread, keep inventory, etc.).

- `/f admin update mixin` checks for the latest version
  and downloads it if a newer version is available
- Auto-download can be toggled on or off per server

>[!TIP] After downloading a new mixin version, a server restart is required for the changes to take effect.

## Rollback Procedure

If an update causes issues:

1. Stop the server
2. Replace the plugin JAR with the previous version
3. Start the server
4. Verify functionality with `/f admin version`

>[!WARNING] Downgrading may require a config migration reset. Always keep backups before updating.
