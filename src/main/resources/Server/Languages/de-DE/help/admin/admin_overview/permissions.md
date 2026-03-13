---
id: admin_permissions
---
# Admin-Berechtigungen

Alle Admin-Funktionen sind hinter Berechtigungsknoten im `hyperfactions.admin`-Namensraum gesperrt.

## Berechtigungsknoten

| Berechtigung | Beschreibung |
|-----------|-------------|
| `hyperfactions.admin.*` | Gewaehrt **alle** Admin-Berechtigungen |
| `hyperfactions.admin.use` | Zugang zum `/f admin` Dashboard |
| `hyperfactions.admin.reload` | Konfigurationsdateien neu laden |
| `hyperfactions.admin.debug` | Debug-Protokollierungskategorien umschalten |
| `hyperfactions.admin.zones` | Zonen erstellen, bearbeiten und loeschen |
| `hyperfactions.admin.disband` | Jede Fraktion zwangsaufloesen |
| `hyperfactions.admin.modify` | Einstellungen jeder Fraktion aendern |
| `hyperfactions.admin.bypass.limits` | Anspruchs- und Machtgrenzen umgehen |
| `hyperfactions.admin.backup` | Backups erstellen und wiederherstellen |
| `hyperfactions.admin.power` | Spieler-Machtwerte ueberschreiben |
| `hyperfactions.admin.economy` | Fraktions-Schatzkammern verwalten |

## Fallback-Verhalten

Wenn **kein Berechtigungs-Plugin** installiert ist, fallen Admin-Berechtigungen auf den Server-Operator (OP)-Status zurueck. Dies wird durch `adminRequiresOp` in der Serverkonfiguration gesteuert (Standard: `true`).

>[!NOTE] Der `hyperfactions.admin.*`-Platzhalter gewaehrt jede Admin-Berechtigung. Nutze individuelle Knoten fuer granulare Kontrolle ueber dein Team.

## Reihenfolge der Berechtigungsaufloesung

1. **VaultUnlocked** Anbieter (falls verfuegbar)
2. **HyperPerms** Anbieter (falls verfuegbar)
3. **LuckPerms** Anbieter (falls verfuegbar)
4. **OP-Pruefung** fuer Admin-Knoten (Fallback)

>[!WARNING] Ohne Berechtigungs-Plugin und mit deaktiviertem `adminRequiresOp` sind Admin-Befehle **fuer alle Spieler offen**. Verwende im Produktivbetrieb immer ein Berechtigungs-Plugin.
