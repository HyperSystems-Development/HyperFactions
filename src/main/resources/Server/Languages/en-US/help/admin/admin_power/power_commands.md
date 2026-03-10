---
id: admin_power_commands
---
# Power Admin Commands

Override player and faction power values. All commands require `hyperfactions.admin.power` permission.

## Player Power Commands

| Command | Description |
|---------|-------------|
| `/f admin power set <player> <amount>` | Set exact power value |
| `/f admin power add <player> <amount>` | Add power to player |
| `/f admin power remove <player> <amount>` | Remove power from player |
| `/f admin power reset <player>` | Reset to default starting power |
| `/f admin power info <player>` | View detailed power breakdown |

## How Power Affects Factions

A faction's total power is the sum of all its members' individual power. Territory claims require sufficient total power to maintain.

| Scenario | Effect |
|----------|--------|
| Power set higher | Faction can claim more territory |
| Power set lower | Faction may become vulnerable to overclaim |
| Power reset | Returns player to default starting value |

>[!WARNING] Lowering a player's power may cause their faction to lose territory if total power drops below the number of claimed chunks.

## Examples

- `/f admin power set Steve 50` -- set to exactly 50
- `/f admin power add Steve 10` -- increase by 10
- `/f admin power remove Steve 5` -- decrease by 5
- `/f admin power reset Steve` -- back to default
- `/f admin power info Steve` -- show full breakdown

>[!TIP] Use `/f admin power info <player>` to see current power, max power, and any active overrides before making changes.
