---
id: admin_imports
---
# Data Import

Import faction data from other plugins to migrate your server to HyperFactions.

## Import Command

`/f admin import <source> [path] [flags]`

**Permission**: `hyperfactions.admin.use`

## Supported Sources

| Source | Description |
|--------|-------------|
| `elbaphfactions` | Import from ElbaphFactions data |
| `hyfactions` | Import from HyFactions v1 data |

## Import Flags

| Flag | Description |
|------|-------------|
| `--dry-run` | Validate data without importing anything |
| `--overwrite` | Overwrite existing factions with same name |
| `--no-zones` | Skip zone data during import |
| `--no-power` | Skip power data during import |

>[!TIP] Always run with `--dry-run` first to preview what will be imported and catch any data issues before committing changes.

## Import Process

1. A pre-import backup is created automatically
2. Player name mappings are loaded
3. Factions, claims, and zones are converted
4. Data is validated and saved

## Examples

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Using `--overwrite` will **replace** any existing faction that shares a name with an imported faction. Member data and claims will be overwritten. Run with `--dry-run` first to identify conflicts.

>[!NOTE] Some source-specific data (e.g., worker plots, farm plots) has no equivalent in HyperFactions and will be logged as warnings during import.
