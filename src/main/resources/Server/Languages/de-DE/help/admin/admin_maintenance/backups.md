---
id: admin_backups
---
# Backup-System

HyperFactions beinhaltet automatische und manuelle Backups mit GFS-Rotation (Grossvater-Vater-Sohn).

## Backup-Befehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin backup create` | Jetzt ein manuelles Backup erstellen |
| `/f admin backup list` | Alle verfuegbaren Backups auflisten |
| `/f admin backup restore <name>` | Aus einem Backup wiederherstellen |
| `/f admin backup delete <name>` | Ein bestimmtes Backup loeschen |

**Berechtigung**: `hyperfactions.admin.backup`

## GFS-Rotationsstandards

| Typ | Aufbewahrung | Beschreibung |
|------|-----------|-------------|
| Stuendlich | 24 | Letzte 24 stuendliche Schnappschuesse |
| Taeglich | 7 | Letzte 7 taegliche Schnappschuesse |
| Woechentlich | 4 | Letzte 4 woechentliche Schnappschuesse |
| Manuell | 10 | Manuell erstellte Backups |
| Herunterfahren | 5 | Beim Server-Stopp erstellt |

>[!INFO] Herunterfahren-Backups sind standardmaessig aktiviert (`onShutdown=true`). Sie erfassen den letzten Stand vor dem Server-Stopp.

## Backup-Inhalte

Jedes Backup-ZIP-Archiv enthaelt:
- Alle Fraktionsdaten-Dateien
- Spieler-Machtdaten
- Zonendefinitionen
- Chatverlauf und Wirtschaftsdaten
- Einladungs- und Beitrittsanfragedaten
- Konfigurationsdateien

>[!WARNING] **Das Wiederherstellen eines Backups ist destruktiv.** Es ersetzt alle aktuellen Daten durch den Inhalt des Backups. Alle Aenderungen nach der Backup-Erstellung gehen verloren. Erstelle immer ein frisches Backup vor der Wiederherstellung.

## Empfohlene Vorgehensweise

1. Erstelle ein manuelles Backup vor groesseren Admin-Aktionen
2. Ueberpreufe die Backup-Aufbewahrung in `backup.json`
3. Teste die Wiederherstellung zuerst auf einem Testserver
4. Halte Herunterfahren-Backups fuer Absturzwiederherstellung aktiviert
