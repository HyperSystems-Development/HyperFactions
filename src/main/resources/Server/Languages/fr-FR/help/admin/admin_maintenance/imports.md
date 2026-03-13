---
id: admin_imports
---
# Import de donnees

Importez des donnees de faction depuis d'autres plugins pour migrer votre serveur vers HyperFactions.

## Commande d'import

`/f admin import <source> [path] [flags]`

**Permission** : `hyperfactions.admin.use`

## Sources supportees

| Source | Description |
|--------|-------------|
| `elbaphfactions` | Importer depuis les donnees ElbaphFactions |
| `hyfactions` | Importer depuis les donnees HyFactions v1 |

## Drapeaux d'import

| Drapeau | Description |
|---------|-------------|
| `--dry-run` | Valider les donnees sans rien importer |
| `--overwrite` | Ecraser les factions existantes avec le meme nom |
| `--no-zones` | Ignorer les donnees de zone pendant l'import |
| `--no-power` | Ignorer les donnees de puissance pendant l'import |

>[!TIP] Executez toujours avec `--dry-run` d'abord pour previsualiser ce qui sera importe et detecter les problemes de donnees avant de valider les changements.

## Processus d'import

1. Une sauvegarde pre-import est creee automatiquement
2. Les correspondances de noms de joueurs sont chargees
3. Les factions, revendications et zones sont converties
4. Les donnees sont validees et enregistrees

## Exemples

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] L'utilisation de `--overwrite` **remplacera** toute faction existante partageant le meme nom qu'une faction importee. Les donnees des membres et les revendications seront ecrasees. Executez d'abord avec `--dry-run` pour identifier les conflits.

>[!NOTE] Certaines donnees specifiques a la source (ex. : parcelles de travailleurs, parcelles agricoles) n'ont pas d'equivalent dans HyperFactions et seront enregistrees comme avertissements lors de l'import.
