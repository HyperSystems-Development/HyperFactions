---
id: admin_imports
---
# Datenimport

Importiere Fraktionsdaten von anderen Plugins, um deinen Server zu HyperFactions zu migrieren.

## Import-Befehl

`/f admin import <source> [path] [flags]`

**Berechtigung**: `hyperfactions.admin.use`

## Unterstuetzte Quellen

| Quelle | Beschreibung |
|--------|-------------|
| `elbaphfactions` | Import von ElbaphFactions-Daten |
| `hyfactions` | Import von HyFactions v1-Daten |

## Import-Flags

| Flag | Beschreibung |
|------|-------------|
| `--dry-run` | Daten validieren, ohne etwas zu importieren |
| `--overwrite` | Bestehende Fraktionen mit gleichem Namen ueberschreiben |
| `--no-zones` | Zonendaten beim Import ueberspringen |
| `--no-power` | Machtdaten beim Import ueberspringen |

>[!TIP] Fuehre immer zuerst mit `--dry-run` aus, um eine Vorschau dessen zu erhalten, was importiert wird, und Datenprobleme vor der endgueltigen Uebernahme zu erkennen.

## Importprozess

1. Ein Vor-Import-Backup wird automatisch erstellt
2. Spielernamens-Zuordnungen werden geladen
3. Fraktionen, Ansprueche und Zonen werden konvertiert
4. Daten werden validiert und gespeichert

## Beispiele

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Die Verwendung von `--overwrite` wird jede bestehende Fraktion **ersetzen**, die denselben Namen wie eine importierte Fraktion traegt. Mitgliederdaten und Ansprueche werden ueberschrieben. Fuehre zuerst `--dry-run` aus, um Konflikte zu identifizieren.

>[!NOTE] Einige quellenspezifische Daten (z.B. Arbeitergrundstucke, Farmgrundstucke) haben kein Aequivalent in HyperFactions und werden als Warnungen waehrend des Imports protokolliert.
