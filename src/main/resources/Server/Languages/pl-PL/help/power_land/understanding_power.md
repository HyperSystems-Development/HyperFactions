---
id: power_understanding
commands: power
---
# Zrozumienie mocy

Moc to podstawowy zasob, ktory okresla, ile terytorium moze utrzymac twoja frakcja. Kazdy gracz ma osobista moc, ktora wlicza sie do lacznej mocy frakcji.

---

## Domyslne wartosci mocy

| Ustawienie | Wartosc |
|---------|-------|
| Maksymalna moc na gracza | 20 |
| Moc startowa | 10 |
| Kara za smierc | -1.0 za smierc |
| Nagroda za zabojstwo | 0.0 |
| Tempo regeneracji | +0.1 na minute (bedac online) |
| Koszt mocy na zajecie | 2.0 |
| Wylogowanie podczas oznaczenia | -1.0 dodatkowo |

>[!NOTE] To sa wartosci domyslne. Administrator serwera mogl skonfigurowac inne ustawienia.

## Jak to dziala

Laczna moc twojej frakcji to suma osobistej mocy wszystkich czlonkow. Wymagana moc to liczba zajec pomnozona przez 2.0. Dopoki laczna moc pozostaje powyzej wymaganej mocy, twoje terytorium jest bezpieczne.

>[!INFO] Moc regeneruje sie pasywnie z predkoscia 0.1 na minute, gdy jestes online. W tym tempie odzyskanie 1.0 mocy zajmuje okolo 10 minut.

---

## Sprawdzanie mocy

`/f power`

Pokazuje twoja osobista moc, laczna moc frakcji i ile jest potrzebne do utrzymania obecnych zajec.

## Strefa zagrozenia

Jesli laczna moc spadnie ponizej wymaganej ilosci dla twoich zajec, twoja frakcja staje sie podatna. Wrogowie moga przejac twoje chunki.

>[!WARNING] Wiele smierci w krotkim okresie moze szybko sie nawarstwiac. Jesli masz 5 czlonkow po 10 mocy kazdy (50 lacznie) i 20 zajec (40 potrzebne), zaledwie 5 smierci w twoim zespole obnizy moc do 45 -- wciaz bezpiecznie. Ale 11 smierci da wam 39, ponizej progu 40.

>[!TIP] Utrzymuj zapas mocy. Nie zajmuj kazdego chunka, na jaki cie stac -- zostaw margines na kilka smierci bez stawania sie podatnym na rajdy.
