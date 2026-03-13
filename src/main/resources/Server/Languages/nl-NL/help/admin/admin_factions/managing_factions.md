---
id: admin_managing_factions
---
# Facties Beheren

Admins kunnen elke factie op de server inspecteren en wijzigen via het dashboard of commando's.

## Facties Bekijken

`/f admin factions`
Opent de admin-factiebrowser. Bekijk alle facties met ledenaantallen, powerniveaus en grondgebied.

`/f admin info <faction>`
Opent het admin-infopaneel voor een specifieke factie met volledige details en beheeropties.

## Factie-instellingen Wijzigen

Met de `hyperfactions.admin.modify` permissie kun je:

- **Hernoemen** van een factie om conflicten op te lossen
- **Kleur instellen** om weergaveproblemen te verhelpen
- **Open/gesloten schakelen** om het toetredingsbeleid te overschrijven
- **Beschrijving bewerken** voor moderatiedoeleinden

>[!TIP] Gebruik `/f admin who <player>` om op te zoeken bij welke factie een specifieke speler hoort en hun details te bekijken.

## Leden en Relaties Bekijken

Het admin-infopaneel toont:

| Sectie | Details |
|--------|---------|
| **Leden** | Volledige ledenlijst met rollen en laatst gezien |
| **Relaties** | Alle bondgenoot-, vijand- en neutrale verhoudingen |
| **Grondgebied** | Geclaimde chunks en powerbalans |
| **Economie** | Schatkistsaldo en transactielog |

>[!NOTE] Admin-inspectiecommando's melden de bekeken factie niet. Alleen wijzigingen activeren meldingen.
