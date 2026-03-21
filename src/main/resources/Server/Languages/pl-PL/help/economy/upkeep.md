---
id: economy_upkeep
---
# Utrzymanie terytorium

Frakcje musza placic biezace koszty utrzymania swoich zajetych terytoriow. Zapobiega to gromadzeniu ziem i utrzymuje mape dynamiczna.

## Koszty utrzymania

| Ustawienie | Domyslnie |
|---------|---------|
| Koszt za chunk | 2.0 za cykl |
| Interwal platnosci | Co 24 godziny |
| Darmowe chunki | 3 (bez kosztu) |
| Tryb skalowania | Stawka stala |

>[!NOTE] To sa wartosci domyslne. Administrator serwera mogl skonfigurowac inne ustawienia.

Twoje pierwsze 3 chunki sa darmowe. Powyzej tego, kazdy dodatkowy zajety chunk kosztuje 2.0 za cykl platnosci.

## Automatyczna platnosc

Automatyczna platnosc jest domyslnie wlaczona. System automatycznie potraca koszty utrzymania ze skarbca w kazdym interwale. Nie wymaga recznej akcji.

---

## Okres karencji

Jesli twoj skarbiec nie pokrywa kosztow utrzymania, rozpoczyna sie 48-godzinny okres karencji. Ostrzezenie jest wysylane 6 godzin przed rozpoczeciem utraty zajec.

>[!WARNING] Jesli koszty utrzymania pozostana nieoplacone po okresie karencji, twoja frakcja traci 1 zajecie na cykl, dopoki koszty nie zostana pokryte lub wszystkie dodatkowe zajecia nie zostana utracone.

## Przyklad

*Frakcja z 8 zajeciami placi za 5 chunkow (8 minus 3 darmowe). Przy 2.0 za chunk, to 10.0 za cykl.*

>[!TIP] Utrzymuj skarbiec zasilony powyzej kosztu utrzymania. Uzywaj /f balance, aby sprawdzic rezerwy.
