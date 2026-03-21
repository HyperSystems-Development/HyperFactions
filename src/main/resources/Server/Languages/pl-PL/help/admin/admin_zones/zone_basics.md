---
id: admin_zone_basics
---
# Podstawy stref

Strefy to kontrolowane przez administratorow terytoria z niestandardowymi zasadami, ktore nadpisuja normalna ochrone terytorialna frakcji.

## Typy stref

- **SafeZone** -- Brak PvP, brak budowania, brak obrazen.
Idealne dla stref odrodzenia i hubow handlowych.
- **WarZone** -- PvP zawsze wlaczone, brak budowania.
Idealne dla aren i spornych stref walki.

## Tworzenie stref

`/f admin safezone <name>`
Tworzy SafeZone i zajmuje twoj obecny chunk.

`/f admin warzone <name>`
Tworzy WarZone i zajmuje twoj obecny chunk.

Po utworzeniu stan na dodatkowych chunkach i uzyj `/f admin zone claim <zone>`, aby rozszerzyc strefe.

## Zarzadzanie chunkami stref

`/f admin zone claim <zone>`
Dodaj obecny chunk do nazwanej strefy.

`/f admin zone unclaim <zone>`
Usun obecny chunk ze strefy.

`/f admin zone radius <zone> <radius>`
Zajmij kwadrat chunkow wokol twojej pozycji.

## Usuwanie stref

`/f admin removezone <name>`
Trwale usuwa strefe i zwalnia wszystkie jej zajete chunki.

>[!WARNING] Usuniecie strefy natychmiast zwalnia wszystkie jej chunki. Nie mozna tego cofnac bez przywrocenia kopii zapasowej.

>[!INFO] Zasady stref **zawsze nadpisuja** zasady terytoriow frakcji. SafeZone na wrogim terenie wciaz jest bezpieczna.
