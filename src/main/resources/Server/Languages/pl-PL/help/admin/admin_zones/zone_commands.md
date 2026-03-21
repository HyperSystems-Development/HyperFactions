---
id: admin_zone_commands
---
# Opis komend stref

Kompletna lista wszystkich komend zarzadzania strefami. Wszystkie wymagaja uprawnienia `hyperfactions.admin.zones`.

## Szybkie tworzenie

| Komenda | Opis |
|---------|-------------|
| `/f admin safezone <name>` | Utworz SafeZone na obecnym chunku |
| `/f admin warzone <name>` | Utworz WarZone na obecnym chunku |
| `/f admin removezone <name>` | Usun strefe i zwolnij chunki |

## Zarzadzanie strefami

| Komenda | Opis |
|---------|-------------|
| `/f admin zone create <name> <type>` | Utworz strefe (safezone/warzone) |
| `/f admin zone delete <name>` | Usun strefe |
| `/f admin zone claim <zone>` | Dodaj obecny chunk do strefy |
| `/f admin zone unclaim <zone>` | Usun obecny chunk ze strefy |
| `/f admin zone radius <zone> <r>` | Zajmij kwadratowy promien chunkow |
| `/f admin zone list` | Lista wszystkich stref z liczba chunkow |
| `/f admin zone notify <zone> <true/false>` | Przelacz wiadomosci wejscia/wyjscia |
| `/f admin zone title <zone> upper/lower <text>` | Ustaw tekst tytulu strefy |
| `/f admin zone properties <zone>` | Otworz GUI wlasciwosci strefy |

## Zarzadzanie flagami

| Komenda | Opis |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Ustaw konkretna flage |

>[!TIP] Uzyj **GUI wlasciwosci** strefy dla wizualnego edytora z przelacznikami dla kazdej flagi, zorganizowanymi wedlug kategorii.

## Przyklady

- `/f admin safezone Spawn` -- utworz ochrone spawnu
- `/f admin zone radius Spawn 3` -- rozszerz do 7x7 chunkow
- `/f admin zoneflag Spawn door_use true` -- zezwol na drzwi
- `/f admin zone notify Spawn true` -- pokaz wiadomosci wejscia
