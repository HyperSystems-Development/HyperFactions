---
id: combat_death
commands: home, sethome, stuck
---
# Smierc i odzyskiwanie

Smierc niesie realne konsekwencje we frakcjach. Kazda smierc kosztuje cie osobista moc, oslabiajac zdolnosc twojej frakcji do utrzymania terytorium.

## Utrata mocy

Kazda smierc kosztuje -1.0 mocy z twojego osobistego stanu. To obniza laczna moc frakcji.

| Zdarzenie | Zmiana mocy |
|-------|-------------|
| Smierc (dowolna przyczyna) | -1.0 |
| Regeneracja online | +0.1 na minute |
| Wylogowanie w walce | -1.0 (zabity) |

>[!NOTE] To sa wartosci domyslne. Administrator serwera mogl skonfigurowac inne ustawienia.

## Przykladowe scenariusze

*5 czlonkow po 10.0 mocy kazdy = 50 lacznie, 20 zajec.*
*Jeden czlonek ginie dwukrotnie: 8.0 mocy, lacznie we frakcji 48.*
*Trzech czlonkow ginie po razie: lacznie spada do 47.*

>[!WARNING] Jesli moc twojej frakcji spadnie ponizej liczby zajec, wrogowie moga przejac twoje terytorium.

## Odzyskiwanie

Moc regeneruje sie z predkoscia 0.1 na minute bedac online. Odzyskanie 1.0 utraconej mocy zajmuje okolo 10 minut. Wielokrotne smierci sie kumuluja, wiec unikaj powtarzanych walk.

---

## Wszystkie rodzaje smierci

Utrata mocy dotyczy wszystkich smierci: PvP, zabojstw przez moby, obrazen od upadku, utoniecia i kazdej innej przyczyny. Nie ma bezpiecznego sposobu na smierc.

>[!TIP] Ustaw baze frakcji komenda /f sethome, aby czlonkowie mogli szybko sie przegrupowac po smierci.
