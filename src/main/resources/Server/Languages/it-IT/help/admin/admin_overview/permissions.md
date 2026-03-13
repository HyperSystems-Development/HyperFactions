---
id: admin_permissions
---
# Permessi Admin

Tutte le funzionalita' admin sono protette da nodi di permesso nel namespace `hyperfactions.admin`.

## Nodi di Permesso

| Permesso | Descrizione |
|----------|-------------|
| `hyperfactions.admin.*` | Concede **tutti** i permessi admin |
| `hyperfactions.admin.use` | Accesso alla dashboard `/f admin` |
| `hyperfactions.admin.reload` | Ricaricare i file di configurazione |
| `hyperfactions.admin.debug` | Attivare/disattivare le categorie di log debug |
| `hyperfactions.admin.zones` | Creare, modificare ed eliminare zone |
| `hyperfactions.admin.disband` | Forzare lo scioglimento di qualsiasi fazione |
| `hyperfactions.admin.modify` | Modificare le impostazioni di qualsiasi fazione |
| `hyperfactions.admin.bypass.limits` | Ignorare i limiti di claim e potere |
| `hyperfactions.admin.backup` | Creare e ripristinare backup |
| `hyperfactions.admin.power` | Sovrascrivere i valori di potere dei giocatori |
| `hyperfactions.admin.economy` | Gestire i tesori delle fazioni |

## Comportamento di Fallback

Quando **nessun plugin di permessi** e' installato, i permessi admin ricadono sullo stato di operatore del server (OP). Questo e' controllato da `adminRequiresOp` nella configurazione del server (predefinito: `true`).

>[!NOTE] Il wildcard `hyperfactions.admin.*` concede ogni permesso admin. Usa i nodi individuali per un controllo granulare sul tuo team di staff.

## Ordine di Risoluzione dei Permessi

1. Provider **VaultUnlocked** (se disponibile)
2. Provider **HyperPerms** (se disponibile)
3. Provider **LuckPerms** (se disponibile)
4. **Controllo OP** per i nodi admin (fallback)

>[!WARNING] Senza un plugin di permessi e con `adminRequiresOp` disabilitato, i comandi admin sono **aperti a tutti i giocatori**. Usa sempre un plugin di permessi in produzione.
