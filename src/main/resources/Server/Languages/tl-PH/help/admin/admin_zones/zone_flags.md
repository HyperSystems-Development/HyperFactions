---
id: admin_zone_flags
---
# Mga Zone Flag

Ang mga zone ay sumusuporta sa **47 boolean flag** sa 10 kategorya. Bawat flag ay nagkokontrol ng partikular na gawi sa loob ng zone.

## Pangkalahatang-tanaw ng mga Flag Category

| Kategorya | Bilang | Mga Pangunahing Flag |
|-----------|--------|---------------------|
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

## Mga Default na Halaga (SafeZone vs WarZone)

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

>[!NOTE] Ang ilang flag ay nangangailangan ng **HyperProtect-Mixin** para gumana (hal., keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Kung wala ang mixin, ang mga flag na ito ay walang epekto kahit naka-enable.

## Pagse-set ng mga Flag

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Gamitin ang `/f admin zone properties <zone>` para sa visual toggle editor na naka-grupo ayon sa kategorya.
