---
id: combat_death
commands: home, sethome, stuck
---
# Śmierć i odzyskiwanie

Śmierć niesie realne konsekwencje we frakcjach. Każda śmierć kosztuje cię osobistą moc, osłabiając zdolność twojej frakcji do utrzymania terytorium.

## Utrata mocy

Każda śmierć kosztuje -1.0 mocy z twojego osobistego stanu. To obniża łączną moc frakcji.

| Zdarzenie | Zmiana mocy |
|-------|-------------|
| Śmierć (dowolna przyczyna) | -1.0 |
| Regeneracja online | +0.1 na minutę |
| Wylogowanie w walce | -1.0 (zabity) |

>[!NOTE] To są wartości domyślne. Administrator serwera mógł skonfigurować inne ustawienia.

## Przykładowe scenariusze

*5 członków po 10.0 mocy każdy = 50 łącznie, 20 zajęć.*
*Jeden członek ginie dwukrotnie: 8.0 mocy, łącznie we frakcji 48.*
*Trzech członków ginie po razie: łącznie spada do 47.*

>[!WARNING] Jeśli moc twojej frakcji spadnie poniżej liczby zajęć, wrogowie mogą przejąć twoje terytorium.

## Odzyskiwanie

Moc regeneruje się z prędkością 0.1 na minutę będąc online. Odzyskanie 1.0 utraconej mocy zajmuje około 10 minut. Wielokrotne śmierci się kumulują, więc unikaj powtarzanych walk.

---

## Wszystkie rodzaje śmierci

Utrata mocy dotyczy wszystkich śmierci: PvP, zabójstw przez moby, obrażeń od upadku, utonięcia i każdej innej przyczyny. Nie ma bezpiecznego sposobu na śmierć.

>[!TIP] Ustaw bazę frakcji komendą /f sethome, aby członkowie mogli szybko się przegrupować po śmierci.
