---
id: economy_upkeep
---
# Utrzymanie terytorium

Frakcje muszą płacić bieżące koszty utrzymania swoich zajętych terytoriów. Zapobiega to gromadzeniu ziem i utrzymuje mapę dynamiczną.

## Koszty utrzymania

| Ustawienie | Domyślnie |
|---------|---------|
| Koszt za chunk | 2.0 za cykl |
| Interwał płatności | Co 24 godziny |
| Darmowe chunki | 3 (bez kosztu) |
| Tryb skalowania | Stawka stała |

>[!NOTE] To są wartości domyślne. Administrator serwera mógł skonfigurować inne ustawienia.

Twoje pierwsze 3 chunki są darmowe. Powyżej tego, każdy dodatkowy zajęty chunk kosztuje 2.0 za cykl płatności.

## Automatyczna płatność

Automatyczna płatność jest domyślnie włączona. System automatycznie potrąca koszty utrzymania ze skarbca w każdym interwale. Nie wymaga ręcznej akcji.

---

## Okres karencji

Jeśli twój skarbiec nie pokrywa kosztów utrzymania, rozpoczyna się 48-godzinny okres karencji. Ostrzeżenie jest wysyłane 6 godzin przed rozpoczęciem utraty zajęć.

>[!WARNING] Jeśli koszty utrzymania pozostaną nieopłacone po okresie karencji, twoja frakcja traci 1 zajęcie na cykl, dopóki koszty nie zostaną pokryte lub wszystkie dodatkowe zajęcia nie zostaną utracone.

## Przykład

*Frakcja z 8 zajęciami płaci za 5 chunków (8 minus 3 darmowe). Przy 2.0 za chunk, to 10.0 za cykl.*

>[!TIP] Utrzymuj skarbiec zasilony powyżej kosztu utrzymania. Używaj /f balance, aby sprawdzić rezerwy.
