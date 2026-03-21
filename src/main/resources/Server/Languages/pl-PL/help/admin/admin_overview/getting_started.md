---
id: admin_getting_started
---
# Pierwsze kroki jako administrator

Witaj w administracji HyperFactions. Ten poradnik opisuje twoje pierwsze kroki po zainstalowaniu pluginu.

## Otwieranie panelu administracyjnego

`/f admin`
Otwiera GUI panelu administracyjnego z dostepem do wszystkich narzedzi zarzadzania, edytorow stref i ustawien serwera.

>[!INFO] Potrzebujesz uprawnienia **hyperfactions.admin.use** lub statusu OP, aby uzyskac dostep do komend administracyjnych.

## Wymagania

- **Z pluginem uprawnien**: Nadaj `hyperfactions.admin.use`
- **Bez pluginu uprawnien**: Gracz musi byc operatorem serwera (`adminRequiresOp=true` domyslnie)

## Pierwsze kroki po instalacji

1. Wpisz `/f admin`, aby zweryfikowac swoj dostep
2. Otworz **Konfiguracje**, aby przejrzec domyslne ustawienia frakcji
3. Utworz **SafeZone** na spawnie komenda `/f admin safezone Spawn`
4. Opcjonalnie utworz **WarZone** dla aren PvP
5. Przejrzyj ustawienia **kopii zapasowych**, aby zapewnic bezpieczenstwo danych

## Mozliwosci administracyjne

| Obszar | Co mozesz zrobic |
|------|----------------|
| Frakcje | Przegladaj, modyfikuj lub wymus rozwiazanie dowolnej frakcji |
| Strefy | Tworz SafeZone i WarZone z niestandardowymi flagami |
| Moc | Nadpisuj wartosci mocy graczy/frakcji |
| Ekonomia | Zarzadzaj skarbcami frakcji i utrzymaniem |
| Konfiguracja | Edytuj ustawienia na zywo przez GUI lub przeladuj z dysku |
| Kopie zapasowe | Tworz, przywracaj i zarzadzaj kopiami zapasowymi danych |
| Importy | Migruj dane z innych pluginow frakcji |

>[!TIP] Uzyj `/f admin --text`, aby uzyskac wynik tekstowy na czacie zamiast GUI -- przydatne dla konsoli lub automatyzacji.
