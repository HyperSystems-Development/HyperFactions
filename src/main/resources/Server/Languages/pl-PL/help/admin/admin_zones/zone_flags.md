---
id: admin_zone_flags
---
# Flagi stref

Strefy obsługują **47 flag boolowskich** w 10 kategoriach. Każda flaga kontroluje konkretne zachowanie wewnątrz strefy.

## Przegląd kategorii flag

| Kategoria | Liczba | Kluczowe flagi |
|----------|-------|-----------|
| Walka | 7 | pvp_enabled, friendly_fire, mob_damage, pve_damage |
| Obrażenia | 4 | fall_damage, explosion_damage, fire_spread |
| Śmierć | 2 | keep_inventory, power_loss |
| Budowanie | 4 | build_allowed, block_place, hammer_use |
| Interakcja | 13 | door_use, container_use, bench_use, npc_tame |
| Transport | 3 | teleporter_use, portal_use, mount_entry |
| Przedmioty | 4 | item_drop, item_pickup, invincible_items |
| Pojawianie mobów | 5 | mob_spawning, hostile/passive/neutral |
| Czyszczenie mobów | 4 | mob_clear, hostile/passive/neutral clear |
| Integracja | 5 | gravestone_access, show_on_map, essentials_homes |

## Wartości domyślne (SafeZone vs WarZone)

| Flaga | SafeZone | WarZone |
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

>[!NOTE] Niektóre flagi wymagają **HyperProtect-Mixin** do działania (np. keep_inventory, explosion_damage, fire_spread, block_place, npc_tame). Bez mixina te flagi nie mają efektu, nawet gdy są włączone.

## Ustawianie flag

`/f admin zoneflag <zone> <flag> <true/false>`

>[!TIP] Użyj `/f admin zone properties <zone>` dla wizualnego edytora przełączników pogrupowanych według kategorii.
