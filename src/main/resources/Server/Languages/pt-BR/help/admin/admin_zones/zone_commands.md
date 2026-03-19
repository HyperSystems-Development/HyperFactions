---
id: admin_zone_commands
---
# Referência de Comandos de Zona

Referência completa de todos os comandos de gerenciamento de zona. Todos requerem a permissão `hyperfactions.admin.zones`.

## Criação Rápida

| Comando | Descrição |
|---------|-----------|
| `/f admin safezone <name>` | Criar uma SafeZone no chunk atual |
| `/f admin warzone <name>` | Criar uma WarZone no chunk atual |
| `/f admin removezone <name>` | Excluir uma zona e liberar chunks |

## Gerenciamento de Zona

| Comando | Descrição |
|---------|-----------|
| `/f admin zone create <name> <type>` | Criar uma zona (safezone/warzone) |
| `/f admin zone delete <name>` | Excluir uma zona |
| `/f admin zone claim <zone>` | Adicionar chunk atual à zona |
| `/f admin zone unclaim <zone>` | Remover chunk atual da zona |
| `/f admin zone radius <zone> <r>` | Reivindicar raio quadrado de chunks |
| `/f admin zone list` | Listar todas as zonas com contagem de chunks |
| `/f admin zone notify <zone> <true/false>` | Alternar mensagens de entrada/saída |
| `/f admin zone title <zone> upper/lower <text>` | Definir texto do título da zona |
| `/f admin zone properties <zone>` | Abrir GUI de propriedades da zona |

## Gerenciamento de Flags

| Comando | Descrição |
|---------|-----------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Definir uma flag específica |

>[!TIP] Use a **GUI de propriedades** da zona para um editor visual com toggles para cada flag, organizados por categoria.

## Exemplos

- `/f admin safezone Spawn` -- criar proteção de spawn
- `/f admin zone radius Spawn 3` -- expandir para 7x7 chunks
- `/f admin zoneflag Spawn door_use true` -- permitir portas
- `/f admin zone notify Spawn true` -- mostrar mensagens de entrada
