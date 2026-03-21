---
id: admin_treasury_management
---
# Zarzadzanie skarbcem

Komendy administracyjne do zarzadzania skarbcami frakcji. Wymaga uprawnienia `hyperfactions.admin.economy`.

## Komendy skarbca

| Komenda | Opis |
|---------|-------------|
| `/f admin economy balance <faction>` | Wyswietl saldo skarbca frakcji |
| `/f admin economy set <faction> <amount>` | Ustaw dokladne saldo |
| `/f admin economy add <faction> <amount>` | Dodaj fundusze do skarbca |
| `/f admin economy take <faction> <amount>` | Usun fundusze ze skarbca |
| `/f admin economy reset <faction>` | Resetuj skarbiec do zera |

## Przyklady

- `/f admin economy balance Vikings` -- sprawdz saldo
- `/f admin economy set Vikings 5000` -- ustaw na 5000
- `/f admin economy add Vikings 1000` -- wplac 1000
- `/f admin economy take Vikings 500` -- wyplac 500
- `/f admin economy reset Vikings` -- wyzeruj saldo

>[!TIP] Uzyj `/f admin info <faction>`, aby zobaczyc pelny przeglad ekonomii, w tym historie transakcji obok salda skarbca.

## Przypadki uzycia

| Scenariusz | Komenda |
|----------|---------|
| Dystrybucja nagrod za wydarzenie | `economy add <faction> <prize>` |
| Kara za zlamanie regulaminu | `economy take <faction> <fine>` |
| Reset ekonomii po wipe | `economy reset <faction>` |
| Kompensacja za bledy | `economy add <faction> <amount>` |

>[!WARNING] Zmiany w skarbcu sa rejestrowane w historii transakcji frakcji. Modyfikacje administracyjne sa zapisywane z nazwa administratora dla odpowiedzialnosci.

>[!NOTE] Wszystkie komendy ekonomii administracyjnej dzialaja nawet gdy modul ekonomii jest wylaczony w konfiguracji. Dane sa przechowywane niezaleznie od statusu modulu.
