---
id: admin_zone_basics
---
# Conceptos Basicos de Zonas

Las zonas son territorios controlados por el administrador con reglas personalizadas que anulan la proteccion normal de facciones.

## Tipos de Zonas

- **Zona Segura** -- Sin PvP, sin construccion, sin dano.
Ideal para areas de spawn y centros de comercio.
- **Zona de Guerra** -- PvP siempre habilitado, sin construccion.
Ideal para arenas y areas de batalla disputadas.

## Crear Zonas

`/f admin safezone <name>`
Crea una Zona Segura y reclama tu chunk actual.

`/f admin warzone <name>`
Crea una Zona de Guerra y reclama tu chunk actual.

Despues de la creacion, colocate en chunks adicionales y usa `/f admin zone claim <zone>` para expandir la zona.

## Gestionar Chunks de Zonas

`/f admin zone claim <zone>`
Agrega el chunk actual a la zona indicada.

`/f admin zone unclaim <zone>`
Remueve el chunk actual de la zona indicada.

`/f admin zone radius <zone> <radius>`
Reclama un cuadrado de chunks alrededor de tu posicion.

## Eliminar Zonas

`/f admin removezone <name>`
Elimina permanentemente la zona y libera todos sus chunks reclamados.

>[!WARNING] Eliminar una zona libera todos sus chunks instantaneamente. Esto no se puede deshacer sin una restauracion de copia de seguridad.

>[!INFO] Las reglas de zona **siempre anulan** las reglas de territorio de faccion. Una Zona Segura dentro de territorio enemigo sigue siendo segura.
