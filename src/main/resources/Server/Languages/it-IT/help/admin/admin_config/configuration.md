---
id: admin_configuration
---
# Sistema di Configurazione

HyperFactions utilizza un sistema di configurazione JSON modulare con 11 file di configurazione.

## Comandi Configurazione Admin

| Comando | Descrizione |
|---------|-------------|
| `/f admin config` | Apri la GUI dell'editor visuale di configurazione |
| `/f admin reload` | Ricarica tutti i file di configurazione dal disco |
| `/f admin sync` | Sincronizza i dati delle fazioni con lo storage |

## File di Configurazione

| File | Contenuti |
|------|----------|
| `factions.json` | Ruoli, potere, claim, combattimento, relazioni |
| `server.json` | Teletrasporto, salvataggio automatico, messaggi, GUI, permessi |
| `economy.json` | Tesoro, mantenimento, impostazioni transazioni |
| `backup.json` | Rotazione backup e impostazioni di conservazione |
| `chat.json` | Formattazione chat fazione e alleati |
| `debug.json` | Categorie di log debug |
| `faction-permissions.json` | Permessi predefiniti per ruolo |
| `announcements.json` | Notifiche eventi e territorio |
| `gravestones.json` | Impostazioni integrazione tombe |
| `worldmap.json` | Modalita' aggiornamento mappa mondo |
| `worlds.json` | Override comportamento per mondo |

>[!TIP] La GUI di configurazione fornisce un editor visuale con descrizioni per ogni impostazione. Le modifiche vengono salvate immediatamente ma alcune richiedono `/f admin reload` per avere pieno effetto.

## Posizione Configurazione

Tutti i file sono salvati in:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Le modifiche manuali al JSON richiedono `/f admin reload` per essere applicate. Un JSON non valido causera' il salto del file con un avviso nel log del server.

>[!NOTE] La versione della configurazione e' tracciata in `server.json`. Il plugin migra automaticamente le configurazioni piu' vecchie all'avvio.
