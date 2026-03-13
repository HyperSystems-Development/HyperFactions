---
id: admin_imports
---
# Import danych

Importuj dane frakcji z innych pluginów, aby zmigrować swój serwer na HyperFactions.

## Komenda importu

`/f admin import <source> [path] [flags]`

**Uprawnienie**: `hyperfactions.admin.use`

## Obsługiwane źródła

| Źródło | Opis |
|--------|-------------|
| `elbaphfactions` | Import z danych ElbaphFactions |
| `hyfactions` | Import z danych HyFactions v1 |

## Flagi importu

| Flaga | Opis |
|------|-------------|
| `--dry-run` | Waliduj dane bez importowania czegokolwiek |
| `--overwrite` | Nadpisz istniejące frakcje o tej samej nazwie |
| `--no-zones` | Pomiń dane stref podczas importu |
| `--no-power` | Pomiń dane mocy podczas importu |

>[!TIP] Zawsze uruchom najpierw z `--dry-run`, aby zobaczyć podgląd tego, co zostanie zaimportowane i wykryć problemy z danymi przed zatwierdzeniem zmian.

## Proces importu

1. Kopia zapasowa przed importem jest tworzona automatycznie
2. Mapowania nazw graczy są ładowane
3. Frakcje, zajęcia i strefy są konwertowane
4. Dane są walidowane i zapisywane

## Przykłady

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Użycie `--overwrite` **zastąpi** każdą istniejącą frakcję, która dzieli nazwę z importowaną frakcją. Dane członków i zajęcia zostaną nadpisane. Uruchom najpierw z `--dry-run`, aby zidentyfikować konflikty.

>[!NOTE] Niektóre dane specyficzne dla źródła (np. działki robocze, działki rolnicze) nie mają odpowiednika w HyperFactions i zostaną zalogowane jako ostrzeżenia podczas importu.
