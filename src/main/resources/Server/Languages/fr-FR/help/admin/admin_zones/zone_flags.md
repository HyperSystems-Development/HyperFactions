---
id: admin_zone_flags
---
# Drapeaux de zone

Les zones supportent **47 drapeaux booleens** repartis en 10 categories. Chaque drapeau controle un comportement specifique dans la zone.

## Apercu des categories de drapeaux

| Categorie | Nombre | Drapeaux cles |
|-----------|--------|---------------|
| Combat | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Degats | 4 | fall_damage, explosion_damage, fire_spread |
| Mort | 2 | keep_inventory, power_loss |
| Construction | 4 | build_allowed, block_place, hammer_use |
| Interaction | 13 | door_use, container_use, bench_use, npc_tame |
| Transport | 3 | teleporter_use, portal_use, mount_entry |
| Objets | 4 | item_drop, item_pickup, invincible_items |
| Apparition de mobs | 5 | mob_spawning, hostile/passive/neutral |
| Nettoyage de mobs | 4 | mob_clear, hostile/passive/neutral clear |
| Integration | 5 | gravestone_access, show_on_map, essentials_homes |

## Valeurs par defaut (SafeZone vs WarZone)

| Drapeau | SafeZone | WarZone |
|---------|----------|---------|
| pvp_enabled | false | **true** |
| build_allowed | false | false |
| fall_damage | false | **true** |
| keep_inventory | **true** | false |
| power_loss | false | **true** |
| mob_spawning | false | **true** |
| item_drop | false | **true** |
| door_use | **true** | **true** |
| container_use | false | **true** |

>[!NOTE] Certains drapeaux necessitent **HyperProtect-Mixin** pour fonctionner (ex. : keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Sans le mixin, ces drapeaux n'ont aucun effet meme lorsqu'ils sont actives.

## Definir des drapeaux

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Utilisez `/f admin zone properties <zone>` pour un editeur visuel avec bascules groupees par categorie.
