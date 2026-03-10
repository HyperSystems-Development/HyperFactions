---
id: admin_power_overrides
---
# Power Overrides

Special power commands that change how power behaves for specific players or factions.

## Override Commands

| Command | Description |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Set custom max power cap |
| `/f admin power noloss <player>` | Toggle death power penalty immunity |
| `/f admin power nodecay <player>` | Toggle offline power decay immunity |
| `/f admin power info <player>` | View all overrides and power details |

## Custom Max Power

`/f admin power setmax <player> <amount>`
Sets a personal maximum power cap for the player, overriding the server default.

>[!INFO] Setting a custom max does **not** change current power. It only changes the ceiling. The player must still earn power up to the new limit.

## No-Loss Mode

`/f admin power noloss <player>`
Toggles death power loss immunity. When enabled, the player will **not** lose power on death.

Useful for:
- New player protection periods
- Event participants
- Staff members

## No-Decay Mode

`/f admin power nodecay <player>`
Toggles offline power decay immunity. When enabled, the player's power will **not** decrease while offline.

Useful for:
- Players on extended leave
- VIP members
- Seasonal protection

## Power Info

`/f admin power info <player>`
Shows a complete breakdown:

- Current power and max power
- Active overrides (noloss, nodecay, custom max)
- Last death time and power lost
- Faction contribution percentage

>[!TIP] All power overrides persist across server restarts and are stored in the player's data file.
