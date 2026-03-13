---
id: admin_treasury_management
---
# Gerenciamento do Tesouro

Comandos de admin para gerenciar tesouros de facção. Requer a permissão `hyperfactions.admin.economy`.

## Comandos do Tesouro

| Comando | Descrição |
|---------|-----------|
| `/f admin economy balance <faction>` | Ver saldo do tesouro da facção |
| `/f admin economy set <faction> <amount>` | Definir saldo exato |
| `/f admin economy add <faction> <amount>` | Adicionar fundos ao tesouro |
| `/f admin economy take <faction> <amount>` | Remover fundos do tesouro |
| `/f admin economy reset <faction>` | Resetar tesouro para zero |

## Exemplos

- `/f admin economy balance Vikings` -- verificar saldo
- `/f admin economy set Vikings 5000` -- definir para 5000
- `/f admin economy add Vikings 1000` -- depositar 1000
- `/f admin economy take Vikings 500` -- sacar 500
- `/f admin economy reset Vikings` -- zerar saldo

>[!TIP] Use `/f admin info <faction>` para ver a visão geral completa da economia incluindo histórico de transações junto com o saldo do tesouro.

## Casos de Uso

| Cenário | Comando |
|---------|---------|
| Distribuição de prêmio de evento | `economy add <faction> <prize>` |
| Penalidade por violação de regra | `economy take <faction> <fine>` |
| Reset de economia após wipe | `economy reset <faction>` |
| Compensação por bugs | `economy add <faction> <amount>` |

>[!WARNING] Alterações no tesouro são registradas no histórico de transações da facção. Modificações de admin são registradas com o nome do admin para prestação de contas.

>[!NOTE] Todos os comandos de admin de economia funcionam mesmo quando o módulo de economia está desativado na configuração. Os dados são armazenados independentemente do status do módulo.
