---
id: admin_power_overrides
---
# Power Overschrijvingen

Speciale powercommando's die het gedrag van power wijzigen voor specifieke spelers of facties.

## Overschrijvingscommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin power setmax <player> <amount>` | Stel aangepast max power-plafond in |
| `/f admin power noloss <player>` | Schakel immuniteit voor sterfte-powerstraf in/uit |
| `/f admin power nodecay <player>` | Schakel immuniteit voor offline power-verval in/uit |
| `/f admin power info <player>` | Bekijk alle overschrijvingen en powerdetails |

## Aangepaste Max Power

`/f admin power setmax <player> <amount>`
Stelt een persoonlijk maximaal power-plafond in voor de speler, dat de serverstandaard overschrijft.

>[!INFO] Het instellen van een aangepast maximum wijzigt de huidige power **niet**. Het verandert alleen het plafond. De speler moet nog steeds power verdienen tot de nieuwe limiet.

## Geen-verlies Modus

`/f admin power noloss <player>`
Schakelt immuniteit voor sterfte-powerverlies in of uit. Wanneer ingeschakeld, verliest de speler **geen** power bij overlijden.

Handig voor:
- Beschermingsperiodes voor nieuwe spelers
- Evenementdeelnemers
- Staffleden

## Geen-verval Modus

`/f admin power nodecay <player>`
Schakelt immuniteit voor offline power-verval in of uit. Wanneer ingeschakeld, zal de power van de speler **niet** afnemen terwijl deze offline is.

Handig voor:
- Spelers met verlengd verlof
- VIP-leden
- Seizoensgebonden bescherming

## Power Info

`/f admin power info <player>`
Toont een volledig overzicht:

- Huidige power en max power
- Actieve overschrijvingen (noloss, nodecay, aangepast max)
- Laatste sterftijd en verloren power
- Bijdragepercentage aan de factie

>[!TIP] Alle power-overschrijvingen blijven behouden over server-herstarts en worden opgeslagen in het databestand van de speler.
