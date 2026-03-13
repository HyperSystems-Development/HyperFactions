---
id: admin_upkeep_management
---
# Upkeep Management

Faction upkeep charges factions periodically based on their territory and member count.

## Admin Controls

Upkeep settings are managed through the economy config file or the admin config GUI.

`/f admin config`
Open the config editor and navigate to economy settings to adjust upkeep values.

## Default Upkeep Settings

| Setting | Default | Description |
|---------|---------|-------------|
| Upkeep enabled | false | Master toggle for the system |
| Upkeep interval | 24h | How often upkeep is charged |
| Per-claim cost | 5.0 | Cost per claimed chunk per cycle |
| Per-member cost | 0.0 | Cost per member per cycle |
| Grace period | 72h | New factions are exempt |
| Disband on bankrupt | false | Auto-disband if cannot pay |

## Monitoring Upkeep

Use `/f admin info <faction>` to see:
- Current treasury balance
- Estimated upkeep cost per cycle
- Time until next upkeep charge
- Whether the faction can afford upkeep

>[!TIP] Review economy statistics across all factions from the admin dashboard to identify factions at risk of bankruptcy before upkeep triggers.

>[!INFO] Upkeep configuration is stored in `economy.json`. Changes made via the config GUI take effect after reload with `/f admin reload`.

## Upkeep Formula

**Total upkeep** = (claimed chunks x per-claim cost) + (member count x per-member cost)

>[!WARNING] Enabling upkeep on a server with existing factions may cause unexpected bankruptcies. Consider setting a grace period or announcing the change in advance.
