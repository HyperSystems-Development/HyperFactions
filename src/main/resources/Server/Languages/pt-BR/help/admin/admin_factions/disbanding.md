---
id: admin_disbanding
---
# Dissolução Forçada

Admins podem dissolver forçadamente qualquer facção, independentemente da vontade do líder.

## Comando

`/f admin disband <faction>`
Dissolve forçadamente a facção nomeada. Uma confirmação aparecerá antes da ação ser executada.

**Permissão**: `hyperfactions.admin.disband`

>[!WARNING] Dissolver uma facção é **irreversível**. Todas as reivindicações são liberadas, todos os membros são removidos, e a facção deixa de existir. Crie um backup antes.

## Consequências

Quando uma facção é dissolvida:

| Efeito | Descrição |
|--------|-----------|
| **Reivindicações** | Todo o território é liberado imediatamente |
| **Membros** | Todos os jogadores são removidos da lista |
| **Relações** | Todas as alianças e inimizades são removidas |
| **Tesouro** | Tratado conforme configurações de economia |
| **Base** | A base da facção é excluída |
| **Chat** | O histórico de chat da facção é removido |

## Boas Práticas

1. Sempre execute `/f admin backup create` antes de dissolver
2. Notifique os membros da facção quando possível
3. Documente o motivo para os registros do servidor
4. Verifique `/f admin info <faction>` para revisar antes de agir

>[!TIP] Se o problema é com um membro específico, considere usar a GUI de admin de facções para transferir a liderança em vez de dissolver a facção inteira.
