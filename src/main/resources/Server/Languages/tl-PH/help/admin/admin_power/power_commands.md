---
id: admin_power_commands
---
# Mga Power Admin Command

I-override ang player at faction power values. Lahat ng command ay nangangailangan ng `hyperfactions.admin.power` permission.

## Mga Player Power Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin power set <player> <amount>` | I-set ang eksaktong power value |
| `/f admin power add <player> <amount>` | Magdagdag ng power sa manlalaro |
| `/f admin power remove <player> <amount>` | Magtanggal ng power mula sa manlalaro |
| `/f admin power reset <player>` | I-reset sa default na starting power |
| `/f admin power info <player>` | Tingnan ang detalyadong power breakdown |

## Paano Naaapektuhan ng Power ang mga Faction

Ang kabuuang power ng faction ay ang suma ng individual power ng lahat ng miyembro nito. Ang mga territory claim ay nangangailangan ng sapat na kabuuang power para ma-maintain.

| Senaryo | Epekto |
|---------|--------|
| Power na-set na mas mataas | Ang faction ay pwedeng mag-claim ng mas maraming teritoryo |
| Power na-set na mas mababa | Ang faction ay pwedeng maging vulnerable sa overclaim |
| Power na-reset | Binalik ang manlalaro sa default na starting value |

>[!WARNING] Ang pagbaba ng power ng isang manlalaro ay pwedeng maging sanhi ng pagkawala ng teritoryo ng kanilang faction kung bumaba ang kabuuang power sa ibaba ng bilang ng mga na-claim na chunk.

## Mga Halimbawa

- `/f admin power set Steve 50` -- i-set sa eksaktong 50
- `/f admin power add Steve 10` -- dagdagan ng 10
- `/f admin power remove Steve 5` -- bawasan ng 5
- `/f admin power reset Steve` -- ibalik sa default
- `/f admin power info Steve` -- ipakita ang buong breakdown

>[!TIP] Gamitin ang `/f admin power info <player>` para makita ang kasalukuyang power, max power, at anumang aktibong override bago gumawa ng mga pagbabago.
