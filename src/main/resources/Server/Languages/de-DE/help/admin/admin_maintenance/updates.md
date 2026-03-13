---
id: admin_updates
---
# Update-Pruefung

HyperFactions kann nach neuen Versionen suchen und die HyperProtect-Mixin-Abhaengigkeit verwalten.

## Update-Befehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin update` | Nach HyperFactions-Updates suchen |
| `/f admin update mixin` | HyperProtect-Mixin pruefen/herunterladen |
| `/f admin update toggle-mixin-download` | Automatischen Download umschalten |
| `/f admin version` | Aktuelle Version und Build-Info anzeigen |

## Release-Kanaele

| Kanal | Beschreibung |
|---------|-------------|
| **Stable** | Empfohlen fuer Produktivserver |
| **Pre-release** | Fruehzeitiger Zugang zu kommenden Funktionen |

>[!INFO] Die Update-Pruefung benachrichtigt nur ueber neue Versionen. Sie installiert **keine** Updates fuer HyperFactions selbst automatisch.

## HyperProtect-Mixin

HyperProtect-Mixin ist das empfohlene Schutz-Mixin, das erweiterte Zonen-Flags aktiviert (Explosionen, Feuerausbreitung, Inventar behalten usw.).

- `/f admin update mixin` prueft auf die neueste Version
und laedt sie herunter, wenn eine neuere Version verfuegbar ist
- Automatischer Download kann pro Server ein- oder ausgeschaltet werden

>[!TIP] Nach dem Herunterladen einer neuen Mixin-Version ist ein Serverneustart erforderlich, damit die Aenderungen wirksam werden.

## Rollback-Verfahren

Wenn ein Update Probleme verursacht:

1. Stoppe den Server
2. Ersetze die Plugin-JAR-Datei durch die vorherige Version
3. Starte den Server
4. Ueberpreufe die Funktionalitaet mit `/f admin version`

>[!WARNING] Ein Downgrade kann ein Zuruecksetzen der Konfigurationsmigration erfordern. Halte immer Backups bereit, bevor du aktualisierst.
