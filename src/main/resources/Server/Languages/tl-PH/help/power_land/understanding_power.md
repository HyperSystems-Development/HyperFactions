---
id: power_understanding
commands: power
---
# Pag-unawa sa Power

Ang power ang pangunahing resource na nagdedetermina kung gaano karaming teritoryo ang kayang hawakan ng faction mo. Bawat manlalaro ay may personal power na nag-aambag sa kabuuang power ng faction.

---

## Mga Default na Halaga ng Power

| Setting | Halaga |
|---------|--------|
| Maximum power bawat manlalaro | 20 |
| Starting power | 10 |
| Parusa sa pagkamatay | -1.0 bawat pagkamatay |
| Reward sa pag-patay | 0.0 |
| Regen rate | +0.1 bawat minuto (habang online) |
| Power cost bawat claim | 2.0 |
| Logout habang naka-tag | -1.0 karagdagan |

>[!NOTE] Ito ay mga default na halaga. Maaaring iba ang na-configure ng server administrator mo.

## Paano Ito Gumagana

Ang kabuuang power ng faction mo ay ang suma ng personal power ng bawat miyembro. Ang kinakailangang power ay ang bilang ng mga claim na pinarami ng 2.0. Hangga't nananatiling mas mataas ang kabuuang power kaysa sa kinakailangang power, ligtas ang teritoryo mo.

>[!INFO] Ang power ay pasibong nagre-regenerate sa 0.1 bawat minuto habang online ka. Sa rate na iyon, ang pagre-recover ng 1.0 power ay tumatagal ng mga 10 minuto.

---

## Pagsuri ng Power Mo

`/f power`

Ipinapakita ang personal power mo, ang kabuuang power ng faction mo, at kung magkano ang kailangan para ma-maintain ang kasalukuyang mga claim.

## Ang Danger Zone

Kung bumaba ang kabuuang power sa ibaba ng kinakailangang halaga para sa mga claim mo, nagiging vulnerable ang faction mo. Pwedeng mag-overclaim ng mga chunk ang mga kaaway.

>[!WARNING] Ang sunud-sunod na pagkamatay sa maikling panahon ay pwedeng mabilis na bumigat. Kung mayroon kang 5 miyembro na may 10 power bawat isa (50 kabuuan) at 20 claim (40 kailangan), 5 pagkamatay lang sa team mo ay bumababa sa 45 -- ligtas pa. Pero 11 pagkamatay ay naglalagay sa iyo sa 39, mas mababa sa 40 threshold.

>[!TIP] Panatilihin ang power buffer. Huwag i-claim ang lahat ng chunk na kaya mong bayaran -- mag-iwan ng puwang para sa ilang pagkamatay nang hindi nagiging raidable.
