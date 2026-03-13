---
id: admin_upkeep_management
---
# Onderhoudsbeheer

Factieonderhoud brengt facties periodiek kosten in rekening op basis van hun grondgebied en ledenaantal.

## Admin Besturingselementen

Onderhoudsinstellingen worden beheerd via het economie-configuratiebestand of de admin-config-GUI.

`/f admin config`
Open de config-editor en navigeer naar economie-instellingen om onderhoudswaarden aan te passen.

## Standaard Onderhoudsinstellingen

| Instelling | Standaard | Beschrijving |
|------------|-----------|-------------|
| Onderhoud ingeschakeld | false | Hoofdschakelaar voor het systeem |
| Onderhoudsinterval | 24u | Hoe vaak onderhoud wordt geheven |
| Per-claim kosten | 5.0 | Kosten per geclaimde chunk per cyclus |
| Per-lid kosten | 0.0 | Kosten per lid per cyclus |
| Respijtperiode | 72u | Nieuwe facties zijn vrijgesteld |
| Ontbinden bij faillissement | false | Automatisch ontbinden als niet kan betalen |

## Onderhoud Monitoren

Gebruik `/f admin info <faction>` om te zien:
- Huidig schatkistsaldo
- Geschatte onderhoudskosten per cyclus
- Tijd tot volgende onderhoudsheffing
- Of de factie onderhoud kan betalen

>[!TIP] Bekijk economiestatistieken van alle facties vanuit het admin-dashboard om facties met faillissementsrisico te identificeren voordat onderhoud in werking treedt.

>[!INFO] Onderhoudsconfiguratie is opgeslagen in `economy.json`. Wijzigingen via de config-GUI worden van kracht na herladen met `/f admin reload`.

## Onderhoudsformule

**Totaal onderhoud** = (geclaimde chunks x per-claim kosten) + (ledenaantal x per-lid kosten)

>[!WARNING] Het inschakelen van onderhoud op een server met bestaande facties kan onverwachte faillissementen veroorzaken. Overweeg een respijtperiode in te stellen of de wijziging van tevoren aan te kondigen.
