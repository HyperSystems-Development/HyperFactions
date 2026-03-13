---
id: economy_upkeep
---
# Territory Upkeep

Kailangang magbayad ng patuloy na upkeep ang mga faction para ma-maintain ang kanilang na-claim na teritoryo. Pinipigilan nito ang land hoarding at pinapanatiling dynamic ang map.

## Mga Gastos sa Upkeep

| Setting | Default |
|---------|---------|
| Gastos bawat chunk | 2.0 bawat cycle |
| Pagitan ng bayad | Bawat 24 oras |
| Libreng chunk | 3 (walang gastos) |
| Scaling mode | Flat rate |

>[!NOTE] Ito ay mga default na halaga. Maaaring iba ang na-configure ng server administrator mo.

Ang unang 3 chunk mo ay libre. Lagpas doon, bawat karagdagang na-claim na chunk ay nagkakahalaga ng 2.0 bawat payment cycle.

## Auto-Pay

Naka-enable ang auto-pay bilang default. Awtomatikong ibinabawas ng sistema ang upkeep mula sa treasury mo sa bawat interval. Walang manual na aksyon ang kailangan.

---

## Grace Period

Kung hindi kayang bayaran ng treasury mo ang upkeep, magsisimula ang 48-oras na grace period. May ipapadala na babala 6 oras bago magsimulang mawala ang mga claim.

>[!WARNING] Kung hindi pa rin nababayaran ang upkeep pagkatapos ng grace period, mawawalan ang faction mo ng 1 claim bawat cycle hanggang sa mabayaran ang mga gastos o mawala ang lahat ng extra claim.

## Halimbawa

*Ang faction na may 8 claim ay nagbabayad para sa 5 chunk (8 minus 3 libre). Sa 2.0 bawat chunk, iyon ay 10.0 bawat cycle.*

>[!TIP] Panatilihing may pondo ang treasury mo na mas mataas sa upkeep cost mo. Gamitin ang /f balance para suriin ang mga reserba mo.
