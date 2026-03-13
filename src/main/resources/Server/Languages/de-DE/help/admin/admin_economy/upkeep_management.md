---
id: admin_upkeep_management
---
# Unterhaltsverwaltung

Fraktionsunterhalt belastet Fraktionen periodisch basierend auf ihrem Gebiet und ihrer Mitgliederzahl.

## Admin-Steuerung

Unterhaltseinstellungen werden ueber die Wirtschafts-Konfigurationsdatei oder das Admin-Konfigurations-GUI verwaltet.

`/f admin config`
Oeffne den Konfigurationseditor und navigiere zu den Wirtschaftseinstellungen, um Unterhaltswerte anzupassen.

## Standard-Unterhaltseinstellungen

| Einstellung | Standard | Beschreibung |
|---------|---------|-------------|
| Unterhalt aktiviert | false | Hauptschalter fuer das System |
| Unterhaltsintervall | 24h | Wie oft Unterhalt berechnet wird |
| Kosten pro Anspruch | 5.0 | Kosten pro beanspruchtem Chunk pro Zyklus |
| Kosten pro Mitglied | 0.0 | Kosten pro Mitglied pro Zyklus |
| Gnadenfrist | 72h | Neue Fraktionen sind befreit |
| Aufloesung bei Bankrott | false | Automatische Aufloesung bei Zahlungsunfaehigkeit |

## Unterhalt ueberwachen

Nutze `/f admin info <faction>`, um zu sehen:
- Aktueller Schatzkammer-Kontostand
- Geschaetzte Unterhaltskosten pro Zyklus
- Zeit bis zur naechsten Unterhaltsberechnung
- Ob die Fraktion sich den Unterhalt leisten kann

>[!TIP] Ueberpreufe die Wirtschaftsstatistiken aller Fraktionen vom Admin-Dashboard aus, um Fraktionen zu identifizieren, die vor dem Unterhaltszeitpunkt bankrottgefaehrdet sind.

>[!INFO] Die Unterhaltskonfiguration ist in `economy.json` gespeichert. Aenderungen ueber das Konfigurations-GUI werden nach dem Neuladen mit `/f admin reload` wirksam.

## Unterhaltsformel

**Gesamtunterhalt** = (beanspruchte Chunks x Kosten pro Anspruch) + (Mitgliederzahl x Kosten pro Mitglied)

>[!WARNING] Das Aktivieren von Unterhalt auf einem Server mit bestehenden Fraktionen kann unerwartete Bankrotte verursachen. Erwaege, eine Gnadenfrist festzulegen oder die Aenderung im Voraus anzukuendigen.
