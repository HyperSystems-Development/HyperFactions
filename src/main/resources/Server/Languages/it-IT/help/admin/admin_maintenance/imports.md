---
id: admin_imports
---
# Importazione Dati

Importa dati di fazioni da altri plugin per migrare il tuo server a HyperFactions.

## Comando di Importazione

`/f admin import <source> [path] [flags]`

**Permesso**: `hyperfactions.admin.use`

## Sorgenti Supportate

| Sorgente | Descrizione |
|----------|-------------|
| `elbaphfactions` | Importa da dati ElbaphFactions |
| `hyfactions` | Importa da dati HyFactions v1 |

## Flag di Importazione

| Flag | Descrizione |
|------|-------------|
| `--dry-run` | Valida i dati senza importare nulla |
| `--overwrite` | Sovrascrivi le fazioni esistenti con lo stesso nome |
| `--no-zones` | Salta i dati delle zone durante l'importazione |
| `--no-power` | Salta i dati del potere durante l'importazione |

>[!TIP] Esegui sempre con `--dry-run` prima per visualizzare in anteprima cosa verra' importato e individuare eventuali problemi nei dati prima di confermare le modifiche.

## Processo di Importazione

1. Un backup pre-importazione viene creato automaticamente
2. Le mappature dei nomi giocatore vengono caricate
3. Fazioni, claim e zone vengono convertiti
4. I dati vengono validati e salvati

## Esempi

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Usare `--overwrite` **sostituira'** qualsiasi fazione esistente che condivide un nome con una fazione importata. I dati dei membri e i claim verranno sovrascritti. Esegui prima con `--dry-run` per identificare i conflitti.

>[!NOTE] Alcuni dati specifici della sorgente (es. worker plots, farm plots) non hanno un equivalente in HyperFactions e verranno registrati come avvisi durante l'importazione.
