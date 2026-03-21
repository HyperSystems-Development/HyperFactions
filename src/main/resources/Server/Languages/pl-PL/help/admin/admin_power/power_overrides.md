---
id: admin_power_overrides
---
# Nadpisania mocy

Specjalne komendy mocy, ktore zmieniaja zachowanie mocy dla konkretnych graczy lub frakcji.

## Komendy nadpisan

| Komenda | Opis |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Ustaw niestandardowy maksymalny limit mocy |
| `/f admin power noloss <player>` | Przelacz odpornosc na kare mocy za smierc |
| `/f admin power nodecay <player>` | Przelacz odpornosc na zanikanie mocy offline |
| `/f admin power info <player>` | Wyswietl wszystkie nadpisania i szczegoly mocy |

## Niestandardowa maksymalna moc

`/f admin power setmax <player> <amount>`
Ustawia osobisty limit maksymalnej mocy dla gracza, nadpisujac domyslna wartosc serwera.

>[!INFO] Ustawienie niestandardowego maksimum **nie** zmienia aktualnej mocy. Zmienia jedynie pulap. Gracz wciaz musi zdobywac moc do nowego limitu.

## Tryb bez utraty

`/f admin power noloss <player>`
Przelacza odpornosc na utrate mocy przy smierci. Gdy wlaczony, gracz **nie** traci mocy przy smierci.

Przydatne dla:
- Okresow ochrony nowych graczy
- Uczestnikow wydarzen
- Czlonkow ekipy

## Tryb bez zanikania

`/f admin power nodecay <player>`
Przelacza odpornosc na zanikanie mocy offline. Gdy wlaczony, moc gracza **nie** zmniejsza sie bedac offline.

Przydatne dla:
- Graczy na dluzszej przerwie
- Czlonkow VIP
- Ochrony sezonowej

## Informacje o mocy

`/f admin power info <player>`
Pokazuje kompletny podglad:

- Aktualna moc i maksymalna moc
- Aktywne nadpisania (noloss, nodecay, niestandardowe maksimum)
- Czas ostatniej smierci i utracona moc
- Procentowy wklad we frakcje

>[!TIP] Wszystkie nadpisania mocy zachowuja sie po restartach serwera i sa zapisywane w pliku danych gracza.
