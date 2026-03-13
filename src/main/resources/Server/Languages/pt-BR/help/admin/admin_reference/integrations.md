---
id: admin_integrations
---
# Integrações com Plugins

HyperFactions se integra com vários plugins externos através de dependências opcionais. Todas as integrações são opcionais e falham graciosamente se não estiverem disponíveis.

## Verificando o Status das Integrações

`/f admin version`
Mostra a versão atual e integrações detectadas.

`/f admin integration`
Abre o painel de gerenciamento de integrações com status detalhado para cada plugin detectado.

## Tabela de Integrações

| Plugin | Tipo | Descrição |
|--------|------|-----------|
| **HyperPerms** | Permissões | Sistema completo de permissões com grupos, herança e contexto |
| **LuckPerms** | Permissões | Provedor alternativo de permissões |
| **VaultUnlocked** | Permissões/Economia | Ponte de permissões e economia |
| **HyperProtect-Mixin** | Proteção | Habilita flags avançadas de zona (explosões, fogo, manter inventário) |
| **OrbisGuard-Mixins** | Proteção | Mixin alternativo para aplicação de flags de zona |
| **PlaceholderAPI** | Placeholders | 49 placeholders de facção para outros plugins |
| **WiFlow PlaceholderAPI** | Placeholders | Provedor alternativo de placeholders |
| **GravestonePlugin** | Morte | Controle de acesso a lápides em zonas |
| **HyperEssentials** | Recursos | Flags de zona para homes, warps e kits |
| **KyuubiSoft Core** | Framework | Integração com biblioteca core |
| **Sentry** | Monitoramento | Rastreamento de erros e diagnósticos |

## Prioridade do Provedor de Permissões

1. **VaultUnlocked** (prioridade mais alta)
2. **HyperPerms**
3. **LuckPerms**
4. **Fallback de OP** (se nenhum provedor encontrado)

>[!INFO] As integrações são detectadas uma vez na inicialização usando reflexão. Os resultados são cacheados para a sessão. É necessário reiniciar o servidor após adicionar ou remover um plugin integrado.

>[!TIP] Use `/f admin debug toggle integration` para habilitar log detalhado de integração para solução de problemas.

>[!NOTE] HyperProtect-Mixin é o mixin de proteção **recomendado**. Sem ele, 15 flags de zona não terão efeito.
