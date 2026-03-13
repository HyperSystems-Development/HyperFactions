---
id: admin_power_commands
---
# Power Admincommando's

Overschrijf speler- en factie-powerwaarden. Alle commando's vereisen de `hyperfactions.admin.power` permissie.

## Speler-powercommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin power set <player> <amount>` | Stel exacte powerwaarde in |
| `/f admin power add <player> <amount>` | Voeg power toe aan speler |
| `/f admin power remove <player> <amount>` | Verwijder power van speler |
| `/f admin power reset <player>` | Reset naar standaard startpower |
| `/f admin power info <player>` | Bekijk gedetailleerd power-overzicht |

## Hoe Power Facties Beïnvloedt

De totale power van een factie is de som van de individuele power van alle leden. Gebiedsclaims vereisen voldoende totale power om te onderhouden.

| Scenario | Effect |
|----------|--------|
| Power hoger ingesteld | Factie kan meer grondgebied claimen |
| Power lager ingesteld | Factie kan kwetsbaar worden voor overclaim |
| Power gereset | Speler keert terug naar standaard startwaarde |

>[!WARNING] Het verlagen van de power van een speler kan ertoe leiden dat hun factie grondgebied verliest als de totale power onder het aantal geclaimde chunks zakt.

## Voorbeelden

- `/f admin power set Steve 50` -- instellen op exact 50
- `/f admin power add Steve 10` -- verhogen met 10
- `/f admin power remove Steve 5` -- verlagen met 5
- `/f admin power reset Steve` -- terug naar standaard
- `/f admin power info Steve` -- toon volledig overzicht

>[!TIP] Gebruik `/f admin power info <player>` om huidige power, max power en eventuele actieve overschrijvingen te bekijken voordat je wijzigingen aanbrengt.
