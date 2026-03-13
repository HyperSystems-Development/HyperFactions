---
id: power_understanding
commands: power
---
# Zrozumienie mocy

Moc to podstawowy zasób, który określa, ile terytorium może utrzymać twoja frakcja. Każdy gracz ma osobistą moc, która wlicza się do łącznej mocy frakcji.

---

## Domyślne wartości mocy

| Ustawienie | Wartość |
|---------|-------|
| Maksymalna moc na gracza | 20 |
| Moc startowa | 10 |
| Kara za śmierć | -1.0 za śmierć |
| Nagroda za zabójstwo | 0.0 |
| Tempo regeneracji | +0.1 na minutę (będąc online) |
| Koszt mocy na zajęcie | 2.0 |
| Wylogowanie podczas oznaczenia | -1.0 dodatkowo |

>[!NOTE] To są wartości domyślne. Administrator serwera mógł skonfigurować inne ustawienia.

## Jak to działa

Łączna moc twojej frakcji to suma osobistej mocy wszystkich członków. Wymagana moc to liczba zajęć pomnożona przez 2.0. Dopóki łączna moc pozostaje powyżej wymaganej mocy, twoje terytorium jest bezpieczne.

>[!INFO] Moc regeneruje się pasywnie z prędkością 0.1 na minutę, gdy jesteś online. W tym tempie odzyskanie 1.0 mocy zajmuje około 10 minut.

---

## Sprawdzanie mocy

`/f power`

Pokazuje twoją osobistą moc, łączną moc frakcji i ile jest potrzebne do utrzymania obecnych zajęć.

## Strefa zagrożenia

Jeśli łączna moc spadnie poniżej wymaganej ilości dla twoich zajęć, twoja frakcja staje się podatna. Wrogowie mogą przejąć twoje chunki.

>[!WARNING] Wiele śmierci w krótkim okresie może szybko się nawarstwiać. Jeśli masz 5 członków po 10 mocy każdy (50 łącznie) i 20 zajęć (40 potrzebne), zaledwie 5 śmierci w twoim zespole obniży moc do 45 -- wciąż bezpiecznie. Ale 11 śmierci da wam 39, poniżej progu 40.

>[!TIP] Utrzymuj zapas mocy. Nie zajmuj każdego chunka, na jaki cię stać -- zostaw margines na kilka śmierci bez stawania się podatnym na rajdy.
