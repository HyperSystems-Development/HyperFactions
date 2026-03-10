---
id: admin_power_commands
---
# Comandos de Administracion de Poder

Sobrescribir valores de poder de jugadores y facciones. Todos los comandos requieren el permiso `hyperfactions.admin.power`.

## Comandos de Poder de Jugador

| Comando | Descripcion |
|---------|-------------|
| `/f admin power set <player> <amount>` | Establecer valor exacto de poder |
| `/f admin power add <player> <amount>` | Agregar poder al jugador |
| `/f admin power remove <player> <amount>` | Remover poder del jugador |
| `/f admin power reset <player>` | Restablecer al poder inicial predeterminado |
| `/f admin power info <player>` | Ver desglose detallado de poder |

## Como Afecta el Poder a las Facciones

El poder total de una faccion es la suma del poder individual de todos sus miembros. Las reclamaciones de territorio requieren poder total suficiente para mantenerse.

| Escenario | Efecto |
|----------|--------|
| Poder aumentado | La faccion puede reclamar mas territorio |
| Poder reducido | La faccion puede volverse vulnerable a sobre-reclamacion |
| Poder restablecido | Regresa al jugador al valor inicial predeterminado |

>[!WARNING] Reducir el poder de un jugador puede causar que su faccion pierda territorio si el poder total cae por debajo del numero de chunks reclamados.

## Ejemplos

- `/f admin power set Steve 50` -- establecer exactamente en 50
- `/f admin power add Steve 10` -- aumentar en 10
- `/f admin power remove Steve 5` -- reducir en 5
- `/f admin power reset Steve` -- volver al predeterminado
- `/f admin power info Steve` -- mostrar desglose completo

>[!TIP] Usa `/f admin power info <player>` para ver el poder actual, poder maximo y cualquier sobrescritura activa antes de hacer cambios.
