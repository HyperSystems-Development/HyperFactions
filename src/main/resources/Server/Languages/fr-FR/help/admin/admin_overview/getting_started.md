---
id: admin_getting_started
---
# Premiers pas en tant qu'administrateur

Bienvenue dans l'administration d'HyperFactions. Ce guide couvre vos premieres etapes apres l'installation du plugin.

## Ouvrir le tableau de bord admin

`/f admin`
Ouvre l'interface du tableau de bord admin avec acces a tous les outils de gestion, editeurs de zones et parametres du serveur.

>[!INFO] Vous avez besoin de la permission **hyperfactions.admin.use** ou du statut OP pour acceder aux commandes admin.

## Conditions requises

- **Avec un plugin de permissions** : Accordez `hyperfactions.admin.use`
- **Sans plugin de permissions** : Le joueur doit etre un
operateur du serveur (`adminRequiresOp=true` par defaut)

## Premieres etapes apres l'installation

1. Executez `/f admin` pour verifier votre acces
2. Ouvrez **Config** pour examiner les parametres de faction par defaut
3. Creez une **SafeZone** au spawn avec `/f admin safezone Spawn`
4. Creez eventuellement des **WarZones** pour les arenes JcJ
5. Examinez les parametres de **Sauvegarde** pour assurer la securite des donnees

## Capacites d'administration

| Domaine | Ce que vous pouvez faire |
|---------|--------------------------|
| Factions | Inspecter, modifier ou dissoudre de force n'importe quelle faction |
| Zones | Creer des SafeZones et WarZones avec des drapeaux personnalises |
| Puissance | Remplacer les valeurs de puissance des joueurs/factions |
| Economie | Gerer les tresors de faction et l'entretien |
| Config | Modifier les parametres en direct via l'interface ou recharger depuis le disque |
| Sauvegardes | Creer, restaurer et gerer les sauvegardes de donnees |
| Imports | Migrer les donnees depuis d'autres plugins de faction |

>[!TIP] Utilisez `/f admin --text` pour obtenir une sortie textuelle dans le chat au lieu de l'interface, utile pour la console ou l'automatisation.
