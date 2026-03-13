---
id: admin_power_commands
---
# Macht-Admin-Befehle

Spieler- und Fraktionsmachtwerte ueberschreiben. Alle Befehle erfordern die `hyperfactions.admin.power` Berechtigung.

## Spieler-Machtbefehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin power set <player> <amount>` | Exakten Machtwert setzen |
| `/f admin power add <player> <amount>` | Macht zum Spieler hinzufuegen |
| `/f admin power remove <player> <amount>` | Macht vom Spieler entfernen |
| `/f admin power reset <player>` | Auf Standard-Startmacht zuruecksetzen |
| `/f admin power info <player>` | Detaillierte Machtaufschluesselung anzeigen |

## Wie Macht Fraktionen beeinflusst

Die Gesamtmacht einer Fraktion ist die Summe der individuellen Macht aller Mitglieder. Gebietsansprueche erfordern ausreichend Gesamtmacht fuer den Unterhalt.

| Szenario | Auswirkung |
|----------|--------|
| Macht hoeher gesetzt | Fraktion kann mehr Gebiet beanspruchen |
| Macht niedriger gesetzt | Fraktion kann anfaellig fuer Uebernahmen werden |
| Macht zurueckgesetzt | Setzt Spieler auf Standard-Startwert zurueck |

>[!WARNING] Das Senken der Macht eines Spielers kann dazu fuehren, dass seine Fraktion Gebiet verliert, wenn die Gesamtmacht unter die Anzahl der beanspruchten Chunks faellt.

## Beispiele

- `/f admin power set Steve 50` -- auf genau 50 setzen
- `/f admin power add Steve 10` -- um 10 erhoehen
- `/f admin power remove Steve 5` -- um 5 verringern
- `/f admin power reset Steve` -- auf Standard zuruecksetzen
- `/f admin power info Steve` -- vollstaendige Aufschluesselung anzeigen

>[!TIP] Nutze `/f admin power info <player>`, um aktuelle Macht, maximale Macht und aktive Ueberschreibungen zu sehen, bevor du Aenderungen vornimmst.
