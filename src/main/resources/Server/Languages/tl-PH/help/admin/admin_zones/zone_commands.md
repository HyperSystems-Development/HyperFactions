---
id: admin_zone_commands
---
# Reference ng Zone Command

Kumpletong reference para sa lahat ng zone management command. Lahat ay nangangailangan ng `hyperfactions.admin.zones` permission.

## Mabilis na Paggawa

| Command | Paglalarawan |
|---------|-------------|
| `/f admin safezone <name>` | Gumawa ng SafeZone sa kasalukuyang chunk |
| `/f admin warzone <name>` | Gumawa ng WarZone sa kasalukuyang chunk |
| `/f admin removezone <name>` | I-delete ang zone at bitawan ang mga chunk |

## Pamamahala ng Zone

| Command | Paglalarawan |
|---------|-------------|
| `/f admin zone create <name> <type>` | Gumawa ng zone (safezone/warzone) |
| `/f admin zone delete <name>` | I-delete ang zone |
| `/f admin zone claim <zone>` | Idagdag ang kasalukuyang chunk sa zone |
| `/f admin zone unclaim <zone>` | Tanggalin ang kasalukuyang chunk mula sa zone |
| `/f admin zone radius <zone> <r>` | Mag-claim ng parisukat na radius ng mga chunk |
| `/f admin zone list` | Ilista ang lahat ng zone na may bilang ng chunk |
| `/f admin zone notify <zone> <true/false>` | I-toggle ang entry/leave messages |
| `/f admin zone title <zone> upper/lower <text>` | I-set ang zone title text |
| `/f admin zone properties <zone>` | Buksan ang zone properties GUI |

## Pamamahala ng Flag

| Command | Paglalarawan |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | I-set ang isang partikular na flag |

>[!TIP] Gamitin ang zone **properties GUI** para sa visual editor na may mga toggle para sa bawat flag, naka-organisa ayon sa kategorya.

## Mga Halimbawa

- `/f admin safezone Spawn` -- gumawa ng spawn protection
- `/f admin zone radius Spawn 3` -- palawakin sa 7x7 chunk
- `/f admin zoneflag Spawn door_use true` -- payagan ang mga pinto
- `/f admin zone notify Spawn true` -- ipakita ang entry messages
