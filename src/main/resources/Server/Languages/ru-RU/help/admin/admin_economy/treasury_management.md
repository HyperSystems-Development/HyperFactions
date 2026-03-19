---
id: admin_treasury_management
---
# Управление казной

Админ-команды для управления казнами фракций. Требуется право `hyperfactions.admin.economy`.

## Команды казны

| Команда | Описание |
|---------|----------|
| `/f admin economy balance <faction>` | Просмотр баланса казны фракции |
| `/f admin economy set <faction> <amount>` | Установить точный баланс |
| `/f admin economy add <faction> <amount>` | Добавить средства в казну |
| `/f admin economy take <faction> <amount>` | Снять средства из казны |
| `/f admin economy reset <faction>` | Сбросить казну до нуля |

## Примеры

- `/f admin economy balance Vikings` -- проверить баланс
- `/f admin economy set Vikings 5000` -- установить 5000
- `/f admin economy add Vikings 1000` -- внести 1000
- `/f admin economy take Vikings 500` -- снять 500
- `/f admin economy reset Vikings` -- обнулить баланс

>[!TIP] Используй `/f admin info <faction>`, чтобы увидеть полный обзор экономики, включая историю транзакций вместе с балансом казны.

## Случаи использования

| Сценарий | Команда |
|----------|---------|
| Распределение призов за мероприятие | `economy add <faction> <prize>` |
| Штраф за нарушение правил | `economy take <faction> <fine>` |
| Сброс экономики после вайпа | `economy reset <faction>` |
| Компенсация за баги | `economy add <faction> <amount>` |

>[!WARNING] Изменения казны записываются в историю транзакций фракции. Действия администратора фиксируются с именем админа для подотчётности.

>[!NOTE] Все админ-команды экономики работают даже когда модуль экономики отключён в конфигурации. Данные хранятся независимо от статуса модуля.
