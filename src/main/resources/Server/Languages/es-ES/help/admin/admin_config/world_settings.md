---
id: admin_world_settings
---
# Ajustes por Mundo

HyperFactions soporta configuracion por mundo para
reclamaciones, PvP y comportamiento de proteccion.

## Comandos de Mundo

| Comando | Descripcion |
|---------|-------------|
| `/f admin world list` | Listar todas las sobrescrituras de mundo |
| `/f admin world info <world>` | Mostrar ajustes de un mundo |
| `/f admin world set <world> <key> <value>` | Establecer un ajuste |
| `/f admin world reset <world>` | Restablecer mundo a valores predeterminados |

## Ajustes Disponibles

| Ajuste | Tipo | Descripcion |
|---------|------|-------------|
| claiming_enabled | boolean | Permitir reclamaciones de faccion en este mundo |
| pvp_enabled | boolean | Permitir combate PvP en este mundo |
| power_loss | boolean | Aplicar perdida de poder al morir |
| build_protection | boolean | Aplicar proteccion de construccion en reclamaciones |
| explosion_protection | boolean | Proteger reclamaciones de explosiones |

## Lista Blanca / Lista Negra de Mundos

Controla que mundos permiten funciones de facciones
a traves del archivo de configuracion `worlds.json`:

- **Modo lista blanca**: Solo los mundos listados permiten reclamar
- **Modo lista negra**: Todos los mundos permiten reclamar excepto los listados

>[!INFO] Los ajustes de mundo se almacenan en `worlds.json` y sobrescriben los valores globales predeterminados de `factions.json`.

## Ejemplos

- `/f admin world set survival claiming_enabled true`
- `/f admin world set creative claiming_enabled false`
- `/f admin world set pvp_arena pvp_enabled true`
- `/f admin world reset lobby` -- restaurar todos los valores predeterminados

>[!TIP] Deshabilita las reclamaciones en mundos creativos o de lobby para mantener el sistema de facciones enfocado en la jugabilidad de supervivencia.

>[!NOTE] Los ajustes por mundo tienen prioridad sobre la configuracion global pero son sobrescritos por los indicadores de zona dentro de ese mundo.
