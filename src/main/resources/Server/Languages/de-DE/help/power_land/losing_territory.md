---
id: power_losing
commands: overclaim
---
# Gebiet verlieren

Wenn die Gesamtmacht einer Fraktion unter die Kosten ihrer Ansprueche faellt, wird sie ueberfallbar. Feinde koennen Chunks direkt unter euch wegbeanspruchen.

---

## So funktioniert das Ueberbeanspruchen

`/f overclaim`

Ein Offizier oder Anfuehrer einer feindlichen Fraktion stellt sich in euren beanspruchten Chunk und fuehrt diesen Befehl aus. Wenn eure Fraktion ein Machtdefizit hat, wechselt der Chunk zu deren Fraktion.

## Die Berechnung

Jeder Anspruch kostet 2.0 Macht im Unterhalt. Wenn eure Gesamtmacht unter diese Schwelle faellt, sind die Defizit-Chunks verwundbar.

>[!NOTE] Dies sind Standardwerte. Dein Server-Administrator hat moeglicherweise andere Einstellungen konfiguriert.

>[!WARNING] Ueberbeanspruchung ist dauerhaft. Sobald ein Feind einen Chunk uebernimmt, musst du ihn zurueckerobern (oder zurueckbeanspruchen, wenn sie geschwaecht sind).

---

## Beispielszenario

| Faktor | Wert |
|--------|-------|
| Mitglieder | 5 Spieler |
| Macht pro Mitglied | Jeweils 10 (Start) |
| Gesamtmacht | 50 |
| Ansprueche | 30 Chunks |
| Benoetigte Macht (30 x 2.0) | 60 |
| Defizit | 10 Macht zu wenig |

In diesem Beispiel ist die Fraktion von Anfang an ueberfallbar. Feinde koennten bis zu 5 Chunks ueberbeanspruchen (10 Defizit / 2.0 pro Anspruch), bevor die Fraktion ein Gleichgewicht erreicht.

---

## So verhinderst du Gebietsverlust

- Ueberdehne dich nicht -- halte die Gesamtmacht immer mit einem Puffer ueber deinen Anspruchskosten
- Bleib aktiv -- Macht regeneriert sich nur im Online-Zustand (+0.1/Min.)
- Vermeide unnoetige Tode -- jeder Tod kostet 1.0 Macht
- Rekrutiere mehr Mitglieder -- mehr Spieler bedeuten mehr Gesamtmacht
- Gib ungenutzte Chunks frei -- setze Macht frei mit /f unclaim

>[!TIP] Pruefe regelmaessig deinen Machtstatus mit /f power. Wenn deine Gesamtmacht nahe an deinen Anspruchskosten liegt, erwaege, weniger wichtige Chunks vor einem Krieg freizugeben.
