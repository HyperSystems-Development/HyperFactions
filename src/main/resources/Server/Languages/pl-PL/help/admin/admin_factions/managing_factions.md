---
id: admin_managing_factions
---
# Zarzadzanie frakcjami

Administratorzy moga przegladac i modyfikowac dowolna frakcje na serwerze przez panel administracyjny lub komendy.

## Przegladanie frakcji

`/f admin factions`
Otwiera przegladarke frakcji administracyjna. Wyswietla wszystkie frakcje z liczba czlonkow, poziomami mocy i terytorium.

`/f admin info <faction>`
Otwiera panel informacji administracyjnych dla konkretnej frakcji z pelnymi szczegolami i opcjami zarzadzania.

## Modyfikowanie ustawien frakcji

Z uprawnieniem `hyperfactions.admin.modify` mozesz:

- **Zmienic nazwe** frakcji, aby rozwiazac konflikty
- **Ustawic kolor**, aby naprawic problemy z wyswietlaniem
- **Przelaczyc otwarta/zamknieta**, aby nadpisac polityke dolaczania
- **Edytowac opis** w celach moderacyjnych

>[!TIP] Uzyj `/f admin who <player>`, aby sprawdzic, do ktorej frakcji nalezy dany gracz i wyswietlic jego szczegoly.

## Przegladanie czlonkow i relacji

Panel informacji administracyjnych pokazuje:

| Sekcja | Szczegoly |
|---------|---------|
| **Czlonkowie** | Pelny sklad z rolami i ostatnia aktywnoscia |
| **Relacje** | Wszystkie statusy sojuszy, wrogosci i neutralnosci |
| **Terytorium** | Zajete chunki i bilans mocy |
| **Ekonomia** | Saldo skarbca i log transakcji |

>[!NOTE] Komendy inspekcji administracyjnej nie powiadamiaja przegladanej frakcji. Tylko modyfikacje wywoluja alerty.
