---
id: admin_treasury_management
---
# Schatzkammer-Verwaltung

Admin-Befehle zur Verwaltung von Fraktions-Schatzkammern. Erfordert die `hyperfactions.admin.economy` Berechtigung.

## Schatzkammer-Befehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin economy balance <faction>` | Schatzkammer-Kontostand der Fraktion anzeigen |
| `/f admin economy set <faction> <amount>` | Exakten Kontostand setzen |
| `/f admin economy add <faction> <amount>` | Mittel zur Schatzkammer hinzufuegen |
| `/f admin economy take <faction> <amount>` | Mittel aus der Schatzkammer entfernen |
| `/f admin economy reset <faction>` | Schatzkammer auf Null zuruecksetzen |

## Beispiele

- `/f admin economy balance Vikings` -- Kontostand pruefen
- `/f admin economy set Vikings 5000` -- auf 5000 setzen
- `/f admin economy add Vikings 1000` -- 1000 einzahlen
- `/f admin economy take Vikings 500` -- 500 abheben
- `/f admin economy reset Vikings` -- Kontostand nullen

>[!TIP] Nutze `/f admin info <faction>`, um die vollstaendige Wirtschaftsuebersicht einschliesslich Transaktionsverlauf zusammen mit dem Schatzkammer-Kontostand zu sehen.

## Anwendungsfaelle

| Szenario | Befehl |
|----------|---------|
| Event-Preisverteilung | `economy add <faction> <prize>` |
| Strafe fuer Regelverstoss | `economy take <faction> <fine>` |
| Wirtschaftsreset nach Wipe | `economy reset <faction>` |
| Kompensation fuer Fehler | `economy add <faction> <amount>` |

>[!WARNING] Schatzkammer-Aenderungen werden im Transaktionsverlauf der Fraktion protokolliert. Admin-Aenderungen werden mit dem Namen des Admins fuer die Nachverfolgung aufgezeichnet.

>[!NOTE] Alle Wirtschafts-Admin-Befehle funktionieren auch dann, wenn das Wirtschaftsmodul in der Konfiguration deaktiviert ist. Die Daten werden unabhaengig vom Modulstatus gespeichert.
