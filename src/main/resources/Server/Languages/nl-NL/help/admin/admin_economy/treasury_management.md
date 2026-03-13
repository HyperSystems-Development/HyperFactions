---
id: admin_treasury_management
---
# Schatkistbeheer

Admincommando's voor het beheren van factieschatkisten. Vereist de `hyperfactions.admin.economy` permissie.

## Schatkistcommando's

| Commando | Beschrijving |
|----------|-------------|
| `/f admin economy balance <faction>` | Bekijk factieschatkistsaldo |
| `/f admin economy set <faction> <amount>` | Stel exact saldo in |
| `/f admin economy add <faction> <amount>` | Voeg geld toe aan schatkist |
| `/f admin economy take <faction> <amount>` | Verwijder geld uit schatkist |
| `/f admin economy reset <faction>` | Reset schatkist naar nul |

## Voorbeelden

- `/f admin economy balance Vikings` -- controleer saldo
- `/f admin economy set Vikings 5000` -- stel in op 5000
- `/f admin economy add Vikings 1000` -- stort 1000
- `/f admin economy take Vikings 500` -- neem 500 op
- `/f admin economy reset Vikings` -- zet saldo op nul

>[!TIP] Gebruik `/f admin info <faction>` om het volledige economie-overzicht te bekijken, inclusief transactiegeschiedenis naast het schatkistsaldo.

## Gebruiksscenario's

| Scenario | Commando |
|----------|---------|
| Evenementprijzenverdeling | `economy add <faction> <prize>` |
| Straf voor regelovertreding | `economy take <faction> <fine>` |
| Economie-reset na wipe | `economy reset <faction>` |
| Compensatie voor bugs | `economy add <faction> <amount>` |

>[!WARNING] Schatkistwijzigingen worden gelogd in de transactiegeschiedenis van de factie. Adminwijzigingen worden vastgelegd met de naam van de admin voor verantwoording.

>[!NOTE] Alle economie-admincommando's werken zelfs wanneer de economiemodule is uitgeschakeld in de configuratie. De data wordt opgeslagen ongeacht de modulestatus.
