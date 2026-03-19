---
id: power_losing
commands: overclaim
---
# Pagkawala ng Teritoryo

Kapag ang kabuuang power ng faction ay bumaba sa ibaba ng halaga ng mga claim nito, nagiging raidable ito. Pwedeng mag-overclaim ng mga chunk ang mga kaaway nang direkta mula sa ilalim mo.

---

## Paano Gumagana ang Overclaiming

`/f overclaim`

Ang isang Officer o Leader mula sa isang enemy faction ay tumatayo sa iyong na-claim na chunk at pinapatakbo ang command na ito. Kung ang faction mo ay nasa power deficit, ililipat ang chunk sa kanilang faction.

## Ang Pagkalkula

Bawat claim ay nagkakahalaga ng 2.0 power para ma-maintain. Kung ang kabuuang power mo ay bumaba sa ibaba ng threshold na iyon, ang mga deficit chunk ay vulnerable.

>[!NOTE] Ito ay mga default na halaga. Maaaring iba ang na-configure ng server administrator mo.

>[!WARNING] Ang overclaiming ay permanente. Kapag nakuha na ng kaaway ang isang chunk, kailangan mong i-reclaim ito (o i-overclaim pabalik kung humina sila).

---

## Halimbawang Senaryo

| Salik | Halaga |
|-------|--------|
| Mga Miyembro | 5 manlalaro |
| Power bawat miyembro | 10 bawat isa (simula) |
| Kabuuang power | 50 |
| Mga Claim | 30 chunk |
| Power na kailangan (30 x 2.0) | 60 |
| Deficit | Kulang ng 10 power |

Sa halimbawang ito, raidable na ang faction sa simula pa lang. Pwedeng mag-overclaim ang mga kaaway ng hanggang 5 chunk (10 deficit / 2.0 bawat claim) bago maabot ng faction ang equilibrium.

---

## Paano Mapigilan ang Overclaiming

- Huwag mag-over-expand -- palaging panatilihing mas mataas ang kabuuang power sa halaga ng claim mo na may buffer
- Manatiling aktibo -- ang power ay nagre-regenerate lang habang online (+0.1/min)
- Iwasan ang mga hindi kinakailangang pagkamatay -- bawat pagkamatay ay nagkakahalaga ng 1.0 power
- Mag-recruit ng mas maraming miyembro -- mas maraming manlalaro ay mas maraming kabuuang power
- I-unclaim ang mga hindi ginagamit na chunk -- i-free up ang power gamit ang /f unclaim

>[!TIP] Regular na suriin ang power status mo gamit ang /f power. Kung malapit na ang kabuuang power mo sa halaga ng claim, pag-isipang i-unclaim ang mga hindi gaanong mahalagang chunk bago mag-giyera.
