---
id: admin_upkeep_management
---
# Zarzadzanie utrzymaniem

Utrzymanie frakcji obciaza frakcje okresowo na podstawie ich terytorium i liczby czlonkow.

## Kontrole administracyjne

Ustawienia utrzymania sa zarzadzane przez plik konfiguracji ekonomii lub GUI konfiguracji administracyjnej.

`/f admin config`
Otworz edytor konfiguracji i przejdz do ustawien ekonomii, aby dostosowac wartosci utrzymania.

## Domyslne ustawienia utrzymania

| Ustawienie | Domyslnie | Opis |
|---------|---------|-------------|
| Utrzymanie wlaczone | false | Glowny przelacznik systemu |
| Interwal utrzymania | 24h | Jak czesto pobierane jest utrzymanie |
| Koszt za zajecie | 5.0 | Koszt za zajety chunk na cykl |
| Koszt za czlonka | 0.0 | Koszt za czlonka na cykl |
| Okres karencji | 72h | Nowe frakcje sa zwolnione |
| Rozwiazanie przy bankructwie | false | Automatyczne rozwiazanie jesli nie moze zaplacic |

## Monitorowanie utrzymania

Uzyj `/f admin info <faction>`, aby zobaczyc:
- Aktualne saldo skarbca
- Szacowany koszt utrzymania za cykl
- Czas do nastepnego pobrania utrzymania
- Czy frakcja stac na utrzymanie

>[!TIP] Przegladaj statystyki ekonomii wszystkich frakcji z panelu administracyjnego, aby zidentyfikowac frakcje zagrozone bankructwem przed uruchomieniem utrzymania.

>[!INFO] Konfiguracja utrzymania jest przechowywana w `economy.json`. Zmiany dokonane przez GUI konfiguracji wchodza w zycie po przeladowaniu komenda `/f admin reload`.

## Formula utrzymania

**Laczne utrzymanie** = (zajete chunki x koszt za zajecie) + (liczba czlonkow x koszt za czlonka)

>[!WARNING] Wlaczenie utrzymania na serwerze z istniejacymi frakcjami moze spowodowac niespodziewane bankructwa. Rozwaz ustawienie okresu karencji lub wczesniejsze ogloszenie zmiany.
