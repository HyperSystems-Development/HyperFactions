---
id: admin_updates
---
# Controllo Aggiornamenti

HyperFactions puo' controllare nuove versioni e gestire la dipendenza HyperProtect-Mixin.

## Comandi Aggiornamento

| Comando | Descrizione |
|---------|-------------|
| `/f admin update` | Controlla aggiornamenti di HyperFactions |
| `/f admin update mixin` | Controlla/scarica HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Attiva/disattiva download automatico |
| `/f admin version` | Mostra versione attuale e info build |

## Canali di Rilascio

| Canale | Descrizione |
|--------|-------------|
| **Stable** | Raccomandato per server di produzione |
| **Pre-release** | Accesso anticipato alle prossime funzionalita' |

>[!INFO] Il controllo aggiornamenti notifica solo le nuove versioni. **Non** installa automaticamente gli aggiornamenti di HyperFactions stesso.

## HyperProtect-Mixin

HyperProtect-Mixin e' il mixin di protezione raccomandato che abilita flag avanzati delle zone (esplosioni, propagazione fuoco, conservazione inventario, ecc.).

- `/f admin update mixin` controlla l'ultima versione
e la scarica se una versione piu' recente e' disponibile
- Il download automatico puo' essere attivato o disattivato per ogni server

>[!TIP] Dopo aver scaricato una nuova versione del mixin, e' necessario un riavvio del server affinche' le modifiche abbiano effetto.

## Procedura di Rollback

Se un aggiornamento causa problemi:

1. Ferma il server
2. Sostituisci il JAR del plugin con la versione precedente
3. Avvia il server
4. Verifica il funzionamento con `/f admin version`

>[!WARNING] Il downgrade potrebbe richiedere un reset della migrazione della configurazione. Mantieni sempre i backup prima di aggiornare.
