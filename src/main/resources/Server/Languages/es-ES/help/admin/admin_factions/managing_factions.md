---
id: admin_managing_factions
---
# Gestion de Facciones

Los administradores pueden inspeccionar y modificar cualquier faccion del servidor a traves del panel o comandos.

## Explorar Facciones

`/f admin factions`
Abre el explorador de facciones del administrador. Ve todas las facciones con cantidad de miembros, niveles de poder y territorio.

`/f admin info <faction>`
Abre el panel de informacion del administrador para una faccion especifica con detalles completos y opciones de gestion.

## Modificar Configuracion de Facciones

Con el permiso `hyperfactions.admin.modify`, puedes:

- **Renombrar** una faccion para resolver conflictos
- **Cambiar color** para corregir problemas de visualizacion
- **Alternar abierta/cerrada** para sobrescribir la politica de ingreso
- **Editar descripcion** con fines de moderacion

>[!TIP] Usa `/f admin who <player>` para buscar a que faccion pertenece un jugador especifico y ver sus detalles.

## Ver Miembros y Relaciones

El panel de informacion del administrador muestra:

| Seccion | Detalles |
|---------|---------|
| **Miembros** | Lista completa con roles y ultima conexion |
| **Relaciones** | Todas las posiciones de aliados, enemigos y neutrales |
| **Territorio** | Chunks reclamados y balance de poder |
| **Economia** | Saldo de tesoreria y registro de transacciones |

>[!NOTE] Los comandos de inspeccion del administrador no notifican a la faccion que esta siendo revisada. Solo las modificaciones activan alertas.
