---
id: combat_death
commands: home, sethome, stuck
---
# Pagkamatay at Pagre-recover

Ang pagkamatay ay may totoong mga konsekwensya sa factions. Bawat pagkamatay ay nagpapalugi sa iyo ng personal power, na nagpapahina sa kakayahan ng faction mong hawakan ang teritoryo.

## Pagkawala ng Power

Bawat pagkamatay ay nagkakahalaga ng -1.0 power mula sa iyong personal na kabuuan. Binabawasan nito ang combined power ng faction.

| Pangyayari | Pagbabago ng Power |
|-----------|-------------------|
| Pagkamatay (kahit anong dahilan) | -1.0 |
| Online regen | +0.1 bawat minuto |
| Combat logout | -1.0 (pinatay) |

>[!NOTE] Ito ay mga default na halaga. Maaaring iba ang na-configure ng server administrator mo.

## Mga Halimbawang Senaryo

*5 miyembro na may 10.0 power bawat isa = 50 kabuuan, 20 claim.*
*Isang miyembro ay namatay ng dalawang beses: 8.0 power, faction total 48.*
*Tatlong miyembro ay namatay nang tig-iisa: bumaba ang kabuuan sa 47.*

>[!WARNING] Kung bumaba ang faction power mo sa ibaba ng claim count mo, pwedeng mag-overclaim ng teritoryo mo ang mga kaaway.

## Pagre-recover

Ang power ay nagre-regenerate sa 0.1 bawat minuto habang online. Ang pagre-recover ng 1.0 na nawala ay tumatagal ng mga 10 minuto. Nagsasama-sama ang mga sunud-sunod na pagkamatay, kaya iwasan ang paulit-ulit na away.

---

## Lahat ng Uri ng Pagkamatay

Ang power loss ay umaaplay sa lahat ng pagkamatay: PvP, napatay ng mob, pagbagsak, pagkalunod, at kahit anong ibang dahilan. Walang ligtas na paraan para mamatay.

>[!TIP] Mag-set ng faction home gamit ang /f sethome para mabilis na magsama-sama ulit ang mga miyembro pagkatapos mamatay.
