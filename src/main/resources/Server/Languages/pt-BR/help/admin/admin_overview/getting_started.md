---
id: admin_getting_started
---
# Primeiros Passos como Admin

Bem-vindo à administração do HyperFactions. Este guia cobre seus primeiros passos após instalar o plugin.

## Abrindo o Painel de Admin

`/f admin`
Abre a GUI do painel de administração com acesso a todas as ferramentas de gerenciamento, editores de zona e configurações do servidor.

>[!INFO] Você precisa da permissão **hyperfactions.admin.use** ou status de OP para acessar comandos de admin.

## Requisitos

- **Com um plugin de permissões**: Conceda `hyperfactions.admin.use`
- **Sem um plugin de permissões**: O jogador deve ser um
operador do servidor (`adminRequiresOp=true` por padrão)

## Primeiros Passos Após a Instalação

1. Execute `/f admin` para verificar seu acesso
2. Abra **Config** para revisar as configurações padrão de facção
3. Crie uma **SafeZone** no spawn com `/f admin safezone Spawn`
4. Opcionalmente crie **WarZones** para arenas de PvP
5. Revise as configurações de **Backup** para garantir a segurança dos dados

## Capacidades de Admin

| Área | O Que Você Pode Fazer |
|------|-----------------------|
| Facções | Inspecionar, modificar ou dissolver forçadamente qualquer facção |
| Zonas | Criar SafeZones e WarZones com flags personalizadas |
| Poder | Sobrescrever valores de poder de jogador/facção |
| Economia | Gerenciar tesouros de facção e manutenção |
| Config | Editar configurações ao vivo pela GUI ou recarregar do disco |
| Backups | Criar, restaurar e gerenciar backups de dados |
| Importações | Migrar dados de outros plugins de facção |

>[!TIP] Use `/f admin --text` para obter saída baseada em chat ao invés da GUI, útil para console ou automação.
