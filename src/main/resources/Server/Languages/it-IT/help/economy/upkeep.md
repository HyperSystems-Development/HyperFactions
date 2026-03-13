---
id: economy_upkeep
---
# Mantenimento del Territorio

Le fazioni devono pagare un mantenimento continuo per conservare il territorio reclamato. Questo previene l'accumulo di terreni e mantiene la mappa dinamica.

## Costi di Mantenimento

| Impostazione | Predefinito |
|--------------|-------------|
| Costo per chunk | 2.0 per ciclo |
| Intervallo di pagamento | Ogni 24 ore |
| Chunk gratuiti | 3 (nessun costo) |
| Modalita' di scalatura | Tariffa fissa |

>[!NOTE] Questi sono valori predefiniti. L'amministratore del tuo server potrebbe aver configurato impostazioni diverse.

I tuoi primi 3 chunk sono gratuiti. Oltre a cio', ogni chunk reclamato aggiuntivo costa 2.0 per ciclo di pagamento.

## Pagamento Automatico

Il pagamento automatico e' abilitato per impostazione predefinita. Il sistema deduce automaticamente il mantenimento dal tuo tesoro ad ogni intervallo. Nessuna azione manuale necessaria.

---

## Periodo di Grazia

Se il tuo tesoro non puo' coprire il mantenimento, inizia un periodo di grazia di 48 ore. Un avviso viene inviato 6 ore prima che i claim inizino ad essere persi.

>[!WARNING] Se il mantenimento resta non pagato dopo il periodo di grazia, la tua fazione perde 1 claim per ciclo fino a quando i costi non sono coperti o tutti i claim extra sono stati rimossi.

## Esempio

*Una fazione con 8 claim paga per 5 chunk (8 meno 3 gratuiti). A 2.0 per chunk, sono 10.0 per ciclo.*

>[!TIP] Mantieni il tuo tesoro al di sopra del costo di mantenimento. Usa /f balance per controllare le tue riserve.
