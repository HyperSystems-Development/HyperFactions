---
id: admin_integrations
---
# Integrazioni Plugin

HyperFactions si integra con diversi plugin esterni tramite dipendenze soft. Tutte le integrazioni sono opzionali e gestiscono l'assenza in modo trasparente.

## Controllare lo Stato delle Integrazioni

`/f admin version`
Mostra la versione attuale e le integrazioni rilevate.

`/f admin integration`
Apre il pannello di gestione integrazioni con stato dettagliato per ogni plugin rilevato.

## Tabella Integrazioni

| Plugin | Tipo | Descrizione |
|--------|------|-------------|
| **HyperPerms** | Permessi | Sistema completo di permessi con gruppi, ereditarieta' e contesto |
| **LuckPerms** | Permessi | Provider di permessi alternativo |
| **VaultUnlocked** | Permessi/Economia | Bridge per permessi ed economia |
| **HyperProtect-Mixin** | Protezione | Abilita flag avanzati delle zone (esplosioni, fuoco, conservazione inventario) |
| **OrbisGuard-Mixins** | Protezione | Mixin alternativo per l'applicazione dei flag zone |
| **PlaceholderAPI** | Placeholder | 49 placeholder fazione per altri plugin |
| **WiFlow PlaceholderAPI** | Placeholder | Provider di placeholder alternativo |
| **GravestonePlugin** | Morte | Controllo accesso tombe nelle zone |
| **HyperEssentials** | Funzionalita' | Flag zone per home, warp e kit |
| **KyuubiSoft Core** | Framework | Integrazione libreria core |
| **Sentry** | Monitoraggio | Tracciamento errori e diagnostica |

## Priorita' Provider Permessi

1. **VaultUnlocked** (priorita' massima)
2. **HyperPerms**
3. **LuckPerms**
4. **Fallback OP** (se nessun provider trovato)

>[!INFO] Le integrazioni vengono rilevate una volta all'avvio tramite reflection. I risultati vengono memorizzati per la sessione. E' necessario un riavvio del server dopo aver aggiunto o rimosso un plugin integrato.

>[!TIP] Usa `/f admin debug toggle integration` per abilitare il logging dettagliato delle integrazioni per la risoluzione dei problemi.

>[!NOTE] HyperProtect-Mixin e' il mixin di protezione **raccomandato**. Senza di esso, 15 flag delle zone non avranno effetto.
