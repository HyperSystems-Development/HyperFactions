---
id: admin_configuration
---
# Konfigurationssystem

HyperFactions verwendet ein modulares JSON-Konfigurationssystem mit 11 Konfigurationsdateien.

## Admin-Konfigurationsbefehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin config` | Visuellen Konfigurationseditor-GUI oeffnen |
| `/f admin reload` | Alle Konfigurationsdateien von der Festplatte neu laden |
| `/f admin sync` | Fraktionsdaten mit dem Speicher synchronisieren |

## Konfigurationsdateien

| Datei | Inhalt |
|------|----------|
| `factions.json` | Rollen, Macht, Ansprueche, Kampf, Beziehungen |
| `server.json` | Teleport, Auto-Speichern, Nachrichten, GUI, Berechtigungen |
| `economy.json` | Schatzkammer, Unterhalt, Transaktionseinstellungen |
| `backup.json` | Backup-Rotation und Aufbewahrungseinstellungen |
| `chat.json` | Fraktions- und Verbuendeten-Chat-Formatierung |
| `debug.json` | Debug-Protokollierungskategorien |
| `faction-permissions.json` | Standard-Berechtigungen pro Rolle |
| `announcements.json` | Event-Broadcasts und Gebietsbenachrichtigungen |
| `gravestones.json` | Grabstein-Integrationseinstellungen |
| `worldmap.json` | Weltkarten-Aktualisierungsmodi |
| `worlds.json` | Welt-spezifische Verhaltensaenderungen |

>[!TIP] Das Konfigurations-GUI bietet einen visuellen Editor mit Beschreibungen fuer jede Einstellung. Aenderungen werden sofort gespeichert, aber einige erfordern `/f admin reload`, um vollstaendig wirksam zu werden.

## Konfigurationsort

Alle Dateien sind gespeichert in:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Manuelle JSON-Bearbeitungen erfordern `/f admin reload` zur Anwendung. Ungueltiges JSON fuehrt dazu, dass die Datei mit einer Warnung im Serverlog uebersprungen wird.

>[!NOTE] Die Konfigurationsversion wird in `server.json` verfolgt. Das Plugin migriert aeltere Konfigurationen beim Start automatisch.
