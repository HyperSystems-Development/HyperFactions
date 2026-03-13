---
id: combat_death
commands: home, sethome, stuck
---
# Mort et recuperation

La mort a de vraies consequences dans les factions. Chaque mort vous coute de la puissance personnelle, affaiblissant la capacite de votre faction a detenir du territoire.

## Perte de puissance

Chaque mort coute -1.0 de puissance sur votre total personnel. Cela reduit la puissance combinee de la faction.

| Evenement | Changement de puissance |
|-----------|------------------------|
| Mort (toute cause) | -1.0 |
| Regeneration en ligne | +0.1 par minute |
| Deconnexion en combat | -1.0 (tue) |

>[!NOTE] Ce sont les valeurs par defaut. L'administrateur de votre serveur peut avoir configure des parametres differents.

## Exemples de scenarios

*5 membres a 10.0 de puissance chacun = 50 au total, 20 revendications.*
*Un membre meurt deux fois : 8.0 de puissance, total de la faction 48.*
*Trois membres meurent une fois chacun : le total tombe a 47.*

>[!WARNING] Si la puissance de votre faction tombe en dessous du cout de vos revendications, les ennemis peuvent sur-revendiquer votre territoire.

## Recuperation

La puissance se regenere a 0.1 par minute en ligne. Recuperer 1.0 de puissance perdue prend environ 10 minutes. Les morts multiples s'accumulent, evitez donc les combats repetes.

---

## Tous les types de mort

La perte de puissance s'applique a toutes les morts : JcJ, creatures, degats de chute, noyade et toute autre cause. Il n'y a pas de facon sure de mourir.

>[!TIP] Definissez un foyer de faction avec /f sethome pour que les membres puissent se regrouper rapidement apres etre morts.
