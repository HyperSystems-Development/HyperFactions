---
id: admin_zone_basics
---
# Basi delle Zone

Le zone sono territori controllati dagli admin con regole personalizzate che sovrascrivono la normale protezione delle fazioni.

## Tipi di Zona

- **SafeZone** -- Niente PvP, niente costruzione, niente danni.
Ideale per aree di spawn e hub commerciali.
- **WarZone** -- PvP sempre abilitato, niente costruzione.
Ideale per arene e aree di battaglia contese.

## Creare Zone

`/f admin safezone <name>`
Crea una SafeZone e reclama il tuo chunk corrente.

`/f admin warzone <name>`
Crea una WarZone e reclama il tuo chunk corrente.

Dopo la creazione, posizionati in chunk aggiuntivi e usa `/f admin zone claim <zone>` per espandere la zona.

## Gestire i Chunk della Zona

`/f admin zone claim <zone>`
Aggiungi il chunk corrente alla zona indicata.

`/f admin zone unclaim <zone>`
Rimuovi il chunk corrente dalla zona indicata.

`/f admin zone radius <zone> <radius>`
Reclama un quadrato di chunk intorno alla tua posizione.

## Eliminare Zone

`/f admin removezone <name>`
Elimina permanentemente la zona e rilascia tutti i suoi chunk reclamati.

>[!WARNING] Eliminare una zona rilascia tutti i suoi chunk istantaneamente. Questa operazione non puo' essere annullata senza un ripristino da backup.

>[!INFO] Le regole delle zone **sovrascrivono sempre** le regole del territorio delle fazioni. Una SafeZone all'interno di territorio nemico e' comunque sicura.
