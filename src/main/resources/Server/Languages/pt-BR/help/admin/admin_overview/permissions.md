---
id: admin_permissions
---
# Permissões de Admin

Todas as funcionalidades de admin são protegidas por nós de permissão no namespace `hyperfactions.admin`.

## Nós de Permissão

| Permissão | Descrição |
|-----------|-----------|
| `hyperfactions.admin.*` | Concede **todas** as permissões de admin |
| `hyperfactions.admin.use` | Acessar o painel `/f admin` |
| `hyperfactions.admin.reload` | Recarregar arquivos de configuração |
| `hyperfactions.admin.debug` | Alternar categorias de log de debug |
| `hyperfactions.admin.zones` | Criar, editar e excluir zonas |
| `hyperfactions.admin.disband` | Dissolver forçadamente qualquer facção |
| `hyperfactions.admin.modify` | Modificar configurações de qualquer facção |
| `hyperfactions.admin.bypass.limits` | Ignorar limites de reivindicação e poder |
| `hyperfactions.admin.backup` | Criar e restaurar backups |
| `hyperfactions.admin.power` | Sobrescrever valores de poder dos jogadores |
| `hyperfactions.admin.economy` | Gerenciar tesouros de facção |

## Comportamento de Fallback

Quando **nenhum plugin de permissões** está instalado, as permissões de admin recorrem ao status de operador do servidor (OP). Isso é controlado por `adminRequiresOp` na configuração do servidor (padrão: `true`).

>[!NOTE] O curinga `hyperfactions.admin.*` concede todas as permissões de admin. Use nós individuais para controle granular sobre sua equipe de staff.

## Ordem de Resolução de Permissões

1. Provedor **VaultUnlocked** (se disponível)
2. Provedor **HyperPerms** (se disponível)
3. Provedor **LuckPerms** (se disponível)
4. **Verificação de OP** para nós de admin (fallback)

>[!WARNING] Sem um plugin de permissões e com `adminRequiresOp` desativado, comandos de admin ficam **abertos para todos os jogadores**. Sempre use um plugin de permissões em produção.
