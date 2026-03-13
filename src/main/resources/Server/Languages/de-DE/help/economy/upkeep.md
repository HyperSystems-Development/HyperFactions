---
id: economy_upkeep
---
# Gebietsunterhalt

Fraktionen muessen laufenden Unterhalt zahlen, um ihr beanspruchtes Gebiet zu halten. Dies verhindert Landhamsterei und haelt die Karte dynamisch.

## Unterhaltskosten

| Einstellung | Standard |
|---------|---------|
| Kosten pro Chunk | 2.0 pro Zyklus |
| Zahlungsintervall | Alle 24 Stunden |
| Kostenlose Chunks | 3 (keine Kosten) |
| Skalierungsmodus | Pauschale |

>[!NOTE] Dies sind Standardwerte. Dein Server-Administrator hat moeglicherweise andere Einstellungen konfiguriert.

Deine ersten 3 Chunks sind kostenlos. Darueber hinaus kostet jeder zusaetzliche beanspruchte Chunk 2.0 pro Zahlungszyklus.

## Automatische Zahlung

Automatische Zahlung ist standardmaessig aktiviert. Das System zieht den Unterhalt automatisch bei jedem Intervall von eurer Schatzkammer ab. Kein manuelles Eingreifen noetig.

---

## Gnadenfrist

Wenn eure Schatzkammer den Unterhalt nicht decken kann, beginnt eine 48-stuendige Gnadenfrist. Eine Warnung wird 6 Stunden vor dem Verlust von Anspruechen gesendet.

>[!WARNING] Wenn der Unterhalt nach der Gnadenfrist unbezahlt bleibt, verliert eure Fraktion 1 Anspruch pro Zyklus, bis die Kosten gedeckt sind oder alle zusaetzlichen Ansprueche aufgebraucht sind.

## Beispiel

*Eine Fraktion mit 8 Anspruechen zahlt fuer 5 Chunks (8 minus 3 kostenlose). Bei 2.0 pro Chunk sind das 10.0 pro Zyklus.*

>[!TIP] Halte deine Schatzkammer ueber den Unterhaltskosten. Nutze /f balance, um deine Reserven zu pruefen.
