---
id: admin_world_settings
---
# Parametres par monde

HyperFactions supporte une configuration par monde pour les revendications, le JcJ et le comportement de protection.

## Commandes de monde

| Commande | Description |
|----------|-------------|
| `/f admin world list` | Lister tous les remplacements de monde |
| `/f admin world info <world>` | Afficher les parametres d'un monde |
| `/f admin world set <world> <key> <value>` | Definir un parametre |
| `/f admin world reset <world>` | Reinitialiser le monde aux valeurs par defaut |

## Parametres disponibles

| Parametre | Type | Description |
|-----------|------|-------------|
| claiming_enabled | boolean | Autoriser les revendications de faction dans ce monde |
| pvp_enabled | boolean | Autoriser le combat JcJ dans ce monde |
| power_loss | boolean | Appliquer la perte de puissance a la mort |
| build_protection | boolean | Appliquer la protection de construction des revendications |
| explosion_protection | boolean | Proteger les revendications des explosions |

## Liste blanche / Liste noire de mondes

Controlez quels mondes autorisent les fonctionnalites de faction via le fichier de configuration `worlds.json` :

- **Mode liste blanche** : Seuls les mondes listes autorisent les revendications
- **Mode liste noire** : Tous les mondes autorisent les revendications sauf ceux listes

>[!INFO] Les parametres de monde sont stockes dans `worlds.json` et remplacent les valeurs par defaut globales de `factions.json`.

## Exemples

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- restaurer toutes les valeurs par defaut

>[!TIP] Desactivez les revendications dans les mondes creatif ou lobby pour garder le systeme de factions concentre sur le gameplay de survie.

>[!NOTE] Les parametres par monde ont la priorite sur la configuration globale mais sont remplaces par les drapeaux de zone dans ce monde.
