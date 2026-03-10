---
id: admin_world_settings
---
# Per-World Settings

HyperFactions supports per-world configuration for
claiming, PvP, and protection behavior.

## World Commands

| Command | Description |
|---------|-------------|
| `/f admin world list` | List all world overrides |
| `/f admin world info <world>` | Show settings for a world |
| `/f admin world set <world> <key> <value>` | Set a setting |
| `/f admin world reset <world>` | Reset world to defaults |

## Available Settings

| Setting | Type | Description |
|---------|------|-------------|
| claiming_enabled | boolean | Allow faction claims in this world |
| pvp_enabled | boolean | Allow PvP combat in this world |
| power_loss | boolean | Apply power loss on death |
| build_protection | boolean | Enforce claim build protection |
| explosion_protection | boolean | Protect claims from explosions |

## World Whitelist / Blacklist

Control which worlds allow faction features through
the `worlds.json` config file:

- **Whitelist mode**: Only listed worlds allow claiming
- **Blacklist mode**: All worlds allow claiming except listed

>[!INFO] World settings are stored in `worlds.json` and override the global defaults from `factions.json`.

## Examples

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- restore all defaults

>[!TIP] Disable claiming in creative or lobby worlds to keep the faction system focused on survival gameplay.

>[!NOTE] Per-world settings take priority over global config but are overridden by zone flags within that world.
