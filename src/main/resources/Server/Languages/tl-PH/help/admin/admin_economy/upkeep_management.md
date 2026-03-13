---
id: admin_upkeep_management
---
# Pamamahala ng Upkeep

Ang faction upkeep ay nagsisingil sa mga faction nang pana-panahon batay sa kanilang teritoryo at bilang ng miyembro.

## Mga Admin Control

Ang mga upkeep setting ay pinamamahalaan sa pamamagitan ng economy config file o ng admin config GUI.

`/f admin config`
Buksan ang config editor at mag-navigate sa economy settings para ayusin ang mga upkeep value.

## Mga Default na Upkeep Setting

| Setting | Default | Paglalarawan |
|---------|---------|-------------|
| Upkeep enabled | false | Master toggle para sa sistema |
| Upkeep interval | 24h | Gaano kadalas sisingilin ang upkeep |
| Per-claim cost | 5.0 | Gastos bawat na-claim na chunk bawat cycle |
| Per-member cost | 0.0 | Gastos bawat miyembro bawat cycle |
| Grace period | 72h | Ang mga bagong faction ay exempt |
| Disband on bankrupt | false | Auto-disband kung hindi makabayad |

## Pag-monitor ng Upkeep

Gamitin ang `/f admin info <faction>` para makita ang:
- Kasalukuyang treasury balance
- Tinatantiyang upkeep cost bawat cycle
- Oras bago ang susunod na upkeep charge
- Kung kaya bang bayaran ng faction ang upkeep

>[!TIP] I-review ang economy statistics sa lahat ng faction mula sa admin dashboard para matukoy ang mga faction na malapit nang ma-bankrupt bago mag-trigger ang upkeep.

>[!INFO] Ang upkeep configuration ay naka-store sa `economy.json`. Ang mga pagbabagong ginawa sa config GUI ay magkakabisa pagkatapos mag-reload gamit ang `/f admin reload`.

## Formula ng Upkeep

**Kabuuang upkeep** = (na-claim na chunk x per-claim cost) + (bilang ng miyembro x per-member cost)

>[!WARNING] Ang pag-enable ng upkeep sa isang server na may existing faction ay pwedeng magdulot ng mga hindi inaasahang pagkabangkarote. Pag-isipang mag-set ng grace period o mag-anunsyo ng pagbabago nang maaga.
