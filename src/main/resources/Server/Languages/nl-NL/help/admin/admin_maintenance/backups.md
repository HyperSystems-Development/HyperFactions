---
id: admin_backups
---
# Backupsysteem

HyperFactions bevat automatische en handmatige backups met GFS (Grandfather-Father-Son) rotatie.

## Backupcommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin backup create` | Maak nu een handmatige backup |
| `/f admin backup list` | Toon alle beschikbare backups |
| `/f admin backup restore <name>` | Herstel vanuit een backup |
| `/f admin backup delete <name>` | Verwijder een specifieke backup |

**Permissie**: `hyperfactions.admin.backup`

## GFS Rotatiestandaarden

| Type | Bewaarperiode | Beschrijving |
|------|---------------|-------------|
| Per uur | 24 | Laatste 24 uurlijkse snapshots |
| Dagelijks | 7 | Laatste 7 dagelijkse snapshots |
| Wekelijks | 4 | Laatste 4 wekelijkse snapshots |
| Handmatig | 10 | Handmatig gemaakte backups |
| Afsluiting | 5 | Gemaakt bij serverstop |

>[!INFO] Afsluitingsbackups zijn standaard ingeschakeld (`onShutdown=true`). Ze leggen de laatste staat vast voordat de server stopt.

## Backupinhoud

Elk backup-ZIP-archief bevat:
- Alle factiedatabestanden
- Speler-powerdata
- Zonedefinities
- Chatgeschiedenis en economiedata
- Uitnodigings- en toetredingsverzoekdata
- Configuratiebestanden

>[!WARNING] **Het herstellen van een backup is destructief.** Het vervangt alle huidige data door de inhoud van de backup. Alle wijzigingen na het maken van de backup gaan verloren. Maak altijd een verse backup voordat je herstelt.

## Best Practices

1. Maak een handmatige backup voor belangrijke adminacties
2. Bekijk backup-bewaarinstellingen in `backup.json`
3. Test eerst herstel op een testserver
4. Houd afsluitingsbackups ingeschakeld voor crashherstel
