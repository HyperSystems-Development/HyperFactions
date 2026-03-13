---
id: power_understanding
commands: power
---
# Entendendo o Poder

Poder é o recurso principal que determina quanto território sua facção pode manter. Cada jogador tem poder pessoal que contribui para o total da facção.

---

## Valores Padrão de Poder

| Configuração | Valor |
|--------------|-------|
| Poder máximo por jogador | 20 |
| Poder inicial | 10 |
| Penalidade por morte | -1.0 por morte |
| Recompensa por abate | 0.0 |
| Taxa de regeneração | +0.1 por minuto (enquanto online) |
| Custo de poder por reivindicação | 2.0 |
| Desconexão enquanto marcado | -1.0 adicional |

>[!NOTE] Estes são valores padrão. O administrador do seu servidor pode ter configurado valores diferentes.

## Como Funciona

O poder total da sua facção é a soma do poder pessoal de cada membro. O poder necessário é o número de reivindicações multiplicado por 2.0. Enquanto o poder total ficar acima do poder necessário, seu território está seguro.

>[!INFO] O poder regenera passivamente a 0.1 por minuto enquanto você estiver online. Nessa taxa, recuperar 1.0 de poder leva cerca de 10 minutos.

---

## Verificando Seu Poder

`/f power`

Mostra seu poder pessoal, o poder total da sua facção e quanto é necessário para manter as reivindicações atuais.

## A Zona de Perigo

Se o poder total cair abaixo da quantidade necessária para suas reivindicações, sua facção fica vulnerável. Inimigos podem tomar seus chunks.

>[!WARNING] Múltiplas mortes em um curto período podem escalar rapidamente. Se você tem 5 membros cada um com 10 de poder (50 total) e 20 reivindicações (40 necessários), apenas 5 mortes na equipe reduzem para 45 -- ainda seguro. Mas 11 mortes colocam em 39, abaixo do limite de 40.

>[!TIP] Mantenha uma margem de poder. Não reivindique cada chunk que puder pagar -- deixe espaço para algumas mortes sem ficar vulnerável.
