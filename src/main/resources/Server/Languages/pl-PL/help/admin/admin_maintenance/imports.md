---
id: admin_imports
---
# Import danych

Importuj dane frakcji z innych pluginow, aby zmigrowac swoj serwer na HyperFactions.

## Komenda importu

`/f admin import <source> [path] [flags]`

**Uprawnienie**: `hyperfactions.admin.use`

## Obslugiwane zrodla

| Zrodlo | Opis |
|--------|-------------|
| `elbaphfactions` | Import z danych ElbaphFactions |
| `hyfactions` | Import z danych HyFactions v1 |

## Flagi importu

| Flaga | Opis |
|------|-------------|
| `--dry-run` | Waliduj dane bez importowania czegokolwiek |
| `--overwrite` | Nadpisz istniejace frakcje o tej samej nazwie |
| `--no-zones` | Pomin dane stref podczas importu |
| `--no-power` | Pomin dane mocy podczas importu |

>[!TIP] Zawsze uruchom najpierw z `--dry-run`, aby zobaczyc podglad tego, co zostanie zaimportowane i wykryc problemy z danymi przed zatwierdzeniem zmian.

## Proces importu

1. Kopia zapasowa przed importem jest tworzona automatycznie
2. Mapowania nazw graczy sa ladowane
3. Frakcje, zajecia i strefy sa konwertowane
4. Dane sa walidowane i zapisywane

## Przyklady

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Uzycie `--overwrite` **zastapi** kazda istniejaca frakcje, ktora dzieli nazwe z importowana frakcja. Dane czlonkow i zajecia zostana nadpisane. Uruchom najpierw z `--dry-run`, aby zidentyfikowac konflikty.

>[!NOTE] Niektore dane specyficzne dla zrodla (np. dzialki robocze, dzialki rolnicze) nie maja odpowiednika w HyperFactions i zostana zalogowane jako ostrzezenia podczas importu.
