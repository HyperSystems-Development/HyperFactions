---
id: admin_integrations
---
# Integracje pluginow

HyperFactions integruje sie z kilkoma zewnetrznymi pluginami poprzez miekkie zaleznosci. Wszystkie integracje sa opcjonalne i dzialaja poprawnie, gdy plugin jest niedostepny.

## Sprawdzanie statusu integracji

`/f admin version`
Pokazuje aktualna wersje i wykryte integracje.

`/f admin integration`
Otwiera panel zarzadzania integracjami ze szczegolowym statusem kazdego wykrytego pluginu.

## Tabela integracji

| Plugin | Typ | Opis |
|--------|------|-------------|
| **HyperPerms** | Uprawnienia | Pelny system uprawnien z grupami, dziedziczeniem i kontekstem |
| **LuckPerms** | Uprawnienia | Alternatywny dostawca uprawnien |
| **VaultUnlocked** | Uprawnienia/Ekonomia | Most uprawnien i ekonomii |
| **HyperProtect-Mixin** | Ochrona | Wlacza zaawansowane flagi stref (eksplozje, ogien, zachowanie ekwipunku) |
| **OrbisGuard-Mixins** | Ochrona | Alternatywny mixin do egzekwowania flag stref |
| **PlaceholderAPI** | Placeholdery | 49 placeholderow frakcji dla innych pluginow |
| **WiFlow PlaceholderAPI** | Placeholdery | Alternatywny dostawca placeholderow |
| **GravestonePlugin** | Smierc | Kontrola dostepu do nagrobkow w strefach |
| **HyperEssentials** | Funkcje | Flagi stref dla domow, warpow i kitow |
| **KyuubiSoft Core** | Framework | Integracja z biblioteka bazowa |
| **Sentry** | Monitoring | Sledzenie bledow i diagnostyka |

## Priorytet dostawcy uprawnien

1. **VaultUnlocked** (najwyzszy priorytet)
2. **HyperPerms**
3. **LuckPerms**
4. **Awaryjnie OP** (jesli nie znaleziono dostawcy)

>[!INFO] Integracje sa wykrywane raz przy uruchomieniu za pomoca refleksji. Wyniki sa cachowane na sesje. Restart serwera jest wymagany po dodaniu lub usunieciu zintegrowanego pluginu.

>[!TIP] Uzyj `/f admin debug toggle integration`, aby wlaczyc szczegolowe logowanie integracji do rozwiazywania problemow.

>[!NOTE] HyperProtect-Mixin to **zalecany** mixin ochrony. Bez niego 15 flag stref nie bedzie mialo efektu.
