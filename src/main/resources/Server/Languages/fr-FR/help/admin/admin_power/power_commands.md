---
id: admin_power_commands
---
# Commandes admin de puissance

Remplacez les valeurs de puissance des joueurs et des factions. Toutes les commandes necessitent la permission `hyperfactions.admin.power`.

## Commandes de puissance des joueurs

| Commande | Description |
|----------|-------------|
| `/f admin power set <player> <amount>` | Definir la valeur exacte de puissance |
| `/f admin power add <player> <amount>` | Ajouter de la puissance au joueur |
| `/f admin power remove <player> <amount>` | Retirer de la puissance au joueur |
| `/f admin power reset <player>` | Reinitialiser a la puissance de depart par defaut |
| `/f admin power info <player>` | Voir le detail complet de la puissance |

## Impact de la puissance sur les factions

La puissance totale d'une faction est la somme de la puissance individuelle de tous ses membres. Les revendications territoriales necessitent une puissance totale suffisante pour etre maintenues.

| Scenario | Effet |
|----------|-------|
| Puissance augmentee | La faction peut revendiquer plus de territoire |
| Puissance diminuee | La faction peut devenir vulnerable a la sur-revendication |
| Puissance reinitialisee | Remet le joueur a la valeur de depart par defaut |

>[!WARNING] Diminuer la puissance d'un joueur peut faire perdre du territoire a sa faction si la puissance totale tombe en dessous du nombre de chunks revendiques.

## Exemples

- `/f admin power set Steve 50` -- definir a exactement 50
- `/f admin power add Steve 10` -- augmenter de 10
- `/f admin power remove Steve 5` -- diminuer de 5
- `/f admin power reset Steve` -- retour a la valeur par defaut
- `/f admin power info Steve` -- afficher le detail complet

>[!TIP] Utilisez `/f admin power info <player>` pour voir la puissance actuelle, la puissance maximale et les eventuels remplacement actifs avant d'effectuer des modifications.
