---
id: admin_disbanding
---
# Zwangsaufloesung

Admins koennen jede Fraktion zwangsweise aufloesen, unabhaengig vom Wunsch des Anfuehrers.

## Befehl

`/f admin disband <faction>`
Loest die genannte Fraktion zwangsweise auf. Eine Bestaetigungsabfrage erscheint, bevor die Aktion ausgefuehrt wird.

**Berechtigung**: `hyperfactions.admin.disband`

>[!WARNING] Das Aufloesen einer Fraktion ist **unwiderruflich**. Alle Ansprueche werden freigegeben, alle Mitglieder werden entfernt und die Fraktion hoert auf zu existieren. Erstelle zuerst ein Backup.

## Konsequenzen

Wenn eine Fraktion aufgeloest wird:

| Auswirkung | Beschreibung |
|--------|-------------|
| **Ansprueche** | Alles Gebiet wird sofort freigegeben |
| **Mitglieder** | Alle Spieler werden aus der Liste entfernt |
| **Beziehungen** | Alle Allianzen und Feindschaften werden geloescht |
| **Schatzkammer** | Wird gemaess Wirtschaftskonfiguration behandelt |
| **Zuhause** | Fraktions-Zuhause wird geloescht |
| **Chat** | Fraktions-Chatverlauf wird entfernt |

## Empfohlene Vorgehensweise

1. Fuehre immer `/f admin backup create` vor der Aufloesung aus
2. Benachrichtige die Fraktionsmitglieder wenn moeglich
3. Dokumentiere den Grund fuer die Serveraufzeichnungen
4. Pruefe `/f admin info <faction>` zur Ueberpruefung vor dem Handeln

>[!TIP] Wenn das Problem bei einem bestimmten Mitglied liegt, erwaege, ueber das Admin-Fraktions-GUI die Fuehrung zu uebertragen, anstatt die gesamte Fraktion aufzuloesen.
