---
id: admin_upkeep_management
---
# Zarządzanie utrzymaniem

Utrzymanie frakcji obciąża frakcje okresowo na podstawie ich terytorium i liczby członków.

## Kontrole administracyjne

Ustawienia utrzymania są zarządzane przez plik konfiguracji ekonomii lub GUI konfiguracji administracyjnej.

`/f admin config`
Otwórz edytor konfiguracji i przejdź do ustawień ekonomii, aby dostosować wartości utrzymania.

## Domyślne ustawienia utrzymania

| Ustawienie | Domyślnie | Opis |
|---------|---------|-------------|
| Utrzymanie włączone | false | Główny przełącznik systemu |
| Interwał utrzymania | 24h | Jak często pobierane jest utrzymanie |
| Koszt za zajęcie | 5.0 | Koszt za zajęty chunk na cykl |
| Koszt za członka | 0.0 | Koszt za członka na cykl |
| Okres karencji | 72h | Nowe frakcje są zwolnione |
| Rozwiązanie przy bankructwie | false | Automatyczne rozwiązanie jeśli nie może zapłacić |

## Monitorowanie utrzymania

Użyj `/f admin info <faction>`, aby zobaczyć:
- Aktualne saldo skarbca
- Szacowany koszt utrzymania za cykl
- Czas do następnego pobrania utrzymania
- Czy frakcja stać na utrzymanie

>[!TIP] Przeglądaj statystyki ekonomii wszystkich frakcji z panelu administracyjnego, aby zidentyfikować frakcje zagrożone bankructwem przed uruchomieniem utrzymania.

>[!INFO] Konfiguracja utrzymania jest przechowywana w `economy.json`. Zmiany dokonane przez GUI konfiguracji wchodzą w życie po przeładowaniu komendą `/f admin reload`.

## Formuła utrzymania

**Łączne utrzymanie** = (zajęte chunki x koszt za zajęcie) + (liczba członków x koszt za członka)

>[!WARNING] Włączenie utrzymania na serwerze z istniejącymi frakcjami może spowodować niespodziewane bankructwa. Rozważ ustawienie okresu karencji lub wcześniejsze ogłoszenie zmiany.
