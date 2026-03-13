---
id: admin_getting_started
---
# Pagsisimula bilang Admin

Maligayang pagdating sa administrasyon ng HyperFactions. Sinasaklaw ng gabay na ito ang mga unang hakbang mo pagkatapos i-install ang plugin.

## Pagbukas ng Admin Dashboard

`/f admin`
Binubuksan ang admin dashboard GUI na may access sa lahat ng management tool, zone editor, at server settings.

>[!INFO] Kailangan mo ng **hyperfactions.admin.use** permission o OP status para ma-access ang mga admin command.

## Mga Kinakailangan

- **May permission plugin**: Ibigay ang `hyperfactions.admin.use`
- **Walang permission plugin**: Kailangang server operator ang manlalaro (`adminRequiresOp=true` bilang default)

## Mga Unang Hakbang Pagkatapos Mag-install

1. Patakbuhin ang `/f admin` para i-verify ang access mo
2. Buksan ang **Config** para i-review ang default na faction settings
3. Gumawa ng **SafeZone** sa spawn gamit ang `/f admin safezone Spawn`
4. Opsyonal na gumawa ng mga **WarZone** para sa mga PvP arena
5. I-review ang mga **Backup** setting para masiguro ang kaligtasan ng data

## Mga Kakayahan ng Admin

| Lugar | Ano ang Pwede Mong Gawin |
|-------|-------------------------|
| Factions | Mag-inspect, mag-modify, o mag-force-disband ng kahit anong faction |
| Zones | Gumawa ng mga SafeZone at WarZone na may custom flags |
| Power | I-override ang player/faction power values |
| Economy | Pamahalaan ang mga faction treasury at upkeep |
| Config | Mag-edit ng settings nang live sa GUI o mag-reload mula sa disk |
| Backups | Gumawa, mag-restore, at mamahala ng mga data backup |
| Imports | Mag-migrate ng data mula sa ibang faction plugin |

>[!TIP] Gamitin ang `/f admin --text` para makakuha ng chat-based output sa halip na GUI, kapaki-pakinabang para sa console o automation.
