---
id: admin_power_commands
---
# Komendy administracyjne mocy

Nadpisywanie wartości mocy graczy i frakcji. Wszystkie komendy wymagają uprawnienia `hyperfactions.admin.power`.

## Komendy mocy gracza

| Komenda | Opis |
|---------|-------------|
| `/f admin power set <player> <amount>` | Ustaw dokładną wartość mocy |
| `/f admin power add <player> <amount>` | Dodaj moc graczowi |
| `/f admin power remove <player> <amount>` | Odejmij moc graczowi |
| `/f admin power reset <player>` | Resetuj do domyślnej mocy startowej |
| `/f admin power info <player>` | Wyświetl szczegółowy podgląd mocy |

## Jak moc wpływa na frakcje

Łączna moc frakcji to suma indywidualnej mocy wszystkich jej członków. Zajęcia terytorialne wymagają wystarczającej łącznej mocy do utrzymania.

| Scenariusz | Efekt |
|----------|--------|
| Moc ustawiona wyżej | Frakcja może zajmować więcej terytorium |
| Moc ustawiona niżej | Frakcja może stać się podatna na przejęcie |
| Reset mocy | Przywraca gracza do domyślnej wartości startowej |

>[!WARNING] Obniżenie mocy gracza może spowodować utratę terytorium przez jego frakcję, jeśli łączna moc spadnie poniżej liczby zajętych chunków.

## Przykłady

- `/f admin power set Steve 50` -- ustaw na dokładnie 50
- `/f admin power add Steve 10` -- zwiększ o 10
- `/f admin power remove Steve 5` -- zmniejsz o 5
- `/f admin power reset Steve` -- wróć do domyślnej
- `/f admin power info Steve` -- pokaż pełny podgląd

>[!TIP] Użyj `/f admin power info <player>`, aby zobaczyć aktualną moc, maksymalną moc i wszelkie aktywne nadpisania przed wprowadzeniem zmian.
