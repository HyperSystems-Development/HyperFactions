---
id: admin_configuration
---
# Configuration System

HyperFactions uses a modular JSON config system with
11 configuration files.

## Admin Config Commands

| Command | Description |
|---------|-------------|
| `/f admin config` | Open the visual config editor GUI |
| `/f admin reload` | Reload all config files from disk |
| `/f admin sync` | Synchronize faction data to storage |

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

>[!TIP] The config GUI provides a visual editor with descriptions for every setting. Changes are saved immediately but some require `/f admin reload` to take full effect.

## Config Location

All files are stored in:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Manual JSON edits require `/f admin reload` to apply. Invalid JSON will cause the file to be skipped with a warning in the server log.

>[!NOTE] Config version is tracked in `server.json`. The plugin auto-migrates older configs on startup.
