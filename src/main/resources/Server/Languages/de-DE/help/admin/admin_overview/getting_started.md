---
id: admin_getting_started
---
# Erste Schritte als Admin

Willkommen in der HyperFactions-Administration. Dieser Leitfaden behandelt deine ersten Schritte nach der Installation des Plugins.

## Das Admin-Dashboard oeffnen

`/f admin`
Oeffnet das Admin-Dashboard-GUI mit Zugang zu allen Verwaltungswerkzeugen, Zonen-Editoren und Servereinstellungen.

>[!INFO] Du benoetigst die **hyperfactions.admin.use** Berechtigung oder OP-Status, um auf Admin-Befehle zugreifen zu koennen.

## Voraussetzungen

- **Mit einem Berechtigungs-Plugin**: Vergib `hyperfactions.admin.use`
- **Ohne Berechtigungs-Plugin**: Der Spieler muss ein
Server-Operator sein (`adminRequiresOp=true` standardmaessig)

## Erste Schritte nach der Installation

1. Fuehre `/f admin` aus, um deinen Zugang zu ueberpruefen
2. Oeffne **Config**, um die Standard-Fraktionseinstellungen zu ueberpruefen
3. Erstelle eine **SafeZone** am Spawn mit `/f admin safezone Spawn`
4. Erstelle optional **WarZones** fuer PvP-Arenen
5. Ueberpreufe die **Backup**-Einstellungen, um Datensicherheit zu gewaehrleisten

## Admin-Faehigkeiten

| Bereich | Moeglichkeiten |
|------|----------------|
| Fraktionen | Jede Fraktion einsehen, aendern oder zwangsaufloesen |
| Zonen | SafeZones und WarZones mit benutzerdefinierten Flags erstellen |
| Macht | Spieler-/Fraktionsmachtwerte ueberschreiben |
| Wirtschaft | Fraktions-Schatzkammern und Unterhalt verwalten |
| Konfiguration | Einstellungen live ueber GUI bearbeiten oder von der Festplatte neu laden |
| Backups | Datensicherungen erstellen, wiederherstellen und verwalten |
| Importe | Daten von anderen Fraktions-Plugins migrieren |

>[!TIP] Nutze `/f admin --text`, um Chat-basierte Ausgabe statt des GUIs zu erhalten -- nuetzlich fuer Konsole oder Automatisierung.
