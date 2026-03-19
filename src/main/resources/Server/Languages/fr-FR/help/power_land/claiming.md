---
id: power_claiming
commands: claim, unclaim
---
# Revendiquer un territoire

Revendiquer un chunk le place sous le controle de votre faction. Seuls les membres de la faction peuvent construire, casser ou acceder aux conteneurs dans un territoire revendique.

---

## Comment revendiquer

`/f claim`

Placez-vous dans le chunk que vous souhaitez revendiquer et executez cette commande. Le chunk est immediatement protege. Necessite le rang d'Officier ou superieur.

## Comment annuler une revendication

`/f unclaim`

Libere le chunk dans lequel vous vous trouvez et le remet a l'etat sauvage. Necessite egalement Officier+.

---

## Regles de revendication

| Regle | Valeur par defaut |
|-------|-------------------|
| Cout en puissance par revendication | 2.0 de puissance |
| Maximum de revendications | 100 par faction |
| Adjacence obligatoire | Non (vous pouvez revendiquer n'importe ou) |

>[!NOTE] Ce sont les valeurs par defaut. L'administrateur de votre serveur peut avoir configure des parametres differents.

>[!INFO] Chaque revendication coute 2.0 de puissance a maintenir. Une faction avec 50 de puissance totale peut detenir en securite jusqu'a 25 revendications.

---

## Ce que la protection offre

Dans un territoire revendique, les regles suivantes s'appliquent par defaut :

- Les etrangers ne peuvent ni casser, ni placer, ni interagir avec les blocs
- Les allies peuvent utiliser les portes, les sieges et les transports, mais ne peuvent ni casser ni placer de blocs
- Les Membres et Officiers ont un acces complet pour construire, casser et tout utiliser
- L'acces aux conteneurs (coffres, caisses) est reserve aux membres uniquement

>[!TIP] Vous pouvez aussi revendiquer directement depuis la carte du territoire. Ouvrez /f map et cliquez sur les chunks non revendiques pour les revendiquer.

>[!WARNING] Ne vous etendez pas trop. Si votre faction perd de la puissance a cause des morts, les revendications au-dela de votre budget de puissance deviennent vulnerables a la sur-revendication.
