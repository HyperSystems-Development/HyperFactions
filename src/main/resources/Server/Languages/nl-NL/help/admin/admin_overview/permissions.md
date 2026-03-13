---
id: admin_permissions
---
# Admin Permissies

Alle adminfuncties worden afgeschermd door permissienodes in de `hyperfactions.admin` namespace.

## Permissienodes

| Permissie | Beschrijving |
|-----------|-------------|
| `hyperfactions.admin.*` | Verleent **alle** adminpermissies |
| `hyperfactions.admin.use` | Toegang tot het `/f admin` dashboard |
| `hyperfactions.admin.reload` | Herlaad configuratiebestanden |
| `hyperfactions.admin.debug` | Schakel debug-logcategorieën in/uit |
| `hyperfactions.admin.zones` | Maak zones aan, bewerk en verwijder ze |
| `hyperfactions.admin.disband` | Ontbind elke factie geforceerd |
| `hyperfactions.admin.modify` | Wijzig de instellingen van elke factie |
| `hyperfactions.admin.bypass.limits` | Omzeil claim- en powerlimieten |
| `hyperfactions.admin.backup` | Maak backups en herstel ze |
| `hyperfactions.admin.power` | Overschrijf speler-powerwaarden |
| `hyperfactions.admin.economy` | Beheer factieschatkisten |

## Terugvalgedrag

Wanneer er **geen permissieplugin** is geïnstalleerd, vallen adminpermissies terug op serveroperator (OP) status. Dit wordt bepaald door `adminRequiresOp` in de serverconfiguratie (standaard: `true`).

>[!NOTE] De `hyperfactions.admin.*` wildcard verleent elke adminpermissie. Gebruik individuele nodes voor gedetailleerde controle over je staffteam.

## Volgorde van Permissieresolutie

1. **VaultUnlocked** provider (indien beschikbaar)
2. **HyperPerms** provider (indien beschikbaar)
3. **LuckPerms** provider (indien beschikbaar)
4. **OP-controle** voor admin-nodes (terugval)

>[!WARNING] Zonder een permissieplugin en met `adminRequiresOp` uitgeschakeld, zijn admincommando's **open voor alle spelers**. Gebruik altijd een permissieplugin in productie.
