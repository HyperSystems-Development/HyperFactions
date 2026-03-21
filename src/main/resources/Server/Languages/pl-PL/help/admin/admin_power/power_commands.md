---
id: admin_power_commands
---
# Komendy administracyjne mocy

Nadpisywanie wartosci mocy graczy i frakcji. Wszystkie komendy wymagaja uprawnienia `hyperfactions.admin.power`.

## Komendy mocy gracza

| Komenda | Opis |
|---------|-------------|
| `/f admin power set <player> <amount>` | Ustaw dokladna wartosc mocy |
| `/f admin power add <player> <amount>` | Dodaj moc graczowi |
| `/f admin power remove <player> <amount>` | Odejmij moc graczowi |
| `/f admin power reset <player>` | Resetuj do domyslnej mocy startowej |
| `/f admin power info <player>` | Wyswietl szczegolowy podglad mocy |

## Jak moc wplywa na frakcje

Laczna moc frakcji to suma indywidualnej mocy wszystkich jej czlonkow. Zajecia terytorialne wymagaja wystarczajacej lacznej mocy do utrzymania.

| Scenariusz | Efekt |
|----------|--------|
| Moc ustawiona wyzej | Frakcja moze zajmowac wiecej terytorium |
| Moc ustawiona nizej | Frakcja moze stac sie podatna na przejecie |
| Reset mocy | Przywraca gracza do domyslnej wartosci startowej |

>[!WARNING] Obnizenie mocy gracza moze spowodowac utrate terytorium przez jego frakcje, jesli laczna moc spadnie ponizej liczby zajetych chunkow.

## Przyklady

- `/f admin power set Steve 50` -- ustaw na dokladnie 50
- `/f admin power add Steve 10` -- zwieksz o 10
- `/f admin power remove Steve 5` -- zmniejsz o 5
- `/f admin power reset Steve` -- wroc do domyslnej
- `/f admin power info Steve` -- pokaz pelny podglad

>[!TIP] Uzyj `/f admin power info <player>`, aby zobaczyc aktualna moc, maksymalna moc i wszelkie aktywne nadpisania przed wprowadzeniem zmian.
