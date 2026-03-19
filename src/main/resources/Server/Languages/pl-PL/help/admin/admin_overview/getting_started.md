---
id: admin_getting_started
---
# Pierwsze kroki jako administrator

Witaj w administracji HyperFactions. Ten poradnik opisuje twoje pierwsze kroki po zainstalowaniu pluginu.

## Otwieranie panelu administracyjnego

`/f admin`
Otwiera GUI panelu administracyjnego z dostępem do wszystkich narzędzi zarządzania, edytorów stref i ustawień serwera.

>[!INFO] Potrzebujesz uprawnienia **hyperfactions.admin.use** lub statusu OP, aby uzyskać dostęp do komend administracyjnych.

## Wymagania

- **Z pluginem uprawnień**: Nadaj `hyperfactions.admin.use`
- **Bez pluginu uprawnień**: Gracz musi być operatorem serwera (`adminRequiresOp=true` domyślnie)

## Pierwsze kroki po instalacji

1. Wpisz `/f admin`, aby zweryfikować swój dostęp
2. Otwórz **Konfigurację**, aby przejrzeć domyślne ustawienia frakcji
3. Utwórz **SafeZone** na spawnie komendą `/f admin safezone Spawn`
4. Opcjonalnie utwórz **WarZone** dla aren PvP
5. Przejrzyj ustawienia **kopii zapasowych**, aby zapewnić bezpieczeństwo danych

## Możliwości administracyjne

| Obszar | Co możesz zrobić |
|------|----------------|
| Frakcje | Przeglądaj, modyfikuj lub wymuś rozwiązanie dowolnej frakcji |
| Strefy | Twórz SafeZone i WarZone z niestandardowymi flagami |
| Moc | Nadpisuj wartości mocy graczy/frakcji |
| Ekonomia | Zarządzaj skarbcami frakcji i utrzymaniem |
| Konfiguracja | Edytuj ustawienia na żywo przez GUI lub przeładuj z dysku |
| Kopie zapasowe | Twórz, przywracaj i zarządzaj kopiami zapasowymi danych |
| Importy | Migruj dane z innych pluginów frakcji |

>[!TIP] Użyj `/f admin --text`, aby uzyskać wynik tekstowy na czacie zamiast GUI -- przydatne dla konsoli lub automatyzacji.
