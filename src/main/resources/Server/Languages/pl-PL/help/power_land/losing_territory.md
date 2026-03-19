---
id: power_losing
commands: overclaim
---
# Tracenie terytorium

Gdy łączna moc frakcji spadnie poniżej kosztu jej zajęć, staje się ona podatna na rajdy. Wrogowie mogą przejmować chunki spod twoich nóg.

---

## Jak działa przejmowanie

`/f overclaim`

Oficer lub Lider z wrogiej frakcji staje na twoim zajętym chunku i wpisuje tę komendę. Jeśli twoja frakcja ma deficyt mocy, chunk przechodzi pod ich kontrolę.

## Matematyka

Każde zajęcie kosztuje 2.0 mocy w utrzymaniu. Jeśli twoja łączna moc spadnie poniżej tego progu, chunki z deficytu są podatne na przejęcie.

>[!NOTE] To są wartości domyślne. Administrator serwera mógł skonfigurować inne ustawienia.

>[!WARNING] Przejęcie jest trwałe. Gdy wróg zabierze chunk, musisz go odzyskać (lub przejąć z powrotem, jeśli osłabną).

---

## Przykładowy scenariusz

| Czynnik | Wartość |
|--------|-------|
| Członkowie | 5 graczy |
| Moc na członka | 10 każdy (startowa) |
| Łączna moc | 50 |
| Zajęcia | 30 chunków |
| Wymagana moc (30 x 2.0) | 60 |
| Deficyt | brakuje 10 mocy |

W tym przykładzie frakcja jest podatna na rajdy od samego początku. Wrogowie mogą przejąć do 5 chunków (10 deficytu / 2.0 na zajęcie) zanim frakcja osiągnie równowagę.

---

## Jak zapobiegać przejęciu

- Nie rozszerzaj się nadmiernie -- zawsze utrzymuj łączną moc powyżej kosztu zajęć z zapasem
- Bądź aktywny -- moc regeneruje się tylko będąc online (+0.1/min)
- Unikaj niepotrzebnych śmierci -- każda śmierć kosztuje 1.0 mocy
- Rekrutuj więcej członków -- więcej graczy oznacza więcej łącznej mocy
- Oddawaj nieużywane chunki -- zwolnij moc komendą /f unclaim

>[!TIP] Regularnie sprawdzaj status mocy komendą /f power. Jeśli twoja łączna moc jest blisko kosztu zajęć, rozważ oddanie mniej ważnych chunków przed wojną.
