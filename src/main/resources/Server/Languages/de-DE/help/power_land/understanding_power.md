---
id: power_understanding
commands: power
---
# Macht verstehen

Macht ist die zentrale Ressource, die bestimmt, wie viel Gebiet deine Fraktion halten kann. Jeder Spieler hat persoenliche Macht, die zur Fraktionsgesamtmacht beitraegt.

---

## Standard-Machtwerte

| Einstellung | Wert |
|---------|-------|
| Maximale Macht pro Spieler | 20 |
| Startmacht | 10 |
| Todesstrafe | -1.0 pro Tod |
| Belohnung fuer Kills | 0.0 |
| Regenerationsrate | +0.1 pro Minute (solange online) |
| Machtkosten pro Anspruch | 2.0 |
| Abmeldung waehrend Markierung | -1.0 zusaetzlich |

>[!NOTE] Dies sind Standardwerte. Dein Server-Administrator hat moeglicherweise andere Einstellungen konfiguriert.

## So funktioniert es

Die Gesamtmacht deiner Fraktion ist die Summe der persoenlichen Macht aller Mitglieder. Die benoetigte Macht ist die Anzahl der Ansprueche multipliziert mit 2.0. Solange die Gesamtmacht ueber der benoetigten Macht bleibt, ist euer Gebiet sicher.

>[!INFO] Macht regeneriert sich passiv mit 0.1 pro Minute, solange du online bist. Mit dieser Rate dauert die Erholung von 1.0 Macht etwa 10 Minuten.

---

## Deine Macht pruefen

`/f power`

Zeigt deine persoenliche Macht, die Gesamtmacht deiner Fraktion und wie viel benoetigt wird, um die aktuellen Ansprueche zu halten.

## Die Gefahrenzone

Wenn die Gesamtmacht unter den fuer eure Ansprueche benoetigten Betrag faellt, wird eure Fraktion verwundbar. Feinde koennen eure Chunks ueberbeanspruchen.

>[!WARNING] Mehrere Tode in kurzer Zeit koennen sich schnell aufsummieren. Wenn ihr 5 Mitglieder mit je 10 Macht habt (50 gesamt) und 20 Ansprueche (40 benoetigt), bringen euch 5 Tode im Team auf 45 -- noch sicher. Aber 11 Tode bringen euch auf 39, unter die 40er-Schwelle.

>[!TIP] Halte einen Machtpuffer. Beanspruche nicht jeden Chunk, den du dir leisten kannst -- lass Spielraum fuer ein paar Tode, ohne ueberfallbar zu werden.
