---
id: admin_zone_flags
---
# Flag delle Zone

Le zone supportano **47 flag booleani** in 10 categorie. Ogni flag controlla un comportamento specifico all'interno della zona.

## Panoramica Categorie Flag

| Categoria | Conteggio | Flag Principali |
|-----------|-----------|-----------------|
| Combattimento | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Danni | 4 | fall_damage, explosion_damage, fire_spread |
| Morte | 2 | keep_inventory, power_loss |
| Costruzione | 4 | build_allowed, block_place, hammer_use |
| Interazione | 13 | door_use, container_use, bench_use, npc_tame |
| Trasporto | 3 | teleporter_use, portal_use, mount_entry |
| Oggetti | 4 | item_drop, item_pickup, invincible_items |
| Spawn Mob | 5 | mob_spawning, hostile/passive/neutral |
| Rimozione Mob | 4 | mob_clear, hostile/passive/neutral clear |
| Integrazione | 5 | gravestone_access, show_on_map, essentials_homes |

## Valori Predefiniti (SafeZone vs WarZone)

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

>[!NOTE] Alcuni flag richiedono **HyperProtect-Mixin** per funzionare (es. keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Senza il mixin, questi flag non hanno effetto anche quando abilitati.

## Impostare i Flag

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Usa `/f admin zone properties <zone>` per un editor visuale con interruttori raggruppati per categoria.
