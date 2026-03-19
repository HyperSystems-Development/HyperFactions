---
id: admin_power_overrides
---
# Override del Potere

Comandi speciali del potere che cambiano il comportamento del potere per giocatori o fazioni specifici.

## Comandi Override

| Comando | Descrizione |
|---------|-------------|
| `/f admin power setmax <player> <amount>` | Imposta un tetto massimo di potere personalizzato |
| `/f admin power noloss <player>` | Attiva/disattiva l'immunita' alla penalita' di morte |
| `/f admin power nodecay <player>` | Attiva/disattiva l'immunita' al decadimento offline |
| `/f admin power info <player>` | Visualizza tutti gli override e i dettagli del potere |

## Potere Massimo Personalizzato

`/f admin power setmax <player> <amount>`
Imposta un tetto massimo di potere personale per il giocatore, sovrascrivendo il valore predefinito del server.

>[!INFO] Impostare un massimo personalizzato **non** cambia il potere attuale. Cambia solo il tetto. Il giocatore deve comunque guadagnare potere fino al nuovo limite.

## Modalita' No-Loss

`/f admin power noloss <player>`
Attiva/disattiva l'immunita' alla perdita di potere per morte. Quando abilitata, il giocatore **non** perdera' potere alla morte.

Utile per:
- Periodi di protezione nuovi giocatori
- Partecipanti ad eventi
- Membri dello staff

## Modalita' No-Decay

`/f admin power nodecay <player>`
Attiva/disattiva l'immunita' al decadimento del potere offline. Quando abilitata, il potere del giocatore **non** diminuira' mentre e' offline.

Utile per:
- Giocatori in congedo prolungato
- Membri VIP
- Protezione stagionale

## Info Potere

`/f admin power info <player>`
Mostra un dettaglio completo:

- Potere attuale e potere massimo
- Override attivi (noloss, nodecay, massimo personalizzato)
- Orario ultima morte e potere perso
- Percentuale di contributo alla fazione

>[!TIP] Tutti gli override del potere persistono attraverso i riavvii del server e sono salvati nel file dati del giocatore.
