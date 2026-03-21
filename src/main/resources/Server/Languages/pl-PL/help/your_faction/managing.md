---
id: faction_managing
commands: invite, kick, promote, demote, transfer
---
# Zarzadzanie czlonkami

Oficerowie i Liderzy wspolnie odpowiadaja za zarzadzanie skladem frakcji. Oto kluczowe komendy i kto moze ich uzywac.

---

## Komendy

| Komenda | Opis | Wymagana rola |
|---------|-------------|---------------|
| `/f invite <player>` | Wysyla zaproszenie do dolaczenia (wygasa po 5 min) | Oficer+ |
| `/f kick <player>` | Usuwa czlonka z frakcji | Oficer+ (patrz uwaga) |
| `/f promote <player>` | Awansuje Czlonka na Oficera | Tylko Lider |
| `/f demote <player>` | Degraduje Oficera na Czlonka | Tylko Lider |
| `/f transfer <player>` | Przekazuje wlasnosc frakcji | Tylko Lider |

>[!NOTE] Oficerowie moga wyrzucac tylko Czlonkow. Aby usunac innego Oficera, Lider musi go najpierw zdegradowac lub wyrzucic bezposrednio.

---

## Zaproszenia

- Zaproszenia wygasaja po 5 minutach, jesli nie zostana zaakceptowane
- Zaproszony gracz widzi je w zakladce Zaproszenia po otwarciu /f
- Nie ma limitu na liczbe wyslanych zaproszen jednoczesnie
- Twoja frakcja moze miec lacznie do 50 czlonkow

## Awanse i degradacje

- Tylko Lider moze awansowac lub degradowac
- /f promote podnosi Czlonka do rangi Oficera
- /f demote obniza Oficera z powrotem do Czlonka

## Przekazywanie przywodztwa

>[!WARNING] Przekazanie przywodztwa jest nieodwracalne. Zostaniesz zdegradowany do Oficera, a wybrany gracz stanie sie nowym Liderem. Upewnij sie, ze mu w pelni ufasz.

`/f transfer <player>`

Wybrany gracz musi byc aktualnym czlonkiem twojej frakcji.
