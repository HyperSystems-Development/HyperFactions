---
id: admin_disbanding
---
# Geforceerd Ontbinden

Admins kunnen elke factie geforceerd ontbinden, ongeacht de wensen van de leider.

## Commando

`/f admin disband <faction>`
Ontbindt de genoemde factie geforceerd. Er verschijnt een bevestigingsvraag voordat de actie wordt uitgevoerd.

**Permissie**: `hyperfactions.admin.disband`

>[!WARNING] Het ontbinden van een factie is **onomkeerbaar**. Alle claims worden vrijgegeven, alle leden worden verwijderd en de factie houdt op te bestaan. Maak eerst een backup.

## Gevolgen

Wanneer een factie wordt ontbonden:

| Effect | Beschrijving |
|--------|-------------|
| **Claims** | Al het grondgebied wordt direct vrijgegeven |
| **Leden** | Alle spelers worden van de ledenlijst verwijderd |
| **Relaties** | Alle bondgenootschappen en vijandschappen worden gewist |
| **Schatkist** | Afgehandeld volgens economie-configuratie |
| **Thuis** | Factiehuis wordt verwijderd |
| **Chat** | Factiechatgeschiedenis wordt verwijderd |

## Best Practices

1. Voer altijd `/f admin backup create` uit voor het ontbinden
2. Informeer factieleden wanneer mogelijk
3. Documenteer de reden voor serveradministratie
4. Controleer `/f admin info <faction>` om te beoordelen voor actie

>[!TIP] Als het probleem bij een specifiek lid ligt, overweeg dan om via de admin-facties-GUI het leiderschap over te dragen in plaats van de hele factie te ontbinden.
