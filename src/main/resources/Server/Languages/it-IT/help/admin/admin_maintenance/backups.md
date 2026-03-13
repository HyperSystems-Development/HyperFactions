---
id: admin_backups
---
# Sistema di Backup

HyperFactions include backup automatici e manuali con rotazione GFS (Nonno-Padre-Figlio).

## Comandi Backup

| Comando | Descrizione |
|---------|-------------|
| `/f admin backup create` | Crea un backup manuale ora |
| `/f admin backup list` | Elenca tutti i backup disponibili |
| `/f admin backup restore <name>` | Ripristina da un backup |
| `/f admin backup delete <name>` | Elimina un backup specifico |

**Permesso**: `hyperfactions.admin.backup`

## Valori Predefiniti Rotazione GFS

| Tipo | Conservazione | Descrizione |
|------|---------------|-------------|
| Orario | 24 | Ultimi 24 snapshot orari |
| Giornaliero | 7 | Ultimi 7 snapshot giornalieri |
| Settimanale | 4 | Ultimi 4 snapshot settimanali |
| Manuale | 10 | Backup creati manualmente |
| Spegnimento | 5 | Creati allo stop del server |

>[!INFO] I backup allo spegnimento sono abilitati per impostazione predefinita (`onShutdown=true`). Catturano lo stato piu' recente prima dell'arresto del server.

## Contenuti del Backup

Ogni archivio ZIP di backup contiene:
- Tutti i file dati delle fazioni
- Dati potere dei giocatori
- Definizioni delle zone
- Storico chat e dati economia
- Dati inviti e richieste di adesione
- File di configurazione

>[!WARNING] **Il ripristino di un backup e' distruttivo.** Sostituisce tutti i dati attuali con i contenuti del backup. Qualsiasi modifica fatta dopo la creazione del backup andra' persa. Crea sempre un backup fresco prima di ripristinare.

## Buone Pratiche

1. Crea un backup manuale prima di azioni admin importanti
2. Controlla la conservazione dei backup in `backup.json`
3. Testa il ripristino su un server di staging prima
4. Mantieni i backup allo spegnimento abilitati per il recupero da crash
