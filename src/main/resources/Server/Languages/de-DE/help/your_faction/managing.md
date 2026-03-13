---
id: faction_managing
commands: invite, kick, promote, demote, transfer
---
# Mitglieder verwalten

Offiziere und Anfuehrer teilen sich die Verantwortung fuer die Verwaltung der Fraktions-Mitgliederliste. Hier sind die wichtigsten Befehle und wer sie nutzen kann.

---

## Befehle

| Befehl | Beschreibung | Benoetigter Rang |
|---------|-------------|---------------|
| `/f invite <player>` | Sendet eine Beitrittseinladung (laeuft in 5 Min. ab) | Offizier+ |
| `/f kick <player>` | Entfernt ein Mitglied aus der Fraktion | Offizier+ (siehe Hinweis) |
| `/f promote <player>` | Befoerdert ein Mitglied zum Offizier | Nur Anfuehrer |
| `/f demote <player>` | Degradiert einen Offizier zum Mitglied | Nur Anfuehrer |
| `/f transfer <player>` | Uebertraegt die Fraktionsfuehrung | Nur Anfuehrer |

>[!NOTE] Offiziere koennen nur Mitglieder entfernen. Um einen anderen Offizier zu entfernen, muss der Anfuehrer ihn entweder zuerst degradieren oder direkt entfernen.

---

## Einladungen

- Einladungen laufen nach 5 Minuten ab, wenn sie nicht angenommen werden
- Der eingeladene Spieler sieht sie im Einladungs-Tab, wenn er /f oeffnet
- Es gibt kein Limit fuer die Anzahl gleichzeitig versendeter Einladungen
- Deine Fraktion kann insgesamt bis zu 50 Mitglieder haben

## Befoerderungen und Degradierungen

- Nur der Anfuehrer kann befoerdern oder degradieren
- /f promote befoerdert ein Mitglied zum Offizier
- /f demote degradiert einen Offizier zurueck zum Mitglied

## Fuehrung uebertragen

>[!WARNING] Die Uebertragung der Fuehrung ist unwiderruflich. Du wirst zum Offizier degradiert und der Zielspieler wird der neue Anfuehrer. Stelle sicher, dass du ihm vollstaendig vertraust.

`/f transfer <player>`

Das Ziel muss ein aktuelles Mitglied deiner Fraktion sein.
