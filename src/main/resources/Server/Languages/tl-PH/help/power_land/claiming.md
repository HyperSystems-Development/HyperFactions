---
id: power_claiming
commands: claim, unclaim
---
# Pag-claim ng Teritoryo

Ang pag-claim ng chunk ay pinoprotektahan ito sa ilalim ng kontrol ng faction mo. Tanging mga faction member lamang ang pwedeng mag-build, mag-break, o mag-access ng mga container sa loob ng na-claim na teritoryo.

---

## Paano Mag-claim

`/f claim`

Tumayo sa chunk na gusto mong i-claim at patakbuhin ang command na ito. Agad na mapoprotektahan ang chunk. Kailangan ng Officer rank o mas mataas pa.

## Paano Mag-unclaim

`/f unclaim`

Binibitawan ang chunk kung saan ka nakatayo pabalik sa wilderness. Kailangan din ng Officer+.

---

## Mga Patakaran sa Pag-claim

| Patakaran | Default |
|-----------|---------|
| Power cost bawat claim | 2.0 power |
| Maximum claims | 100 bawat faction |
| Katabing chunk lang | Hindi (pwede kang mag-claim kahit saan) |

>[!NOTE] Ito ay mga default na halaga. Maaaring iba ang na-configure ng server administrator mo.

>[!INFO] Bawat claim ay nagkakahalaga ng 2.0 power para ma-maintain. Ang faction na may 50 kabuuang power ay pwedeng humawak ng hanggang 25 claim nang ligtas.

---

## Ano ang Proteksyon na Ibinibigay

Sa loob ng na-claim na teritoryo, ang sumusunod ay ipinapatupad bilang default:

- Hindi pwedeng mag-break, mag-place, o mag-interact sa mga block ang mga outsider
- Ang mga ally ay pwedeng gumamit ng mga pinto, upuan, at transport pero hindi pwedeng mag-break o mag-place ng mga block
- Ang mga Member at Officer ay may buong access para mag-build, mag-break, at gumamit ng lahat
- Ang container access (mga chest, crate) ay limitado sa mga miyembro lamang

>[!TIP] Pwede ka ring mag-claim nang direkta mula sa territory map. Buksan ang /f map at i-click ang mga unclaimed chunk para i-claim sila.

>[!WARNING] Huwag mag-over-expand. Kung mawalan ng power ang faction mo dahil sa mga pagkamatay, ang mga claim na lagpas sa power budget mo ay magiging vulnerable sa overclaiming.
