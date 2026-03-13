---
id: admin_permissions
---
# Permissions admin

Toutes les fonctionnalites admin sont protegees par des noeuds de permission dans l'espace de noms `hyperfactions.admin`.

## Noeuds de permission

| Permission | Description |
|-----------|-------------|
| `hyperfactions.admin.*` | Accorde **toutes** les permissions admin |
| `hyperfactions.admin.use` | Acceder au tableau de bord `/f admin` |
| `hyperfactions.admin.reload` | Recharger les fichiers de configuration |
| `hyperfactions.admin.debug` | Activer/desactiver les categories de journalisation de debogage |
| `hyperfactions.admin.zones` | Creer, modifier et supprimer des zones |
| `hyperfactions.admin.disband` | Dissoudre de force n'importe quelle faction |
| `hyperfactions.admin.modify` | Modifier les parametres de n'importe quelle faction |
| `hyperfactions.admin.bypass.limits` | Contourner les limites de revendication et de puissance |
| `hyperfactions.admin.backup` | Creer et restaurer des sauvegardes |
| `hyperfactions.admin.power` | Remplacer les valeurs de puissance des joueurs |
| `hyperfactions.admin.economy` | Gerer les tresors de faction |

## Comportement de repli

Lorsqu'**aucun plugin de permissions** n'est installe, les permissions admin se rabattent sur le statut d'operateur du serveur (OP). Ceci est controle par `adminRequiresOp` dans la configuration du serveur (defaut : `true`).

>[!NOTE] Le joker `hyperfactions.admin.*` accorde toutes les permissions admin. Utilisez des noeuds individuels pour un controle granulaire de votre equipe de staff.

## Ordre de resolution des permissions

1. Fournisseur **VaultUnlocked** (si disponible)
2. Fournisseur **HyperPerms** (si disponible)
3. Fournisseur **LuckPerms** (si disponible)
4. **Verification OP** pour les noeuds admin (repli)

>[!WARNING] Sans plugin de permissions et avec `adminRequiresOp` desactive, les commandes admin sont **ouvertes a tous les joueurs**. Utilisez toujours un plugin de permissions en production.
