---
id: combat_death
commands: home, sethome, stuck
---
# Morte e Recuperação

Morrer tem consequências reais em facções. Cada morte custa poder pessoal, enfraquecendo a capacidade da sua facção de manter território.

## Perda de Poder

Cada morte custa -1.0 de poder do seu total pessoal. Isso reduz o poder combinado da facção.

| Evento | Alteração de Poder |
|--------|-------------------|
| Morte (qualquer causa) | -1.0 |
| Regeneração online | +0.1 por minuto |
| Desconexão em combate | -1.0 (morto) |

>[!NOTE] Estes são valores padrão. O administrador do seu servidor pode ter configurado valores diferentes.

## Cenários de Exemplo

*5 membros com 10.0 de poder cada = 50 total, 20 reivindicações.*
*Um membro morre duas vezes: 8.0 de poder, total da facção 48.*
*Três membros morrem uma vez cada: total cai para 47.*

>[!WARNING] Se o poder da sua facção cair abaixo da contagem de reivindicações, inimigos podem tomar seu território.

## Recuperação

O poder regenera a 0.1 por minuto enquanto online. Recuperar 1.0 de poder perdido leva cerca de 10 minutos. Múltiplas mortes acumulam, então evite lutas repetidas.

---

## Todos os Tipos de Morte

A perda de poder se aplica a todas as mortes: PvP, mobs, dano de queda, afogamento e qualquer outra causa. Não existe maneira segura de morrer.

>[!TIP] Defina uma base da facção com /f sethome para que membros possam se reagrupar rapidamente após morrer.
