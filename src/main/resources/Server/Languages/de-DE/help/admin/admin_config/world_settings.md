---
id: admin_world_settings
---
# Welt-spezifische Einstellungen

HyperFactions unterstuetzt welt-spezifische Konfiguration fuer Beanspruchung, PvP und Schutzverhalten.

## Welt-Befehle

| Befehl | Beschreibung |
|---------|-------------|
| `/f admin world list` | Alle Welt-Ueberschreibungen auflisten |
| `/f admin world info <world>` | Einstellungen fuer eine Welt anzeigen |
| `/f admin world set <world> <key> <value>` | Eine Einstellung setzen |
| `/f admin world reset <world>` | Welt auf Standards zuruecksetzen |

## Verfuegbare Einstellungen

| Einstellung | Typ | Beschreibung |
|---------|------|-------------|
| claiming_enabled | boolean | Fraktions-Beanspruchungen in dieser Welt erlauben |
| pvp_enabled | boolean | PvP-Kampf in dieser Welt erlauben |
| power_loss | boolean | Machtverlust bei Tod anwenden |
| build_protection | boolean | Anspruchs-Bauschutz durchsetzen |
| explosion_protection | boolean | Ansprueche vor Explosionen schuetzen |

## Welt-Whitelist / Blacklist

Steuere, welche Welten Fraktionsfunktionen erlauben, ueber die `worlds.json` Konfigurationsdatei:

- **Whitelist-Modus**: Nur gelistete Welten erlauben Beanspruchung
- **Blacklist-Modus**: Alle Welten erlauben Beanspruchung ausser den gelisteten

>[!INFO] Welt-Einstellungen sind in `worlds.json` gespeichert und ueberschreiben die globalen Standards aus `factions.json`.

## Beispiele

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- alle Standards wiederherstellen

>[!TIP] Deaktiviere Beanspruchung in Kreativ- oder Lobby-Welten, um das Fraktionssystem auf das Survival-Gameplay zu konzentrieren.

>[!NOTE] Welt-spezifische Einstellungen haben Vorrang vor der globalen Konfiguration, werden aber von Zonen-Flags innerhalb dieser Welt ueberschrieben.
