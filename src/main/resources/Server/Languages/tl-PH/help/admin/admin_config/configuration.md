---
id: admin_configuration
---
# Sistema ng Configuration

Ang HyperFactions ay gumagamit ng modular na JSON config system na may 11 configuration file.

## Mga Admin Config Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin config` | Buksan ang visual config editor GUI |
| `/f admin reload` | Mag-reload ng lahat ng config file mula sa disk |
| `/f admin sync` | I-synchronize ang faction data sa storage |

## Mga Configuration File

| File | Nilalaman |
|------|----------|
| `factions.json` | Roles, power, claims, combat, relations |
| `server.json` | Teleport, auto-save, messages, GUI, permissions |
| `economy.json` | Treasury, upkeep, transaction settings |
| `backup.json` | Backup rotation at retention settings |
| `chat.json` | Faction at ally chat formatting |
| `debug.json` | Debug logging categories |
| `faction-permissions.json` | Per-role permission defaults |
| `announcements.json` | Event broadcast at territory notifications |
| `gravestones.json` | Gravestone integration settings |
| `worldmap.json` | World map refresh modes |
| `worlds.json` | Per-world behavior overrides |

>[!TIP] Ang config GUI ay nagbibigay ng visual editor na may mga paglalarawan para sa bawat setting. Agad na nase-save ang mga pagbabago pero ang ilan ay nangangailangan ng `/f admin reload` para lubos na magkabisa.

## Lokasyon ng Config

Lahat ng file ay naka-store sa:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Ang mga manual na JSON edit ay nangangailangan ng `/f admin reload` para ma-apply. Ang invalid na JSON ay magdudulot na ma-skip ang file na may babala sa server log.

>[!NOTE] Ang config version ay naka-track sa `server.json`. Awtomatikong nag-migrate ang plugin ng mga lumang config sa startup.
