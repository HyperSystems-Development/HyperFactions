---
id: admin_world_settings
---
# Mga Per-World Setting

Ang HyperFactions ay sumusuporta ng per-world configuration para sa claiming, PvP, at protection behavior.

## Mga World Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin world list` | Ilista ang lahat ng world override |
| `/f admin world info <world>` | Ipakita ang mga setting para sa isang mundo |
| `/f admin world set <world> <key> <value>` | Mag-set ng setting |
| `/f admin world reset <world>` | I-reset ang mundo sa mga default |

## Mga Available na Setting

| Setting | Uri | Paglalarawan |
|---------|-----|-------------|
| claiming_enabled | boolean | Payagan ang faction claims sa mundong ito |
| pvp_enabled | boolean | Payagan ang PvP combat sa mundong ito |
| power_loss | boolean | I-apply ang power loss sa pagkamatay |
| build_protection | boolean | Ipatupad ang claim build protection |
| explosion_protection | boolean | Protektahan ang mga claim mula sa mga pagsabog |

## World Whitelist / Blacklist

Kontrolin kung aling mga mundo ang nagpapahintulot ng faction features sa pamamagitan ng `worlds.json` config file:

- **Whitelist mode**: Tanging ang mga naka-listang mundo lang ang pwedeng mag-claim
- **Blacklist mode**: Lahat ng mundo ay pwedeng mag-claim maliban sa mga nakalista

>[!INFO] Ang mga world setting ay naka-store sa `worlds.json` at nag-o-override ng mga global default mula sa `factions.json`.

## Mga Halimbawa

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- ibalik ang lahat ng default

>[!TIP] I-disable ang claiming sa mga creative o lobby world para mapanatiling nakapokus ang faction system sa survival gameplay.

>[!NOTE] Ang mga per-world setting ay mas mataas ang priority kaysa sa global config pero nao-override ng mga zone flag sa loob ng mundong iyon.
