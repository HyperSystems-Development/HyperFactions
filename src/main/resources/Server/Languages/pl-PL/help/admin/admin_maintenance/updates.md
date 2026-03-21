---
id: admin_updates
---
# Sprawdzanie aktualizacji

HyperFactions moze sprawdzac nowe wersje i zarzadzac zaleznoscia HyperProtect-Mixin.

## Komendy aktualizacji

| Komenda | Opis |
|---------|-------------|
| `/f admin update` | Sprawdz aktualizacje HyperFactions |
| `/f admin update mixin` | Sprawdz/pobierz HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Przelacz automatyczne pobieranie |
| `/f admin version` | Pokaz aktualna wersje i informacje o buildzie |

## Kanaly wydan

| Kanal | Opis |
|---------|-------------|
| **Stable** | Zalecany dla serwerow produkcyjnych |
| **Pre-release** | Wczesny dostep do nadchodzacych funkcji |

>[!INFO] Sprawdzanie aktualizacji jedynie powiadamia o nowych wersjach. **Nie** instaluje automatycznie aktualizacji samego HyperFactions.

## HyperProtect-Mixin

HyperProtect-Mixin to zalecany mixin ochrony, ktory wlacza zaawansowane flagi stref (eksplozje, rozprzestrzenianie ognia, zachowanie ekwipunku, itp.).

- `/f admin update mixin` sprawdza najnowsza wersje
i pobiera ja, jesli nowsza wersja jest dostepna
- Automatyczne pobieranie mozna wlaczac i wylaczac dla kazdego serwera

>[!TIP] Po pobraniu nowej wersji mixina wymagany jest restart serwera, aby zmiany zadzialaly.

## Procedura wycofania

Jesli aktualizacja powoduje problemy:

1. Zatrzymaj serwer
2. Zastap plik JAR pluginu poprzednia wersja
3. Uruchom serwer
4. Zweryfikuj funkcjonalnosc komenda `/f admin version`

>[!WARNING] Obnizenie wersji moze wymagac resetu migracji konfiguracji. Zawsze utrzymuj kopie zapasowe przed aktualizacja.
