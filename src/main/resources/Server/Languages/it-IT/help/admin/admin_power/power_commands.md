---
id: admin_power_commands
---
# Comandi Admin Potere

Sovrascrivi i valori di potere di giocatori e fazioni. Tutti i comandi richiedono il permesso `hyperfactions.admin.power`.

## Comandi Potere Giocatore

| Comando | Descrizione |
|---------|-------------|
| `/f admin power set <player> <amount>` | Imposta il valore esatto di potere |
| `/f admin power add <player> <amount>` | Aggiunge potere al giocatore |
| `/f admin power remove <player> <amount>` | Rimuove potere dal giocatore |
| `/f admin power reset <player>` | Ripristina al potere iniziale predefinito |
| `/f admin power info <player>` | Visualizza il dettaglio completo del potere |

## Come il Potere Influisce sulle Fazioni

Il potere totale di una fazione e' la somma del potere individuale di tutti i suoi membri. I claim territoriali richiedono un potere totale sufficiente per essere mantenuti.

| Scenario | Effetto |
|----------|---------|
| Potere impostato piu' alto | La fazione puo' reclamare piu' territorio |
| Potere impostato piu' basso | La fazione potrebbe diventare vulnerabile al sovra-claim |
| Potere resettato | Riporta il giocatore al valore iniziale predefinito |

>[!WARNING] Ridurre il potere di un giocatore potrebbe causare alla sua fazione la perdita di territorio se il potere totale scende sotto il numero di chunk reclamati.

## Esempi

- `/f admin power set Steve 50` -- imposta a esattamente 50
- `/f admin power add Steve 10` -- aumenta di 10
- `/f admin power remove Steve 5` -- diminuisce di 5
- `/f admin power reset Steve` -- riporta al predefinito
- `/f admin power info Steve` -- mostra il dettaglio completo

>[!TIP] Usa `/f admin power info <player>` per vedere il potere attuale, il potere massimo e qualsiasi override attivo prima di apportare modifiche.
