---
id: admin_zone_commands
---
# Zone Command Reference

Complete reference for all zone management commands. All require `hyperfactions.admin.zones` permission.

## Quick Creation

| Command | Description |
|---------|-------------|
| `/f admin safezone <name>` | Create a SafeZone at current chunk |
| `/f admin warzone <name>` | Create a WarZone at current chunk |
| `/f admin removezone <name>` | Delete a zone and release chunks |

## Zone Management

| Command | Description |
|---------|-------------|
| `/f admin zone create <name> <type>` | Create a zone (safezone/warzone) |
| `/f admin zone delete <name>` | Delete a zone |
| `/f admin zone claim <zone>` | Add current chunk to zone |
| `/f admin zone unclaim <zone>` | Remove current chunk from zone |
| `/f admin zone radius <zone> <r>` | Claim square radius of chunks |
| `/f admin zone list` | List all zones with chunk counts |
| `/f admin zone notify <zone> <true/false>` | Toggle entry/leave messages |
| `/f admin zone title <zone> upper/lower <text>` | Set zone title text |
| `/f admin zone properties <zone>` | Open zone properties GUI |

## Flag Management

| Command | Description |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Set a specific flag |

>[!TIP] Use the zone **properties GUI** for a visual editor with toggles for every flag, organized by category.

## Examples

- `/f admin safezone Spawn` -- create spawn protection
- `/f admin zone radius Spawn 3` -- expand to 7x7 chunks
- `/f admin zoneflag Spawn door_use true` -- allow doors
- `/f admin zone notify Spawn true` -- show entry messages
