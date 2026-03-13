---
id: admin_configuration
---
# Systeme de configuration

HyperFactions utilise un systeme de configuration JSON modulaire avec 11 fichiers de configuration.

## Commandes de configuration admin

| Commande | Description |
|----------|-------------|
| `/f admin config` | Ouvrir l'editeur visuel de configuration |
| `/f admin reload` | Recharger tous les fichiers de configuration depuis le disque |
| `/f admin sync` | Synchroniser les donnees de faction vers le stockage |

## Fichiers de configuration

| Fichier | Contenu |
|---------|---------|
| `factions.json` | Roles, puissance, revendications, combat, relations |
| `server.json` | Teleportation, sauvegarde auto, messages, interface, permissions |
| `economy.json` | Tresor, entretien, parametres de transaction |
| `backup.json` | Rotation et retention des sauvegardes |
| `chat.json` | Formatage de la discussion de faction et d'allie |
| `debug.json` | Categories de journalisation de debogage |
| `faction-permissions.json` | Permissions par defaut par role |
| `announcements.json` | Diffusion d'evenements et notifications territoriales |
| `gravestones.json` | Parametres d'integration des pierres tombales |
| `worldmap.json` | Modes de rafraichissement de la carte du monde |
| `worlds.json` | Remplacements de comportement par monde |

>[!TIP] L'interface de configuration fournit un editeur visuel avec des descriptions pour chaque parametre. Les modifications sont enregistrees immediatement mais certaines necessitent `/f admin reload` pour prendre pleinement effet.

## Emplacement de la configuration

Tous les fichiers sont stockes dans :
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Les modifications manuelles du JSON necessitent `/f admin reload` pour etre appliquees. Un JSON invalide entrainera le saut du fichier avec un avertissement dans le journal du serveur.

>[!NOTE] La version de configuration est suivie dans `server.json`. Le plugin migre automatiquement les anciennes configurations au demarrage.
