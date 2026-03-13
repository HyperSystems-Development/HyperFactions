---
id: admin_power_overrides
---
# Mga Power Override

Mga espesyal na power command na nagbabago kung paano gumagana ang power para sa mga partikular na manlalaro o faction.

## Mga Override Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | I-set ang custom max power cap |
| `/f admin power noloss <player>` | I-toggle ang death power penalty immunity |
| `/f admin power nodecay <player>` | I-toggle ang offline power decay immunity |
| `/f admin power info <player>` | Tingnan ang lahat ng override at power details |

## Custom Max Power

`/f admin power setmax <player> <amount>`
Nagse-set ng personal na maximum power cap para sa manlalaro, na nag-o-override ng server default.

>[!INFO] Ang pagse-set ng custom max ay **hindi** nagbabago ng kasalukuyang power. Binabago lang nito ang ceiling. Kailangan pa ring kumita ng power ang manlalaro hanggang sa bagong limit.

## No-Loss Mode

`/f admin power noloss <player>`
Tino-toggle ang death power loss immunity. Kapag naka-enable, ang manlalaro ay **hindi** mawawalan ng power sa pagkamatay.

Kapaki-pakinabang para sa:
- Mga panahon ng proteksyon ng bagong manlalaro
- Mga kalahok sa event
- Mga staff member

## No-Decay Mode

`/f admin power nodecay <player>`
Tino-toggle ang offline power decay immunity. Kapag naka-enable, ang power ng manlalaro ay **hindi** bababa habang offline.

Kapaki-pakinabang para sa:
- Mga manlalarong matagal na hindi makakapaglaro
- Mga VIP member
- Seasonal protection

## Power Info

`/f admin power info <player>`
Nagpapakita ng kumpletong breakdown:

- Kasalukuyang power at max power
- Mga aktibong override (noloss, nodecay, custom max)
- Huling oras ng pagkamatay at power na nawala
- Porsyento ng faction contribution

>[!TIP] Lahat ng power override ay nananatili kahit mag-restart ang server at naka-store sa data file ng manlalaro.
