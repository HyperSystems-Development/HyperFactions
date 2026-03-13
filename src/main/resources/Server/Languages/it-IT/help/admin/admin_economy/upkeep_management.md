---
id: admin_upkeep_management
---
# Gestione del Mantenimento

Il mantenimento delle fazioni addebita le fazioni periodicamente in base al loro territorio e numero di membri.

## Controlli Admin

Le impostazioni di mantenimento sono gestite attraverso il file di configurazione economia o la GUI di configurazione admin.

`/f admin config`
Apri l'editor di configurazione e naviga alle impostazioni economia per regolare i valori di mantenimento.

## Impostazioni Predefinite del Mantenimento

| Impostazione | Predefinito | Descrizione |
|--------------|-------------|-------------|
| Mantenimento abilitato | false | Interruttore principale del sistema |
| Intervallo mantenimento | 24h | Quanto spesso viene addebitato il mantenimento |
| Costo per claim | 5.0 | Costo per chunk reclamato per ciclo |
| Costo per membro | 0.0 | Costo per membro per ciclo |
| Periodo di grazia | 72h | Le nuove fazioni sono esenti |
| Scioglimento per bancarotta | false | Scioglimento automatico se non puo' pagare |

## Monitorare il Mantenimento

Usa `/f admin info <faction>` per vedere:
- Saldo attuale del tesoro
- Costo stimato di mantenimento per ciclo
- Tempo fino al prossimo addebito di mantenimento
- Se la fazione puo' permettersi il mantenimento

>[!TIP] Controlla le statistiche economiche di tutte le fazioni dalla dashboard admin per identificare le fazioni a rischio di bancarotta prima che il mantenimento venga addebitato.

>[!INFO] La configurazione del mantenimento e' salvata in `economy.json`. Le modifiche fatte tramite la GUI di configurazione hanno effetto dopo il ricaricamento con `/f admin reload`.

## Formula del Mantenimento

**Mantenimento totale** = (chunk reclamati x costo per claim) + (numero membri x costo per membro)

>[!WARNING] Abilitare il mantenimento su un server con fazioni esistenti potrebbe causare bancarotte inaspettate. Considera di impostare un periodo di grazia o annunciare il cambiamento in anticipo.
