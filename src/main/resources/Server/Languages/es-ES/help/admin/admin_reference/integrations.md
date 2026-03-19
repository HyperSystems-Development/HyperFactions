---
id: admin_integrations
---
# Integraciones de Plugins

HyperFactions se integra con varios plugins externos a traves de dependencias suaves. Todas las integraciones son opcionales y funcionan correctamente si no estan disponibles.

## Verificar Estado de Integraciones

`/f admin version`
Muestra la version actual y las integraciones detectadas.

`/f admin integration`
Abre el panel de gestion de integraciones con el estado detallado de cada plugin detectado.

## Tabla de Integraciones

| Plugin | Tipo | Descripcion |
|--------|------|-------------|
| **HyperPerms** | Permisos | Sistema completo de permisos con grupos, herencia y contexto |
| **LuckPerms** | Permisos | Proveedor alternativo de permisos |
| **VaultUnlocked** | Permisos/Economia | Puente de permisos y economia |
| **HyperProtect-Mixin** | Proteccion | Habilita indicadores de zona avanzados (explosiones, fuego, conservar inventario) |
| **OrbisGuard-Mixins** | Proteccion | Mixin alternativo para aplicacion de indicadores de zona |
| **PlaceholderAPI** | Marcadores | 49 marcadores de faccion para otros plugins |
| **WiFlow PlaceholderAPI** | Marcadores | Proveedor alternativo de marcadores |
| **GravestonePlugin** | Muerte | Control de acceso a lapidas en zonas |
| **HyperEssentials** | Funciones | Indicadores de zona para hogares, warps y kits |
| **KyuubiSoft Core** | Framework | Integracion de libreria base |
| **Sentry** | Monitoreo | Rastreo de errores y diagnosticos |

## Prioridad de Proveedor de Permisos

1. **VaultUnlocked** (mayor prioridad)
2. **HyperPerms**
3. **LuckPerms**
4. **Alternativa de OP** (si no se encuentra proveedor)

>[!INFO] Las integraciones se detectan una vez al iniciar usando reflexion. Los resultados se almacenan en cache para la sesion. Se requiere reiniciar el servidor despues de agregar o remover un plugin integrado.

>[!TIP] Usa `/f admin debug toggle integration` para habilitar el registro detallado de integraciones para solucion de problemas.

>[!NOTE] HyperProtect-Mixin es el mixin de proteccion **recomendado**. Sin el, 15 indicadores de zona no tendran efecto.
