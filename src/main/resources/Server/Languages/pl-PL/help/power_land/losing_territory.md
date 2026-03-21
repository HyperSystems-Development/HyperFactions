---
id: power_losing
commands: overclaim
---
# Tracenie terytorium

Gdy laczna moc frakcji spadnie ponizej kosztu jej zajec, staje sie ona podatna na rajdy. Wrogowie moga przejmowac chunki spod twoich nog.

---

## Jak dziala przejmowanie

`/f overclaim`

Oficer lub Lider z wrogiej frakcji staje na twoim zajetym chunku i wpisuje te komende. Jesli twoja frakcja ma deficyt mocy, chunk przechodzi pod ich kontrole.

## Matematyka

Kazde zajecie kosztuje 2.0 mocy w utrzymaniu. Jesli twoja laczna moc spadnie ponizej tego progu, chunki z deficytu sa podatne na przejecie.

>[!NOTE] To sa wartosci domyslne. Administrator serwera mogl skonfigurowac inne ustawienia.

>[!WARNING] Przejecie jest trwale. Gdy wrog zabierze chunk, musisz go odzyskac (lub przejac z powrotem, jesli oslabna).

---

## Przykladowy scenariusz

| Czynnik | Wartosc |
|--------|-------|
| Czlonkowie | 5 graczy |
| Moc na czlonka | 10 kazdy (startowa) |
| Laczna moc | 50 |
| Zajecia | 30 chunkow |
| Wymagana moc (30 x 2.0) | 60 |
| Deficyt | brakuje 10 mocy |

W tym przykladzie frakcja jest podatna na rajdy od samego poczatku. Wrogowie moga przejac do 5 chunkow (10 deficytu / 2.0 na zajecie) zanim frakcja osiagnie rownowage.

---

## Jak zapobiegac przejeciu

- Nie rozszerzaj sie nadmiernie -- zawsze utrzymuj laczna moc powyzej kosztu zajec z zapasem
- Badz aktywny -- moc regeneruje sie tylko bedac online (+0.1/min)
- Unikaj niepotrzebnych smierci -- kazda smierc kosztuje 1.0 mocy
- Rekrutuj wiecej czlonkow -- wiecej graczy oznacza wiecej lacznej mocy
- Oddawaj nieuzywane chunki -- zwolnij moc komenda /f unclaim

>[!TIP] Regularnie sprawdzaj status mocy komenda /f power. Jesli twoja laczna moc jest blisko kosztu zajec, rozwaz oddanie mniej waznych chunkow przed wojna.
