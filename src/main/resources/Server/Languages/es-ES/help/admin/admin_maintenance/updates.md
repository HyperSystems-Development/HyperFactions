---
id: admin_updates
---
# Verificacion de Actualizaciones

HyperFactions puede verificar nuevas versiones y gestionar la dependencia HyperProtect-Mixin.

## Comandos de Actualizacion

| Comando | Descripcion |
|---------|-------------|
| `/f admin update` | Verificar actualizaciones de HyperFactions |
| `/f admin update mixin` | Verificar/descargar HyperProtect-Mixin |
| `/f admin update toggle-mixin-download` | Alternar descarga automatica |
| `/f admin version` | Mostrar version actual e informacion de compilacion |

## Canales de Lanzamiento

| Canal | Descripcion |
|---------|-------------|
| **Estable** | Recomendado para servidores de produccion |
| **Pre-lanzamiento** | Acceso anticipado a funciones proximas |

>[!INFO] El verificador de actualizaciones solo notifica sobre nuevas versiones. **No** instala automaticamente actualizaciones de HyperFactions.

## HyperProtect-Mixin

HyperProtect-Mixin es el mixin de proteccion recomendado que habilita indicadores de zona avanzados (explosiones, propagacion de fuego, conservar inventario, etc.).

- `/f admin update mixin` verifica la ultima version
y la descarga si hay una version mas nueva disponible
- La descarga automatica puede alternarse por servidor

>[!TIP] Despues de descargar una nueva version del mixin, se requiere reiniciar el servidor para que los cambios tomen efecto.

## Procedimiento de Reversion

Si una actualizacion causa problemas:

1. Detiene el servidor
2. Reemplaza el JAR del plugin con la version anterior
3. Inicia el servidor
4. Verifica la funcionalidad con `/f admin version`

>[!WARNING] Revertir a una version anterior puede requerir un reinicio de migracion de configuracion. Siempre conserva copias de seguridad antes de actualizar.
