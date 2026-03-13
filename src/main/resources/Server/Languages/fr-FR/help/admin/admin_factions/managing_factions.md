---
id: admin_managing_factions
---
# Gerer les factions

Les administrateurs peuvent inspecter et modifier n'importe quelle faction sur le serveur via le tableau de bord ou les commandes.

## Parcourir les factions

`/f admin factions`
Ouvre le navigateur de factions admin. Consultez toutes les factions avec le nombre de membres, les niveaux de puissance et le territoire.

`/f admin info <faction>`
Ouvre le panneau d'informations admin pour une faction specifique avec tous les details et options de gestion.

## Modifier les parametres de faction

Avec la permission `hyperfactions.admin.modify`, vous pouvez :

- **Renommer** une faction pour resoudre des conflits
- **Definir la couleur** pour corriger des problemes d'affichage
- **Basculer ouvert/ferme** pour remplacer la politique d'adhesion
- **Modifier la description** a des fins de moderation

>[!TIP] Utilisez `/f admin who <player>` pour rechercher a quelle faction un joueur specifique appartient et consulter ses details.

## Consulter les membres et relations

Le panneau d'informations admin affiche :

| Section | Details |
|---------|---------|
| **Membres** | Liste complete avec les roles et la derniere connexion |
| **Relations** | Toutes les relations d'alliance, d'inimitie et de neutralite |
| **Territoire** | Chunks revendiques et equilibre de puissance |
| **Economie** | Solde du tresor et journal des transactions |

>[!NOTE] Les commandes d'inspection admin ne notifient pas la faction inspectee. Seules les modifications declenchent des alertes.
