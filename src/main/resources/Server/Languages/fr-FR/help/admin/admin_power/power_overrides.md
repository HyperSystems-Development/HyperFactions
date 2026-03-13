---
id: admin_power_overrides
---
# Remplacements de puissance

Commandes speciales de puissance qui modifient le comportement de la puissance pour des joueurs ou factions specifiques.

## Commandes de remplacement

| Commande | Description |
|----------|-------------|
| `/f admin power setmax <player> <amount>` | Definir un plafond de puissance maximale personnalise |
| `/f admin power noloss <player>` | Activer/desactiver l'immunite a la penalite de mort |
| `/f admin power nodecay <player>` | Activer/desactiver l'immunite a la decroissance hors ligne |
| `/f admin power info <player>` | Voir tous les remplacements et details de puissance |

## Puissance maximale personnalisee

`/f admin power setmax <player> <amount>`
Definit un plafond de puissance maximale personnalise pour le joueur, remplacant la valeur par defaut du serveur.

>[!INFO] Definir un maximum personnalise ne **modifie pas** la puissance actuelle. Cela change uniquement le plafond. Le joueur doit toujours gagner de la puissance jusqu'a la nouvelle limite.

## Mode sans perte

`/f admin power noloss <player>`
Active/desactive l'immunite a la perte de puissance a la mort. Lorsqu'il est active, le joueur ne **perdra pas** de puissance en mourant.

Utile pour :
- Periodes de protection des nouveaux joueurs
- Participants a des evenements
- Membres du staff

## Mode sans decroissance

`/f admin power nodecay <player>`
Active/desactive l'immunite a la decroissance de puissance hors ligne. Lorsqu'il est active, la puissance du joueur ne **diminuera pas** en etant hors ligne.

Utile pour :
- Joueurs en absence prolongee
- Membres VIP
- Protection saisonniere

## Informations de puissance

`/f admin power info <player>`
Affiche un detail complet :

- Puissance actuelle et puissance maximale
- Remplacements actifs (noloss, nodecay, max personnalise)
- Derniere mort et puissance perdue
- Pourcentage de contribution a la faction

>[!TIP] Tous les remplacements de puissance persistent entre les redemarrages du serveur et sont stockes dans le fichier de donnees du joueur.
