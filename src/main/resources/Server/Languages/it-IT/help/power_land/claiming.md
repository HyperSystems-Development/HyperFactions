---
id: power_claiming
commands: claim, unclaim
---
# Reclamare Territorio

Reclamare un chunk lo protegge sotto il controllo della tua fazione. Solo i membri della fazione possono costruire, distruggere o accedere ai contenitori nel territorio reclamato.

---

## Come Reclamare

`/f claim`

Posizionati nel chunk che vuoi reclamare ed esegui questo comando. Il chunk viene immediatamente protetto. Richiede il grado di Ufficiale o superiore.

## Come Rilasciare

`/f unclaim`

Rilascia il chunk in cui ti trovi riportandolo a natura selvaggia. Richiede anch'esso Ufficiale+.

---

## Regole di Claim

| Regola | Predefinito |
|--------|-------------|
| Costo in potere per claim | 2.0 potere |
| Claim massimi | 100 per fazione |
| Solo adiacenti | No (puoi reclamare ovunque) |

>[!NOTE] Questi sono valori predefiniti. L'amministratore del tuo server potrebbe aver configurato impostazioni diverse.

>[!INFO] Ogni claim costa 2.0 potere da mantenere. Una fazione con 50 potere totale puo' mantenere fino a 25 claim in sicurezza.

---

## Cosa Fornisce la Protezione

All'interno del territorio reclamato, le seguenti regole sono applicate per impostazione predefinita:

- Gli esterni non possono distruggere, piazzare o interagire con i blocchi
- Gli alleati possono usare porte, sedili e trasporti ma non possono distruggere o piazzare blocchi
- Membri e Ufficiali hanno pieno accesso per costruire, distruggere e usare tutto
- L'accesso ai contenitori (casse, bauli) e' limitato ai soli membri

>[!TIP] Puoi anche reclamare direttamente dalla mappa del territorio. Apri /f map e clicca sui chunk non reclamati per reclamarli.

>[!WARNING] Non espanderti troppo. Se la tua fazione perde potere a causa delle morti, i claim oltre il tuo budget di potere diventano vulnerabili al sovra-claim.
