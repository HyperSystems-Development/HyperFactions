---
id: power_losing
commands: overclaim
---
# Perte de territoire

Lorsque la puissance totale d'une faction tombe en dessous du cout de ses revendications, elle devient pillable. Les ennemis peuvent sur-revendiquer des chunks directement sous vos pieds.

---

## Comment fonctionne la sur-revendication

`/f overclaim`

Un Officier ou Chef d'une faction ennemie se place dans votre chunk revendique et execute cette commande. Si votre faction est en deficit de puissance, le chunk est transfere a leur faction.

## Le calcul

Chaque revendication coute 2.0 de puissance a maintenir. Si votre puissance totale tombe en dessous de ce seuil, les chunks en deficit sont vulnerables.

>[!NOTE] Ce sont les valeurs par defaut. L'administrateur de votre serveur peut avoir configure des parametres differents.

>[!WARNING] La sur-revendication est permanente. Une fois qu'un ennemi prend un chunk, vous devez le re-revendiquer (ou le sur-revendiquer en retour s'il s'affaiblit).

---

## Exemple de scenario

| Facteur | Valeur |
|---------|--------|
| Membres | 5 joueurs |
| Puissance par membre | 10 chacun (initiale) |
| Puissance totale | 50 |
| Revendications | 30 chunks |
| Puissance requise (30 x 2.0) | 60 |
| Deficit | 10 de puissance en moins |

Dans cet exemple, la faction est deja pillable des le depart. Les ennemis pourraient sur-revendiquer jusqu'a 5 chunks (deficit de 10 / 2.0 par revendication) avant que la faction n'atteigne l'equilibre.

---

## Comment prevenir la sur-revendication

- Ne vous etendez pas trop -- gardez toujours la puissance totale au-dessus du cout de vos revendications avec une marge
- Restez actifs -- la puissance ne se regenere qu'en ligne (+0.1/min)
- Evitez les morts inutiles -- chaque mort coute 1.0 de puissance
- Recrutez plus de membres -- plus de joueurs signifie plus de puissance totale
- Annulez la revendication des chunks inutilises -- liberez de la puissance avec /f unclaim

>[!TIP] Verifiez regulierement votre statut de puissance avec /f power. Si votre puissance totale est proche du cout de vos revendications, envisagez d'annuler la revendication de chunks moins importants avant une guerre.
