---
id: admin_updates
---
# Pagsuri ng Update

Ang HyperFactions ay pwedeng magsuri ng mga bagong bersyon at pamahalaan ang HyperProtect-Mixin dependency.

## Mga Update Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin update` | Magsuri ng mga HyperFactions update |
| `/f admin update mixin` | Magsuri/mag-download ng HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | I-toggle ang auto-download |
| `/f admin version` | Ipakita ang kasalukuyang bersyon at build info |

## Mga Release Channel

| Channel | Paglalarawan |
|---------|-------------|
| **Stable** | Inirerekomenda para sa mga production server |
| **Pre-release** | Maagang access sa mga paparating na feature |

>[!INFO] Ang update checker ay nag-notify lang tungkol sa mga bagong bersyon. **Hindi** ito awtomatikong nag-i-install ng mga update sa HyperFactions mismo.

## HyperProtect-Mixin

Ang HyperProtect-Mixin ang inirerekomendang protection mixin na nag-e-enable ng mga advanced zone flag (explosions, fire spread, keep inventory, atbp.).

- Sinusuri ng `/f admin update mixin` ang pinakabagong bersyon
at dini-download ito kung may mas bagong bersyon na available
- Ang auto-download ay pwedeng i-toggle on o off bawat server

>[!TIP] Pagkatapos mag-download ng bagong mixin version, kailangang mag-restart ng server para magkabisa ang mga pagbabago.

## Proseso ng Rollback

Kung may problema ang isang update:

1. I-stop ang server
2. Palitan ang plugin JAR ng nakaraang bersyon
3. I-start ang server
4. I-verify ang functionality gamit ang `/f admin version`

>[!WARNING] Ang pag-downgrade ay maaaring mangailangan ng config migration reset. Palaging panatilihin ang mga backup bago mag-update.
