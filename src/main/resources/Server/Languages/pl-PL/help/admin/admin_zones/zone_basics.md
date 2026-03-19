---
id: admin_zone_basics
---
# Podstawy stref

Strefy to kontrolowane przez administratorów terytoria z niestandardowymi zasadami, które nadpisują normalną ochronę terytorialną frakcji.

## Typy stref

- **SafeZone** -- Brak PvP, brak budowania, brak obrażeń.
Idealne dla stref odrodzenia i hubów handlowych.
- **WarZone** -- PvP zawsze włączone, brak budowania.
Idealne dla aren i spornych stref walki.

## Tworzenie stref

`/f admin safezone <name>`
Tworzy SafeZone i zajmuje twój obecny chunk.

`/f admin warzone <name>`
Tworzy WarZone i zajmuje twój obecny chunk.

Po utworzeniu stań na dodatkowych chunkach i użyj `/f admin zone claim <zone>`, aby rozszerzyć strefę.

## Zarządzanie chunkami stref

`/f admin zone claim <zone>`
Dodaj obecny chunk do nazwanej strefy.

`/f admin zone unclaim <zone>`
Usuń obecny chunk ze strefy.

`/f admin zone radius <zone> <radius>`
Zajmij kwadrat chunków wokół twojej pozycji.

## Usuwanie stref

`/f admin removezone <name>`
Trwale usuwa strefę i zwalnia wszystkie jej zajęte chunki.

>[!WARNING] Usunięcie strefy natychmiast zwalnia wszystkie jej chunki. Nie można tego cofnąć bez przywrócenia kopii zapasowej.

>[!INFO] Zasady stref **zawsze nadpisują** zasady terytoriów frakcji. SafeZone na wrogim terenie wciąż jest bezpieczna.
