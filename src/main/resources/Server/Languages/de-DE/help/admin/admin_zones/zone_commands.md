---
id: admin_zone_commands
---
# Zonen-Befehlsreferenz

Vollstaendige Referenz fuer alle Zonen-Verwaltungsbefehle. Alle erfordern die `hyperfactions.admin.zones` Berechtigung.

## Schnellerstellung

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin safezone <name>` | SafeZone am aktuellen Chunk erstellen |
| `/f admin warzone <name>` | WarZone am aktuellen Chunk erstellen |
| `/f admin removezone <name>` | Zone loeschen und Chunks freigeben |

## Zonenverwaltung

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin zone create <name> <type>` | Zone erstellen (safezone/warzone) |
| `/f admin zone delete <name>` | Zone loeschen |
| `/f admin zone claim <zone>` | Aktuellen Chunk zur Zone hinzufuegen |
| `/f admin zone unclaim <zone>` | Aktuellen Chunk aus Zone entfernen |
| `/f admin zone radius <zone> <r>` | Quadratischen Radius von Chunks beanspruchen |
| `/f admin zone list` | Alle Zonen mit Chunk-Anzahl auflisten |
| `/f admin zone notify <zone> <true/false>` | Betreten-/Verlassen-Nachrichten umschalten |
| `/f admin zone title <zone> upper/lower <text>` | Zonen-Titeltext setzen |
| `/f admin zone properties <zone>` | Zonen-Eigenschaften-GUI oeffnen |

## Flag-Verwaltung

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Ein bestimmtes Flag setzen |

>[!TIP] Nutze das Zonen-**Eigenschaften-GUI** fuer einen visuellen Editor mit Schaltern fuer jedes Flag, nach Kategorie geordnet.

## Beispiele

- `/f admin safezone Spawn` -- Spawn-Schutz erstellen
- `/f admin zone radius Spawn 3` -- auf 7x7 Chunks erweitern
- `/f admin zoneflag Spawn door_use true` -- Tueren erlauben
- `/f admin zone notify Spawn true` -- Eintrittsnachrichten anzeigen
