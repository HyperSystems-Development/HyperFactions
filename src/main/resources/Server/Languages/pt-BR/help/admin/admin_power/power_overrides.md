---
id: admin_power_overrides
---
# Sobrescritas de Poder

Comandos especiais de poder que alteram o comportamento do poder para jogadores ou facções específicos.

## Comandos de Sobrescrita

| Comando | Descrição |
|---------|-----------|
| `/f admin power setmax <player> <amount>` | Definir limite máximo de poder personalizado |
| `/f admin power noloss <player>` | Alternar imunidade à penalidade de morte |
| `/f admin power nodecay <player>` | Alternar imunidade ao decaimento de poder offline |
| `/f admin power info <player>` | Ver todas as sobrescritas e detalhes de poder |

## Poder Máximo Personalizado

`/f admin power setmax <player> <amount>`
Define um limite máximo de poder pessoal para o jogador, sobrescrevendo o padrão do servidor.

>[!INFO] Definir um máximo personalizado **não** altera o poder atual. Apenas muda o teto. O jogador ainda precisa ganhar poder até o novo limite.

## Modo Sem Perda

`/f admin power noloss <player>`
Alterna a imunidade à perda de poder por morte. Quando ativado, o jogador **não** perderá poder ao morrer.

Útil para:
- Períodos de proteção para novos jogadores
- Participantes de eventos
- Membros do staff

## Modo Sem Decaimento

`/f admin power nodecay <player>`
Alterna a imunidade ao decaimento de poder offline. Quando ativado, o poder do jogador **não** diminuirá enquanto offline.

Útil para:
- Jogadores em ausência prolongada
- Membros VIP
- Proteção sazonal

## Informações de Poder

`/f admin power info <player>`
Mostra um detalhamento completo:

- Poder atual e poder máximo
- Sobrescritas ativas (noloss, nodecay, máximo personalizado)
- Hora da última morte e poder perdido
- Percentual de contribuição para a facção

>[!TIP] Todas as sobrescritas de poder persistem entre reinícios do servidor e são armazenadas no arquivo de dados do jogador.
