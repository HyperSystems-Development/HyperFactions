---
id: admin_zone_commands
---
# Referencia de Comandos de Zonas

Referencia completa de todos los comandos de gestion
de zonas. Todos requieren el permiso `hyperfactions.admin.zones`.

## Creacion Rapida

| Comando | Descripcion |
|---------|-------------|
| `/f admin safezone <name>` | Crear una Zona Segura en el chunk actual |
| `/f admin warzone <name>` | Crear una Zona de Guerra en el chunk actual |
| `/f admin removezone <name>` | Eliminar una zona y liberar chunks |

## Gestion de Zonas

| Comando | Descripcion |
|---------|-------------|
| `/f admin zone create <name> <type>` | Crear una zona (safezone/warzone) |
| `/f admin zone delete <name>` | Eliminar una zona |
| `/f admin zone claim <zone>` | Agregar chunk actual a la zona |
| `/f admin zone unclaim <zone>` | Remover chunk actual de la zona |
| `/f admin zone radius <zone> <r>` | Reclamar radio cuadrado de chunks |
| `/f admin zone list` | Listar todas las zonas con cantidad de chunks |
| `/f admin zone notify <zone> <true/false>` | Alternar mensajes de entrada/salida |
| `/f admin zone title <zone> upper/lower <text>` | Establecer texto del titulo de zona |
| `/f admin zone properties <zone>` | Abrir la GUI de propiedades de zona |

## Gestion de Indicadores

| Comando | Descripcion |
|---------|-------------|
| `/f admin zoneflag <zone> <flag> <true/false>` | Establecer un indicador especifico |

>[!TIP] Usa la **GUI de propiedades** de zona para un editor visual con interruptores para cada indicador, organizados por categoria.

## Ejemplos

- `/f admin safezone Spawn` -- crear proteccion de spawn
- `/f admin zone radius Spawn 3` -- expandir a 7x7 chunks
- `/f admin zoneflag Spawn door_use true` -- permitir puertas
- `/f admin zone notify Spawn true` -- mostrar mensajes de entrada
