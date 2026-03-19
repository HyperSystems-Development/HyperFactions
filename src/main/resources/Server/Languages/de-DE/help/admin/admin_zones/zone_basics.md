---
id: admin_zone_basics
---
# Zonen-Grundlagen

Zonen sind von Admins kontrollierte Gebiete mit benutzerdefinierten Regeln, die den normalen Fraktionsschutz ueberschreiben.

## Zonentypen

- **SafeZone** -- Kein PvP, kein Bauen, kein Schaden.
Ideal fuer Spawngebiete und Handelsplaetze.
- **WarZone** -- PvP ist immer aktiviert, kein Bauen.
Ideal fuer Arenen und umkaempfte Kampfgebiete.

## Zonen erstellen

`/f admin safezone <name>`
Erstellt eine SafeZone und beansprucht deinen aktuellen Chunk.

`/f admin warzone <name>`
Erstellt eine WarZone und beansprucht deinen aktuellen Chunk.

Nach der Erstellung stelle dich in weitere Chunks und nutze `/f admin zone claim <zone>`, um die Zone zu erweitern.

## Zonen-Chunks verwalten

`/f admin zone claim <zone>`
Fuegt den aktuellen Chunk zur benannten Zone hinzu.

`/f admin zone unclaim <zone>`
Entfernt den aktuellen Chunk aus der benannten Zone.

`/f admin zone radius <zone> <radius>`
Beansprucht ein Quadrat von Chunks um deine Position.

## Zonen loeschen

`/f admin removezone <name>`
Loescht die Zone dauerhaft und gibt alle beanspruchten Chunks frei.

>[!WARNING] Das Loeschen einer Zone gibt alle Chunks sofort frei. Dies kann ohne Backup-Wiederherstellung nicht rueckgaengig gemacht werden.

>[!INFO] Zonenregeln **ueberschreiben immer** Fraktions-Gebietsregeln. Eine SafeZone in feindlichem Land ist trotzdem sicher.
