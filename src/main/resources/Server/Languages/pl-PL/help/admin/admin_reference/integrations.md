---
id: admin_integrations
---
# Integracje pluginów

HyperFactions integruje się z kilkoma zewnętrznymi pluginami poprzez miękkie zależności. Wszystkie integracje są opcjonalne i działają poprawnie, gdy plugin jest niedostępny.

## Sprawdzanie statusu integracji

`/f admin version`
Pokazuje aktualną wersję i wykryte integracje.

`/f admin integration`
Otwiera panel zarządzania integracjami ze szczegółowym statusem każdego wykrytego pluginu.

## Tabela integracji

| Plugin | Typ | Opis |
|--------|------|-------------|
| **HyperPerms** | Uprawnienia | Pełny system uprawnień z grupami, dziedziczeniem i kontekstem |
| **LuckPerms** | Uprawnienia | Alternatywny dostawca uprawnień |
| **VaultUnlocked** | Uprawnienia/Ekonomia | Most uprawnień i ekonomii |
| **HyperProtect-Mixin** | Ochrona | Włącza zaawansowane flagi stref (eksplozje, ogień, zachowanie ekwipunku) |
| **OrbisGuard-Mixins** | Ochrona | Alternatywny mixin do egzekwowania flag stref |
| **PlaceholderAPI** | Placeholdery | 49 placeholderów frakcji dla innych pluginów |
| **WiFlow PlaceholderAPI** | Placeholdery | Alternatywny dostawca placeholderów |
| **GravestonePlugin** | Śmierć | Kontrola dostępu do nagrobków w strefach |
| **HyperEssentials** | Funkcje | Flagi stref dla domów, warpów i kitów |
| **KyuubiSoft Core** | Framework | Integracja z biblioteką bazową |
| **Sentry** | Monitoring | Śledzenie błędów i diagnostyka |

## Priorytet dostawcy uprawnień

1. **VaultUnlocked** (najwyższy priorytet)
2. **HyperPerms**
3. **LuckPerms**
4. **Awaryjnie OP** (jeśli nie znaleziono dostawcy)

>[!INFO] Integracje są wykrywane raz przy uruchomieniu za pomocą refleksji. Wyniki są cachowane na sesję. Restart serwera jest wymagany po dodaniu lub usunięciu zintegrowanego pluginu.

>[!TIP] Użyj `/f admin debug toggle integration`, aby włączyć szczegółowe logowanie integracji do rozwiązywania problemów.

>[!NOTE] HyperProtect-Mixin to **zalecany** mixin ochrony. Bez niego 15 flag stref nie będzie miało efektu.
