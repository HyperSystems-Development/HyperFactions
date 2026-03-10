---
id: admin_getting_started
---
# Primeros Pasos como Administrador

Bienvenido a la administracion de HyperFactions. Esta
guia cubre tus primeros pasos despues de instalar el plugin.

## Abrir el Panel de Administracion

`/f admin`
Abre la interfaz del panel de administracion con acceso
a todas las herramientas de gestion, editores de zonas
y configuracion del servidor.

>[!INFO] Necesitas el permiso **hyperfactions.admin.use** o estado de OP para acceder a los comandos de administracion.

## Requisitos

- **Con un plugin de permisos**: Otorga `hyperfactions.admin.use`
- **Sin un plugin de permisos**: El jugador debe ser un
  operador del servidor (`adminRequiresOp=true` por defecto)

## Primeros Pasos Tras la Instalacion

1. Ejecuta `/f admin` para verificar tu acceso
2. Abre **Configuracion** para revisar los ajustes predeterminados de facciones
3. Crea una **Zona Segura** en el spawn con `/f admin safezone Spawn`
4. Opcionalmente crea **Zonas de Guerra** para arenas PvP
5. Revisa los ajustes de **Copia de seguridad** para asegurar la proteccion de datos

## Capacidades del Administrador

| Area | Lo Que Puedes Hacer |
|------|----------------|
| Facciones | Inspeccionar, modificar o disolver cualquier faccion |
| Zonas | Crear Zonas Seguras y Zonas de Guerra con indicadores personalizados |
| Poder | Sobrescribir valores de poder de jugadores/facciones |
| Economia | Gestionar tesorerias de facciones y mantenimiento |
| Configuracion | Editar ajustes en vivo via GUI o recargar desde disco |
| Copias de seguridad | Crear, restaurar y gestionar copias de seguridad de datos |
| Importaciones | Migrar datos desde otros plugins de facciones |

>[!TIP] Usa `/f admin --text` para obtener salida por chat en lugar de la GUI, util para consola o automatizacion.
