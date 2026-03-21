---
id: admin_permissions
---
# Uprawnienia administracyjne

Wszystkie funkcje administracyjne sa chronione wezlami uprawnien w przestrzeni nazw `hyperfactions.admin`.

## Wezly uprawnien

| Uprawnienie | Opis |
|-----------|-------------|
| `hyperfactions.admin.*` | Nadaje **wszystkie** uprawnienia administracyjne |
| `hyperfactions.admin.use` | Dostep do panelu `/f admin` |
| `hyperfactions.admin.reload` | Przeladowanie plikow konfiguracyjnych |
| `hyperfactions.admin.debug` | Przelaczanie kategorii logowania debugowego |
| `hyperfactions.admin.zones` | Tworzenie, edycja i usuwanie stref |
| `hyperfactions.admin.disband` | Wymuszone rozwiazanie dowolnej frakcji |
| `hyperfactions.admin.modify` | Modyfikacja ustawien dowolnej frakcji |
| `hyperfactions.admin.bypass.limits` | Pomijanie limitow zajec i mocy |
| `hyperfactions.admin.backup` | Tworzenie i przywracanie kopii zapasowych |
| `hyperfactions.admin.power` | Nadpisywanie wartosci mocy graczy |
| `hyperfactions.admin.economy` | Zarzadzanie skarbcami frakcji |

## Zachowanie awaryjne

Gdy **nie jest zainstalowany zaden plugin uprawnien**, uprawnienia administracyjne przechodza na status operatora serwera (OP). Kontroluje to `adminRequiresOp` w konfiguracji serwera (domyslnie: `true`).

>[!NOTE] Wieloznacznik `hyperfactions.admin.*` nadaje kazde uprawnienie administracyjne. Uzywaj indywidualnych wezlow dla szczegolowej kontroli nad swoim zespolem.

## Kolejnosc rozwiazywania uprawnien

1. **VaultUnlocked** (najwyzszy priorytet)
2. **HyperPerms** (jesli dostepny)
3. **LuckPerms** (jesli dostepny)
4. **Sprawdzenie OP** dla wezlow administracyjnych (awaryjnie)

>[!WARNING] Bez pluginu uprawnien i z wylaczonym `adminRequiresOp`, komendy administracyjne sa **otwarte dla wszystkich graczy**. Zawsze uzywaj pluginu uprawnien na serwerze produkcyjnym.
