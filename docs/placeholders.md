# HyperFactions Placeholders

> **Version**: 0.9.0 | **Expansion Identifier**: `factions` | **35 placeholders**

HyperFactions exposes faction data as placeholders through two placeholder APIs: **PlaceholderAPI (PAPI)** and **WiFlow PlaceholderAPI**. Both APIs support the same set of placeholders with identical behavior.

---

## Table of Contents

- [Placeholder Format](#placeholder-format)
- [Null Behavior](#null-behavior)
- [Player Faction Info](#player-faction-info)
- [Power](#power)
- [Territory](#territory)
- [Faction Home](#faction-home)
- [Members & Relations](#members--relations)
- [Configuration](#configuration)
- [Usage Examples](#usage-examples)

---

## Placeholder Format

| API | Format | Example |
|-----|--------|---------|
| PlaceholderAPI (PAPI) | `%factions_<placeholder>%` | `%factions_name%` |
| WiFlow PlaceholderAPI | `{factions_<placeholder>}` | `{factions_name}` |

Both APIs are registered under the identifier `factions`. Detection is automatic — if the API mod is installed, the expansion registers on startup.

---

## Factionless Player Behavior

All placeholders return a value even when the player has no faction. This ensures placeholder APIs (especially WiFlow, which preserves raw text on `null`) always resolve cleanly.

### Default Values for Factionless Players

| Type | Default | Examples |
|------|---------|----------|
| Text placeholders | `""` (empty string) | `name`, `tag`, `display`, `color`, `role`, `description`, `leader`, `leader_id`, `open`, `created` |
| Numeric placeholders | `"0"` or `"0.0"` | `faction_power`, `faction_maxpower`, `faction_power_percent`, `land`, `land_max`, `members`, `members_online`, `allies`, `enemies`, `neutrals`, `relations` |
| Boolean placeholders | `"false"` | `raidable` |
| Home placeholders | `""` (empty string) | `home_world`, `home_x`, `home_y`, `home_z`, `home_coords`, `home_yaw`, `home_pitch` |

### Placeholders That Always Return Meaningful Data

| Placeholder | Reason |
|-------------|--------|
| `has_faction` | Always returns `yes` or `no` |
| `power` | Player power always exists (defaults to config values) |
| `maxpower` | Player max power always exists |
| `power_percent` | Calculated from power/maxpower |

---

## Player Faction Info

12 placeholders for basic faction information.

| Placeholder | Description | Returns | Example |
|-------------|-------------|---------|---------|
| `has_faction` | Whether the player is in a faction | `yes` / `no` | `yes` |
| `name` | Faction name | String or `""` | `Warriors` |
| `faction_id` | Faction UUID | UUID string or `""` | `a1b2c3d4-...` |
| `tag` | Faction short tag | String or `""` | `WAR` |
| `display` | Display text (tag, name, or none based on config) | String or `""` | `WAR` |
| `color` | Faction color hex code | Hex string or `""` | `#FF5555` |
| `role` | Player's faction role | Role name or `""` | `Leader`, `Officer`, `Member` |
| `description` | Faction description text | String or `""` | `The best faction` |
| `leader` | Faction leader's username | String or `""` | `Steve` |
| `leader_id` | Faction leader's UUID | UUID string or `""` | `d4e5f6a7-...` |
| `open` | Whether faction accepts join requests | `"false"` if no faction | `true` |
| `created` | Faction creation date | `yyyy-MM-dd` or `""` | `2025-01-15` |

### Display Placeholder Behavior

The `display` placeholder respects the `chatTagDisplay` config setting:

| Config Value | Behavior |
|-------------|----------|
| `tag` | Returns faction tag; falls back to first 3 characters of name (uppercased) if no tag set |
| `name` | Returns full faction name |
| `none` | Returns `""` (empty string) |

---

## Power

7 placeholders for power-related data.

| Placeholder | Description | Returns | Example |
|-------------|-------------|---------|---------|
| `power` | Player's current power (1 decimal) | Always present | `8.5` |
| `maxpower` | Player's max power (1 decimal) | Always present | `10.0` |
| `power_percent` | Player's power as percentage | Always present | `85` |
| `faction_power` | Faction's total power (1 decimal) | `"0"` if no faction | `42.5` |
| `faction_maxpower` | Faction's max power (1 decimal) | `"0"` if no faction | `50.0` |
| `faction_power_percent` | Faction's power as percentage | `"0"` if no faction | `85` |
| `raidable` | Whether faction is raidable (power < land) | `"false"` if no faction | `false` |

> **Note**: `power`, `maxpower`, and `power_percent` are player-level and always available even without a faction. The `faction_*` variants require faction membership.

---

## Territory

4 placeholders for location-based territory data.

| Placeholder | Description | Returns | Example |
|-------------|-------------|---------|---------|
| `land` | Number of chunks claimed by faction | `"0"` if no faction | `12` |
| `land_max` | Maximum claimable chunks | `"0"` if no faction | `20` |
| `territory` | Name of faction owning current chunk | String or `""` | `Warriors` |
| `territory_type` | Type of territory at current location | String or `""` | `Claimed` |

### Territory Values

| Value | Meaning |
|-------|---------|
| `Wilderness` | Unclaimed land |
| `SafeZone` | Admin-defined safe zone |
| `WarZone` | Admin-defined war zone |
| `Claimed` | Claimed by a faction (territory_type only) |
| *Faction name* | Name of owning faction (territory only) |

> **Note**: `territory` returns the faction name for claimed chunks, while `territory_type` returns `Claimed`. Both return `SafeZone`/`WarZone`/`Wilderness` for non-faction-owned chunks.

---

## Faction Home

7 placeholders for faction home location.

| Placeholder | Description | Returns | Example |
|-------------|-------------|---------|---------|
| `home_world` | World name of faction home | String or `""` | `world` |
| `home_x` | X coordinate (2 decimals) | `"0"` if no faction | `123.45` |
| `home_y` | Y coordinate (2 decimals) | `"0"` if no faction | `64.00` |
| `home_z` | Z coordinate (2 decimals) | `"0"` if no faction | `-456.78` |
| `home_coords` | Combined X, Y, Z (2 decimals) | String or `""` | `123.45, 64.00, -456.78` |
| `home_yaw` | Yaw angle (2 decimals) | `"0"` if no faction | `90.00` |
| `home_pitch` | Pitch angle (2 decimals) | `"0"` if no faction | `0.00` |

All home placeholders return `""` (empty string) if the player has no faction or the faction has no home set.

---

## Members & Relations

6 placeholders for membership and diplomatic relations.

| Placeholder | Description | Returns | Example |
|-------------|-------------|---------|---------|
| `members` | Total faction member count | `"0"` if no faction | `5` |
| `members_online` | Currently online member count | `"0"` if no faction | `3` |
| `allies` | Number of allied factions | `"0"` if no faction | `2` |
| `enemies` | Number of enemy factions | `"0"` if no faction | `1` |
| `neutrals` | Number of neutral relations | `"0"` if no faction | `4` |
| `relations` | Total number of relations (all types) | `"0"` if no faction | `7` |

---

## Configuration

### PAPI Integration

PlaceholderAPI registration happens automatically when the `PlaceholderAPI` mod is detected at startup. No configuration is needed.

**Requirements**:
- PlaceholderAPI mod installed on the server
- HyperFactions loaded and enabled

### WiFlow Integration

WiFlow PlaceholderAPI registration happens automatically when the `WiFlow` mod is detected at startup. No configuration is needed.

**Requirements**:
- WiFlow mod installed on the server (includes built-in PlaceholderAPI)
- HyperFactions loaded and enabled

### Display Configuration

The `display` placeholder behavior is controlled by the `chatTagDisplay` setting in `config/chat.json`:

```json
{
  "chatTagDisplay": "tag"
}
```

Valid values: `tag`, `name`, `none`

---

## Usage Examples

### BetterScoreBoard

```
# Show faction name on scoreboard
{factions_name}

# Show power bar
Power: {factions_power}/{factions_maxpower}

# Show territory
Location: {factions_territory}
```

### Chat Formatting (WiFlow)

```
# Prefix with faction tag
[{factions_tag}] {player_name}: {message}

# Show role
[{factions_role}] {player_name}
```

### Chat Formatting (PAPI)

```
# Prefix with faction tag
[%factions_tag%] %player_name%: %message%

# Show faction color + name
%factions_color%%factions_name%
```

### Conditional Display

Faction-specific placeholders return empty strings or zero values for factionless players. This means `{factions_name}` resolves to `""` rather than showing the raw placeholder text. Consumers can use `{factions_has_faction}` to conditionally show/hide faction-related sections.

---

## Technical Details

### Source Files

| File | Purpose |
|------|---------|
| [`integration/placeholder/HyperFactionsExpansion.java`](../src/main/java/com/hyperfactions/integration/placeholder/HyperFactionsExpansion.java) | PAPI expansion (33 placeholders) |
| [`integration/placeholder/WiFlowExpansion.java`](../src/main/java/com/hyperfactions/integration/placeholder/WiFlowExpansion.java) | WiFlow expansion (33 placeholders) |
| [`integration/placeholder/PlaceholderAPIIntegration.java`](../src/main/java/com/hyperfactions/integration/placeholder/PlaceholderAPIIntegration.java) | PAPI detection and registration |
| [`integration/placeholder/WiFlowPlaceholderIntegration.java`](../src/main/java/com/hyperfactions/integration/placeholder/WiFlowPlaceholderIntegration.java) | WiFlow detection and registration |

### Persistence

Both expansions use `persist() = true`, which means they survive plugin reloads without needing to re-register.

### Territory Coordinate Handling

- **PAPI**: Uses `TransformComponent` from the player's ECS entity to get world position, then converts to chunk coordinates via `>> 4`
- **WiFlow**: Uses `PlaceholderContext.getPosX()/getPosZ()` (block coordinates) and converts to chunk coordinates via `>> 4`

---

## Quick Reference — All Placeholders

Complete side-by-side table of every placeholder in both formats.

| # | PAPI | WiFlow | Description | Example |
|---|------|--------|-------------|---------|
| | **Player Faction Info** | | | |
| 1 | `%factions_has_faction%` | `{factions_has_faction}` | Has a faction (yes/no) | `yes` |
| 2 | `%factions_name%` | `{factions_name}` | Faction name | `Warriors` |
| 3 | `%factions_faction_id%` | `{factions_faction_id}` | Faction UUID | `a1b2c3d4-...` |
| 4 | `%factions_tag%` | `{factions_tag}` | Faction short tag | `WAR` |
| 5 | `%factions_display%` | `{factions_display}` | Display text (config-dependent) | `WAR` |
| 6 | `%factions_color%` | `{factions_color}` | Faction color hex | `#FF5555` |
| 7 | `%factions_role%` | `{factions_role}` | Player's role | `Officer` |
| 8 | `%factions_description%` | `{factions_description}` | Faction description | `The best faction` |
| 9 | `%factions_leader%` | `{factions_leader}` | Leader's username | `Steve` |
| 10 | `%factions_leader_id%` | `{factions_leader_id}` | Leader's UUID | `d4e5f6a7-...` |
| 11 | `%factions_open%` | `{factions_open}` | Open status | `true` |
| 12 | `%factions_created%` | `{factions_created}` | Creation date | `2025-01-15` |
| | **Power** | | | |
| 13 | `%factions_power%` | `{factions_power}` | Player power | `8.5` |
| 14 | `%factions_maxpower%` | `{factions_maxpower}` | Player max power | `10.0` |
| 15 | `%factions_power_percent%` | `{factions_power_percent}` | Player power % | `85` |
| 16 | `%factions_faction_power%` | `{factions_faction_power}` | Faction total power | `42.5` |
| 17 | `%factions_faction_maxpower%` | `{factions_faction_maxpower}` | Faction max power | `50.0` |
| 18 | `%factions_faction_power_percent%` | `{factions_faction_power_percent}` | Faction power % | `85` |
| 19 | `%factions_raidable%` | `{factions_raidable}` | Raidable status | `false` |
| | **Territory** | | | |
| 20 | `%factions_land%` | `{factions_land}` | Claimed chunk count | `12` |
| 21 | `%factions_land_max%` | `{factions_land_max}` | Max claimable chunks | `20` |
| 22 | `%factions_territory%` | `{factions_territory}` | Owner of current chunk | `Warriors` |
| 23 | `%factions_territory_type%` | `{factions_territory_type}` | Territory type | `Claimed` |
| | **Faction Home** | | | |
| 24 | `%factions_home_world%` | `{factions_home_world}` | Home world name | `world` |
| 25 | `%factions_home_x%` | `{factions_home_x}` | Home X coordinate | `123.45` |
| 26 | `%factions_home_y%` | `{factions_home_y}` | Home Y coordinate | `64.00` |
| 27 | `%factions_home_z%` | `{factions_home_z}` | Home Z coordinate | `-456.78` |
| 28 | `%factions_home_coords%` | `{factions_home_coords}` | Home X, Y, Z combined | `123.45, 64.00, -456.78` |
| 29 | `%factions_home_yaw%` | `{factions_home_yaw}` | Home yaw angle | `90.00` |
| 30 | `%factions_home_pitch%` | `{factions_home_pitch}` | Home pitch angle | `0.00` |
| | **Members & Relations** | | | |
| 31 | `%factions_members%` | `{factions_members}` | Total member count | `5` |
| 32 | `%factions_members_online%` | `{factions_members_online}` | Online member count | `3` |
| 33 | `%factions_allies%` | `{factions_allies}` | Allied faction count | `2` |
| 34 | `%factions_enemies%` | `{factions_enemies}` | Enemy faction count | `1` |
| 35 | `%factions_neutrals%` | `{factions_neutrals}` | Neutral relation count | `4` |
| 36 | `%factions_relations%` | `{factions_relations}` | Total relation count | `7` |
