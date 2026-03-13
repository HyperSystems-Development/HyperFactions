---
id: admin_treasury_management
---
# Gestione del Tesoro

Comandi admin per gestire i tesori delle fazioni. Richiede il permesso `hyperfactions.admin.economy`.

## Comandi del Tesoro

| Comando | Descrizione |
|---------|-------------|
| `/f admin economy balance <faction>` | Visualizza il saldo del tesoro della fazione |
| `/f admin economy set <faction> <amount>` | Imposta il saldo esatto |
| `/f admin economy add <faction> <amount>` | Aggiungi fondi al tesoro |
| `/f admin economy take <faction> <amount>` | Rimuovi fondi dal tesoro |
| `/f admin economy reset <faction>` | Azzera il tesoro |

## Esempi

- `/f admin economy balance Vikings` -- controlla il saldo
- `/f admin economy set Vikings 5000` -- imposta a 5000
- `/f admin economy add Vikings 1000` -- deposita 1000
- `/f admin economy take Vikings 500` -- preleva 500
- `/f admin economy reset Vikings` -- azzera il saldo

>[!TIP] Usa `/f admin info <faction>` per vedere la panoramica economica completa incluso lo storico transazioni insieme al saldo del tesoro.

## Casi d'Uso

| Scenario | Comando |
|----------|---------|
| Distribuzione premi evento | `economy add <faction> <prize>` |
| Penalita' per violazione regole | `economy take <faction> <fine>` |
| Reset economia dopo wipe | `economy reset <faction>` |
| Compensazione per bug | `economy add <faction> <amount>` |

>[!WARNING] Le modifiche al tesoro vengono registrate nello storico transazioni della fazione. Le modifiche admin vengono registrate con il nome dell'admin per responsabilita'.

>[!NOTE] Tutti i comandi admin economia funzionano anche quando il modulo economia e' disabilitato nella configurazione. I dati vengono salvati indipendentemente dallo stato del modulo.
