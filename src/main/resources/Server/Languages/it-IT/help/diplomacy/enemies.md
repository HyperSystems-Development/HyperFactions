---
id: diplomacy_enemies
commands: enemy, neutral
---
# Fazioni Nemiche

Dichiarare un nemico e' un'azione unilaterale che abilita immediatamente il PvP e l'aggressione territoriale contro la fazione bersaglio. Non e' richiesto alcun accordo.

---

## Dichiarare un Nemico

`/f enemy <faction>`

Segna istantaneamente la fazione bersaglio come tuo nemico. Ha effetto immediato -- nessuna conferma dall'altra parte e' necessaria. Richiede il grado di Ufficiale o superiore.

## Ripristinare a Neutrale

`/f neutral <faction>`

Termina lo stato di nemico e riporta la relazione a neutrale. Richiede anch'esso Ufficiale+ e ha effetto immediato.

---

## Cosa Abilita lo Stato di Nemico

| Effetto | Dettagli |
|---------|----------|
| PvP nel territorio | Il PvP completo e' abilitato nel territorio di entrambe le fazioni |
| Sovra-claim | Puoi sovra-reclamare i loro chunk se sono in deficit di potere |
| Segnalazione sulla mappa | Il territorio nemico appare in rosso sulla mappa del territorio |
| Nessuna protezione | La protezione territoriale standard non impedisce il PvP nemico |

>[!WARNING] Dichiarare un nemico e' una decisione seria. Anche i loro membri possono combatterti nel tuo stesso territorio una volta che dichiari.

---

## Considerazioni Strategiche

- Le dichiarazioni di nemico sono unilaterali -- puoi dichiarare senza il loro consenso, ma anche loro ti vedranno come ostile
- Prima di dichiarare, controlla il potere del bersaglio con /f info. Se sono forti, potresti perdere territorio invece tu
- Indebolisci i nemici attraverso combattimenti ripetuti per drenare il loro potere, poi sovra-reclama il loro terreno
- Non c'e' limite al numero di nemici che puoi avere, ma combattere su piu' fronti e' rischioso

>[!TIP] Usa /f neutral per de-escalare i conflitti. A volte una pace strategica e' piu' preziosa di una guerra continua.

>[!NOTE] Se sei alleato con una fazione e la dichiari nemica, l'alleanza viene rotta prima.
