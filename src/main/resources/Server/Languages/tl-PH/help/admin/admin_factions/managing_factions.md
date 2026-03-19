---
id: admin_managing_factions
---
# Pamamahala ng mga Faction

Ang mga admin ay pwedeng mag-inspect at mag-modify ng kahit anong faction sa server sa pamamagitan ng dashboard o mga command.

## Pag-browse ng mga Faction

`/f admin factions`
Binubuksan ang admin faction browser. Tingnan ang lahat ng faction na may bilang ng miyembro, power level, at teritoryo.

`/f admin info <faction>`
Binubuksan ang admin info panel para sa isang partikular na faction na may buong detalye at management options.

## Pag-modify ng Faction Settings

Gamit ang `hyperfactions.admin.modify` permission, pwede mong:

- **I-rename** ang isang faction para malutas ang mga conflict
- **I-set ang kulay** para ayusin ang mga display issue
- **I-toggle ang open/close** para i-override ang join policy
- **I-edit ang description** para sa mga moderation purpose

>[!TIP] Gamitin ang `/f admin who <player>` para alamin kung saang faction kabilang ang isang partikular na manlalaro at tingnan ang mga detalye nila.

## Pagtingin ng mga Miyembro at Relasyon

Ipinapakita ng admin info panel ang:

| Seksyon | Mga Detalye |
|---------|-------------|
| **Members** | Buong roster na may mga role at huling nakita |
| **Relations** | Lahat ng ally, enemy, at neutral standing |
| **Territory** | Mga na-claim na chunk at power balance |
| **Economy** | Treasury balance at transaction log |

>[!NOTE] Ang mga admin inspection command ay hindi nag-notify sa faction na tinitingnan. Ang mga modification lang ang nagti-trigger ng mga alerto.
