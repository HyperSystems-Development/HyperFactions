---
id: admin_zone_commands
---
# Riferimento Comandi Zone

Riferimento completo per tutti i comandi di gestione zone. Tutti richiedono il permesso `hyperfactions.admin.zones`.

## Creazione Rapida

| Comando | Descrizione |
|---------|-------------|
| `/f admin safezone <name>` | Crea una SafeZone nel chunk corrente |
| `/f admin warzone <name>` | Crea una WarZone nel chunk corrente |
| `/f admin removezone <name>` | Elimina una zona e rilascia i chunk |

## Gestione Zone

| Comando | Descrizione |
|---------|-------------|
| `/f admin zone create <name> <type>` | Crea una zona (safezone/warzone) |
| `/f admin zone delete <name>` | Elimina una zona |
| `/f admin zone claim <zone>` | Aggiungi il chunk corrente alla zona |
| `/f admin zone unclaim <zone>` | Rimuovi il chunk corrente dalla zona |
| `/f admin zone radius <zone> <r>` | Reclama un raggio quadrato di chunk |
| `/f admin zone list` | Elenca tutte le zone con conteggio chunk |
| `/f admin zone notify <zone> <true/false>` | Attiva/disattiva messaggi di ingresso/uscita |
| `/f admin zone title <zone> upper/lower <text>` | Imposta il testo del titolo della zona |
| `/f admin zone properties <zone>` | Apri la GUI proprieta' della zona |

## Gestione Flag

| Comando | Descrizione |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Imposta un flag specifico |

>[!TIP] Usa la **GUI proprieta'** della zona per un editor visuale con interruttori per ogni flag, organizzati per categoria.

## Esempi

- `/f admin safezone Spawn` -- crea protezione spawn
- `/f admin zone radius Spawn 3` -- espandi a 7x7 chunk
- `/f admin zoneflag Spawn door_use true` -- permetti le porte
- `/f admin zone notify Spawn true` -- mostra messaggi di ingresso
