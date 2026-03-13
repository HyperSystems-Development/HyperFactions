---
id: admin_managing_factions
---
# Fraktionen verwalten

Admins koennen jede Fraktion auf dem Server ueber das Dashboard oder Befehle einsehen und aendern.

## Fraktionen durchsuchen

`/f admin factions`
Oeffnet den Admin-Fraktionsbrowser. Zeigt alle Fraktionen mit Mitgliederzahlen, Machtwerten und Gebiet an.

`/f admin info <faction>`
Oeffnet das Admin-Infopanel fuer eine bestimmte Fraktion mit allen Details und Verwaltungsoptionen.

## Fraktionseinstellungen aendern

Mit der `hyperfactions.admin.modify` Berechtigung kannst du:

- Fraktion **umbenennen**, um Konflikte zu loesen
- **Farbe setzen**, um Anzeigeprobleme zu beheben
- **Offen/Geschlossen umschalten**, um die Beitrittspolitik zu ueberschreiben
- **Beschreibung bearbeiten** fuer Moderationszwecke

>[!TIP] Nutze `/f admin who <player>`, um nachzuschlagen, zu welcher Fraktion ein bestimmter Spieler gehoert, und seine Details einzusehen.

## Mitglieder und Beziehungen einsehen

Das Admin-Infopanel zeigt:

| Bereich | Details |
|---------|---------|
| **Mitglieder** | Vollstaendige Liste mit Rollen und letzter Aktivitaet |
| **Beziehungen** | Alle Verbuendeten-, Feind- und Neutral-Verhaeltnisse |
| **Gebiet** | Beanspruchte Chunks und Machtbilanz |
| **Wirtschaft** | Schatzkammer-Kontostand und Transaktionsprotokoll |

>[!NOTE] Admin-Einsichtsbefehle benachrichtigen die eingesehene Fraktion nicht. Nur Aenderungen loesen Benachrichtigungen aus.
