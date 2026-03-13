---
id: admin_managing_factions
---
# Zarządzanie frakcjami

Administratorzy mogą przeglądać i modyfikować dowolną frakcję na serwerze przez panel administracyjny lub komendy.

## Przeglądanie frakcji

`/f admin factions`
Otwiera przeglądarkę frakcji administracyjną. Wyświetla wszystkie frakcje z liczbą członków, poziomami mocy i terytorium.

`/f admin info <faction>`
Otwiera panel informacji administracyjnych dla konkretnej frakcji z pełnymi szczegółami i opcjami zarządzania.

## Modyfikowanie ustawień frakcji

Z uprawnieniem `hyperfactions.admin.modify` możesz:

- **Zmienić nazwę** frakcji, aby rozwiązać konflikty
- **Ustawić kolor**, aby naprawić problemy z wyświetlaniem
- **Przełączyć otwartą/zamkniętą**, aby nadpisać politykę dołączania
- **Edytować opis** w celach moderacyjnych

>[!TIP] Użyj `/f admin who <player>`, aby sprawdzić, do której frakcji należy dany gracz i wyświetlić jego szczegóły.

## Przeglądanie członków i relacji

Panel informacji administracyjnych pokazuje:

| Sekcja | Szczegóły |
|---------|---------|
| **Członkowie** | Pełny skład z rolami i ostatnią aktywnością |
| **Relacje** | Wszystkie statusy sojuszy, wrogości i neutralności |
| **Terytorium** | Zajęte chunki i bilans mocy |
| **Ekonomia** | Saldo skarbca i log transakcji |

>[!NOTE] Komendy inspekcji administracyjnej nie powiadamiają przeglądanej frakcji. Tylko modyfikacje wywołują alerty.
