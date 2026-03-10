---
id: admin_imports
---
# Importacion de Datos

Importa datos de facciones desde otros plugins para migrar tu servidor a HyperFactions.

## Comando de Importacion

`/f admin import <source> [path] [flags]`

**Permiso**: `hyperfactions.admin.use`

## Fuentes Soportadas

| Fuente | Descripcion |
|--------|-------------|
| `elbaphfactions` | Importar desde datos de ElbaphFactions |
| `hyfactions` | Importar desde datos de HyFactions v1 |

## Indicadores de Importacion

| Indicador | Descripcion |
|------|-------------|
| `--dry-run` | Validar datos sin importar nada |
| `--overwrite` | Sobrescribir facciones existentes con el mismo nombre |
| `--no-zones` | Omitir datos de zonas durante la importacion |
| `--no-power` | Omitir datos de poder durante la importacion |

>[!TIP] Siempre ejecuta con `--dry-run` primero para previsualizar lo que sera importado y detectar cualquier problema de datos antes de confirmar los cambios.

## Proceso de Importacion

1. Se crea una copia de seguridad previa automaticamente
2. Se cargan las asignaciones de nombres de jugadores
3. Se convierten facciones, reclamaciones y zonas
4. Los datos son validados y guardados

## Ejemplos

- `/f admin import elbaphfactions --dry-run`
- `/f admin import elbaphfactions --overwrite`
- `/f admin import hyfactions --no-zones --no-power`
- `/f admin import elbaphfactions /custom/path`

>[!WARNING] Usar `--overwrite` **reemplazara** cualquier faccion existente que comparta nombre con una faccion importada. Los datos de miembros y reclamaciones seran sobrescritos. Ejecuta con `--dry-run` primero para identificar conflictos.

>[!NOTE] Algunos datos especificos de la fuente (ej., parcelas de trabajadores, parcelas de granja) no tienen equivalente en HyperFactions y se registraran como advertencias durante la importacion.
