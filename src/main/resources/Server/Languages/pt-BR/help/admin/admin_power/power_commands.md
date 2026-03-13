---
id: admin_power_commands
---
# Comandos Admin de Poder

Sobrescreva valores de poder de jogadores e facções. Todos os comandos requerem a permissão `hyperfactions.admin.power`.

## Comandos de Poder do Jogador

| Comando | Descrição |
|---------|-----------|
| `/f admin power set <player> <amount>` | Definir valor exato de poder |
| `/f admin power add <player> <amount>` | Adicionar poder ao jogador |
| `/f admin power remove <player> <amount>` | Remover poder do jogador |
| `/f admin power reset <player>` | Resetar para o poder inicial padrão |
| `/f admin power info <player>` | Ver detalhamento completo de poder |

## Como o Poder Afeta as Facções

O poder total de uma facção é a soma do poder individual de todos os seus membros. Reivindicações de território requerem poder total suficiente para serem mantidas.

| Cenário | Efeito |
|---------|--------|
| Poder definido mais alto | Facção pode reivindicar mais território |
| Poder definido mais baixo | Facção pode ficar vulnerável a tomadas |
| Poder resetado | Retorna o jogador ao valor inicial padrão |

>[!WARNING] Reduzir o poder de um jogador pode fazer sua facção perder território se o poder total cair abaixo do número de chunks reivindicados.

## Exemplos

- `/f admin power set Steve 50` -- definir para exatamente 50
- `/f admin power add Steve 10` -- aumentar em 10
- `/f admin power remove Steve 5` -- diminuir em 5
- `/f admin power reset Steve` -- voltar ao padrão
- `/f admin power info Steve` -- mostrar detalhamento completo

>[!TIP] Use `/f admin power info <player>` para ver o poder atual, poder máximo e quaisquer sobrescritas ativas antes de fazer alterações.
