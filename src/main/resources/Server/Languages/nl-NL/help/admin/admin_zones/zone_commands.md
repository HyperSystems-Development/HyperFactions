---
id: admin_zone_commands
---
# Zone Commandoreferentie

Volledige referentie voor alle zonebeheercommando's. Alle vereisen de `hyperfactions.admin.zones` permissie.

## Snel Aanmaken

| Commando | Beschrijving |
|----------|-------------|
| `/f admin safezone <name>` | Maak een SafeZone aan bij de huidige chunk |
| `/f admin warzone <name>` | Maak een WarZone aan bij de huidige chunk |
| `/f admin removezone <name>` | Verwijder een zone en geef chunks vrij |

## Zonebeheer

| Commando | Beschrijving |
|----------|-------------|
| `/f admin zone create <name> <type>` | Maak een zone aan (safezone/warzone) |
| `/f admin zone delete <name>` | Verwijder een zone |
| `/f admin zone claim <zone>` | Voeg huidige chunk toe aan zone |
| `/f admin zone unclaim <zone>` | Verwijder huidige chunk uit zone |
| `/f admin zone radius <zone> <r>` | Claim vierkante radius aan chunks |
| `/f admin zone list` | Toon alle zones met chunkaantallen |
| `/f admin zone notify <zone> <true/false>` | Schakel betreed/verlaat-berichten in/uit |
| `/f admin zone title <zone> upper/lower <text>` | Stel zonetiteltekst in |
| `/f admin zone properties <zone>` | Open zone-eigenschappen-GUI |

## Vlagbeheer

| Commando | Beschrijving |
|----------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Stel een specifieke vlag in |

>[!TIP] Gebruik de zone-**eigenschappen-GUI** voor een visuele editor met schakelaars voor elke vlag, georganiseerd per categorie.

## Voorbeelden

- `/f admin safezone Spawn` -- maak spawnbescherming aan
- `/f admin zone radius Spawn 3` -- breid uit naar 7x7 chunks
- `/f admin zoneflag Spawn door_use true` -- sta deuren toe
- `/f admin zone notify Spawn true` -- toon betreedberichten
