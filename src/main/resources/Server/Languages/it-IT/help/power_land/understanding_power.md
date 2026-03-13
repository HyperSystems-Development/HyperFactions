---
id: power_understanding
commands: power
---
# Comprendere il Potere

Il potere e' la risorsa fondamentale che determina quanto territorio la tua fazione puo' mantenere. Ogni giocatore ha un potere personale che contribuisce al totale della fazione.

---

## Valori Predefiniti del Potere

| Impostazione | Valore |
|--------------|--------|
| Potere massimo per giocatore | 20 |
| Potere iniziale | 10 |
| Penalita' morte | -1.0 per morte |
| Ricompensa uccisione | 0.0 |
| Tasso di rigenerazione | +0.1 al minuto (mentre online) |
| Costo potere per claim | 2.0 |
| Disconnessione mentre taggato | -1.0 aggiuntivo |

>[!NOTE] Questi sono valori predefiniti. L'amministratore del tuo server potrebbe aver configurato impostazioni diverse.

## Come Funziona

Il potere totale della tua fazione e' la somma del potere personale di ogni membro. Il potere richiesto e' il numero di claim moltiplicato per 2.0. Finche' il potere totale resta sopra il potere richiesto, il tuo territorio e' al sicuro.

>[!INFO] Il potere si rigenera passivamente a 0.1 al minuto mentre sei online. A quel ritmo, recuperare 1.0 potere richiede circa 10 minuti.

---

## Controllare il Tuo Potere

`/f power`

Mostra il tuo potere personale, il potere totale della fazione e quanto e' necessario per mantenere i claim attuali.

## La Zona di Pericolo

Se il potere totale scende sotto la quantita' richiesta per i tuoi claim, la tua fazione diventa vulnerabile. I nemici possono sovra-reclamare i tuoi chunk.

>[!WARNING] Morti multiple in un breve periodo possono accumulare conseguenze rapidamente. Se hai 5 membri ciascuno con 10 potere (50 totale) e 20 claim (40 necessari), appena 5 morti nel tuo team ti portano a 45 -- ancora al sicuro. Ma 11 morti ti portano a 39, sotto la soglia di 40.

>[!TIP] Mantieni un margine di potere. Non reclamare ogni chunk che puoi permetterti -- lascia spazio per qualche morte senza diventare attaccabile.
