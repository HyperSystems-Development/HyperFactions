---
id: admin_disbanding
---
# Dissolution forcee

Les administrateurs peuvent dissoudre de force n'importe quelle faction, independamment des souhaits du chef.

## Commande

`/f admin disband <faction>`
Dissout de force la faction nommee. Une invite de confirmation apparaitra avant l'execution de l'action.

**Permission** : `hyperfactions.admin.disband`

>[!WARNING] Dissoudre une faction est **irreversible**. Toutes les revendications sont liberees, tous les membres sont retires et la faction cesse d'exister. Creez d'abord une sauvegarde.

## Consequences

Lorsqu'une faction est dissoute :

| Effet | Description |
|-------|-------------|
| **Revendications** | Tout le territoire est libere immediatement |
| **Membres** | Tous les joueurs sont retires de la liste |
| **Relations** | Toutes les alliances et inimities sont effacees |
| **Tresor** | Gere selon les parametres de configuration de l'economie |
| **Foyer** | Le foyer de faction est supprime |
| **Discussion** | L'historique de discussion de faction est supprime |

## Bonnes pratiques

1. Executez toujours `/f admin backup create` avant de dissoudre
2. Notifiez les membres de la faction si possible
3. Documentez la raison pour les archives du serveur
4. Verifiez avec `/f admin info <faction>` avant d'agir

>[!TIP] Si le probleme concerne un membre specifique, envisagez d'utiliser l'interface admin des factions pour transferer le leadership plutot que de dissoudre la faction entiere.
