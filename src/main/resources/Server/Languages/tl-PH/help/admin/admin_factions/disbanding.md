---
id: admin_disbanding
---
# Force Disbanding

Pwedeng puwersahang i-disband ng mga admin ang kahit anong faction, anuman ang gusto ng leader.

## Command

`/f admin disband <faction>`
Puwersahang i-disband ang pinangalanang faction. May lalabas na confirmation prompt bago isagawa ang aksyon.

**Permission**: `hyperfactions.admin.disband`

>[!WARNING] Ang pag-disband ng faction ay **hindi na pwedeng i-undo**. Lahat ng claim ay mabibigyang-laya, lahat ng miyembro ay tatanggalin, at matitigil ang pag-iral ng faction. Gumawa muna ng backup.

## Mga Konsekwensya

Kapag na-disband ang isang faction:

| Epekto | Paglalarawan |
|--------|-------------|
| **Claims** | Lahat ng teritoryo ay agad na ire-release |
| **Members** | Lahat ng manlalaro ay tatanggalin mula sa roster |
| **Relations** | Lahat ng alyansa at kaaway ay maki-clear |
| **Treasury** | Hahawakan ayon sa economy config settings |
| **Home** | Madi-delete ang faction home |
| **Chat** | Matatanggal ang faction chat history |

## Mga Best Practice

1. Palaging patakbuhin ang `/f admin backup create` bago mag-disband
2. I-notify ang mga faction member kung posible
3. I-document ang dahilan para sa server records
4. Suriin ang `/f admin info <faction>` para mag-review bago kumilos

>[!TIP] Kung ang problema ay sa isang partikular na miyembro, pag-isipang gamitin ang admin factions GUI para ilipat ang leadership sa halip na i-disband ang buong faction.
