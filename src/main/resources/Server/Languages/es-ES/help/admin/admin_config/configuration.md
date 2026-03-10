---
id: admin_configuration
---
# Sistema de Configuracion

HyperFactions usa un sistema de configuracion modular
en JSON con 11 archivos de configuracion.

## Comandos de Configuracion del Administrador

| Comando | Descripcion |
|---------|-------------|
| `/f admin config` | Abrir la GUI del editor visual de configuracion |
| `/f admin reload` | Recargar todos los archivos de configuracion desde disco |
| `/f admin sync` | Sincronizar datos de facciones al almacenamiento |

## Archivos de Configuracion

| Archivo | Contenido |
|------|----------|
| `factions.json` | Roles, poder, reclamaciones, combate, relaciones |
| `server.json` | Teletransporte, auto-guardado, mensajes, GUI, permisos |
| `economy.json` | Tesoreria, mantenimiento, ajustes de transacciones |
| `backup.json` | Rotacion y retencion de copias de seguridad |
| `chat.json` | Formato de chat de faccion y aliados |
| `debug.json` | Categorias de registro de depuracion |
| `faction-permissions.json` | Permisos predeterminados por rol |
| `announcements.json` | Difusion de eventos y notificaciones de territorio |
| `gravestones.json` | Ajustes de integracion de lapidas |
| `worldmap.json` | Modos de actualizacion del mapa del mundo |
| `worlds.json` | Sobrescrituras de comportamiento por mundo |

>[!TIP] La GUI de configuracion proporciona un editor visual con descripciones para cada ajuste. Los cambios se guardan inmediatamente pero algunos requieren `/f admin reload` para tomar efecto completo.

## Ubicacion de Configuracion

Todos los archivos se almacenan en:
`mods/com.hyperfactions_HyperFactions/config/`

>[!WARNING] Las ediciones manuales de JSON requieren `/f admin reload` para aplicarse. Un JSON invalido causara que el archivo sea omitido con una advertencia en el registro del servidor.

>[!NOTE] La version de configuracion se rastrea en `server.json`. El plugin auto-migra configuraciones anteriores al iniciar.
