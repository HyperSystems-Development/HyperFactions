---
id: admin_quickref_commands
---
# Admin Commandoreferentie

Volledige lijst van alle `/f admin` subcommando's met syntax en vereiste permissies.

## Dashboard en Algemeen

| Commando | Permissie |
|----------|----------|
| `/f admin` | admin.use |
| `/f admin version` | admin.use |
| `/f admin reload` | admin.reload |
| `/f admin sync` | admin.use |
| `/f admin sentry` | admin.use |

## Factiebeheer

| Commando | Permissie |
|----------|----------|
| `/f admin factions` | admin.use |
| `/f admin info <faction>` | admin.use |
| `/f admin who <player>` | admin.use |
| `/f admin disband <faction>` | admin.disband |
| `/f admin log` | admin.use |

## Zonebeheer

| Commando | Permissie |
|----------|----------|
| `/f admin safezone <name>` | admin.zones |
| `/f admin warzone <name>` | admin.zones |
| `/f admin removezone <name>` | admin.zones |
| `/f admin zone create/delete/claim/unclaim` | admin.zones |
| `/f admin zone radius <zone> <r>` | admin.zones |
| `/f admin zone list` | admin.zones |
| `/f admin zone notify <zone> <bool>` | admin.zones |
| `/f admin zone title <zone> upper/lower <text>` | admin.zones |
| `/f admin zone properties <zone>` | admin.zones |
| `/f admin zoneflag <zone> <flag> <bool>` | admin.zones |

## Power en Economie

| Commando | Permissie |
|----------|----------|
| `/f admin power set/add/remove/reset <player> [amt]` | admin.power |
| `/f admin power setmax/noloss/nodecay <player> [amt]` | admin.power |
| `/f admin power info <player>` | admin.power |
| `/f admin economy balance/set/add/take/reset <faction> [amt]` | admin.economy |

## Onderhoud

| Commando | Permissie |
|----------|----------|
| `/f admin backup create/list/restore/delete` | admin.backup |
| `/f admin import <source> [flags]` | admin.use |
| `/f admin update` | admin.use |
| `/f admin update mixin` | admin.use |
| `/f admin config` | admin.use |
| `/f admin world list/info/set/reset` | admin.use |
| `/f admin debug toggle <category>` | admin.debug |
| `/f admin integration` | admin.use |

>[!NOTE] Alle permissienodes hebben het voorvoegsel `hyperfactions.` (bijv. `hyperfactions.admin.use`).
