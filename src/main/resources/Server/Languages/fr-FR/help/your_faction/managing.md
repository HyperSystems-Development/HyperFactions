---
id: faction_managing
commands: invite, kick, promote, demote, transfer
---
# Gerer les membres

Les Officiers et le Chef partagent la responsabilite de gerer la liste des membres de la faction. Voici les commandes cles et qui peut les utiliser.

---

## Commandes

| Commande | Ce qu'elle fait | Role requis |
|----------|----------------|-------------|
| `/f invite <player>` | Envoie une invitation (expire dans 5 min) | Officier+ |
| `/f kick <player>` | Retire un membre de la faction | Officier+ (voir note) |
| `/f promote <player>` | Promeut un Membre en Officier | Chef uniquement |
| `/f demote <player>` | Retrograde un Officier en Membre | Chef uniquement |
| `/f transfer <player>` | Transfere la propriete de la faction | Chef uniquement |

>[!NOTE] Les Officiers ne peuvent expulser que des Membres. Pour retirer un autre Officier, le Chef doit d'abord le retrograder ou l'expulser directement.

---

## Invitations

- Les invitations expirent apres 5 minutes si elles ne sont pas acceptees
- Le joueur invite les voit dans son onglet Invitations en ouvrant /f
- Il n'y a pas de limite au nombre d'invitations que vous pouvez envoyer a la fois
- Votre faction peut accueillir jusqu'a 50 membres au total

## Promotions et retrogradations

- Seul le Chef peut promouvoir ou retrograder
- /f promote eleve un Membre au rang d'Officier
- /f demote rabaisse un Officier au rang de Membre

## Transfert de leadership

>[!WARNING] Le transfert de leadership est irreversible. Vous serez retrograde au rang d'Officier et le joueur cible deviendra le nouveau Chef. Assurez-vous de lui faire entierement confiance.

`/f transfer <player>`

La cible doit etre un membre actuel de votre faction.
