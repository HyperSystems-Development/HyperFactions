---
id: economy_upkeep
---
# Manutenção Territorial

Facções devem pagar manutenção contínua para manter seu território reivindicado. Isso impede acúmulo de terras e mantém o mapa dinâmico.

## Custos de Manutenção

| Configuração | Padrão |
|--------------|--------|
| Custo por chunk | 2.0 por ciclo |
| Intervalo de pagamento | A cada 24 horas |
| Chunks gratuitos | 3 (sem custo) |
| Modo de escala | Taxa fixa |

>[!NOTE] Estes são valores padrão. O administrador do seu servidor pode ter configurado valores diferentes.

Seus primeiros 3 chunks são gratuitos. Além disso, cada chunk reivindicado adicional custa 2.0 por ciclo de pagamento.

## Pagamento Automático

O pagamento automático é ativado por padrão. O sistema deduz automaticamente a manutenção do seu tesouro a cada intervalo. Nenhuma ação manual necessária.

---

## Período de Carência

Se o seu tesouro não puder cobrir a manutenção, um período de carência de 48 horas começa. Um aviso é enviado 6 horas antes das reivindicações começarem a ser perdidas.

>[!WARNING] Se a manutenção permanecer não paga após o período de carência, sua facção perde 1 reivindicação por ciclo até que os custos sejam cobertos ou todas as reivindicações extras tenham acabado.

## Exemplo

*Uma facção com 8 reivindicações paga por 5 chunks (8 menos 3 gratuitos). A 2.0 por chunk, isso dá 10.0 por ciclo.*

>[!TIP] Mantenha seu tesouro acima do custo de manutenção. Use /f balance para verificar suas reservas.
