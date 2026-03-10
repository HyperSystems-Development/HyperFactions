---
id: admin_zone_flags
---
# Zone Flags

Zones support **47 boolean flags** across 10 categories.
Each flag controls a specific behavior within the zone.

## Flag Categories Overview

| Category | Count | Key Flags |
|----------|-------|-----------|
| Combat | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Damage | 4 | fall_damage, explosion_damage, fire_spread |
| Death | 2 | keep_inventory, power_loss |
| Building | 4 | build_allowed, block_place, hammer_use |
| Interaction | 13 | door_use, container_use, bench_use, npc_tame |
| Transport | 3 | teleporter_use, portal_use, mount_entry |
| Items | 4 | item_drop, item_pickup, invincible_items |
| Mob Spawning | 5 | mob_spawning, hostile/passive/neutral |
| Mob Clearing | 4 | mob_clear, hostile/passive/neutral clear |
| Integration | 5 | gravestone_access, show_on_map, essentials_homes |

## Default Values (SafeZone vs WarZone)

| Flag | SafeZone | WarZone |
|------|----------|---------|
| pvp_enabled | false | **true** |
| build_allowed | false | false |
| fall_damage | false | **true** |
| keep_inventory | **true** | false |
| power_loss | false | **true** |
| mob_spawning | false | **true** |
| item_drop | false | **true** |
| door_use | **true** | **true** |
| container_use | false | **true** |

>[!NOTE] Some flags require **HyperProtect-Mixin** to function (e.g., keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Without the mixin, these flags have no effect even when enabled.

## Setting Flags

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Use `/f admin zone properties <zone>` for a visual toggle editor grouped by category.
