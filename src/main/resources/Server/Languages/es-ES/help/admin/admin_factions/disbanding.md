---
id: admin_disbanding
---
# Disolucion Forzada

Los administradores pueden disolver cualquier faccion por la fuerza, sin importar los deseos del lider.

## Comando

`/f admin disband <faction>`
Disuelve la faccion indicada por la fuerza. Aparecera un mensaje de confirmacion antes de ejecutar la accion.

**Permiso**: `hyperfactions.admin.disband`

>[!WARNING] Disolver una faccion es **irreversible**. Todas las reclamaciones son liberadas, todos los miembros son removidos y la faccion deja de existir. Crea una copia de seguridad primero.

## Consecuencias

Cuando una faccion es disuelta:

| Efecto | Descripcion |
|--------|-------------|
| **Reclamaciones** | Todo el territorio es liberado inmediatamente |
| **Miembros** | Todos los jugadores son removidos de la lista |
| **Relaciones** | Todas las alianzas y enemistades son eliminadas |
| **Tesoreria** | Gestionada segun la configuracion de economia |
| **Hogar** | El hogar de la faccion es eliminado |
| **Chat** | El historial del chat de faccion es removido |

## Buenas Practicas

1. Siempre ejecuta `/f admin backup create` antes de disolver
2. Notifica a los miembros de la faccion cuando sea posible
3. Documenta la razon para los registros del servidor
4. Revisa `/f admin info <faction>` antes de actuar

>[!TIP] Si el problema es con un miembro especifico, considera usar el panel de administracion de facciones para transferir el liderazgo en lugar de disolver toda la faccion.
