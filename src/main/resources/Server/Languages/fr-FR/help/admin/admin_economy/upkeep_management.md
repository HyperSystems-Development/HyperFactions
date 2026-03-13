---
id: admin_upkeep_management
---
# Gestion de l'entretien

L'entretien de faction facture les factions periodiquement en fonction de leur territoire et du nombre de membres.

## Controles admin

Les parametres d'entretien sont geres via le fichier de configuration economique ou l'interface de configuration admin.

`/f admin config`
Ouvrir l'editeur de configuration et naviguer vers les parametres economiques pour ajuster les valeurs d'entretien.

## Parametres d'entretien par defaut

| Parametre | Defaut | Description |
|-----------|--------|-------------|
| Entretien active | false | Interrupteur principal du systeme |
| Intervalle d'entretien | 24h | Frequence de facturation de l'entretien |
| Cout par revendication | 5.0 | Cout par chunk revendique par cycle |
| Cout par membre | 0.0 | Cout par membre par cycle |
| Periode de grace | 72h | Les nouvelles factions sont exemptees |
| Dissolution en cas de faillite | false | Dissolution automatique si le paiement est impossible |

## Surveiller l'entretien

Utilisez `/f admin info <faction>` pour voir :
- Le solde actuel du tresor
- Le cout estime d'entretien par cycle
- Le temps restant avant le prochain prelevement d'entretien
- Si la faction peut se permettre l'entretien

>[!TIP] Consultez les statistiques economiques de toutes les factions depuis le tableau de bord admin pour identifier les factions a risque de faillite avant que l'entretien ne se declenche.

>[!INFO] La configuration de l'entretien est stockee dans `economy.json`. Les modifications effectuees via l'interface de configuration prennent effet apres un rechargement avec `/f admin reload`.

## Formule d'entretien

**Entretien total** = (chunks revendiques x cout par revendication) + (nombre de membres x cout par membre)

>[!WARNING] Activer l'entretien sur un serveur avec des factions existantes peut provoquer des faillites inattendues. Envisagez de definir une periode de grace ou d'annoncer le changement a l'avance.
