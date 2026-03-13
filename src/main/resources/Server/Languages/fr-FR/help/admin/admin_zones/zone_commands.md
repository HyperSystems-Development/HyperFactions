---
id: admin_zone_commands
---
# Reference des commandes de zone

Reference complete de toutes les commandes de gestion de zone. Toutes necessitent la permission `hyperfactions.admin.zones`.

## Creation rapide

| Commande | Description |
|----------|-------------|
| `/f admin safezone <name>` | Creer une SafeZone au chunk actuel |
| `/f admin warzone <name>` | Creer une WarZone au chunk actuel |
| `/f admin removezone <name>` | Supprimer une zone et liberer les chunks |

## Gestion des zones

| Commande | Description |
|----------|-------------|
| `/f admin zone create <name> <type>` | Creer une zone (safezone/warzone) |
| `/f admin zone delete <name>` | Supprimer une zone |
| `/f admin zone claim <zone>` | Ajouter le chunk actuel a la zone |
| `/f admin zone unclaim <zone>` | Retirer le chunk actuel de la zone |
| `/f admin zone radius <zone> <r>` | Revendiquer un rayon carre de chunks |
| `/f admin zone list` | Lister toutes les zones avec le nombre de chunks |
| `/f admin zone notify <zone> <true/false>` | Activer/desactiver les messages d'entree/sortie |
| `/f admin zone title <zone> upper/lower <text>` | Definir le texte du titre de zone |
| `/f admin zone properties <zone>` | Ouvrir l'interface des proprietes de zone |

## Gestion des drapeaux

| Commande | Description |
|----------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Definir un drapeau specifique |

>[!TIP] Utilisez l'interface des **proprietes** de zone pour un editeur visuel avec des bascules pour chaque drapeau, organise par categorie.

## Exemples

- `/f admin safezone Spawn` -- creer une protection de spawn
- `/f admin zone radius Spawn 3` -- etendre a 7x7 chunks
- `/f admin zoneflag Spawn door_use true` -- autoriser les portes
- `/f admin zone notify Spawn true` -- afficher les messages d'entree
