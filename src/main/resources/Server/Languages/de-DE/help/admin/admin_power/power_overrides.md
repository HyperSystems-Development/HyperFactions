---
id: admin_power_overrides
---
# Macht-Ueberschreibungen

Spezielle Machtbefehle, die das Machtverhalten fuer bestimmte Spieler oder Fraktionen aendern.

## Ueberschreibungsbefehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Benutzerdefiniertes Macht-Maximum setzen |
| `/f admin power noloss <player>` | Todes-Machtverlust-Immunitaet umschalten |
| `/f admin power nodecay <player>` | Offline-Machtverfall-Immunitaet umschalten |
| `/f admin power info <player>` | Alle Ueberschreibungen und Machtdetails anzeigen |

## Benutzerdefiniertes Macht-Maximum

`/f admin power setmax <player> <amount>`
Setzt ein persoenliches maximales Macht-Limit fuer den Spieler, das den Serverstandard ueberschreibt.

>[!INFO] Das Setzen eines benutzerdefinierten Maximums aendert **nicht** die aktuelle Macht. Es aendert nur die Obergrenze. Der Spieler muss Macht bis zum neuen Limit noch verdienen.

## Kein-Verlust-Modus

`/f admin power noloss <player>`
Schaltet die Todes-Machtverlust-Immunitaet um. Wenn aktiviert, verliert der Spieler beim Tod **keine** Macht.

Nuetzlich fuer:
- Schutzperioden fuer neue Spieler
- Event-Teilnehmer
- Team-Mitglieder

## Kein-Verfall-Modus

`/f admin power nodecay <player>`
Schaltet die Offline-Machtverfall-Immunitaet um. Wenn aktiviert, wird die Macht des Spielers im Offline-Zustand **nicht** abnehmen.

Nuetzlich fuer:
- Spieler in laengerer Abwesenheit
- VIP-Mitglieder
- Saisonaler Schutz

## Macht-Info

`/f admin power info <player>`
Zeigt eine vollstaendige Aufschluesselung:

- Aktuelle Macht und maximale Macht
- Aktive Ueberschreibungen (noloss, nodecay, benutzerdefiniertes Maximum)
- Letzter Todeszeitpunkt und verlorene Macht
- Fraktionsbeitragsprozentsatz

>[!TIP] Alle Macht-Ueberschreibungen bleiben ueber Serverneustarts bestehen und werden in der Datendatei des Spielers gespeichert.
