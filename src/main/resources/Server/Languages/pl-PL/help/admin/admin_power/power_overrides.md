---
id: admin_power_overrides
---
# Nadpisania mocy

Specjalne komendy mocy, które zmieniają zachowanie mocy dla konkretnych graczy lub frakcji.

## Komendy nadpisań

| Komenda | Opis |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Ustaw niestandardowy maksymalny limit mocy |
| `/f admin power noloss <player>` | Przełącz odporność na karę mocy za śmierć |
| `/f admin power nodecay <player>` | Przełącz odporność na zanikanie mocy offline |
| `/f admin power info <player>` | Wyświetl wszystkie nadpisania i szczegóły mocy |

## Niestandardowa maksymalna moc

`/f admin power setmax <player> <amount>`
Ustawia osobisty limit maksymalnej mocy dla gracza, nadpisując domyślną wartość serwera.

>[!INFO] Ustawienie niestandardowego maksimum **nie** zmienia aktualnej mocy. Zmienia jedynie pułap. Gracz wciąż musi zdobywać moc do nowego limitu.

## Tryb bez utraty

`/f admin power noloss <player>`
Przełącza odporność na utratę mocy przy śmierci. Gdy włączony, gracz **nie** traci mocy przy śmierci.

Przydatne dla:
- Okresów ochrony nowych graczy
- Uczestników wydarzeń
- Członków ekipy

## Tryb bez zanikania

`/f admin power nodecay <player>`
Przełącza odporność na zanikanie mocy offline. Gdy włączony, moc gracza **nie** zmniejsza się będąc offline.

Przydatne dla:
- Graczy na dłuższej przerwie
- Członków VIP
- Ochrony sezonowej

## Informacje o mocy

`/f admin power info <player>`
Pokazuje kompletny podgląd:

- Aktualna moc i maksymalna moc
- Aktywne nadpisania (noloss, nodecay, niestandardowe maksimum)
- Czas ostatniej śmierci i utracona moc
- Procentowy wkład we frakcję

>[!TIP] Wszystkie nadpisania mocy zachowują się po restartach serwera i są zapisywane w pliku danych gracza.
