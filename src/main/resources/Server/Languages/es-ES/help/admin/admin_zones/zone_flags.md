---
id: admin_zone_flags
---
# Indicadores de Zona

Las zonas soportan **47 indicadores booleanos** en 10 categorias.
Cada indicador controla un comportamiento especifico dentro de la zona.

## Resumen de Categorias de Indicadores

| Categoria | Cantidad | Indicadores Clave |
|----------|-------|-----------|
| Combate | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Dano | 4 | fall_damage, explosion_damage, fire_spread |
| Muerte | 2 | keep_inventory, power_loss |
| Construccion | 4 | build_allowed, block_place, hammer_use |
| Interaccion | 13 | door_use, container_use, bench_use, npc_tame |
| Transporte | 3 | teleporter_use, portal_use, mount_entry |
| Objetos | 4 | item_drop, item_pickup, invincible_items |
| Aparicion de Mobs | 5 | mob_spawning, hostile/passive/neutral |
| Limpieza de Mobs | 4 | mob_clear, hostile/passive/neutral clear |
| Integracion | 5 | gravestone_access, show_on_map, essentials_homes |

## Valores Predeterminados (Zona Segura vs Zona de Guerra)

| Indicador | Zona Segura | Zona de Guerra |
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

>[!NOTE] Algunos indicadores requieren **HyperProtect-Mixin** para funcionar (ej., keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Sin el mixin, estos indicadores no tienen efecto aunque esten habilitados.

## Establecer Indicadores

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Usa `/f admin zone properties <zone>` para un editor visual con interruptores agrupados por categoria.
