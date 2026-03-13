---
id: admin_configuration
---
# Configuratiesysteem

HyperFactions gebruikt een modulair JSON-configuratiesysteem met 11 configuratiebestanden.

## Admin Config-commando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin config` | Open de visuele config-editor-GUI |
| `/f admin reload` | Herlaad alle configuratiebestanden van schijf |
| `/f admin sync` | Synchroniseer factiedata naar opslag |

## Configuratiebestanden

| Bestand | Inhoud |
|---------|--------|
| `factions.json` | Rollen, power, claims, gevecht, relaties |
| `server.json` | Teleport, automatisch opslaan, berichten, GUI, permissies |
| `economy.json` | Schatkist, onderhoud, transactie-instellingen |
| `backup.json` | Backuprotatie en bewaarinstellingen |
| `chat.json` | Factie- en bondgenotenchat-opmaak |
| `debug.json` | Debug-logcategorieën |
| `faction-permissions.json` | Standaard permissies per rol |
| `announcements.json` | Evenementuitzendingen en gebiedsmeldingen |
| `gravestones.json` | Gravestone-integratie-instellingen |
| `worldmap.json` | Wereldkaart-verversingsmodi |
| `worlds.json` | Per-wereld gedragsoverschrijvingen |

>[!TIP] De config-GUI biedt een visuele editor met beschrijvingen voor elke instelling. Wijzigingen worden direct opgeslagen, maar sommige vereisen `/f admin reload` om volledig van kracht te worden.

## Configuratielocatie

Alle bestanden zijn opgeslagen in:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Handmatige JSON-bewerkingen vereisen `/f admin reload` om toe te passen. Ongeldige JSON zorgt ervoor dat het bestand wordt overgeslagen met een waarschuwing in het serverlog.

>[!NOTE] De configuratieversie wordt bijgehouden in `server.json`. De plugin migreert oudere configuraties automatisch bij het opstarten.
