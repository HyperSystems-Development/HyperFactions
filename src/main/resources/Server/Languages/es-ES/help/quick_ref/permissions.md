---
id: quickref_permissions
---
# Permisos

Nodos de permisos clave para HyperFactions. Todos los nodos estan bajo el espacio de nombres raiz **hyperfactions**.

## Permisos Principales

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.use | Acceso a comandos basicos de faccion |
| hyperfactions.faction.create | Crear una nueva faccion |
| hyperfactions.faction.disband | Disolver tu faccion |

## Membresia

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.member.invite | Invitar jugadores |
| hyperfactions.member.kick | Expulsar miembros |
| hyperfactions.member.promote | Promover miembros |

## Territorio

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.territory.claim | Reclamar chunks |
| hyperfactions.territory.unclaim | Liberar chunks |
| hyperfactions.territory.overclaim | Sobrereclamar territorio debilitado |

## Teletransporte

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.teleport.home | Usar hogar de faccion |
| hyperfactions.teleport.sethome | Establecer hogar de faccion |
| hyperfactions.teleport.stuck | Usar teletransporte de emergencia |

## Diplomacia y Chat

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.relation.ally | Gestionar alianzas |
| hyperfactions.relation.enemy | Declarar enemigos |
| hyperfactions.chat.faction | Usar chat de faccion |
| hyperfactions.chat.ally | Usar chat de aliados |

## Informacion y Economia

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.info.show | Ver informacion de faccion |
| hyperfactions.info.list | Explorar facciones |
| hyperfactions.economy.deposit | Depositar en tesoreria |
| hyperfactions.economy.withdraw | Retirar de tesoreria |

## Permisos de Bypass

| Permiso | Descripcion |
|---------|-------------|
| hyperfactions.bypass.* | Saltar todas las restricciones |
| hyperfactions.bypass.combat | Saltar etiqueta de combate |
| hyperfactions.bypass.power | Saltar limites de poder |
| hyperfactions.bypass.territory | Saltar proteccion de territorio |

>[!INFO] Los administradores pueden otorgar hyperfactions.* para dar acceso a todos los permisos de una vez.

>[!NOTE] Algunos permisos estan restringidos por el rol de faccion independientemente de los nodos de permiso. Por ejemplo, solo los Oficiales pueden reclamar incluso teniendo el permiso.
