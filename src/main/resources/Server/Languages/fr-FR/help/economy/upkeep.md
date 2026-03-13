---
id: economy_upkeep
---
# Entretien du territoire

Les factions doivent payer un entretien continu pour maintenir leur territoire revendique. Cela empeche l'accumulation de terres et maintient la carte dynamique.

## Couts d'entretien

| Parametre | Valeur par defaut |
|-----------|-------------------|
| Cout par chunk | 2.0 par cycle |
| Intervalle de paiement | Toutes les 24 heures |
| Chunks gratuits | 3 (sans cout) |
| Mode de calcul | Taux fixe |

>[!NOTE] Ce sont les valeurs par defaut. L'administrateur de votre serveur peut avoir configure des parametres differents.

Vos 3 premiers chunks sont gratuits. Au-dela, chaque chunk revendique supplementaire coute 2.0 par cycle de paiement.

## Paiement automatique

Le paiement automatique est active par defaut. Le systeme deduit automatiquement l'entretien de votre tresor a chaque intervalle. Aucune action manuelle n'est necessaire.

---

## Periode de grace

Si votre tresor ne peut pas couvrir l'entretien, une periode de grace de 48 heures commence. Un avertissement est envoye 6 heures avant que les revendications ne commencent a etre perdues.

>[!WARNING] Si l'entretien reste impaye apres la periode de grace, votre faction perd 1 revendication par cycle jusqu'a ce que les couts soient couverts ou que toutes les revendications supplementaires soient perdues.

## Exemple

*Une faction avec 8 revendications paie pour 5 chunks (8 moins 3 gratuits). A 2.0 par chunk, cela fait 10.0 par cycle.*

>[!TIP] Gardez votre tresor approvisionne au-dessus de votre cout d'entretien. Utilisez /f balance pour verifier vos reserves.
