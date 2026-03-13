---
id: power_understanding
commands: power
---
# Comprendre la puissance

La puissance est la ressource centrale qui determine la quantite de territoire que votre faction peut detenir. Chaque joueur possede une puissance personnelle qui contribue au total de la faction.

---

## Valeurs de puissance par defaut

| Parametre | Valeur |
|-----------|--------|
| Puissance maximale par joueur | 20 |
| Puissance de depart | 10 |
| Penalite de mort | -1.0 par mort |
| Recompense d'elimination | 0.0 |
| Taux de regeneration | +0.1 par minute (en ligne) |
| Cout en puissance par revendication | 2.0 |
| Deconnexion en etant marque | -1.0 supplementaire |

>[!NOTE] Ce sont les valeurs par defaut. L'administrateur de votre serveur peut avoir configure des parametres differents.

## Comment ca fonctionne

La puissance totale de votre faction est la somme de la puissance personnelle de chaque membre. Votre puissance requise est le nombre de revendications multiplie par 2.0. Tant que la puissance totale reste au-dessus de la puissance requise, votre territoire est en securite.

>[!INFO] La puissance se regenere passivement a 0.1 par minute tant que vous etes en ligne. A ce rythme, recuperer 1.0 de puissance prend environ 10 minutes.

---

## Verifier votre puissance

`/f power`

Affiche votre puissance personnelle, la puissance totale de votre faction et la quantite necessaire pour maintenir les revendications actuelles.

## La zone de danger

Si la puissance totale tombe en dessous du montant requis pour vos revendications, votre faction devient vulnerable. Les ennemis peuvent sur-revendiquer vos chunks.

>[!WARNING] Plusieurs morts en peu de temps peuvent s'enchainer rapidement. Si vous avez 5 membres chacun a 10 de puissance (50 au total) et 20 revendications (40 necessaires), 5 morts dans votre equipe vous font descendre a 45 -- toujours en securite. Mais 11 morts vous mettent a 39, en dessous du seuil de 40.

>[!TIP] Gardez une marge de puissance. Ne revendiquez pas chaque chunk que vous pouvez vous permettre -- laissez de la place pour quelques morts sans devenir pillable.
