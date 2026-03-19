---
id: admin_backups
---
# Sistema de Copias de Seguridad

HyperFactions incluye copias de seguridad automaticas y manuales con rotacion GFS (Abuelo-Padre-Hijo).

## Comandos de Copias de Seguridad

| Comando | Descripcion |
|---------|-------------|
| `/f admin backup create` | Crear una copia de seguridad manual ahora |
| `/f admin backup list` | Listar todas las copias de seguridad disponibles |
| `/f admin backup restore <name>` | Restaurar desde una copia de seguridad |
| `/f admin backup delete <name>` | Eliminar una copia de seguridad especifica |

**Permiso**: `hyperfactions.admin.backup`

## Valores Predeterminados de Rotacion GFS

| Tipo | Retencion | Descripcion |
|------|-----------|-------------|
| Cada hora | 24 | Ultimas 24 capturas por hora |
| Diaria | 7 | Ultimas 7 capturas diarias |
| Semanal | 4 | Ultimas 4 capturas semanales |
| Manual | 10 | Copias creadas manualmente |
| Apagado | 5 | Creadas al detener el servidor |

>[!INFO] Las copias de seguridad al apagar estan habilitadas por defecto (`onShutdown=true`). Capturan el estado mas reciente antes de que el servidor se detenga.

## Contenido de las Copias de Seguridad

Cada archivo ZIP de copia de seguridad contiene:
- Todos los archivos de datos de facciones
- Datos de poder de jugadores
- Definiciones de zonas
- Historial de chat y datos de economia
- Datos de invitaciones y solicitudes de ingreso
- Archivos de configuracion

>[!WARNING] **Restaurar una copia de seguridad es destructivo.** Reemplaza todos los datos actuales con el contenido de la copia de seguridad. Cualquier cambio realizado despues de que la copia fue creada se perdera. Siempre crea una copia de seguridad nueva antes de restaurar.

## Buenas Practicas

1. Crea una copia de seguridad manual antes de acciones importantes del administrador
2. Revisa la retencion de copias de seguridad en `backup.json`
3. Prueba la restauracion en un servidor de pruebas primero
4. Mantiene habilitadas las copias al apagar para recuperacion tras fallos
