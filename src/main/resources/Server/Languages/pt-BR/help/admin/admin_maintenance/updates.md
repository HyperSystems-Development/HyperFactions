---
id: admin_updates
---
# Verificação de Atualizações

HyperFactions pode verificar por novas versões e gerenciar a dependência HyperProtect-Mixin.

## Comandos de Atualização

| Comando | Descrição |
|---------|-----------|
| `/f admin update` | Verificar atualizações do HyperFactions |
| `/f admin update mixin` | Verificar/baixar HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Alternar download automático |
| `/f admin version` | Mostrar versão atual e informações de build |

## Canais de Lançamento

| Canal | Descrição |
|-------|-----------|
| **Stable** | Recomendado para servidores de produção |
| **Pre-release** | Acesso antecipado a recursos futuros |

>[!INFO] O verificador de atualizações apenas notifica sobre novas versões. Ele **não** instala atualizações do HyperFactions automaticamente.

## HyperProtect-Mixin

HyperProtect-Mixin é o mixin de proteção recomendado que habilita flags avançadas de zona (explosões, propagação de fogo, manter inventário, etc.).

- `/f admin update mixin` verifica a versão mais recente
e baixa se uma versão mais nova estiver disponível
- O download automático pode ser ativado ou desativado por servidor

>[!TIP] Após baixar uma nova versão do mixin, é necessário reiniciar o servidor para que as alterações entrem em vigor.

## Procedimento de Rollback

Se uma atualização causar problemas:

1. Pare o servidor
2. Substitua o JAR do plugin pela versão anterior
3. Inicie o servidor
4. Verifique o funcionamento com `/f admin version`

>[!WARNING] Fazer downgrade pode requerer um reset de migração de configuração. Sempre mantenha backups antes de atualizar.
