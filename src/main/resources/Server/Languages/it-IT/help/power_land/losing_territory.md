---
id: power_losing
commands: overclaim
---
# Perdere Territorio

Quando il potere totale di una fazione scende sotto il costo dei suoi claim, diventa attaccabile. I nemici possono sovra-reclamare i chunk togliendoteli da sotto i piedi.

---

## Come Funziona il Sovra-Claim

`/f overclaim`

Un Ufficiale o Leader di una fazione nemica si posiziona nel tuo chunk reclamato ed esegue questo comando. Se la tua fazione e' in deficit di potere, il chunk viene trasferito alla loro fazione.

## I Calcoli

Ogni claim costa 2.0 potere da mantenere. Se il tuo potere totale scende sotto quella soglia, i chunk in deficit sono vulnerabili.

>[!NOTE] Questi sono valori predefiniti. L'amministratore del tuo server potrebbe aver configurato impostazioni diverse.

>[!WARNING] Il sovra-claim e' permanente. Una volta che un nemico prende un chunk, devi reclamarlo di nuovo (o sovra-reclamarlo a tua volta se si indeboliscono).

---

## Scenario di Esempio

| Fattore | Valore |
|---------|--------|
| Membri | 5 giocatori |
| Potere per membro | 10 ciascuno (iniziale) |
| Potere totale | 50 |
| Claim | 30 chunk |
| Potere necessario (30 x 2.0) | 60 |
| Deficit | 10 potere in meno |

In questo esempio, la fazione e' gia' attaccabile fin dall'inizio. I nemici potrebbero sovra-reclamare fino a 5 chunk (10 deficit / 2.0 per claim) prima che la fazione raggiunga l'equilibrio.

---

## Come Prevenire il Sovra-Claim

- Non espanderti troppo -- mantieni sempre il potere totale sopra il costo dei claim con un margine
- Resta attivo -- il potere si rigenera solo mentre sei online (+0.1/min)
- Evita morti inutili -- ogni morte costa 1.0 potere
- Recluta piu' membri -- piu' giocatori significa piu' potere totale
- Rilascia i chunk inutilizzati -- libera potere con /f unclaim

>[!TIP] Controlla regolarmente il tuo stato di potere con /f power. Se il tuo potere totale e' vicino al costo dei claim, considera di rilasciare i chunk meno importanti prima di una guerra.
