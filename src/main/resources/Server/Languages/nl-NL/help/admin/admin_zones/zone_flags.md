---
id: admin_zone_flags
---
# Zonevlaggen

Zones ondersteunen **47 booleaanse vlaggen** verdeeld over 10 categorieën. Elke vlag regelt een specifiek gedrag binnen de zone.

## Overzicht Vlagcategorieën

| Categorie | Aantal | Belangrijkste Vlaggen |
|-----------|--------|----------------------|
| Gevecht | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Schade | 4 | fall_damage, explosion_damage, fire_spread |
| Dood | 2 | keep_inventory, power_loss |
| Bouwen | 4 | build_allowed, block_place, hammer_use |
| Interactie | 13 | door_use, container_use, bench_use, npc_tame |
| Transport | 3 | teleporter_use, portal_use, mount_entry |
| Items | 4 | item_drop, item_pickup, invincible_items |
| Mob Spawning | 5 | mob_spawning, hostile/passive/neutral |
| Mob Verwijderen | 4 | mob_clear, hostile/passive/neutral clear |
| Integratie | 5 | gravestone_access, show_on_map, essentials_homes |

## Standaardwaarden (SafeZone vs WarZone)

| Vlag | SafeZone | WarZone |
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

>[!NOTE] Sommige vlaggen vereisen **HyperProtect-Mixin** om te functioneren (bijv. keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Zonder de mixin hebben deze vlaggen geen effect, zelfs als ze zijn ingeschakeld.

## Vlaggen Instellen

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Gebruik `/f admin zone properties <zone>` voor een visuele schakel-editor gegroepeerd per categorie.
