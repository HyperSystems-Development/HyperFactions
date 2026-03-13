---
id: admin_zone_basics
---
# Zone Basis

Zones zijn door admins beheerde gebieden met aangepaste regels die de normale factiegebiedsbescherming overschrijven.

## Zonetypes

- **SafeZone** -- Geen PvP, geen bouwen, geen schade.
Ideaal voor spawngebieden en handelscentra.
- **WarZone** -- PvP altijd ingeschakeld, geen bouwen.
Ideaal voor arena's en betwiste gevechtsgebieden.

## Zones Aanmaken

`/f admin safezone <name>`
Maakt een SafeZone aan en claimt je huidige chunk.

`/f admin warzone <name>`
Maakt een WarZone aan en claimt je huidige chunk.

Ga na het aanmaken in extra chunks staan en gebruik `/f admin zone claim <zone>` om de zone uit te breiden.

## Zonechunks Beheren

`/f admin zone claim <zone>`
Voeg de huidige chunk toe aan de genoemde zone.

`/f admin zone unclaim <zone>`
Verwijder de huidige chunk uit de genoemde zone.

`/f admin zone radius <zone> <radius>`
Claim een vierkant van chunks rondom je positie.

## Zones Verwijderen

`/f admin removezone <name>`
Verwijdert de zone permanent en geeft al haar geclaimde chunks vrij.

>[!WARNING] Het verwijderen van een zone geeft al haar chunks direct vrij. Dit kan niet ongedaan worden gemaakt zonder een backup-herstel.

>[!INFO] Zoneregels **overschrijven altijd** factiegebiedsregels. Een SafeZone in vijandelijk land is nog steeds veilig.
