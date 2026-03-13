---
id: admin_permissions
---
# Permisos de Administracion

Todas las funciones de administracion estan protegidas por nodos de permisos en el espacio `hyperfactions.admin`.

## Nodos de Permisos

| Permiso | Descripcion |
|-----------|-------------|
| `hyperfactions.admin.*` | Otorga **todos** los permisos de administracion |
| `hyperfactions.admin.use` | Acceso al panel `/f admin` |
| `hyperfactions.admin.reload` | Recargar archivos de configuracion |
| `hyperfactions.admin.debug` | Alternar categorias de registro de depuracion |
| `hyperfactions.admin.zones` | Crear, editar y eliminar zonas |
| `hyperfactions.admin.disband` | Disolver cualquier faccion por la fuerza |
| `hyperfactions.admin.modify` | Modificar los ajustes de cualquier faccion |
| `hyperfactions.admin.bypass.limits` | Ignorar limites de reclamacion y poder |
| `hyperfactions.admin.backup` | Crear y restaurar copias de seguridad |
| `hyperfactions.admin.power` | Sobrescribir valores de poder de jugadores |
| `hyperfactions.admin.economy` | Gestionar tesorerias de facciones |

## Comportamiento Alternativo

Cuando **no hay un plugin de permisos** instalado, los permisos de administracion recurren al estado de operador del servidor (OP). Esto se controla mediante `adminRequiresOp` en la configuracion del servidor (por defecto: `true`).

>[!NOTE] El comodin `hyperfactions.admin.*` otorga todos los permisos de administracion. Usa nodos individuales para un control granular sobre tu equipo de staff.

## Orden de Resolucion de Permisos

1. Proveedor **VaultUnlocked** (si esta disponible)
2. Proveedor **HyperPerms** (si esta disponible)
3. Proveedor **LuckPerms** (si esta disponible)
4. **Verificacion de OP** para nodos de administracion (alternativa)

>[!WARNING] Sin un plugin de permisos y con `adminRequiresOp` deshabilitado, los comandos de administracion estan **abiertos a todos los jugadores**. Siempre usa un plugin de permisos en produccion.
