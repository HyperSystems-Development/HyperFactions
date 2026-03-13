---
id: admin_configuration
---
# Sistema de Configuração

HyperFactions usa um sistema de configuração modular em JSON com 11 arquivos de configuração.

## Comandos de Configuração Admin

| Comando | Descrição |
|---------|-----------|
| `/f admin config` | Abrir a GUI do editor visual de configuração |
| `/f admin reload` | Recarregar todos os arquivos de configuração do disco |
| `/f admin sync` | Sincronizar dados de facção com o armazenamento |

## Arquivos de Configuração

| Arquivo | Conteúdo |
|---------|----------|
| `factions.json` | Cargos, poder, reivindicações, combate, relações |
| `server.json` | Teleporte, salvamento automático, mensagens, GUI, permissões |
| `economy.json` | Tesouro, manutenção, configurações de transação |
| `backup.json` | Rotação e retenção de backups |
| `chat.json` | Formatação de chat de facção e aliados |
| `debug.json` | Categorias de log de debug |
| `faction-permissions.json` | Padrões de permissão por cargo |
| `announcements.json` | Transmissões de eventos e notificações de território |
| `gravestones.json` | Configurações de integração com lápides |
| `worldmap.json` | Modos de atualização do mapa do mundo |
| `worlds.json` | Sobrescritas de comportamento por mundo |

>[!TIP] A GUI de configuração fornece um editor visual com descrições para cada configuração. Alterações são salvas imediatamente, mas algumas requerem `/f admin reload` para entrar em pleno efeito.

## Localização das Configurações

Todos os arquivos são armazenados em:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Edições manuais em JSON requerem `/f admin reload` para serem aplicadas. JSON inválido fará com que o arquivo seja ignorado com um aviso no log do servidor.

>[!NOTE] A versão da configuração é rastreada em `server.json`. O plugin migra automaticamente configurações antigas na inicialização.
