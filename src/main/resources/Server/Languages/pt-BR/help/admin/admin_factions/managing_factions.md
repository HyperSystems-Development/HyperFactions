---
id: admin_managing_factions
---
# Gerenciando Facções

Admins podem inspecionar e modificar qualquer facção no servidor através do painel ou comandos.

## Navegando por Facções

`/f admin factions`
Abre o navegador de facções do admin. Veja todas as facções com contagem de membros, níveis de poder e território.

`/f admin info <faction>`
Abre o painel de informações do admin para uma facção específica com todos os detalhes e opções de gerenciamento.

## Modificando Configurações da Facção

Com a permissão `hyperfactions.admin.modify`, você pode:

- **Renomear** uma facção para resolver conflitos
- **Definir cor** para corrigir problemas de exibição
- **Alternar aberta/fechada** para sobrescrever a política de entrada
- **Editar descrição** para fins de moderação

>[!TIP] Use `/f admin who <player>` para descobrir a qual facção um jogador específico pertence e ver seus detalhes.

## Visualizando Membros e Relações

O painel de informações do admin mostra:

| Seção | Detalhes |
|-------|----------|
| **Membros** | Lista completa com cargos e última vez visto |
| **Relações** | Todas as posições de aliado, inimigo e neutro |
| **Território** | Chunks reivindicados e balanço de poder |
| **Economia** | Saldo do tesouro e log de transações |

>[!NOTE] Comandos de inspeção de admin não notificam a facção sendo visualizada. Apenas modificações disparam alertas.
