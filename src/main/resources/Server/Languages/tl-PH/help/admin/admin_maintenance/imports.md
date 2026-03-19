---
id: admin_imports
---
# Pag-import ng Data

Mag-import ng faction data mula sa ibang plugin para i-migrate ang server mo sa HyperFactions.

## Import Command

`/f admin import <source> [path] [flags]`

**Permission**: `hyperfactions.admin.use`

## Mga Supported na Source

| Source | Paglalarawan |
|--------|-------------|
| `elbaphfactions` | Mag-import mula sa ElbaphFactions data |
| `hyfactions` | Mag-import mula sa HyFactions v1 data |

## Mga Import Flag

| Flag | Paglalarawan |
|------|-------------|
| `--dry-run` | I-validate ang data nang hindi nag-i-import ng kahit ano |
| `--overwrite` | I-overwrite ang mga existing faction na may parehong pangalan |
| `--no-zones` | Laktawan ang zone data sa pag-import |
| `--no-power` | Laktawan ang power data sa pag-import |

>[!TIP] Palaging patakbuhin muna gamit ang `--dry-run` para ma-preview kung ano ang ii-import at mahuli ang mga data issue bago mag-commit ng mga pagbabago.

## Proseso ng Import

1. Awtomatikong gumagawa ng pre-import backup
2. Lino-load ang mga player name mapping
3. Kino-convert ang mga faction, claim, at zone
4. Vine-validate at sine-save ang data

## Mga Halimbawa

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Ang paggamit ng `--overwrite` ay **magpapalit** ng kahit anong existing faction na may parehong pangalan ng na-import na faction. Mao-overwrite ang member data at mga claim. Patakbuhin muna gamit ang `--dry-run` para matukoy ang mga conflict.

>[!NOTE] Ang ilang source-specific na data (hal., worker plots, farm plots) ay walang katumbas sa HyperFactions at ilo-log bilang mga babala sa pag-import.
