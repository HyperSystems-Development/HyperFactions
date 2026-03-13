---
id: admin_getting_started
---
# Per Iniziare come Admin

Benvenuto nell'amministrazione di HyperFactions. Questa guida copre i tuoi primi passi dopo l'installazione del plugin.

## Aprire la Dashboard Admin

`/f admin`
Apre la GUI della dashboard admin con accesso a tutti gli strumenti di gestione, editor di zone e impostazioni del server.

>[!INFO] Hai bisogno del permesso **hyperfactions.admin.use** o dello stato OP per accedere ai comandi admin.

## Requisiti

- **Con un plugin di permessi**: Assegna `hyperfactions.admin.use`
- **Senza un plugin di permessi**: Il giocatore deve essere un
operatore del server (`adminRequiresOp=true` per impostazione predefinita)

## Primi Passi Dopo l'Installazione

1. Esegui `/f admin` per verificare il tuo accesso
2. Apri **Config** per rivedere le impostazioni predefinite della fazione
3. Crea una **SafeZone** allo spawn con `/f admin safezone Spawn`
4. Opzionalmente crea **WarZone** per arene PvP
5. Controlla le impostazioni di **Backup** per garantire la sicurezza dei dati

## Capacita' Admin

| Area | Cosa Puoi Fare |
|------|----------------|
| Fazioni | Ispezionare, modificare o forzare lo scioglimento di qualsiasi fazione |
| Zone | Creare SafeZone e WarZone con flag personalizzati |
| Potere | Sovrascrivere i valori di potere di giocatori/fazioni |
| Economia | Gestire i tesori delle fazioni e il mantenimento |
| Configurazione | Modificare le impostazioni in tempo reale tramite GUI o ricaricare da disco |
| Backup | Creare, ripristinare e gestire backup dei dati |
| Importazioni | Migrare dati da altri plugin di fazioni |

>[!TIP] Usa `/f admin --text` per ottenere output basato su chat invece della GUI, utile per console o automazione.
