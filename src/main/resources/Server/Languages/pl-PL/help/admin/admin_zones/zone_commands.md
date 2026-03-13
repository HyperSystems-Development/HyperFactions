---
id: admin_zone_commands
---
# Opis komend stref

Kompletna lista wszystkich komend zarządzania strefami. Wszystkie wymagają uprawnienia `hyperfactions.admin.zones`.

## Szybkie tworzenie

| Komenda | Opis |
|---------|-------------|
| `/f admin safezone <name>` | Utwórz SafeZone na obecnym chunku |
| `/f admin warzone <name>` | Utwórz WarZone na obecnym chunku |
| `/f admin removezone <name>` | Usuń strefę i zwolnij chunki |

## Zarządzanie strefami

| Komenda | Opis |
|---------|-------------|
| `/f admin zone create <name> <type>` | Utwórz strefę (safezone/warzone) |
| `/f admin zone delete <name>` | Usuń strefę |
| `/f admin zone claim <zone>` | Dodaj obecny chunk do strefy |
| `/f admin zone unclaim <zone>` | Usuń obecny chunk ze strefy |
| `/f admin zone radius <zone> <r>` | Zajmij kwadratowy promień chunków |
| `/f admin zone list` | Lista wszystkich stref z liczbą chunków |
| `/f admin zone notify <zone> <true/false>` | Przełącz wiadomości wejścia/wyjścia |
| `/f admin zone title <zone> upper/lower <text>` | Ustaw tekst tytułu strefy |
| `/f admin zone properties <zone>` | Otwórz GUI właściwości strefy |

## Zarządzanie flagami

| Komenda | Opis |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Ustaw konkretną flagę |

>[!TIP] Użyj **GUI właściwości** strefy dla wizualnego edytora z przełącznikami dla każdej flagi, zorganizowanymi według kategorii.

## Przykłady

- `/f admin safezone Spawn` -- utwórz ochronę spawnu
- `/f admin zone radius Spawn 3` -- rozszerz do 7x7 chunków
- `/f admin zoneflag Spawn door_use true` -- zezwól na drzwi
- `/f admin zone notify Spawn true` -- pokaż wiadomości wejścia
