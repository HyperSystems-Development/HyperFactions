---
id: admin_integrations
---
# Plugin-Integrationen

HyperFactions integriert sich mit mehreren externen Plugins ueber weiche Abhaengigkeiten. Alle Integrationen sind optional und funktionieren problemlos auch ohne die externen Plugins.

## Integrationsstatus pruefen

`/f admin version`
Zeigt aktuelle Version und erkannte Integrationen an.

`/f admin integration`
Oeffnet das Integrationsverwaltungs-Panel mit detailliertem Status fuer jedes erkannte Plugin.

## Integrationstabelle

| Plugin | Typ | Beschreibung |
|--------|------|-------------|
| **HyperPerms** | Berechtigungen | Vollstaendiges Berechtigungssystem mit Gruppen, Vererbung und Kontext |
| **LuckPerms** | Berechtigungen | Alternativer Berechtigungsanbieter |
| **VaultUnlocked** | Berechtigungen/Wirtschaft | Berechtigungs- und Wirtschaftsbruecke |
| **HyperProtect-Mixin** | Schutz | Aktiviert erweiterte Zonen-Flags (Explosionen, Feuer, Inventar behalten) |
| **OrbisGuard-Mixins** | Schutz | Alternatives Mixin fuer Zonen-Flag-Durchsetzung |
| **PlaceholderAPI** | Platzhalter | 49 Fraktions-Platzhalter fuer andere Plugins |
| **WiFlow PlaceholderAPI** | Platzhalter | Alternativer Platzhalter-Anbieter |
| **GravestonePlugin** | Tod | Grabstein-Zugriffskontrolle in Zonen |
| **HyperEssentials** | Funktionen | Zonen-Flags fuer Zuhause, Warps und Kits |
| **KyuubiSoft Core** | Framework | Kernbibliotheks-Integration |
| **Sentry** | Ueberwachung | Fehlerverfolgung und Diagnose |

## Prioritaet der Berechtigungsanbieter

1. **VaultUnlocked** (hoechste Prioritaet)
2. **HyperPerms**
3. **LuckPerms**
4. **OP-Fallback** (wenn kein Anbieter gefunden)

>[!INFO] Integrationen werden einmalig beim Start per Reflection erkannt. Ergebnisse werden fuer die Sitzung zwischengespeichert. Ein Serverneustart ist erforderlich, nachdem ein integriertes Plugin hinzugefuegt oder entfernt wurde.

>[!TIP] Nutze `/f admin debug toggle integration`, um detaillierte Integrations-Protokollierung zur Fehlerbehebung zu aktivieren.

>[!NOTE] HyperProtect-Mixin ist das **empfohlene** Schutz-Mixin. Ohne es haben 15 Zonen-Flags keine Wirkung.
