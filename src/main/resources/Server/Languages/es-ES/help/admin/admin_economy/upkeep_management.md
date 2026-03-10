---
id: admin_upkeep_management
---
# Gestion de Mantenimiento

El mantenimiento de faccion cobra a las facciones
periodicamente basandose en su territorio y cantidad
de miembros.

## Controles del Administrador

Los ajustes de mantenimiento se gestionan a traves del
archivo de configuracion de economia o la GUI de
configuracion del administrador.

`/f admin config`
Abre el editor de configuracion y navega a los ajustes
de economia para modificar valores de mantenimiento.

## Ajustes Predeterminados de Mantenimiento

| Ajuste | Predeterminado | Descripcion |
|---------|---------|-------------|
| Mantenimiento habilitado | false | Interruptor principal del sistema |
| Intervalo de mantenimiento | 24h | Frecuencia de cobro del mantenimiento |
| Costo por reclamacion | 5.0 | Costo por chunk reclamado por ciclo |
| Costo por miembro | 0.0 | Costo por miembro por ciclo |
| Periodo de gracia | 72h | Las facciones nuevas estan exentas |
| Disolver por bancarrota | false | Disolucion automatica si no puede pagar |

## Monitorear el Mantenimiento

Usa `/f admin info <faction>` para ver:
- Saldo actual de tesoreria
- Costo estimado de mantenimiento por ciclo
- Tiempo hasta el proximo cobro de mantenimiento
- Si la faccion puede cubrir el mantenimiento

>[!TIP] Revisa las estadisticas de economia de todas las facciones desde el panel de administracion para identificar facciones en riesgo de bancarrota antes de que se active el mantenimiento.

>[!INFO] La configuracion de mantenimiento se almacena en `economy.json`. Los cambios realizados a traves de la GUI de configuracion toman efecto despues de recargar con `/f admin reload`.

## Formula de Mantenimiento

**Mantenimiento total** = (chunks reclamados x costo por reclamacion) +
(cantidad de miembros x costo por miembro)

>[!WARNING] Habilitar el mantenimiento en un servidor con facciones existentes puede causar bancarrotas inesperadas. Considera establecer un periodo de gracia o anunciar el cambio con anticipacion.
