---
id: admin_imports
---
# Data Importeren

Importeer factiedata van andere plugins om je server te migreren naar HyperFactions.

## Importcommando

`/f admin import <source> [path] [flags]`

**Permissie**: `hyperfactions.admin.use`

## Ondersteunde Bronnen

| Bron | Beschrijving |
|------|-------------|
| `elbaphfactions` | Importeer vanuit ElbaphFactions-data |
| `hyfactions` | Importeer vanuit HyFactions v1-data |

## Importvlaggen

| Vlag | Beschrijving |
|------|-------------|
| `--dry-run` | Valideer data zonder iets te importeren |
| `--overwrite` | Overschrijf bestaande facties met dezelfde naam |
| `--no-zones` | Sla zonedata over tijdens import |
| `--no-power` | Sla powerdata over tijdens import |

>[!TIP] Voer altijd eerst uit met `--dry-run` om te bekijken wat er geïmporteerd wordt en dataproblemen te ontdekken voordat je wijzigingen doorvoert.

## Importproces

1. Er wordt automatisch een pre-import backup gemaakt
2. Spelernaam-koppelingen worden geladen
3. Facties, claims en zones worden geconverteerd
4. Data wordt gevalideerd en opgeslagen

## Voorbeelden

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Het gebruik van `--overwrite` zal elke bestaande factie die dezelfde naam deelt met een geïmporteerde factie **vervangen**. Ledendata en claims worden overschreven. Voer eerst `--dry-run` uit om conflicten te identificeren.

>[!NOTE] Sommige bronspecifieke data (bijv. werkpercelen, boerderijpercelen) heeft geen equivalent in HyperFactions en wordt als waarschuwingen gelogd tijdens de import.
