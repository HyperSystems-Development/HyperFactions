---
id: admin_backups
---
# Backup System

HyperFactions includes automatic and manual backups
with GFS (Grandfather-Father-Son) rotation.

## Backup Commands

| Command | Description |
|---------|-------------|
| `/f admin backup create` | Create a manual backup now |
| `/f admin backup list` | List all available backups |
| `/f admin backup restore <name>` | Restore from a backup |
| `/f admin backup delete <name>` | Delete a specific backup |

**Permission**: `hyperfactions.admin.backup`

## GFS Rotation Defaults

| Type | Retention | Description |
|------|-----------|-------------|
| Hourly | 24 | Last 24 hourly snapshots |
| Daily | 7 | Last 7 daily snapshots |
| Weekly | 4 | Last 4 weekly snapshots |
| Manual | 10 | Manually created backups |
| Shutdown | 5 | Created on server stop |

>[!INFO] Shutdown backups are enabled by default (`onShutdown=true`). They capture the latest state before the server stops.

## Backup Contents

Each backup ZIP archive contains:
- All faction data files
- Player power data
- Zone definitions
- Chat history and economy data
- Invite and join request data
- Configuration files

>[!WARNING] **Restoring a backup is destructive.** It replaces all current data with the backup's contents. Any changes made after the backup was created will be lost. Always create a fresh backup before restoring.

## Best Practices

1. Create a manual backup before major admin actions
2. Review backup retention in `backup.json`
3. Test restore on a staging server first
4. Keep shutdown backups enabled for crash recovery
