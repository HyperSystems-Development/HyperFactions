---
id: admin_zone_commands
---
# Справочник команд зон

Полный справочник по всем командам управления зонами. Все требуют право `hyperfactions.admin.zones`.

## Быстрое создание

| Команда | Описание |
|---------|----------|
| `/f admin safezone <name>` | Создать SafeZone в текущем чанке |
| `/f admin warzone <name>` | Создать WarZone в текущем чанке |
| `/f admin removezone <name>` | Удалить зону и освободить чанки |

## Управление зонами

| Команда | Описание |
|---------|----------|
| `/f admin zone create <name> <type>` | Создать зону (safezone/warzone) |
| `/f admin zone delete <name>` | Удалить зону |
| `/f admin zone claim <zone>` | Добавить текущий чанк в зону |
| `/f admin zone unclaim <zone>` | Убрать текущий чанк из зоны |
| `/f admin zone radius <zone> <r>` | Захватить квадратный радиус чанков |
| `/f admin zone list` | Список всех зон с количеством чанков |
| `/f admin zone notify <zone> <true/false>` | Переключить сообщения входа/выхода |
| `/f admin zone title <zone> upper/lower <text>` | Задать текст заголовка зоны |
| `/f admin zone properties <zone>` | Открыть меню свойств зоны |

## Управление флагами

| Команда | Описание |
|---------|----------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Установить конкретный флаг |

>[!TIP] Используй меню **свойств зоны** для визуального редактора с переключателями для каждого флага, сгруппированными по категориям.

## Примеры

- `/f admin safezone Spawn` -- создать защиту спавна
- `/f admin zone radius Spawn 3` -- расширить до 7x7 чанков
- `/f admin zoneflag Spawn door_use true` -- разрешить двери
- `/f admin zone notify Spawn true` -- показывать сообщения при входе
