---
id: admin_backups
---
# Sistema de Backup

HyperFactions inclui backups automáticos e manuais com rotação GFS (Avô-Pai-Filho).

## Comandos de Backup

| Comando | Descrição |
|---------|-----------|
| `/f admin backup create` | Criar um backup manual agora |
| `/f admin backup list` | Listar todos os backups disponíveis |
| `/f admin backup restore <name>` | Restaurar a partir de um backup |
| `/f admin backup delete <name>` | Excluir um backup específico |

**Permissão**: `hyperfactions.admin.backup`

## Padrões de Rotação GFS

| Tipo | Retenção | Descrição |
|------|----------|-----------|
| Por hora | 24 | Últimos 24 snapshots por hora |
| Diário | 7 | Últimos 7 snapshots diários |
| Semanal | 4 | Últimos 4 snapshots semanais |
| Manual | 10 | Backups criados manualmente |
| Desligamento | 5 | Criados ao parar o servidor |

>[!INFO] Backups de desligamento são ativados por padrão (`onShutdown=true`). Eles capturam o estado mais recente antes do servidor parar.

## Conteúdo do Backup

Cada arquivo ZIP de backup contém:
- Todos os arquivos de dados de facção
- Dados de poder dos jogadores
- Definições de zonas
- Histórico de chat e dados de economia
- Dados de convites e solicitações de entrada
- Arquivos de configuração

>[!WARNING] **Restaurar um backup é destrutivo.** Ele substitui todos os dados atuais pelo conteúdo do backup. Quaisquer alterações feitas após a criação do backup serão perdidas. Sempre crie um backup novo antes de restaurar.

## Boas Práticas

1. Crie um backup manual antes de ações importantes de admin
2. Revise a retenção de backups em `backup.json`
3. Teste a restauração em um servidor de testes primeiro
4. Mantenha backups de desligamento ativados para recuperação de falhas
