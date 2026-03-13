---
id: admin_upkeep_management
---
# Gerenciamento de Manutenção

A manutenção de facção cobra das facções periodicamente com base em seu território e número de membros.

## Controles de Admin

As configurações de manutenção são gerenciadas através do arquivo de configuração de economia ou pela GUI de configuração do admin.

`/f admin config`
Abra o editor de configuração e navegue até as configurações de economia para ajustar os valores de manutenção.

## Configurações Padrão de Manutenção

| Configuração | Padrão | Descrição |
|--------------|--------|-----------|
| Manutenção ativada | false | Botão mestre do sistema |
| Intervalo de manutenção | 24h | Frequência da cobrança |
| Custo por reivindicação | 5.0 | Custo por chunk reivindicado por ciclo |
| Custo por membro | 0.0 | Custo por membro por ciclo |
| Período de carência | 72h | Facções novas são isentas |
| Dissolver se falida | false | Dissolução automática se não puder pagar |

## Monitorando a Manutenção

Use `/f admin info <faction>` para ver:
- Saldo atual do tesouro
- Custo estimado de manutenção por ciclo
- Tempo até a próxima cobrança de manutenção
- Se a facção pode arcar com a manutenção

>[!TIP] Revise as estatísticas de economia de todas as facções pelo painel de admin para identificar facções em risco de falência antes que a manutenção seja cobrada.

>[!INFO] A configuração de manutenção é armazenada em `economy.json`. Alterações feitas pela GUI de configuração entram em vigor após recarregar com `/f admin reload`.

## Fórmula de Manutenção

**Manutenção total** = (chunks reivindicados x custo por reivindicação) + (número de membros x custo por membro)

>[!WARNING] Ativar a manutenção em um servidor com facções existentes pode causar falências inesperadas. Considere definir um período de carência ou anunciar a mudança com antecedência.
