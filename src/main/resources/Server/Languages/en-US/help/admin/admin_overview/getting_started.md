---
id: admin_getting_started
---
# Getting Started as Admin

Welcome to HyperFactions administration. This guide covers your first steps after installing the plugin.

## Opening the Admin Dashboard

`/f admin`
Opens the admin dashboard GUI with access to all management tools, zone editors, and server settings.

>[!INFO] You need **hyperfactions.admin.use** permission or OP status to access admin commands.

## Requirements

- **With a permission plugin**: Grant `hyperfactions.admin.use`
- **Without a permission plugin**: The player must be a
server operator (`adminRequiresOp=true` by default)

## First Steps After Install

1. Run `/f admin` to verify your access
2. Open **Config** to review default faction settings
3. Create a **SafeZone** at spawn with `/f admin safezone Spawn`
4. Optionally create **WarZones** for PvP arenas
5. Review **Backup** settings to ensure data safety

## Admin Capabilities

| Area | What You Can Do |
|------|----------------|
| Factions | Inspect, modify, or force-disband any faction |
| Zones | Create SafeZones and WarZones with custom flags |
| Power | Override player/faction power values |
| Economy | Manage faction treasuries and upkeep |
| Config | Edit settings live via GUI or reload from disk |
| Backups | Create, restore, and manage data backups |
| Imports | Migrate data from other faction plugins |

>[!TIP] Use `/f admin --text` to get chat-based output instead of the GUI, useful for console or automation.
