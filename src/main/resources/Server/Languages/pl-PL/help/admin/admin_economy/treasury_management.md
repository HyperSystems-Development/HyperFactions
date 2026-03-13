---
id: admin_treasury_management
---
# Zarządzanie skarbcem

Komendy administracyjne do zarządzania skarbcami frakcji. Wymaga uprawnienia `hyperfactions.admin.economy`.

## Komendy skarbca

| Komenda | Opis |
|---------|-------------|
| `/f admin economy balance <faction>` | Wyświetl saldo skarbca frakcji |
| `/f admin economy set <faction> <amount>` | Ustaw dokładne saldo |
| `/f admin economy add <faction> <amount>` | Dodaj fundusze do skarbca |
| `/f admin economy take <faction> <amount>` | Usuń fundusze ze skarbca |
| `/f admin economy reset <faction>` | Resetuj skarbiec do zera |

## Przykłady

- `/f admin economy balance Vikings` -- sprawdź saldo
- `/f admin economy set Vikings 5000` -- ustaw na 5000
- `/f admin economy add Vikings 1000` -- wpłać 1000
- `/f admin economy take Vikings 500` -- wypłać 500
- `/f admin economy reset Vikings` -- wyzeruj saldo

>[!TIP] Użyj `/f admin info <faction>`, aby zobaczyć pełny przegląd ekonomii, w tym historię transakcji obok salda skarbca.

## Przypadki użycia

| Scenariusz | Komenda |
|----------|---------|
| Dystrybucja nagród za wydarzenie | `economy add <faction> <prize>` |
| Kara za złamanie regulaminu | `economy take <faction> <fine>` |
| Reset ekonomii po wipe | `economy reset <faction>` |
| Kompensacja za błędy | `economy add <faction> <amount>` |

>[!WARNING] Zmiany w skarbcu są rejestrowane w historii transakcji frakcji. Modyfikacje administracyjne są zapisywane z nazwą administratora dla odpowiedzialności.

>[!NOTE] Wszystkie komendy ekonomii administracyjnej działają nawet gdy moduł ekonomii jest wyłączony w konfiguracji. Dane są przechowywane niezależnie od statusu modułu.
