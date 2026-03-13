---
id: admin_updates
---
# Sprawdzanie aktualizacji

HyperFactions może sprawdzać nowe wersje i zarządzać zależnością HyperProtect-Mixin.

## Komendy aktualizacji

| Komenda | Opis |
|---------|-------------|
| `/f admin update` | Sprawdź aktualizacje HyperFactions |
| `/f admin update mixin` | Sprawdź/pobierz HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Przełącz automatyczne pobieranie |
| `/f admin version` | Pokaż aktualną wersję i informacje o buildzie |

## Kanały wydań

| Kanał | Opis |
|---------|-------------|
| **Stable** | Zalecany dla serwerów produkcyjnych |
| **Pre-release** | Wczesny dostęp do nadchodzących funkcji |

>[!INFO] Sprawdzanie aktualizacji jedynie powiadamia o nowych wersjach. **Nie** instaluje automatycznie aktualizacji samego HyperFactions.

## HyperProtect-Mixin

HyperProtect-Mixin to zalecany mixin ochrony, który włącza zaawansowane flagi stref (eksplozje, rozprzestrzenianie ognia, zachowanie ekwipunku, itp.).

- `/f admin update mixin` sprawdza najnowszą wersję
i pobiera ją, jeśli nowsza wersja jest dostępna
- Automatyczne pobieranie można włączać i wyłączać dla każdego serwera

>[!TIP] Po pobraniu nowej wersji mixina wymagany jest restart serwera, aby zmiany zadziałały.

## Procedura wycofania

Jeśli aktualizacja powoduje problemy:

1. Zatrzymaj serwer
2. Zastąp plik JAR pluginu poprzednią wersją
3. Uruchom serwer
4. Zweryfikuj funkcjonalność komendą `/f admin version`

>[!WARNING] Obniżenie wersji może wymagać resetu migracji konfiguracji. Zawsze utrzymuj kopie zapasowe przed aktualizacją.
