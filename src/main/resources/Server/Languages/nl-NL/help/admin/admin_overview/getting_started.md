---
id: admin_getting_started
---
# Aan de Slag als Admin

Welkom bij HyperFactions administratie. Deze gids behandelt je eerste stappen na het installeren van de plugin.

## Het Admin Dashboard Openen

`/f admin`
Opent de admin-dashboard-GUI met toegang tot alle beheertools, zone-editors en serverinstellingen.

>[!INFO] Je hebt de **hyperfactions.admin.use** permissie of OP-status nodig om admincommando's te gebruiken.

## Vereisten

- **Met een permissieplugin**: Ken `hyperfactions.admin.use` toe
- **Zonder een permissieplugin**: De speler moet een
serveroperator zijn (`adminRequiresOp=true` standaard)

## Eerste Stappen na Installatie

1. Voer `/f admin` uit om je toegang te verifiëren
2. Open **Config** om de standaard factie-instellingen te bekijken
3. Maak een **SafeZone** bij de spawn met `/f admin safezone Spawn`
4. Maak optioneel **WarZones** aan voor PvP-arena's
5. Bekijk **Backup**-instellingen om dataveiligheid te waarborgen

## Admin Mogelijkheden

| Gebied | Wat je kunt doen |
|--------|-----------------|
| Facties | Inspecteer, wijzig of ontbind elke factie geforceerd |
| Zones | Maak SafeZones en WarZones aan met aangepaste vlaggen |
| Power | Overschrijf speler/factie-powerwaarden |
| Economie | Beheer factieschatkisten en onderhoud |
| Config | Bewerk instellingen live via GUI of herlaad van schijf |
| Backups | Maak backups, herstel en beheer ze |
| Imports | Migreer data van andere factieplugins |

>[!TIP] Gebruik `/f admin --text` om chatgebaseerde uitvoer te krijgen in plaats van de GUI, handig voor console of automatisering.
