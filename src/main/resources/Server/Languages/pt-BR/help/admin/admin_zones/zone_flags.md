---
id: admin_zone_flags
---
# Flags de Zona

Zonas suportam **47 flags booleanas** em 10 categorias. Cada flag controla um comportamento específico dentro da zona.

## Visão Geral das Categorias de Flags

| Categoria | Quantidade | Flags Principais |
|-----------|------------|------------------|
| Combate | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Dano | 4 | fall_damage, explosion_damage, fire_spread |
| Morte | 2 | keep_inventory, power_loss |
| Construção | 4 | build_allowed, block_place, hammer_use |
| Interação | 13 | door_use, container_use, bench_use, npc_tame |
| Transporte | 3 | teleporter_use, portal_use, mount_entry |
| Itens | 4 | item_drop, item_pickup, invincible_items |
| Spawn de Mobs | 5 | mob_spawning, hostile/passive/neutral |
| Limpeza de Mobs | 4 | mob_clear, hostile/passive/neutral clear |
| Integração | 5 | gravestone_access, show_on_map, essentials_homes |

## Valores Padrão (SafeZone vs WarZone)

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

>[!NOTE] Algumas flags requerem **HyperProtect-Mixin** para funcionar (ex.: keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Sem o mixin, essas flags não têm efeito mesmo quando ativadas.

## Definindo Flags

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Use `/f admin zone properties <zone>` para um editor visual com toggles agrupados por categoria.
