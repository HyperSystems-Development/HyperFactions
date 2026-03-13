---
id: diplomacy_enemies
commands: enemy, neutral
---
# Factions ennemies

Declarer un ennemi est une action unilaterale qui active immediatement le JcJ et l'agression territoriale contre la faction cible. Aucun accord n'est requis.

---

## Declarer un ennemi

`/f enemy <faction>`

Marque instantanement la faction cible comme votre ennemi. Cela prend effet immediatement -- aucune confirmation de l'autre partie n'est necessaire. Necessite le rang d'Officier ou superieur.

## Reinitialiser a neutre

`/f neutral <faction>`

Met fin au statut d'ennemi et reinitialise la relation a neutre. Cela necessite egalement Officier+ et prend effet immediatement.

---

## Ce que le statut d'ennemi active

| Effet | Details |
|-------|---------|
| JcJ dans le territoire | Le JcJ complet est active dans le territoire des deux factions |
| Sur-revendication | Vous pouvez sur-revendiquer leurs chunks s'ils sont en deficit de puissance |
| Marquage sur la carte | Le territoire ennemi s'affiche en rouge sur la carte du territoire |
| Pas de protection | La protection territoriale standard n'empeche pas le JcJ ennemi |

>[!WARNING] Declarer un ennemi est une decision serieuse. Leurs membres peuvent aussi vous combattre dans votre propre territoire une fois la declaration faite.

---

## Considerations strategiques

- Les declarations d'ennemi sont unilaterales -- vous pouvez declarer sans leur consentement, mais ils vous voient egalement comme hostile
- Avant de declarer, verifiez la puissance de la cible avec /f info. S'ils sont forts, vous pourriez perdre du territoire a la place
- Affaiblissez les ennemis par des combats repetes pour drainer leur puissance, puis sur-revendiquez leurs terres
- Il n'y a pas de limite au nombre d'ennemis que vous pouvez avoir, mais combattre sur plusieurs fronts est risque

>[!TIP] Utilisez /f neutral pour desamorcer les conflits. Parfois une paix strategique est plus precieuse qu'une guerre continue.

>[!NOTE] Si vous etes allie avec une faction et que vous la declarez ennemie, l'alliance est rompue en premier.
