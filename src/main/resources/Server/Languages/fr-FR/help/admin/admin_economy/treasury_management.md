---
id: admin_treasury_management
---
# Gestion du tresor

Commandes admin pour gerer les tresors de faction. Necessite la permission `hyperfactions.admin.economy`.

## Commandes du tresor

| Commande | Description |
|----------|-------------|
| `/f admin economy balance <faction>` | Voir le solde du tresor de la faction |
| `/f admin economy set <faction> <amount>` | Definir le solde exact |
| `/f admin economy add <faction> <amount>` | Ajouter des fonds au tresor |
| `/f admin economy take <faction> <amount>` | Retirer des fonds du tresor |
| `/f admin economy reset <faction>` | Reinitialiser le tresor a zero |

## Exemples

- `/f admin economy balance Vikings` -- verifier le solde
- `/f admin economy set Vikings 5000` -- definir a 5000
- `/f admin economy add Vikings 1000` -- deposer 1000
- `/f admin economy take Vikings 500` -- retirer 500
- `/f admin economy reset Vikings` -- remettre le solde a zero

>[!TIP] Utilisez `/f admin info <faction>` pour voir l'apercu economique complet incluant l'historique des transactions en plus du solde du tresor.

## Cas d'utilisation

| Scenario | Commande |
|----------|----------|
| Distribution de prix d'evenement | `economy add <faction> <prix>` |
| Sanction pour violation de regles | `economy take <faction> <amende>` |
| Reinitialisation economique apres un wipe | `economy reset <faction>` |
| Compensation pour des bugs | `economy add <faction> <montant>` |

>[!WARNING] Les modifications du tresor sont enregistrees dans l'historique des transactions de la faction. Les modifications admin sont enregistrees avec le nom de l'administrateur pour la tracabilite.

>[!NOTE] Toutes les commandes admin d'economie fonctionnent meme lorsque le module economique est desactive dans la configuration. Les donnees sont stockees independamment du statut du module.
