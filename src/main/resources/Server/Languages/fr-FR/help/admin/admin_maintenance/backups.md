---
id: admin_backups
---
# Systeme de sauvegarde

HyperFactions inclut des sauvegardes automatiques et manuelles avec une rotation GFS (Grand-pere-Pere-Fils).

## Commandes de sauvegarde

| Commande | Description |
|----------|-------------|
| `/f admin backup create` | Creer une sauvegarde manuelle maintenant |
| `/f admin backup list` | Lister toutes les sauvegardes disponibles |
| `/f admin backup restore <name>` | Restaurer a partir d'une sauvegarde |
| `/f admin backup delete <name>` | Supprimer une sauvegarde specifique |

**Permission** : `hyperfactions.admin.backup`

## Parametres de rotation GFS par defaut

| Type | Retention | Description |
|------|-----------|-------------|
| Horaire | 24 | Les 24 derniers cliches horaires |
| Quotidien | 7 | Les 7 derniers cliches quotidiens |
| Hebdomadaire | 4 | Les 4 derniers cliches hebdomadaires |
| Manuel | 10 | Sauvegardes creees manuellement |
| Arret | 5 | Creees a l'arret du serveur |

>[!INFO] Les sauvegardes a l'arret sont activees par defaut (`onShutdown=true`). Elles capturent l'etat le plus recent avant l'arret du serveur.

## Contenu des sauvegardes

Chaque archive ZIP de sauvegarde contient :
- Tous les fichiers de donnees de faction
- Les donnees de puissance des joueurs
- Les definitions de zones
- L'historique de discussion et les donnees economiques
- Les donnees d'invitations et de demandes d'adhesion
- Les fichiers de configuration

>[!WARNING] **Restaurer une sauvegarde est destructif.** Cela remplace toutes les donnees actuelles par le contenu de la sauvegarde. Tout changement effectue apres la creation de la sauvegarde sera perdu. Creez toujours une nouvelle sauvegarde avant de restaurer.

## Bonnes pratiques

1. Creez une sauvegarde manuelle avant les actions admin majeures
2. Examinez la retention des sauvegardes dans `backup.json`
3. Testez d'abord la restauration sur un serveur de test
4. Gardez les sauvegardes a l'arret activees pour la recuperation apres un crash
