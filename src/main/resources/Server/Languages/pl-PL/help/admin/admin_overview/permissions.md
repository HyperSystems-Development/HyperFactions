---
id: admin_permissions
---
# Uprawnienia administracyjne

Wszystkie funkcje administracyjne są chronione węzłami uprawnień w przestrzeni nazw `hyperfactions.admin`.

## Węzły uprawnień

| Uprawnienie | Opis |
|-----------|-------------|
| `hyperfactions.admin.*` | Nadaje **wszystkie** uprawnienia administracyjne |
| `hyperfactions.admin.use` | Dostęp do panelu `/f admin` |
| `hyperfactions.admin.reload` | Przeładowanie plików konfiguracyjnych |
| `hyperfactions.admin.debug` | Przełączanie kategorii logowania debugowego |
| `hyperfactions.admin.zones` | Tworzenie, edycja i usuwanie stref |
| `hyperfactions.admin.disband` | Wymuszone rozwiązanie dowolnej frakcji |
| `hyperfactions.admin.modify` | Modyfikacja ustawień dowolnej frakcji |
| `hyperfactions.admin.bypass.limits` | Pomijanie limitów zajęć i mocy |
| `hyperfactions.admin.backup` | Tworzenie i przywracanie kopii zapasowych |
| `hyperfactions.admin.power` | Nadpisywanie wartości mocy graczy |
| `hyperfactions.admin.economy` | Zarządzanie skarbcami frakcji |

## Zachowanie awaryjne

Gdy **nie jest zainstalowany żaden plugin uprawnień**, uprawnienia administracyjne przechodzą na status operatora serwera (OP). Kontroluje to `adminRequiresOp` w konfiguracji serwera (domyślnie: `true`).

>[!NOTE] Wieloznacznik `hyperfactions.admin.*` nadaje każde uprawnienie administracyjne. Używaj indywidualnych węzłów dla szczegółowej kontroli nad swoim zespołem.

## Kolejność rozwiązywania uprawnień

1. **VaultUnlocked** (najwyższy priorytet)
2. **HyperPerms** (jeśli dostępny)
3. **LuckPerms** (jeśli dostępny)
4. **Sprawdzenie OP** dla węzłów administracyjnych (awaryjnie)

>[!WARNING] Bez pluginu uprawnień i z wyłączonym `adminRequiresOp`, komendy administracyjne są **otwarte dla wszystkich graczy**. Zawsze używaj pluginu uprawnień na serwerze produkcyjnym.
