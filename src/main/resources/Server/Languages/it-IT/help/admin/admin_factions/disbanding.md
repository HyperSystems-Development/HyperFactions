---
id: admin_disbanding
---
# Scioglimento Forzato

Gli admin possono sciogliere forzatamente qualsiasi fazione, indipendentemente dalla volonta' del leader.

## Comando

`/f admin disband <faction>`
Scioglie forzatamente la fazione indicata. Apparira' un messaggio di conferma prima che l'azione venga eseguita.

**Permesso**: `hyperfactions.admin.disband`

>[!WARNING] Sciogliere una fazione e' **irreversibile**. Tutti i claim vengono rilasciati, tutti i membri vengono rimossi e la fazione cessa di esistere. Crea un backup prima.

## Conseguenze

Quando una fazione viene sciolta:

| Effetto | Descrizione |
|---------|-------------|
| **Claim** | Tutto il territorio viene rilasciato immediatamente |
| **Membri** | Tutti i giocatori vengono rimossi dal roster |
| **Relazioni** | Tutte le alleanze e le inimicizie vengono cancellate |
| **Tesoro** | Gestito secondo le impostazioni della configurazione economia |
| **Home** | La home della fazione viene eliminata |
| **Chat** | Lo storico della chat della fazione viene rimosso |

## Buone Pratiche

1. Esegui sempre `/f admin backup create` prima di sciogliere
2. Notifica i membri della fazione quando possibile
3. Documenta il motivo per i registri del server
4. Controlla `/f admin info <faction>` per rivedere prima di agire

>[!TIP] Se il problema e' con un membro specifico, considera di usare la GUI admin fazioni per trasferire la leadership piuttosto che sciogliere l'intera fazione.
