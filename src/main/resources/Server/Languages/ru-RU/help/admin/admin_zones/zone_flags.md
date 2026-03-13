---
id: admin_zone_flags
---
# Флаги зон

Зоны поддерживают **47 булевых флагов** в 10 категориях. Каждый флаг контролирует конкретное поведение внутри зоны.

## Обзор категорий флагов

| Категория | Кол-во | Ключевые флаги |
|-----------|--------|----------------|
| Бой | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Урон | 4 | fall_damage, explosion_damage, fire_spread |
| Смерть | 2 | keep_inventory, power_loss |
| Строительство | 4 | build_allowed, block_place, hammer_use |
| Взаимодействие | 13 | door_use, container_use, bench_use, npc_tame |
| Транспорт | 3 | teleporter_use, portal_use, mount_entry |
| Предметы | 4 | item_drop, item_pickup, invincible_items |
| Спавн мобов | 5 | mob_spawning, hostile/passive/neutral |
| Очистка мобов | 4 | mob_clear, hostile/passive/neutral clear |
| Интеграция | 5 | gravestone_access, show_on_map, essentials_homes |

## Значения по умолчанию (SafeZone vs WarZone)

| Флаг | SafeZone | WarZone |
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

>[!NOTE] Некоторые флаги требуют **HyperProtect-Mixin** для работы (например, keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Без миксина эти флаги не действуют, даже если включены.

## Установка флагов

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Используй `/f admin zone properties <zone>` для визуального редактора переключателей, сгруппированных по категориям.
