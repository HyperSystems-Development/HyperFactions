---
id: admin_integrations
---
# Plugin Integraties

HyperFactions integreert met diverse externe plugins via zachte afhankelijkheden. Alle integraties zijn optioneel en vallen gracelijk terug als ze niet beschikbaar zijn.

## Integratiestatus Controleren

`/f admin version`
Toont de huidige versie en gedetecteerde integraties.

`/f admin integration`
Opent het integratiebeheervenster met gedetailleerde status voor elke gedetecteerde plugin.

## Integratietabel

| Plugin | Type | Beschrijving |
|--------|------|-------------|
| **HyperPerms** | Permissies | Volledig permissiesysteem met groepen, overerving en context |
| **LuckPerms** | Permissies | Alternatieve permissieprovider |
| **VaultUnlocked** | Permissies/Economie | Permissie- en economiebrug |
| **HyperProtect-Mixin** | Bescherming | Schakelt geavanceerde zonevlaggen in (explosies, brand, inventaris behouden) |
| **OrbisGuard-Mixins** | Bescherming | Alternatieve mixin voor zonevlaghandhaving |
| **PlaceholderAPI** | Placeholders | 49 factie-placeholders voor andere plugins |
| **WiFlow PlaceholderAPI** | Placeholders | Alternatieve placeholder-provider |
| **GravestonePlugin** | Dood | Grafsteentoegangscontrole in zones |
| **HyperEssentials** | Functies | Zonevlaggen voor homes, warps en kits |
| **KyuubiSoft Core** | Framework | Core-bibliotheekintegratie |
| **Sentry** | Monitoring | Foutopsporing en diagnostiek |

## Prioriteit Permissieprovider

1. **VaultUnlocked** (hoogste prioriteit)
2. **HyperPerms**
3. **LuckPerms**
4. **OP-terugval** (als geen provider gevonden)

>[!INFO] Integraties worden eenmalig bij het opstarten gedetecteerd via reflectie. Resultaten worden gecached voor de sessie. Een serverherstart is vereist na het toevoegen of verwijderen van een geïntegreerde plugin.

>[!TIP] Gebruik `/f admin debug toggle integration` om gedetailleerde integratielogging in te schakelen voor probleemoplossing.

>[!NOTE] HyperProtect-Mixin is de **aanbevolen** beschermingsmixin. Zonder deze hebben 15 zonevlaggen geen effect.
