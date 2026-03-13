---
id: admin_zone_flags
---
# Zonen-Flags

Zonen unterstuetzen **47 boolesche Flags** in 10 Kategorien. Jedes Flag steuert ein bestimmtes Verhalten innerhalb der Zone.

## Flag-Kategorienuebersicht

| Kategorie | Anzahl | Wichtige Flags |
|----------|-------|-----------|
| Kampf | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Schaden | 4 | fall_damage, explosion_damage, fire_spread |
| Tod | 2 | keep_inventory, power_loss |
| Bauen | 4 | build_allowed, block_place, hammer_use |
| Interaktion | 13 | door_use, container_use, bench_use, npc_tame |
| Transport | 3 | teleporter_use, portal_use, mount_entry |
| Gegenstaende | 4 | item_drop, item_pickup, invincible_items |
| Mob-Spawning | 5 | mob_spawning, hostile/passive/neutral |
| Mob-Bereinigung | 4 | mob_clear, hostile/passive/neutral clear |
| Integration | 5 | gravestone_access, show_on_map, essentials_homes |

## Standardwerte (SafeZone vs WarZone)

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

>[!NOTE] Einige Flags erfordern **HyperProtect-Mixin** zur Funktion (z.B. keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Ohne das Mixin haben diese Flags keine Wirkung, selbst wenn sie aktiviert sind.

## Flags setzen

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Nutze `/f admin zone properties <zone>` fuer einen visuellen Schalter-Editor, nach Kategorie gruppiert.
