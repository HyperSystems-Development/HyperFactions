---
id: admin_world_settings
---
# Per-wereld Instellingen

HyperFactions ondersteunt per-wereld configuratie voor claimen, PvP en beschermingsgedrag.

## Wereldcommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin world list` | Toon alle wereldoverschrijvingen |
| `/f admin world info <world>` | Toon instellingen voor een wereld |
| `/f admin world set <world> <key> <value>` | Stel een instelling in |
| `/f admin world reset <world>` | Reset wereld naar standaardwaarden |

## Beschikbare Instellingen

| Instelling | Type | Beschrijving |
|------------|------|-------------|
| claiming_enabled | boolean | Sta factieclaims toe in deze wereld |
| pvp_enabled | boolean | Sta PvP-gevecht toe in deze wereld |
| power_loss | boolean | Pas powerverlies toe bij overlijden |
| build_protection | boolean | Dwing claimbouwbescherming af |
| explosion_protection | boolean | Bescherm claims tegen explosies |

## Wereld Whitelist / Blacklist

Bepaal welke werelden factiefuncties toestaan via het `worlds.json` configuratiebestand:

- **Whitelist-modus**: Alleen vermelde werelden staan claimen toe
- **Blacklist-modus**: Alle werelden staan claimen toe behalve de vermelde

>[!INFO] Wereldinstellingen worden opgeslagen in `worlds.json` en overschrijven de globale standaardwaarden uit `factions.json`.

## Voorbeelden

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- herstel alle standaardwaarden

>[!TIP] Schakel claimen uit in creative- of lobbywerelden om het factiesysteem gericht te houden op survival-gameplay.

>[!NOTE] Per-wereld instellingen hebben prioriteit boven globale configuratie, maar worden overschreven door zonevlaggen binnen die wereld.
