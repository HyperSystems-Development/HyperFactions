---
id: admin_configuration
---
# Configuration System

HyperFactions uses a modular JSON config system with 11 configuration files.

## Admin Config Commands

| Command | Description |
|---------|-------------|
| `/f admin config` | Open the visual config editor GUI |
| `/f admin config <tab>` | Open config editor to a specific tab |
| `/f admin reload` | Reload all config files from disk |
| `/f admin sync` | Synchronize faction data to storage |

**Tab names:** `server` (srv), `chat`, `announcements` (announce, ann), `economy` (eco), `factions` (fac), `factionperms` (facperms, perms), `worldmap` (map), `worlds` (world), `backup`, `debug` (dbg), `gravestones` (graves)

## Configuration Files

| File | Contents |
|------|----------|
| `factions.json` | Roles, power, claims, combat, relations |
| `server.json` | Teleport, auto-save, messages, GUI, permissions |
| `economy.json` | Treasury, upkeep, transaction settings |
| `backup.json` | Backup rotation and retention settings |
| `chat.json` | Faction and ally chat formatting |
| `debug.json` | Debug logging categories |
| `faction-permissions.json` | Per-role permission defaults |
| `announcements.json` | Event broadcast and territory notifications |
| `gravestones.json` | Gravestone integration settings |
| `worldmap.json` | World map refresh modes |
| `worlds.json` | Per-world behavior overrides |

>[!TIP] The config GUI provides a visual editor for every setting. Changes take effect immediately on save — periodic tasks (auto-save, mob clear, upkeep) and the worldmap scheduler are automatically restarted.

## Config Location

All files are stored in:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Manual JSON edits require `/f admin reload` to apply. Invalid JSON will cause the file to be skipped with a warning in the server log.

>[!NOTE] Config version is tracked in `server.json`. The plugin auto-migrates older configs on startup.
