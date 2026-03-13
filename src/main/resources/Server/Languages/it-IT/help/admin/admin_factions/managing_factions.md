---
id: admin_managing_factions
---
# Gestione delle Fazioni

Gli admin possono ispezionare e modificare qualsiasi fazione sul server tramite la dashboard o i comandi.

## Sfogliare le Fazioni

`/f admin factions`
Apre il browser admin delle fazioni. Visualizza tutte le fazioni con numero di membri, livelli di potere e territorio.

`/f admin info <faction>`
Apre il pannello info admin per una fazione specifica con tutti i dettagli e le opzioni di gestione.

## Modificare le Impostazioni della Fazione

Con il permesso `hyperfactions.admin.modify`, puoi:

- **Rinominare** una fazione per risolvere conflitti
- **Impostare il colore** per risolvere problemi di visualizzazione
- **Attivare/disattivare aperta/chiusa** per sovrascrivere la politica di adesione
- **Modificare la descrizione** per scopi di moderazione

>[!TIP] Usa `/f admin who <player>` per cercare a quale fazione appartiene un giocatore specifico e visualizzare i suoi dettagli.

## Visualizzare Membri e Relazioni

Il pannello info admin mostra:

| Sezione | Dettagli |
|---------|----------|
| **Membri** | Roster completo con ruoli e ultimo accesso |
| **Relazioni** | Tutti gli stati di alleato, nemico e neutrale |
| **Territorio** | Chunk reclamati e bilancio di potere |
| **Economia** | Saldo del tesoro e log delle transazioni |

>[!NOTE] I comandi di ispezione admin non notificano la fazione che viene visualizzata. Solo le modifiche attivano gli avvisi.
