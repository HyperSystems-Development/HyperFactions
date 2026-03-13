---
id: admin_zone_basics
---
# Bases des zones

Les zones sont des territoires controles par les administrateurs avec des regles personnalisees qui remplacent la protection normale des factions.

## Types de zones

- **SafeZone** -- Pas de JcJ, pas de construction, pas de degats.
Ideal pour les zones de reapparition et les centres commerciaux.
- **WarZone** -- JcJ toujours active, pas de construction.
Ideal pour les arenes et les zones de bataille disputees.

## Creer des zones

`/f admin safezone <name>`
Cree une SafeZone et revendique votre chunk actuel.

`/f admin warzone <name>`
Cree une WarZone et revendique votre chunk actuel.

Apres la creation, placez-vous dans des chunks supplementaires et utilisez `/f admin zone claim <zone>` pour etendre la zone.

## Gerer les chunks de zone

`/f admin zone claim <zone>`
Ajouter le chunk actuel a la zone nommee.

`/f admin zone unclaim <zone>`
Retirer le chunk actuel de la zone nommee.

`/f admin zone radius <zone> <radius>`
Revendiquer un carre de chunks autour de votre position.

## Supprimer des zones

`/f admin removezone <name>`
Supprime definitivement la zone et libere tous ses chunks revendiques.

>[!WARNING] Supprimer une zone libere tous ses chunks instantanement. Cela ne peut pas etre annule sans une restauration de sauvegarde.

>[!INFO] Les regles de zone **remplacent toujours** les regles de territoire de faction. Une SafeZone dans un territoire ennemi reste sure.
