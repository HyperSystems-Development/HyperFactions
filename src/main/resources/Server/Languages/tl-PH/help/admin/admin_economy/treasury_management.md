---
id: admin_treasury_management
---
# Pamamahala ng Treasury

Mga admin command para sa pamamahala ng mga faction treasury. Nangangailangan ng `hyperfactions.admin.economy` permission.

## Mga Treasury Command

| Command | Paglalarawan |
|---------|-------------|
| `/f admin economy balance <faction>` | Tingnan ang faction treasury balance |
| `/f admin economy set <faction> <amount>` | I-set ang eksaktong balance |
| `/f admin economy add <faction> <amount>` | Magdagdag ng pondo sa treasury |
| `/f admin economy take <faction> <amount>` | Magtanggal ng pondo mula sa treasury |
| `/f admin economy reset <faction>` | I-reset ang treasury sa zero |

## Mga Halimbawa

- `/f admin economy balance Vikings` -- suriin ang balance
- `/f admin economy set Vikings 5000` -- i-set sa 5000
- `/f admin economy add Vikings 1000` -- mag-deposit ng 1000
- `/f admin economy take Vikings 500` -- mag-withdraw ng 500
- `/f admin economy reset Vikings` -- i-zero out ang balance

>[!TIP] Gamitin ang `/f admin info <faction>` para makita ang buong economy overview kasama ang transaction history katabi ng treasury balance.

## Mga Use Case

| Senaryo | Command |
|---------|---------|
| Pamamahagi ng event prize | `economy add <faction> <prize>` |
| Parusa sa paglabag sa patakaran | `economy take <faction> <fine>` |
| Economy reset pagkatapos ng wipe | `economy reset <faction>` |
| Kompensasyon para sa mga bug | `economy add <faction> <amount>` |

>[!WARNING] Ang mga pagbabago sa treasury ay naka-log sa transaction history ng faction. Ang mga admin modification ay naitatala kasama ang pangalan ng admin para sa accountability.

>[!NOTE] Lahat ng economy admin command ay gumagana kahit naka-disable ang economy module sa config. Ang data ay naka-store anuman ang status ng module.
